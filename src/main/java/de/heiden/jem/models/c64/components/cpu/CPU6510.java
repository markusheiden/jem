package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.bytes.HexUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.InputOutputPortImpl;
import de.heiden.jem.components.ports.InputPort;
import de.heiden.jem.components.ports.InputPortImpl;
import de.heiden.jem.components.ports.OutputPortImpl;
import de.heiden.jem.models.c64.components.memory.Patchable;
import org.serialthreads.Interruptible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * CPU.
 */
public class CPU6510 implements ClockedComponent {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Enable debug features.
     */
    protected static final boolean DEBUG = false;

    /**
     * State.
     */
    protected final CPU6510State state;

    /**
     * Tick.
     */
    @SuppressWarnings("NullAway.Init")
    protected Tick tick;

    /**
     * Bus.
     */
    @SuppressWarnings("NullAway.Init")
    protected BusDevice bus;

    /**
     * Port at address $0000/$0001 controlled by this CPU.
     */
    private final InputOutputPortImpl portOut;

    /**
     * Input for port at address $0000/$0001 driven by surrounding hardware.
     */
    private final OutputPortImpl portIn;

    /**
     * Port for interrupt.
     */
    private final InputPort irq;
    private boolean irqState = false;

    /**
     * Port for NMI.
     */
    private final InputPort nmi;
    private boolean nmiState = false;

    /**
     * Interrupt lines as seen in the last cycle.
     */
    private boolean irqLine = false;
    private boolean nmiLine = false;

    /**
     * Interrupts polled in the last cycle: The interrupt lines as seen in the cycle before.
     * At the start of an instruction they are the interrupts polled in the last cycle of the previous instruction.
     */
    private boolean irqPolled = false;
    private boolean nmiPolled = false;

    /**
     * Port for ready, e.g. connected to BA of the VIC. Low halts the CPU at its next read cycle.
     */
    private final InputPort rdy;
    private boolean ready = true;

    /**
     * Has the CPU been halted by RDY at the last read cycle?
     */
    private boolean halted = false;

    /**
     * Byte of the last access to the bus.
     */
    private int busData = 0xFF;

    /**
     * Address -> Patch.
     */
    private final Map<Integer, Patch> patches = new HashMap<>();

    /**
     * Constructor.
     */
    public CPU6510() {
        state = new CPU6510State();

        // TODO mh: Connect to emulated hardware.
        portIn = new OutputPortImpl();
        portIn.setOutputMask(0xFF);
        // The unconnected bits 6 and 7 are low, until they are driven high as outputs.
        portIn.setOutputData(0x1F);
        portOut = new InputOutputPortImpl();
        portOut.connect(portIn);

        irq = new InputPortImpl();
        irq.addInputPortListener((value, mask) -> {
            // IRQ is low active.
            var irq = (value & 0x01) == 0;
            if (irq && !irqState) {
                state.triggerIRQ();
            } else if (!irq) {
                // Normally IRQ will be reset when it is about to be executed,
                // but if the IRQ is not being handled due to the I flag,
                // then it will be reset when the interrupt request is cleared.
                state.resetIRQ();
            }
            irqState = irq;
        });

        nmi = new InputPortImpl();
        nmi.addInputPortListener((value, mask) -> {
            // NMI is low active.
            // The falling edge is latched until the NMI is executed.
            var nmi = (value & 0x01) == 0;
            if (nmi && !nmiState) {
                state.NMI = true;
            }
            nmiState = nmi;
        });

        rdy = new InputPortImpl();
        rdy.addInputPortListener((value, mask) -> ready = (value & 0x01) != 0);

        logger.debug("start cpu");
    }

    @Override
    public void setTick(Tick tick) {
        this.tick = tick;
    }

    /**
     * Connect to bus.
     *
     * @param bus
     *         CPU bus.
     */
    public void connect(BusDevice bus) {
        this.bus = bus;
    }

    /**
     * Add a patch.
     *
     * @param patch
     *         Patch
     */
    public void add(Patch patch) {
        patches.put(patch.getAddress(), patch);
        writePort(0xFF, 0x0001); // Standard memory layout.
        patch.replaced = bus.read(patch.getAddress());
        ((Patchable) bus).patch(0x02, patch.getAddress()); // Add breakpoint.
    }

    /**
     * Reset CPU.
     * <p>
     * TODO Should be protected?
     */
    @Interruptible
    public void reset() {
        logger.debug("reset");

        // Wait for first tick.
        tick.waitForTick();

        // TODO Init something else?
        state.S = 0xFF;
        state.setP(0x00); // TODO Correct?
        state.PC = readAbsoluteAddress(0xFFFC);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * CPU port.
     */
    public InputOutputPort getPort() {
        return portOut;
    }

    /**
     * IRQ input signal.
     */
    public InputPort getIRQ() {
        return irq;
    }

    /**
     * NMI input signal.
     */
    public InputPort getNMI() {
        return nmi;
    }

    /**
     * RDY input signal.
     */
    public InputPort getRDY() {
        return rdy;
    }

    @Override
    @Interruptible
    public final void run() {
        reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            if (nmiPolled) {
                nmi();

            } else if (irqPolled) {
                irq();

            } else {
                execute();
            }
        }
    }

    /**
     * Execute NMI.
     */
    @Interruptible
    private void nmi() {
        state.NMI = false;
        // Opcode fetch and operand read are executed, but discarded.
        idleRead();
        idleRead();
        interrupt(0xFFFA, state.getP() & ~CPU6510State.B_BIT);
    }

    /**
     * Execute IRQ.
     */
    @Interruptible
    private void irq() {
        // Opcode fetch and operand read are executed, but discarded.
        idleRead();
        idleRead();
        interrupt(0xFFFE, state.getP() & ~CPU6510State.B_BIT);
    }

    @Interruptible
    protected void execute() {
//      int b = readBytePC();
//      var opcode = OPCODES[b];
//      opcode.execute();
        OPCODES[readBytePC()].execute();
    }

    // <editor-fold desc="Opcodes">

    @SuppressWarnings({ "Convert2Lambda", "Anonymous2MethodRef" })
    private final Opcode[] OPCODES =
            {
                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $00: BRK (7)
                        {
                            readBytePC();
                            state.B = true;
                            interrupt(0xFFFE, state.getP());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $01: ORA ($XX,X) (6) // izx
                        {
                            or(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $02: *KIL (*) // TODO imm?
                        {
                            // Use opcode $02 as escape.
                            var patch = patches.get(state.PC - 1);
                            if (patch != null) {
                                var opcode = patch.execute(state, bus);
                                if (opcode != Patch.DO_NOT_EXECUTE) {
                                    OPCODES[opcode].execute();
                                }
                            } else {
                                // No patch -> standard behaviour: crash.
                                crash();
                            }
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $03: *SLO ($XX,X) (8) // izx
                        {
                            slo(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $04: *NOP $XX (3) // zp
                        {
                            read(readAbsoluteZeropageAddressPC());
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $05: ORA $XX (3) // zp
                        {
                            or(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $06: ASL $XX (5) // zp
                        {
                            var addr = readAbsoluteZeropageAddressPC();
                            write(shiftLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $07: *SLO $XX (5) // zp
                        {
                            slo(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $08: PHP (3) // no
                        {
                            idleRead();
                            pushByte(state.getP());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $09: ORA #$XX (2)
                        {
                            or(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $0A: ASL (2) // no
                        {
                            idleRead(); // During operation.
                            state.A = shiftLeft(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $0B: *ANC #$XX (2)
                        {
                            anc(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $0C: *NOP $XXXX (4) // abs
                        {
                            read(readAbsoluteAddressPC());
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $0D: ORA $XXXX (4)
                        {
                            or(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $0E: ASL $XXXX (6)
                        {
                            var addr = readAbsoluteAddressPC();
                            write(shiftLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $0F: *SLO $XXXX (6)
                        {
                            slo(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $10: BPL $XXXX (2/3) // rel
                        {
                            branchIf(!state.N);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $11: ORA ($XX),Y (5/6) // izy
                        {
                            or(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $12: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $13: *SLO ($XX),Y (8) // izy
                        {
                            // TODO 1 tick.
                            slo(readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $14: *NOP $XX,X (4) // zpx
                        {
                            read(readAbsoluteZeropageAddressPC(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $15: ORA $XX,X (4) // zpx
                        {
                            or(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $16: ASL $XX,X (6) // zpx
                        {
                            var addr = readAbsoluteZeropageAddressPC(state.X);
                            write(shiftLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $17: *SLO $XX,X (6) // zpx
                        {
                            slo(readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $18: CLC (2) // no
                        {
                            idleRead(); // During operation.
                            state.C = false;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $19: ORA $XXXX,Y (4/5)
                        {
                            or(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $1A: *NOP (2) // no
                        {
                            readImpliedPC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $1B: *SLO $XXXX,Y (7)
                        {
                            // TODO 1 tick.
                            slo(readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $1C: *NOP $XXXX,X (4/5) // abx
                        {
                            read(readAbsoluteAddressPCForRead(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $1D: ORA $XXXX,X (4/5)
                        {
                            or(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $1E: ASL $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            var addr = readAbsoluteAddressPCForWrite(state.X);
                            write(shiftLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $1F: *SLO $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            slo(readAbsoluteAddressPCForWrite(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $20: JSR $XXXX (6)
                        {
                            var low = readBytePC();
                            // Internal operation: Dummy read from the stack.
                            stackRead();
                            // The return address is the address of the high byte, it is read after the pushes.
                            pushWord(state.PC);
                            state.PC = low | read(state.PC) << 8;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $21: AND ($XX,X) (6) // izx
                        {
                            and(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $22: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $23: *RLA ($XX,X) (8) // izx
                        {
                            rla(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $24: BIT $XX (3) // zp
                        {
                            bit(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $25: AND $XX (3) // zp
                        {
                            and(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $26: ROL $XX (5) // zp
                        {
                            var addr = readAbsoluteZeropageAddressPC();
                            write(rotateLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $27: *RLA $XX (5) // zp
                        {
                            rla(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $28: PLP (4) // no
                        {
                            idleRead();
                            stackRead();
                            state.setP(popByte());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $29: AND #$XX (2)
                        {
                            and(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $2A: ROL (2) // no
                        {
                            idleRead(); // During operation.
                            state.A = rotateLeft(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $2B: *ANC #$XX (2)
                        {
                            anc(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $2C: BIT $XXXX (4)
                        {
                            bit(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $2D: AND $XXXX (4)
                        {
                            and(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $2E: ROL $XXXX (6)
                        {
                            var addr = readAbsoluteAddressPC();
                            write(rotateLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $2F: *RLA $XXXX (6)
                        {
                            rla(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $30: BMI $XXXX (2/3) // rel
                        {
                            branchIf(state.N);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $31: AND ($XX),Y (5/6) // izy
                        {
                            and(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $32: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $33: *RLA ($XX),Y (8) // izy
                        {
                            // TODO 1 tick.
                            rla(readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $34: *NOP $XX,X (4) // zpx
                        {
                            read(readAbsoluteZeropageAddressPC(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $35: AND $XX,X (4) // zpx
                        {
                            and(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $36: ROL $XX,X (6) // zpx
                        {
                            var addr = readAbsoluteZeropageAddressPC(state.X);
                            write(rotateLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $37: *RLA $XX,X (6) // zpx
                        {
                            rla(readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $38: SEC (2) // no
                        {
                            idleRead(); // During operation.
                            state.C = true;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $39: AND $XXXX,Y (4/5)
                        {
                            and(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $3A: *NOP (2) // no
                        {
                            readImpliedPC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $3B: *RLA $XXXX,Y (7)
                        {
                            // TODO 1 tick.
                            rla(readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $3C: *NOP $XXXX,X (4/5) // abx
                        {
                            read(readAbsoluteAddressPCForRead(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $3D: AND $XXXX,X (4/5) // abx
                        {
                            and(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $3E: ROL $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            var addr = readAbsoluteAddressPCForWrite(state.X);
                            write(rotateLeft(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $3F: *RLA $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            rla(readAbsoluteAddressPCForWrite(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $40: RTI (6)
                        {
                            idleRead();
                            stackRead();
                            state.setP(popByte());
                            state.PC = popWord();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $41: EOR ($XX,X) (6) // izx
                        {
                            xor(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $42: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $43: *LSE ($XX,X) // izx
                        {
                            lse(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $44: *NOP $XX (3) // zp
                        {
                            read(readAbsoluteZeropageAddressPC());
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $45: EOR $XX (3) // zp
                        {
                            xor(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $46: LSR $XX (5) // zp
                        {
                            var addr = readAbsoluteZeropageAddressPC();
                            write(shiftRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $47: *LSE $XX // zp
                        {
                            lse(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $48: PHA (3) // no
                        {
                            idleRead();
                            pushByte(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $49: EOR #$XX (2)
                        {
                            xor(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $4A: LSR (2) // no
                        {
                            idleRead(); // During operation.
                            state.A = shiftRight(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $4B: ALR #$XX (2)
                        {
                            alr(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $4C: JMP $XXXX (3) (AAY64)
                        {
                            state.PC = readAbsoluteAddressPC();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $4D: EOR $XXXX (4)
                        {
                            xor(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $4E: LSR $XXXX (6)
                        {
                            var addr = readAbsoluteAddressPC();
                            write(shiftRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $4F: *LSE $XXXX
                        {
                            lse(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $50: BVC $XXXX (2/3) // rel
                        {
                            branchIf(!state.V);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $51: EOR ($XX),Y (5/6) // izy
                        {
                            xor(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $52: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $53: *LSE ($XX),Y // izy
                        {
                            lse(readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $54: *NOP $XX,X (4) // zpx
                        {
                            read(readAbsoluteZeropageAddressPC(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $55: EOR $XX,X (4) // zpx
                        {
                            xor(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $56: LSR $XX,X (6) // zpx
                        {
                            var addr = readAbsoluteZeropageAddressPC(state.X);
                            write(shiftRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $57: *LSE $XX,X // zpx
                        {
                            lse(readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $58: CLI (2) // no
                        {
                            if (ready) {
                                idleRead(); // During operation.
                                state.cli();
                            } else {
                                // Halted: The flag is changed before the interrupts are polled again.
                                state.cli();
                                idleRead();
                            }
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $59: EOR $XXXX,Y (4/5)
                        {
                            xor(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $5A: *NOP (2) // no
                        {
                            readImpliedPC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $5B: *LSE $XXXX,Y
                        {
                            lse(readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $5C: *NOP $XXXX,X (4/5) // abx
                        {
                            read(readAbsoluteAddressPCForRead(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $5D: EOR $XXXX,X (4/5)
                        {
                            xor(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $5E: LSR $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            var addr = readAbsoluteAddressPCForWrite(state.X);
                            write(shiftRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $5F: *LSE $XXXX,X
                        {
                            lse(readAbsoluteAddressPCForWrite(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $60: RTS (6)
                        {
                            rts();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $61: ADC ($XX,X) (6) // izx
                        {
                            add(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $62: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $63: *RRA ($XX,X) // izx
                        {
                            rra(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $64: *NOP $XX (3) // zp
                        {
                            read(readAbsoluteZeropageAddressPC());
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $65: ADC $XX (3) // zp
                        {
                            add(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $66: ROR $XX (5) // zp
                        {
                            var addr = readAbsoluteZeropageAddressPC();
                            write(rotateRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $67: *RRA $XX // zp
                        {
                            rra(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $68: PLA (4) // no
                        {
                            idleRead();
                            stackRead();
                            var a = popByte();
                            state.setZeroNegativeP(a);
                            state.A = a;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $69: ADC #$XX (2)
                        {
                            add(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $6A: ROR (2) // no
                        {
                            idleRead(); // During operation.
                            state.A = rotateRight(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $6B: *ARR #$XX (2)
                        {
                            arr(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $6C: JMP ($XXXX) (5)
                        {
                            state.PC = readIndirectAddress();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $6D: ADC $XXXX (4)
                        {
                            add(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $6E: ROR $XXXX (6)
                        {
                            var addr = readAbsoluteAddressPC();
                            write(rotateRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $6F: *RRA $XXXX
                        {
                            rra(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $70: BVS $XXXX (2/3) // rel
                        {
                            branchIf(state.V);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $71: ADC ($XX),Y (5/6) // izy
                        {
                            add(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $72: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $73: *RRA ($XX),Y // izy
                        {
                            rra(readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $74: *NOP $XX,X (4) // zpx
                        {
                            read(readAbsoluteZeropageAddressPC(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $75: ADC $XX,X (4) // zpx
                        {
                            add(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $76: ROR $XX,X (6) // zpx
                        {
                            var addr = readAbsoluteZeropageAddressPC(state.X);
                            write(rotateRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $77: *RRA $XX,X // zpx
                        {
                            rra(readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $78: SEI (2) // no
                        {
                            // An IRQ raised while the CPU is halted at this cycle is masked already.
                            while (!ready) {
                                pollNmi();
                                tick.waitForTick();
                            }
                            idleRead(); // During operation.
                            state.sei();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $79: ADC $XXXX,Y (4/5)
                        {
                            add(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $7A: *NOP (2) // no
                        {
                            readImpliedPC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $7B: *RRA $XXXX,Y
                        {
                            rra(readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $7C: *NOP $XXXX,X (4/5) // abx
                        {
                            read(readAbsoluteAddressPCForRead(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $7D: ADC $XXXX,X (4/5)
                        {
                            add(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $7E: ROR $XXXX,X (7)
                        {
                            // TODO 1 ticks.
                            var addr = readAbsoluteAddressPCForWrite(state.X);
                            write(rotateRight(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $7F: *RRA $XXXX,X
                        {
                            rra(readAbsoluteAddressPCForWrite(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $80: *NOP #$XX (2) // imm
                        {
                            readImmediatePC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $81: STA ($XX,X) (6) // izx
                        {
                            write(state.A, readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $82: *NOP #$XX (2) // imm
                        {
                            readImmediatePC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $83: *AXS ($XX,X) // izx
                        {
                            axs(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $84: STY $XX (3) // zp
                        {
                            write(state.Y, readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $85: STA $XX (3) // zp
                        {
                            write(state.A, readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $86: STX $XX (3) // zp
                        {
                            write(state.X, readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $87: *AXS $XX // zp
                        {
                            axs(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $88: DEY (2) // no
                        {
                            idleRead(); // During operation.
                            state.Y = decrement(state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $89: *NOP (2) // imm
                        {
                            readImmediatePC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $8A: TXA (2) // no
                        {
                            idleRead();
                            state.A = load(state.X);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $8B: *XAA #$XX (?)
                        {
                            xaa(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $8C: STY $XXXX (4)
                        {
                            write(state.Y, readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $8D: STA $XXXX (4)
                        {
                            write(state.A, readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $8E: STX $XXXX (4)
                        {
                            write(state.X, readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $8F: *AXS $XXXX
                        {
                            axs(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $90: BCC $XXXX (2/3) // rel
                        {
                            branchIf(!state.C);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $91: STA ($XX),Y (6) // izy
                        {
                            write(state.A, readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $92: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $93: *AHX ($XX),Y
                        {
                            ahx(readAbsoluteAddressForIndirect(readAbsoluteZeropageAddressPC()), state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $94: STY $XX,X (4) // zpx
                        {
                            write(state.Y, readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $95: STA $XX,X (4) // zpx
                        {
                            write(state.A, readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $96: STX $XX,Y (4) // zpy
                        {
                            write(state.X, readAbsoluteZeropageAddressPC(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $97: *AXS $XX,Y // zpy
                        {
                            axs(readAbsoluteZeropageAddressPC(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $98: TYA (2) // no
                        {
                            idleRead();
                            state.A = load(state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $99: STA $XXXX,Y (5)
                        {
                            // TODO 1 tick.
                            write(state.A, readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $9A: TXS (2) // no
                        {
                            idleRead(); // During operation.
                            state.S = state.X; // No update of P !!!
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $9B: *TAS $XXXX,Y
                        {
                            tas(readAbsoluteAddressPC(), state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $9C: *SHY $XXXX,X
                        {
                            shy(readAbsoluteAddressPC(), state.X);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $9D: STA $XXXX,X (5)
                        {
                            write(state.A, readAbsoluteAddressPCForWrite(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $9E: *SHX $XXXX,Y
                        {
                            shx(readAbsoluteAddressPC(), state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $9F: *AHX $XXXX,Y
                        {
                            ahx(readAbsoluteAddressPC(), state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A0: LDY #$XX (2) // imm
                        {
                            state.Y = load(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A1: LDA ($XX,X) (6) // izx
                        {
                            state.A = load(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A2: LDX #$XX (2) // imm
                        {
                            state.X = load(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A3: *LAX ($XX,X) // izx
                        {
                            lax(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A4: LDY $XX (3) // zp
                        {
                            state.Y = load(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A5: LDA $XX (3) // zp
                        {
                            state.A = load(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A6: LDX $XX (3) // zp
                        {
                            state.X = load(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A7: *LAX $XX // zp
                        {
                            lax(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A8: TAY (2) // no
                        {
                            idleRead();
                            state.Y = load(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $A9: LDA #$XX (2)
                        {
                            state.A = load(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $AA: TAX (2) // no
                        {
                            idleRead();
                            state.X = load(state.A);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $AB: LXA #$XX (?)
                        {
                            lxa(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $AC: LDY $XXXX (4)
                        {
                            state.Y = load(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $AD: LDA $XXXX (4)
                        {
                            state.A = load(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $AE: LDX $XXXX (4)
                        {
                            state.X = load(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $AF: *LAX $XXXX
                        {
                            lax(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B0: BCS $XXXX (2/3) // rel
                        {
                            branchIf(state.C);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B1: LDA ($XX),Y (5/6) // izy
                        {
                            state.A = load(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B2: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B3: *LAX ($XX),Y (5/6) // izy
                        {
                            lax(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B4: LDY $XX,X (4) // zpx
                        {
                            state.Y = load(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B5: LDA $XX,X (4) // zpx
                        {
                            state.A = load(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B6: LDX $XX,Y (4) // zpy
                        {
                            state.X = load(read(readAbsoluteZeropageAddressPC(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B7: *LAX $XX,Y // zpy
                        {
                            lax(read(readAbsoluteZeropageAddressPC(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B8: CLV (2) // no
                        {
                            idleRead(); // During operation.
                            state.V = false;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $B9: LDA $XXXX,Y (4/5)
                        {
                            state.A = load(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $BA: TSX (2) // no
                        {
                            idleRead();
                            state.X = load(state.S);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $BB: *LAS $XXXX,Y (4/5)
                        {
                            las(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $BC: LDY $XXXX,X (4/5)
                        {
                            state.Y = load(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $BD: LDA $XXXX,X (4/5)
                        {
                            state.A = load(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $BE: LDX $XXXX,Y (4/5)
                        {
                            state.X = load(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $BF: *LAX $XXXX,Y (4/5)
                        {
                            lax(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C0: CPY #$XX (2) // imm
                        {
                            compare(state.Y, readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C1: CMP ($XX,X) (6) // izx
                        {
                            compare(state.A, read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C2: *NOP #$XX (2) // imm
                        {
                            readImmediatePC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C3: *DCM ($XX,X) // izx
                        {
                            dcm(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C4: CPY $XX (3) // zp
                        {
                            compare(state.Y, read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C5: CMP $XX (3) // zp
                        {
                            compare(state.A, read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C6: DEC $XX (5) // zp
                        {
                            var addr = readAbsoluteZeropageAddressPC();
                            write(decrement(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C7: *DCM $XX // zp
                        {
                            dcm(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C8: INY (2) // no
                        {
                            idleRead(); // During operation.
                            state.Y = increment(state.Y);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $C9: CMP #$XX (2)
                        {
                            compare(state.A, readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $CA: DEX (2) // no
                        {
                            idleRead(); // During operation.
                            state.X = decrement(state.X);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $CB: *SAX #$XX (?)
                        {
                            sax(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $CC: CPY $XXXX (4)
                        {
                            compare(state.Y, read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $CD: CMP $XXXX (4)
                        {
                            compare(state.A, read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $CE: DEC $XXXX (6)
                        {
                            var addr = readAbsoluteAddressPC();
                            write(decrement(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $CF: *DCM $XXXX
                        {
                            dcm(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D0: BNE $XXXX (2/3) // rel
                        {
                            branchIf(!state.Z);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D1: CMP ($XX),Y (5/6) // izy
                        {
                            compare(state.A, read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D2: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D3: *DCM ($XX),Y // izy
                        {
                            dcm(readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D4: *NOP $XX,X (4) // zpx
                        {
                            read(readAbsoluteZeropageAddressPC(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D5: CMP $XX,X (4) // zpx
                        {
                            compare(state.A, read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D6: DEC $XX,X (6) // zpx
                        {
                            var addr = readAbsoluteZeropageAddressPC(state.X);
                            write(decrement(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D7: *DCM $XX,X // zpx
                        {
                            dcm(readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D8: CLD (2) // no
                        {
                            idleRead(); // During operation.
                            state.D = false;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $D9: CMP $XXXX,Y (4/5)
                        {
                            compare(state.A, read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $DA: *NOP (2) // no
                        {
                            idleRead();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $DB: *DCM $XXXX,Y
                        {
                            dcm(readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $DC: *NOP $XXXX,X (4/5) // abx
                        {
                            read(readAbsoluteAddressPCForRead(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $DD: CMP $XXXX,X (4/5)
                        {
                            compare(state.A, read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $DE: DEC $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            var addr = readAbsoluteAddressPCForWrite(state.X);
                            write(decrement(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $DF: *DCM $XXXX,X
                        {
                            dcm(readAbsoluteAddressPCForWrite(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E0: CPX #$XX (2) // imm
                        {
                            compare(state.X, readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E1: SBC ($XX,X) (6) // izx
                        {
                            subtract(read(readZeropageIndirectXAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E2: *NOP #$XX (2) // TODO imm?
                        {
                            readImmediatePC();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E3: *INS ($XX,X) // izx
                        {
                            ins(readZeropageIndirectXAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E4: CPX $XX (3) // zp
                        {
                            compare(state.X, read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E5: SBC $XX (3) // zp
                        {
                            subtract(read(readAbsoluteZeropageAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E6: INC $XX (5) // zp
                        {
                            var addr = readAbsoluteZeropageAddressPC();
                            write(increment(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E7: *INS $XX // zp
                        {
                            ins(readAbsoluteZeropageAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E8: INX (2) // no
                        {
                            idleRead(); // During operation.
                            state.X = increment(state.X);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $E9: SBC #$XX (2)
                        {
                            subtract(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $EA: NOP (2) // no
                        {
                            idleRead(); // During operation.
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $EB: *SBC #$XX (2)
                        {
                            subtract(readImmediatePC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $EC: CPX $XXXX (4)
                        {
                            compare(state.X, read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $ED: SBC $XXXX (4)
                        {
                            subtract(read(readAbsoluteAddressPC()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $EE: INC $XXXX (6)
                        {
                            var addr = readAbsoluteAddressPC();
                            write(increment(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $EF: *INS $XXXX
                        {
                            ins(readAbsoluteAddressPC());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F0: BEQ $XXXX (2/3) // rel
                        {
                            branchIf(state.Z);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F1: SBC ($XX),Y (5/6) // izy
                        {
                            subtract(read(readZeropageIndirectYAddressPCForRead()));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F2: *KIL (*) // TODO imm?
                        {
                            crash();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F3: *INS ($XX),Y // izy
                        {
                            ins(readZeropageIndirectYAddressPCForWrite());
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F4: *NOP $XX,X (4) // zpx
                        {
                            read(readAbsoluteZeropageAddressPC(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F5: SBC $XX,X (4) // zpx
                        {
                            subtract(read(readAbsoluteZeropageAddressPC(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F6: INC $XX,X (6) // zpx
                        {
                            var addr = readAbsoluteZeropageAddressPC(state.X);
                            write(increment(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F7: *INS $XX,X // zpx
                        {
                            ins(readAbsoluteZeropageAddressPC(state.X));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F8: SED (2) // no
                        {
                            idleRead(); // During operation.
                            state.D = true;
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $F9: SBC $XXXX,Y (4/5)
                        {
                            subtract(read(readAbsoluteAddressPCForRead(state.Y)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $FA: *NOP (2) // no
                        {
                            idleRead();
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $FB: *INS $XXXX,Y
                        {
                            ins(readAbsoluteAddressPCForWrite(state.Y));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $FC: *NOP $XXXX,X (4/5) // abx
                        {
                            read(readAbsoluteAddressPCForRead(state.X));
                            nop();
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $FD: SBC $XXXX,X (4/5)
                        {
                            subtract(read(readAbsoluteAddressPCForRead(state.X)));
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $FE: INC $XXXX,X (7)
                        {
                            // TODO 1 tick.
                            var addr = readAbsoluteAddressPCForWrite(state.X);
                            write(increment(readForModify(addr)), addr);
                        }
                    },

                    new Opcode() {
                        @Override
                        @Interruptible
                        public void execute() // $FF: *INS $XXXX,X
                        {
                            ins(readAbsoluteAddressPCForWrite(state.X));
                        }
                    },
            };

    // </editor-fold>

    /**
     * Handling of not yet implemented opcodes.
     */
    protected final void notImplementedYet() {
        logger.debug("Not implemented yet");
//    if (DEBUG) {
        throw new UnsupportedOperationException();
//    }
    }

    //
    // Commands.
    //

    /**
     * Execute interrupt sequence (BRK, IRQ or NMI) after the first 2 cycles.
     * (5)
     *
     * @param vector
     *         Interrupt vector.
     * @param p
     *         Status register to push.
     */
    @Interruptible
    protected final void interrupt(int vector, int p) {
        pushWord(state.PC);
        if (nmiLine) {
            // A NMI detected until now hijacks the interrupt sequence.
            state.NMI = false;
            vector = 0xFFFA;
        }
        pushByte(p);
        state.sei();
        state.PC = readAbsoluteAddress(vector);
        // The first instruction of the interrupt handler is executed before the next interrupt.
        irqPolled = false;
        nmiPolled = false;
    }

    /**
     * Shift left.
     *
     * @param value
     *         Value.
     */
    protected final int shiftLeft(int value) {
        var result = value << 1;
        state.setCarryZeroNegativeP(result, (result & 0x100) != 0);
        return result & 0xFF;
    }

    /**
     * Shift right.
     * (1)
     *
     * @param value
     *         Value.
     */
    protected final int shiftRight(int value) {
        var result = value >> 1;
        state.setCarryZeroNegativeP(result, (value & 0x01) != 0);
        return result;
    }

    /**
     * Rotate left.
     *
     * @param value
     *         Value.
     */
    protected final int rotateLeft(int value) {
        var result = value << 1;
        if (state.C) {
            result |= 0x01;
        }
        state.setCarryZeroNegativeP(result, (result & 0x100) != 0);
        return result & 0xFF;
    }

    /**
     * Rotate right.
     * (1)
     *
     * @param value
     *         Value.
     */
    protected final int rotateRight(int value) {
        var result = value >> 1;
        if (state.C) {
            result |= 0x80;
        }
        state.setCarryZeroNegativeP(result, (value & 0x01) != 0);
        return result;
    }

    /**
     * And value with A temporary.
     *
     * @param value
     *         Value.
     */
    protected final void bit(int value) {
        state.setZeroOverflowNegativeP(value, (state.A & value) == 0);
    }

    /**
     * And value with A.
     *
     * @param value
     *         Value.
     */
    protected final void and(int value) {
        var a = state.A & value;
        state.setZeroNegativeP(a);
        state.A = a;
    }

    /**
     * Or value with A.
     *
     * @param value
     *         Value.
     */
    protected final void or(int value) {
        var a = state.A | value;
        state.setZeroNegativeP(a);
        state.A = a;
    }

    /**
     * Exclusive or value with A.
     *
     * @param value
     *         Value.
     */
    protected final void xor(int value) {
        var a = state.A ^ value;
        state.setZeroNegativeP(a);
        state.A = a;
    }

    /**
     * Add value to A.
     *
     * @param value
     *         Value.
     */
    protected final void add(int value) {
        if (state.D) {
            addDecimal(value);
        } else {
            addBinary(value);
        }
    }

    /**
     * Add value to A (binary mode).
     *
     * @param value
     *         Value.
     */
    private void addBinary(int value) {
        var a = state.A + value;
        if (state.C) {
            a++;
        }
        state.setCarryZeroOverflowNegativeP(state.A, value, a);
        state.A = a & 0xFF;
    }

    /**
     * Add value to A (decimal mode of the NMOS 6502).
     * Z is set like in binary mode, N and V from the result before the decimal correction of the high nibble.
     *
     * @param value
     *         Value.
     */
    private void addDecimal(int value) {
        var a = state.A;
        var c = state.C ? 1 : 0;
        var lo = (a & 0x0F) + (value & 0x0F) + c;
        if (lo > 0x09) {
            lo += 0x06;
        }
        var result = (lo & 0x0F) + (a & 0xF0) + (value & 0xF0) + (lo > 0x0F ? 0x10 : 0x00);

        state.Z = ((a + value + c) & 0xFF) == 0;
        state.N = (result & 0x80) != 0;
        state.V = ((a ^ result) & ~(a ^ value) & 0x80) != 0;
        if ((result & 0x1F0) > 0x90) {
            result += 0x60;
        }
        state.C = (result & 0xFF0) > 0xF0;
        state.A = result & 0xFF;
    }

    /**
     * Subtract value from A.
     *
     * @param value
     *         Value.
     */
    protected final void subtract(int value) {
        if (state.D) {
            subtractDecimal(value);
        } else {
            addBinary(value ^ 0xFF);
        }
    }

    /**
     * Subtract value from A (decimal mode of the NMOS 6502).
     * The flags are set like in binary mode.
     *
     * @param value
     *         Value.
     */
    private void subtractDecimal(int value) {
        var a = state.A;
        var borrow = state.C ? 0 : 1;
        var lo = (a & 0x0F) - (value & 0x0F) - borrow;
        var result = (lo & 0x10) != 0 ?
                (lo - 0x06) & 0x0F | (a & 0xF0) - (value & 0xF0) - 0x10 :
                lo & 0x0F | (a & 0xF0) - (value & 0xF0);
        if ((result & 0x100) != 0) {
            result -= 0x60;
        }

        state.setCarryZeroOverflowNegativeP(a, value ^ 0xFF, a + (value ^ 0xFF) + 1 - borrow);
        state.A = result & 0xFF;
    }

    /**
     * Load.
     *
     * @param value
     *         Value.
     */
    @Interruptible
    protected final int load(int value) {
        state.setZeroNegativeP(value);
        return value;
    }

    /**
     * Decrement.
     * (1)
     *
     * @param value
     *         Value.
     */
    protected final int decrement(int value) {
        var result = (value - 1) & 0xFF;
        state.setZeroNegativeP(result);
        return result;
    }

    /**
     * Increment.
     * (1)
     *
     * @param value
     *         Value.
     */
    protected final int increment(int value) {
        var result = (value + 1) & 0xFF;
        state.setZeroNegativeP(result);
        return result;
    }

    /**
     * Compare value with value at addr.
     * (0)
     */
    protected final void compare(int value, int value2) {
        var result = value - value2;
        state.setCarryZeroNegativeP(result, result >= 0);
    }

    /**
     * Branch to relative addr if flag is set in P.
     * (2, 3 if branched, 4 if branched with page crossing)
     *
     * @param condition
     *         Condition to branch.
     */
    @Interruptible
    protected final void branchIf(boolean condition) {
        var addr = readRelativeAddressPC();
        if (condition) {
            var pc = state.PC;
            if (((pc ^ addr) & 0xFF00) != 0) {
                read(pc);
                // Page crossing: Dummy read with the high byte not yet fixed.
                read(pc & 0xFF00 | addr & 0x00FF);
            } else {
                // Without page crossing interrupts are not polled again.
                var irq = irqPolled;
                var nmi = nmiPolled;
                read(pc);
                irqPolled = irq;
                nmiPolled = nmi;
            }
            state.PC = addr;
        }
    }

    /**
     * Push the byte onto the stack.
     */
    @Interruptible
    protected final void pushByte(int value) {
        if (DEBUG && state.S == 0x00) {
            throw new IllegalArgumentException("Stack overflow");
        }
        write(value, state.decS());
    }

    /**
     * Push the word onto the stack.
     */
    @Interruptible
    protected final void pushWord(int value) {
        pushByte(value >> 8);
        pushByte(value & 0xFF);
    }

    /**
     * Dummy read at the stack pointer, without changing it.
     * (1)
     */
    @Interruptible
    protected final void stackRead() {
        read(state.getS());
    }

    /**
     * Pop a byte from the stack.
     */
    @Interruptible
    protected final int popByte() {
        if (DEBUG && state.S == 0xFF) {
            throw new IllegalArgumentException("Stack underflow");
        }
        return read(state.incS());
    }

    /**
     * Pop a word from the stack.
     */
    @Interruptible
    protected final int popWord() {
        return popByte() | (popByte() << 8);
    }

    /**
     * RTS.
     */
    @Interruptible
    protected final void rts() {
        idleRead();
        stackRead();
        state.PC = popWord();
        readBytePC();
    }

    //
    // Illegal commands.
    //

    /**
     * Report illegal opcode.
     */
    protected final void reportIllegalOpcode() {
        if (logger.isDebugEnabled()) {
            logger.debug(Monitor.state(state));
            logger.debug(Monitor.disassemble(state.PC, bus));
        }
        var pc = (state.PC - 1) & 0xFFFF;
        throw new IllegalArgumentException("Illegal opcode " + HexUtil.hexByte(bus.read(pc)) + " at " + HexUtil.hexWord(pc));
    }

    /**
     * Crash.
     */
    @Interruptible
    protected final void crash() {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        //noinspection InfiniteLoopStatement
        while (true) {
            tick.waitForTick();
        }
    }

    /**
     * *NOP.
     * (1)
     */
    @Interruptible
    protected void nop() {
        if (DEBUG) {
            reportIllegalOpcode();
        }
    }

    /**
     * *ALR: AND and LSR.
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected void alr(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        var result = state.A & value;
        var c = (result & 0x01) != 0;
        result = result >> 1;
        state.setCarryZeroNegativeP(result, c);
        state.A = result;
    }

    /**
     * *ALR: AND with moving bit 7 to carry (logic from ASL/ROL).
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected void anc(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        var result = state.A & value;
        state.setCarryZeroNegativeP(result, (result & 0x80) != 0);
        state.A = result;
    }

    /**
     * *AHX: A and X and (Highbyte of address + 1).
     *
     * @param base
     *         Base address.
     * @param index
     *         Index.
     */
    @Interruptible
    protected void ahx(int base, int index) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        storeAndHighByte(state.A & state.X, base, index);
    }

    /**
     * *ARR: AND with decimal mode corrections of ADC and with exchanging bit 7 with carry (logic from ROR).
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected void arr(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        if (state.D) {
            // N, Z and V are set from the rotated value, the decimal corrections are based on the value before.
            var and = state.A & value;
            var result = and >> 1;
            if (state.C) {
                result |= 0x80;
            }
            state.setZeroNegativeP(result);
            state.V = ((and ^ result) & 0x40) != 0;
            if ((and & 0x0F) + (and & 0x01) > 0x05) {
                result = result & 0xF0 | (result + 0x06) & 0x0F;
            }
            state.C = (and & 0xF0) + (and & 0x10) > 0x50;
            if (state.C) {
                result = result & 0x0F | (result + 0x60) & 0xF0;
            }
            state.A = result;

        } else {
            var result = state.A & value;
            state.V = ((result ^ (result << 1)) & 0x80) != 0;
            result = result >> 1;
            if (state.C) {
                result |= 0x80;
            }

            state.setZeroNegativeP(result);
            state.C = (result & 0x40) != 0;
            state.A = result;
        }
    }

    /**
     * *AXS: A and X, store result.
     * (?)
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void axs(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        // No state change!
        write(state.A & state.X, addr);
    }

    /**
     * *DCM (*DCP): Decrement and compare.
     * (?)
     * <p>
     * TODO mh: Add value as a parameter instead of reading it here?
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void dcm(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var value = decrement(readForModify(addr));
        write(value, addr);
        compare(state.A, value);
    }

    /**
     * *INS (*ISC): Increment address, subtract value from A.
     * (?)
     * <p>
     * TODO mh: Add value as a parameter instead of reading it here?
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void ins(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var value = increment(readForModify(addr));
        write(value, addr);
        subtract(value);
    }

    /**
     * *LAS: Load A, X and S.
     * (0)
     */
    @Interruptible
    protected final void las(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var result = value & state.S;
        state.setZeroNegativeP(result);
        state.A = result;
        state.X = result;
        state.S = result;
    }

    /**
     * *LAX: Load A and X.
     * (?)
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected final void lax(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        state.setZeroNegativeP(value);
        state.A = value;
        state.X = value;
    }

    /**
     * *LSE: Logical shift right and eor.
     * (?)
     * <p>
     * TODO mh: Add value as a parameter instead of reading it here?
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void lse(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var value = shiftRight(readForModify(addr));
        write(value, addr);
        xor(value);
    }

    /**
     * *LXA: Load A and X.
     * (?)
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected final void lxa(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var result = (state.A | 0xEE) & value;
        state.setZeroNegativeP(result);
        state.A = result;
        state.X = result;
    }

    /**
     * *RLA: Rotate left and and with A, store result.
     * (3)
     * <p>
     * TODO mh: Add value as a parameter instead of reading it here?
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void rla(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var value = (readForModify(addr) << 1) | (state.C ? 1 : 0);
        state.setCarryZeroNegativeP(value, (value & 0x100) != 0);
        write(value & 0xFF, addr);

        state.A &= value & 0xFF;
        state.setZeroNegativeP(state.A);
    }

    /**
     * *RRA: ROR and ADC.
     * (?)
     * <p>
     * TODO mh: Add value as a parameter instead of reading it here?
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void rra(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var value = rotateRight(readForModify(addr));
        write(value, addr);
        add(value);
    }

    /**
     * *SLO: Shift left and or with A.
     * (3)
     * <p>
     * TODO mh: Add value as a parameter instead of reading it here?
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void slo(int addr) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var value = readForModify(addr) << 1;
        state.setCarryZeroNegativeP(value, (value & 0x100) != 0);
        write(value & 0xFF, addr);

        state.A |= value & 0xFF;
        state.setZeroNegativeP(state.A);
    }

    /**
     * *AXS: A and X, store result.
     * (?)
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected final void sax(int value) {
        if (DEBUG) {
            reportIllegalOpcode();
        }
        var result = (state.A & state.X) - value;
        state.setCarryZeroNegativeP(result, result >= 0);
        state.X = result & 0xFF;
    }

    /**
     * *SHX: X and (High byte of address + 1).
     *
     * @param base
     *         Base address.
     * @param index
     *         Index.
     */
    @Interruptible
    protected void shx(int base, int index) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        storeAndHighByte(state.X, base, index);
    }

    /**
     * *SHY: Y and (High byte of address + 1).
     *
     * @param base
     *         Base address.
     * @param index
     *         Index.
     */
    @Interruptible
    protected void shy(int base, int index) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        storeAndHighByte(state.Y, base, index);
    }

    /**
     * *TAS: S = A and X, write S and (High byte of address + 1).
     *
     * @param base
     *         Base address.
     * @param index
     *         Index.
     */
    @Interruptible
    protected void tas(int base, int index) {
        if (DEBUG) {
            reportIllegalOpcode();
        }

        state.S = state.A & state.X;
        storeAndHighByte(state.S, base, index);
    }

    /**
     * Store of *AHX, *SHX, *SHY and *TAS: Value and (high byte of base address + 1) to base address + index.
     * If the page is crossed, the high byte of the address is replaced by that result.
     * If the CPU has been halted by RDY at the dummy read,
     * the "and (high byte of base address + 1)" drops off for the stored value, but not for the address.
     * (2)
     */
    @Interruptible
    private void storeAndHighByte(int value, int base, int index) {
        var addr = indexWithDummyRead(base, index);
        var result = value & ((base >> 8) + 1);
        if (((base ^ addr) & 0xFF00) != 0) {
            addr = result << 8 | addr & 0x00FF;
        }
        write(halted ? value : result, addr);
    }

    /**
     * *XAA (*ANE): TXA and AND.
     * The "magic constant" is unstable and depends on the CPU.
     * $EF makes most programs work, $EE if the CPU has been halted by RDY at the read of the argument
     * (see VICE testprogs CPU/ane).
     *
     * @param value
     *         Argument.
     */
    @Interruptible
    protected final void xaa(int value) {
        var magic = halted ? 0xEE : 0xEF;
        var result = (state.A | magic) & state.X & value;
        state.setZeroNegativeP(result);
        state.A = result;
    }

    //
    // Addressing modes.
    //

    /**
     * Fake read at PC for implied addressing modes.
     * (1)
     */
    @Interruptible
    protected final void readImpliedPC() {
        idleRead();
    }

    /**
     * Read immediate at PC.
     * (1)
     */
    @Interruptible
    protected final int readImmediatePC() {
        return readBytePC();
    }

    /**
     * Read relative address at PC.
     * (1)
     */
    @Interruptible
    protected final int readRelativeAddressPC() {
        return ((byte) readBytePC() + state.PC) & 0xFFFF;
    }

    /**
     * Read an absolute zero-page address at PC.
     * (1)
     */
    @Interruptible
    protected final int readAbsoluteZeropageAddressPC() {
        return readBytePC();
    }

    /**
     * Read an absolute zero-page address at PC indexed by index.
     * (2)
     */
    @Interruptible
    protected final int readAbsoluteZeropageAddressPC(int index) {
        var addr = readBytePC();
        // Fake read, while adding index to addr.
        read(addr);
        return (addr + index) & 0xFF;
    }

    /**
     * Read absolute address at PC. (AAY64)
     * (2)
     */
    @Interruptible
    protected final int readAbsoluteAddressPC() {
        return readWordPC();
    }

    /**
     * Read an absolute address at PC indexed by index, for writing or read-modify-write instructions.
     * The dummy read is always executed.
     * (3)
     */
    @Interruptible
    protected final int readAbsoluteAddressPCForWrite(int index) {
        return indexWithDummyRead(readWordPC(), index);
    }

    /**
     * Read an absolute address at PC indexed by index, for reading instructions.
     * The dummy read is executed only if the page is crossed.
     * (2-3)
     */
    @Interruptible
    protected final int readAbsoluteAddressPCForRead(int index) {
        return indexWithDummyReadOnPageCrossing(readWordPC(), index);
    }

    /**
     * Index an address.
     * The dummy read with just the low byte indexed happens, while the high byte is being corrected.
     * (1)
     */
    @Interruptible
    private int indexWithDummyRead(int addr, int index) {
        var result = (addr + index) & 0xFFFF;
        read(addr & 0xFF00 | result & 0x00FF);
        return result;
    }

    /**
     * Index an address.
     * Reading instructions skip the dummy read, if the page is not crossed.
     * (0-1)
     */
    @Interruptible
    private int indexWithDummyReadOnPageCrossing(int addr, int index) {
        var result = (addr + index) & 0xFFFF;
        if ((result & 0xFF00) != (addr & 0xFF00)) {
            read(addr & 0xFF00 | result & 0x00FF);
        }
        return result;
    }

    /**
     * Read an absolute address at addr. (AAY64)
     * (2)
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final int readAbsoluteAddress(int addr) {
        return read(addr) | (read((addr + 1) & 0xFFFF) << 8);
    }

    /**
     * Read an absolute address at addr for indirect addressing modes. (AAY64)
     * Processor bug: Incrementing address does not increment page.
     * (2)
     *
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final int readAbsoluteAddressForIndirect(int addr) {
        return read(addr) | (read(addr & 0xFF00 | ((addr + 1) & 0xFF)) << 8);
    }

    /**
     * Read an address zero-page indexed by X indirect.
     * (4)
     */
    @Interruptible
    protected final int readZeropageIndirectXAddressPC() {
        return readAbsoluteAddressForIndirect(readAbsoluteZeropageAddressPC(state.X));
    }

    /**
     * Read an address zero-page indirect indexed by Y, for writing or read-modify-write instructions.
     * The dummy read is always executed.
     * (4)
     */
    @Interruptible
    protected final int readZeropageIndirectYAddressPCForWrite() {
        return indexWithDummyRead(readAbsoluteAddressForIndirect(readAbsoluteZeropageAddressPC()), state.Y);
    }

    /**
     * Read an address zero-page indirect indexed by Y, for reading instructions.
     * The dummy read is executed only if the page is crossed.
     * (3-4)
     */
    @Interruptible
    protected final int readZeropageIndirectYAddressPCForRead() {
        return indexWithDummyReadOnPageCrossing(readAbsoluteAddressForIndirect(readAbsoluteZeropageAddressPC()), state.Y);
    }

    /**
     * Read an indirect address at PC.
     * (4)
     */
    @Interruptible
    protected final int readIndirectAddress() {
        return readAbsoluteAddressForIndirect(readAbsoluteAddressPC());
    }

    //
    // Read / write interface.
    //

    /**
     * Byte of the last access of the CPU to the bus.
     * The VIC loads it into the data of sprites without DMA.
     */
    public final int getBusData() {
        return busData;
    }

    /**
     * Read a value for a read-modify-write instruction.
     * In the modify cycle the 6510 writes the unmodified value back, before it writes the result.
     * Programs rely on this, e.g. to acknowledge VIC interrupts with DEC $D019.
     * (2)
     */
    @Interruptible
    protected final int readForModify(int addr) {
        var value = read(addr);
        write(value, addr);
        return value;
    }

    /**
     * Read a byte at PC. Does NOT Increment PC.
     * (1)
     */
    @Interruptible
    protected final void idleRead() {
        read(state.PC);
    }

    /**
     * Read a byte at PC. Increment PC.
     * (1)
     */
    @Interruptible
    protected final int readBytePC() {
        var pc = state.incPC();
        var result = read(pc);

        return result;
    }

    /**
     * Read a word at PC. Increment PC.
     * (2)
     */
    @Interruptible
    protected final int readWordPC() {
        var pc = state.incPC();
        var result = read(pc);
        pc = state.incPC();
        result |= read(pc) << 8;

        return result;
    }

    /**
     * Write the value (byte) to addr.
     * (1)
     *
     * @param value
     *         Value.
     * @param addr
     *         Address.
     */
    @Interruptible
    protected final void write(int value, int addr) {
        poll();

        // Always write to bus!
        busData = value;
        bus.write(value, addr);
        if (addr <= 1) {
            writePort(value, addr);
        }
        tick.waitForTick();
    }

    /**
     * Write the value to an IO port.
     *
     * @param value
     *         Value.
     * @param addr
     *         Port address (0 or 1).
     */
    final void writePort(int value, int addr) {
        if (addr == 0) {
            portOut.setOutputMask(value);
        } else if (addr == 1) {
            portOut.setOutputData(value);
        } else {
            throw new IllegalArgumentException("unreachable code");
        }

        portIn.setOutputData(portOut.outputData() & 0xDF | 0x17);
    }

    /**
     * Read a byte from the addr.
     * (1)
     *
     * @param addr
     *         Address.
     * @return Byte from bus.
     */
    @Interruptible
    protected final int read(int addr) {
        // The CPU halts at read cycles while not ready. Write cycles are not affected.
        // The interrupt lines are still polled while halted,
        // so an interrupt raised meanwhile is not delayed by another instruction.
        halted = !ready;
        while (!ready) {
            poll();
            tick.waitForTick();
        }
        poll();

        // Always read from bus!
        var result = bus.read(addr);
        if (addr <= 1) {
            result = readPort(addr);
        }
        busData = result;
        tick.waitForTick();

        return result;
    }

    /**
     * Poll interrupts.
     */
    private void poll() {
        var state = this.state;
        irqPolled = irqLine;
        nmiPolled = nmiLine;
        irqLine = state.interrupt;
        nmiLine = state.NMI;
    }

    /**
     * Poll the NMI only.
     */
    private void pollNmi() {
        nmiPolled = nmiLine;
        nmiLine = state.NMI;
    }

    /**
     * Read a byte from an IO port.
     *
     * @param addr
     *         Address of port.
     * @return Byte from port.
     */
    private int readPort(int addr) {
        if (addr == 0) {
            return portOut.outputMask();
        } else if (addr == 1) {
            return portOut.outputData();
        } else {
            throw new IllegalArgumentException("unreachable code");
        }
    }

    //
    // External debugging support.
    //

    /**
     * Get CPU state.
     */
    public synchronized CPU6510State getState() {
        return state;
    }

    //
    // Inner classes.
    //

    /**
     * Interface for opcode implementations.
     */
    @FunctionalInterface
    private interface Opcode {
        @Interruptible
        void execute();
    }
}
