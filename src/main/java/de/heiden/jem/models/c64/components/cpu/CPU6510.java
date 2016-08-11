package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import de.heiden.jem.components.ports.*;
import de.heiden.jem.models.c64.components.memory.Patchable;
import de.heiden.jem.models.c64.monitor.Monitor;
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
  protected final CPU6510State _state;

  /**
   * Tick.
   */
  protected Tick _tick;

  /**
   * Bus.
   */
  protected BusDevice _bus;

  /**
   * Port at address $0000/$0001 controlled by this cpu.
   */
  private final InputOutputPortImpl _portOut;

  /**
   * Input for port at address $0000/$0001 driven by surrounding hardware.
   */
  private final OutputPortImpl _portIn;

  /**
   * Port for interrupt.
   */
  private final InputPort _irq;
  private boolean _irqState = false;

  /**
   * Port for nmi.
   */
  private final InputPort _nmi;
  private boolean _nmiState = false;

  /**
   * Address -> Patch.
   */
  private final Map<Integer, Patch> patches = new HashMap<>();

  /**
   * Constructor.
   */
  public CPU6510() {
    _state = new CPU6510State();

    // TODO mh: connect to emulated hardware
    _portIn = new OutputPortImpl();
    _portIn.setOutputMask(0xFF);
    _portIn.setOutputData(0xDF);
    _portOut = new InputOutputPortImpl();
    _portOut.connect(_portIn);


    _irq = new InputPortImpl();
    _irq.addInputPortListener((value, mask) -> {
      // irq is low active
      boolean irq = (value & 0x01) == 0;
      if (irq && !_irqState) {
        _state.triggerIRQ();
      } else if (!irq) {
        // normally irq will be reset when it is about to be executed,
        // but if the irq is not being handled due to the I flag,
        // then it will be reset when the interrupt request is cleared
        _state.resetIRQ();
      }
      _irqState = irq;
    });

    _nmi = new InputPortImpl();
    _nmi.addInputPortListener((value, mask) -> {
      // nmi is low active
      boolean nmi = (value & 0x01) == 0;
      if (nmi && !_nmiState) {
        _state.NMI = true;
      } else if (!nmi) {
        _state.NMI = false;
      }
      _nmiState = nmi;
    });

    logger.debug("start cpu");
  }

  @Override
  public void setTick(Tick tick) {
    _tick = tick;
  }

  /**
   * Connect to bus.
   *
   * @param bus cpu bus
   * @require bus != null
   */
  public void connect(BusDevice bus) {
    assert bus != null : "bus != null";

    _bus = bus;
  }

  /**
   * Add a patch.
   *
   * @param patch Patch
   */
  public void add(Patch patch) {
    assert patch != null : "patch != null";

    patches.put(patch.getAddress(), patch);
    writePort(0xFF, 0x0001); // standard memory layout
    patch.replaced = _bus.read(patch.getAddress());
    ((Patchable) _bus).patch(0x02, patch.getAddress()); // add breakpoint
  }

  /**
   * Reset CPU.
   * <p/>
   * TODO should be protected?
   */
  @Interruptible
  public void reset() {
    logger.debug("reset");

    // wait for first tick
    _tick.waitForTick();

    // TODO init something else?
    _state.S = 0xFF;
    _state.setP(0x00); // TODO correct?
    _state.PC = readAbsoluteAddress(0xFFFC);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * CPU port.
   */
  public InputOutputPort getPort() {
    return _portOut;
  }

  /**
   * IRQ input signal.
   */
  public InputPort getIRQ() {
    return _irq;
  }

  /**
   * NMI input signal.
   */
  public InputPort getNMI() {
    return _nmi;
  }

  @Override
  @Interruptible
  public final void run() {
    reset();

    final CPU6510State state = _state;
    //noinspection InfiniteLoopStatement
    for (;;) {
      if (state.NMI) {
        nmi();

      } else if (state.interrupt) {
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
    _state.NMI = false;
    interrupt(0xFFFA);
  }

  /**
   * Execute IRQ.
   */
  @Interruptible
  private void irq() {
    CPU6510State state = _state;
    state.resetIRQ();
    state.B = false;
    interrupt(0xFFFE);
  }

  @Interruptible
  protected void execute() {
//      int b = readBytePC();
//      Opcode opcode = opcodes[b];
//      opcode.execute();
    OPCODES[readBytePC()].execute();
  }

  // <editor-fold desc="Opcodes">

  @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef", "SpellCheckingInspection"})
  private final Opcode[] OPCODES =
    {
      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $00: BRK (7)
        {
          readBytePC();
          pushWord(_state.PC);
          _state.B = true;
          pushByte(_state.getP());
          _state.I = true;
          _state.PC = readAbsoluteAddress(0xFFFE);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $01: ORA ($XX,X) (6) // izx
        {
          or(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $02: *KIL (*) // TODO imm?
        {
          // Use opcode $02 as escape
          Patch patch = patches.get(_state.PC - 1);
          if (patch != null) {
            int opcode = patch.execute(_state, _bus);
            if (opcode >= 0) {
              OPCODES[opcode].execute();
            }
          } else {
            // no patch -> standard behaviour: crash
            crash();
          }
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $03: *SLO ($XX,X) (8) // izx
        {
          slo(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $04: *NOP $XX (3) // zp
        {
          read(readAbsoluteZeropageAddressPC());
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $05: ORA $XX (3) // zp
        {
          or(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $06: ASL $XX (5) // zp
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $07: *SLO $XX (5) // zp
        {
          slo(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $08: PHP (3) // no
        {
          idleRead();
          pushByte(_state.getP());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $09: ORA #$XX (2)
        {
          or(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $0A: ASL (2) // no
        {
          _state.A = shiftLeft(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $0B: *ANC #$XX (2)
        {
          anc(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $0C: *NOP $XXXX (4) // abs
        {
          read(readAbsoluteAddressPC());
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $0D: ORA $XXXX (4)
        {
          or(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $0E: ASL $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $0F: *SLO $XXXX (6)
        {
          slo(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $10: BPL $XXXX (2/3) // rel
        {
          branchIf(!_state.N);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $11: ORA ($XX),Y (5) // izy
        {
          or(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $12: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $13: *SLO ($XX),Y (8) // izy
        {
          // TODO 1 tick
          slo(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $14: *NOP $XX,X (4) // zpx
        {
          read(readAbsoluteZeropageAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $15: ORA $XX,X (4) // zpx
        {
          or(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $16: ASL $XX,X (6) // zpx
        {
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $17: *SLO $XX,X (6) // zpx
        {
          slo(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $18: CLC (2) // no
        {
          idleRead(); // during operation
          _state.C = false;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $19: ORA $XXXX,Y (4)
        {
          or(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $1A: *NOP (2) // no
        {
          readImpliedPC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $1B: *SLO $XXXX,Y (7)
        {
          // TODO 1 tick
          slo(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $1C: *NOP $XXXX,X (5) // abx
        {
          read(readAbsoluteAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $1D: ORA $XXXX,X (4)
        {
          or(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $1E: ASL $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $1F: *SLO $XXXX,X (7)
        {
          // TODO 1 tick
          slo(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $20: JSR $XXXX (6) (TODO rework: see AAY64)
        {
          int addr = readAbsoluteAddressPC();
          int returnAddr = (_state.PC - 1) & 0xFFFF;
          _tick.waitForTick(); // internal operation
          pushWord(returnAddr);
          _state.PC = addr;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $21: AND ($XX,X) (6) // izx
        {
          and(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $22: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $23: *RLA ($XX,X) (8) // izx
        {
          rla(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $24: BIT $XX (4) // zp
        {
          bit(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $25: AND $XX (3) // zp
        {
          and(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $26: ROL $XX (5) // zp
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $27: *RLA $XX (5) // zp
        {
          rla(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $28: PLP (4) // no
        {
          _state.setP(popByte());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $29: AND #$XX (2)
        {
          and(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $2A: ROL (2) // no
        {
          _state.A = rotateLeft(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $2B: *ANC #$XX (2)
        {
          anc(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $2C: BIT $XXXX (4)
        {
          bit(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $2D: AND $XXXX (4)
        {
          and(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $2E: ROL $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $2F: *RLA $XXXX (6)
        {
          rla(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $30: BMI $XXXX (2/3) // rel
        {
          branchIf(_state.N);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $31: AND ($XX),Y (5) // izy
        {
          and(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $32: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $33: *RLA ($XX),Y (8) // izy
        {
          // TODO 1 tick
          rla(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $34: *NOP $XX,X (4) // zpx
        {
          read(readAbsoluteZeropageAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $35: AND $XX,X (4) // zpx
        {
          and(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $36: ROL $XX,X (6) // zpx
        {
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $37: *RLA $XX,X (6) // zpx
        {
          rla(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $38: SEC (2) // no
        {
          idleRead(); // during operation
          _state.C = true;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $39: AND $XXXX,Y (4)
        {
          and(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $3A: *NOP (2) // no
        {
          readImpliedPC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $3B: *RLA $XXXX,Y (7)
        {
          // TODO 1 tick
          rla(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $3C: *NOP $XXXX,X (5) // abx
        {
          read(readAbsoluteAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $3D: AND $XXXX,X (4) // abx
        {
          and(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $3E: ROL $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $3F: *RLA $XXXX,X (7)
        {
          // TODO 1 tick
          rla(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $40: RTI (6)
        {
          // TODO 2 ticks
          _state.setP(popByte());
          _state.PC = popWord();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $41: EOR ($XX,X) (6) // izx
        {
          xor(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $42: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $43: *LSE ($XX,X) // izx
        {
          lse(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $44: *NOP $XX (3) // zp
        {
          read(readAbsoluteZeropageAddressPC());
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $45: EOR $XX (3) // zp
        {
          xor(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $46: LSR $XX (5) // zp
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $47: *LSE $XX // zp
        {
          lse(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $48: PHA (3) // no
        {
          idleRead();
          pushByte(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $49: EOR #$XX (2)
        {
          xor(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $4A: LSR (2) // no
        {
          _state.A = shiftRight(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $4B: ALR #$XX (2)
        {
          alr(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $4C: JMP $XXXX (3) (AAY64)
        {
          _state.PC = readAbsoluteAddressPC();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $4D: EOR $XXXX (4)
        {
          xor(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $4E: LSR $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $4F: *LSE $XXXX
        {
          lse(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $50: BVC $XXXX (2/3) // rel
        {
          branchIf(!_state.V);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $51: EOR ($XX),Y (5) // izy
        {
          xor(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $52: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $53: *LSE ($XX),Y // izy
        {
          lse(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $54: *NOP $XX,X (4) // zpx
        {
          read(readAbsoluteZeropageAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $55: EOR $XX,X (4) // zpx
        {
          xor(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $56: LSR $XX,X (6) // zpx
        {
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $57: *LSE $XX,X // zpx
        {
          lse(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $58: CLI (2) // no
        {
          idleRead(); // during operation
          _state.cli();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $59: EOR $XXXX,Y (4)
        {
          xor(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $5A: *NOP (2) // no
        {
          readImpliedPC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $5B: *LSE $XXXX,Y
        {
          lse(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $5C: *NOP $XXXX,X (5) // abx
        {
          read(readAbsoluteAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $5D: EOR $XXXX,X (4)
        {
          xor(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $5E: LSR $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $5F: *LSE $XXXX,X
        {
          lse(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $60: RTS (6)
        {
          rts();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $61: ADC ($XX,X) (6) // izx
        {
          add(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $62: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $63: *RRA ($XX,X) // izx
        {
          rra(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $64: *NOP $XX (3) // zp
        {
          read(readAbsoluteZeropageAddressPC());
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $65: ADC $XX (3) // zp
        {
          add(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $66: ROR $XX (5) // zp
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $67: *RRA $XX // zp
        {
          rra(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $68: PLA (4) // no
        {
          int a = popByte();
          _state.setZeroNegativeP(a);
          _state.A = a;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $69: ADC #$XX (2)
        {
          add(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $6A: ROR (2) // no
        {
          _state.A = rotateRight(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $6B: *ARR #$XX (2)
        {
          arr(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $6C: JMP ($XXXX) (5)
        {
          _state.PC = readIndirectAddress();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $6D: ADC $XXXX (4)
        {
          add(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $6E: ROR $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $6F: *RRA $XXXX
        {
          rra(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $70: BVS $XXXX (2/3) // rel
        {
          branchIf(_state.V);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $71: ADC ($XX),Y (4) // izy
        {
          add(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $72: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $73: *RRA ($XX),Y // izy
        {
          rra(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $74: *NOP $XX,X (4) // zpx
        {
          read(readAbsoluteZeropageAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $75: ADC $XX,X (4) // zpx
        {
          add(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $76: ROR $XX,X (6) // zpx
        {
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $77: *RRA $XX,X // zpx
        {
          rra(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $78: SEI (2) // no
        {
          idleRead(); // during operation
          _state.sei();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $79: ADC $XXXX,Y (5)
        {
          add(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $7A: *NOP (2) // no
        {
          readImpliedPC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $7B: *RRA $XXXX,Y
        {
          rra(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $7C: *NOP $XXXX,X (5) // abx
        {
          read(readAbsoluteAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $7D: ADC $XXXX,X (4)
        {
          add(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $7E: ROR $XXXX,X (7)
        {
          // TODO 1 ticks
          int addr = readAbsoluteAddressPC(_state.X);
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $7F: *RRA $XXXX,X
        {
          rra(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $80: *NOP #$XX (2) // imm
        {
          readImmediatePC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $81: STA ($XX,X) (6) // izx
        {
          write(_state.A, readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $82: *NOP #$XX (2) // imm
        {
          readImmediatePC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $83: *AXS ($XX,X) // izx
        {
          axs(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $84: STY $XX (3) // zp
        {
          write(_state.Y, readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $85: STA $XX (3) // zp
        {
          write(_state.A, readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $86: STX $XX (3) // zp
        {
          write(_state.X, readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $87: *AXS $XX // zp
        {
          axs(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $88: DEY (2) // no
        {
          _state.Y = decrement(_state.Y);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $89: *NOP (2) // imm
        {
          readImmediatePC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $8A: TXA (2) // no
        {
          idleRead();
          _state.A = load(_state.X);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $8B: *XAA #$XX (?)
        {
          xaa(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $8C: STY $XXXX (4)
        {
          write(_state.Y, readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $8D: STA $XXXX (4)
        {
          write(_state.A, readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $8E: STX $XXXX (4)
        {
          write(_state.X, readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $8F: *AXS $XXXX
        {
          axs(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $90: BCC $XXXX (2/3) // rel
        {
          branchIf(!_state.C);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $91: STA ($XX),Y (6) // izy
        {
          write(_state.A, readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $92: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $93: *AHX ($XX),Y
        {
          int addr = readZeropageIndirectYAddressPC();
          ahx(read(addr), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $94: STY $XX,X (4) // zpx
        {
          write(_state.Y, readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $95: STA $XX,X (4) // zpx
        {
          write(_state.A, readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $96: STX $XX,Y (4) // zpy
        {
          write(_state.X, readAbsoluteZeropageAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $97: *AXS $XX,Y // zpy
        {
          axs(readAbsoluteZeropageAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $98: TYA (2) // no
        {
          idleRead();
          _state.A = load(_state.Y);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $99: STA $XXXX,Y (5)
        {
          // TODO 1 tick
          write(_state.A, readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $9A: TXS (2) // no
        {
          idleRead(); // during operation
          _state.S = _state.X; // no update of P !!!
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $9B: *TAS $XXXX,Y
        {
          tas(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $9C: *SHY $XXXX,X
        {
          int addr = readAbsoluteAddressPC(_state.X);
          shy(read(addr), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $9D: STA $XXXX,X (5)
        {
          write(_state.A, readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $9E: *SHX $XXXX,Y
        {
          int addr = readAbsoluteAddressPC(_state.Y);
          shx(read(addr), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $9F: *AHX $XXXX,Y
        {
          int addr = readAbsoluteAddressPC(_state.Y);
          ahx(read(addr), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A0: LDY #$XX (2) // imm
        {
          _state.Y = load(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A1: LDA ($XX,X) (6) // izx
        {
          _state.A = load(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A2: LDX #$XX (2) // imm
        {
          _state.X = load(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A3: *LAX ($XX,X) // izx
        {
          lax(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A4: LDY $XX (3) // zp
        {
          _state.Y = load(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A5: LDA $XX (3) // zp
        {
          _state.A = load(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A6: LDX $XX (3) // zp
        {
          _state.X = load(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A7: *LAX $XX // zp
        {
          lax(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A8: TAY (2) // no
        {
          idleRead();
          _state.Y = load(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $A9: LDA #$XX (2)
        {
          _state.A = load(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $AA: TAX (2) // no
        {
          idleRead();
          _state.X = load(_state.A);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $AB: LXA #$XX (?)
        {
          lxa(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $AC: LDY $XXXX (4)
        {
          _state.Y = load(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $AD: LDA $XXXX (4)
        {
          _state.A = load(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $AE: LDX $XXXX (4)
        {
          _state.X = load(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $AF: *LAX $XXXX
        {
          lax(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B0: BCS $XXXX (2/3) // rel
        {
          branchIf(_state.C);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B1: LDA ($XX),Y (5) // izy
        {
          _state.A = load(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B2: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B3: *LAX ($XX),Y // izy
        {
          lax(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B4: LDY $XX,X (4) // zpx
        {
          _state.Y = load(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B5: LDA $XX,X (4) // zpx
        {
          _state.A = load(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B6: LDX $XX,Y (4) // zpy
        {
          _state.X = load(read(readAbsoluteZeropageAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B7: *LAX $XX,Y // zpy
        {
          lax(read(readAbsoluteZeropageAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B8: CLV (2) // no
        {
          idleRead(); // during operation
          _state.V = false;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $B9: LDA $XXXX,Y (4)
        {
          _state.A = load(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $BA: TSX (2) // no
        {
          idleRead();
          _state.X = load(_state.S);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $BB: *LAS $XXXX,Y (4?)
        {
          las(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $BC: LDY $XXXX,X (4)
        {
          _state.Y = load(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $BD: LDA $XXXX,X (4)
        {
          _state.A = load(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $BE: LDX $XXXX,Y (4)
        {
          _state.X = load(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $BF: *LAX $XXXX,Y
        {
          lax(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C0: CPY #$XX (2) // imm
        {
          compare(_state.Y, readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C1: CMP ($XX,X) (6) // izx
        {
          compare(_state.A, read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C2: *NOP #$XX (2) // imm
        {
          readImmediatePC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C3: *DCM ($XX,X) // izx
        {
          dcm(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C4: CPY $XX (3) // zp
        {
          compare(_state.Y, read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C5: CMP $XX (3) // zp
        {
          compare(_state.A, read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C6: DEC $XX (5) // zp
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C7: *DCM $XX // zp
        {
          dcm(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C8: INY (2) // no
        {
          _state.Y = increment(_state.Y);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $C9: CMP #$XX (2)
        {
          compare(_state.A, readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $CA: DEX (2) // no
        {
          _state.X = decrement(_state.X);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $CB: *SAX #$XX (?)
        {
          sax(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $CC: CPY $XXXX (4)
        {
          compare(_state.Y, read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $CD: CMP $XXXX (4)
        {
          compare(_state.A, read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $CE: DEC $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $CF: *DCM $XXXX
        {
          dcm(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D0: BNE $XXXX (2/3) // rel
        {
          branchIf(!_state.Z);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D1: CMP ($XX),Y (5) // izy
        {
          compare(_state.A, read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D2: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D3: *DCM ($XX),Y // izy
        {
          dcm(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D4: *NOP $XX,X (4) // zpx
        {
          read(readAbsoluteZeropageAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D5: CMP $XX,X (4) // zpx
        {
          compare(_state.A, read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D6: DEC $XX,X (6) // zpx
        {
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D7: *DCM $XX,X // zpx
        {
          dcm(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D8: CLD (2) // no
        {
          idleRead(); // during operation
          _state.D = false;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $D9: CMP $XXXX,Y (4)
        {
          compare(_state.A, read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $DA: *NOP (2) // no
        {
          idleRead();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $DB: *DCM $XXXX,Y
        {
          dcm(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $DC: *NOP $XXXX,X (5) // abx
        {
          read(readAbsoluteAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $DD: CMP $XXXX,X (4)
        {
          compare(_state.A, read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $DE: DEC $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $DF: *DCM $XXXX,X
        {
          dcm(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E0: CPX #$XX (2) // imm
        {
          compare(_state.X, readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E1: SBC ($XX,X) (6) // izx
        {
          subtract(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E2: *NOP #$XX (2) // TODO imm?
        {
          readImmediatePC();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E3: *INS ($XX,X) // izx
        {
          ins(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E4: CPX $XX (3) // zp
        {
          compare(_state.X, read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E5: SBC $XX (3) // zp
        {
          subtract(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E6: INC $XX (5) // zp
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(increment(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E7: *INS $XX // zp
        {
          ins(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E8: INX (2) // no
        {
          _state.X = increment(_state.X);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $E9: SBC #$XX (2)
        {
          subtract(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $EA: NOP (2) // no
        {
          idleRead(); // during operation
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $EB: *SBC #$XX (2)
        {
          subtract(readImmediatePC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $EC: CPX $XXXX (4)
        {
          compare(_state.X, read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $ED: SBC $XXXX (4)
        {
          subtract(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $EE: INC $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(increment(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $EF: *INS $XXXX
        {
          ins(readAbsoluteAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F0: BEQ $XXXX (2/3) // rel
        {
          branchIf(_state.Z);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F1: SBC ($XX),Y (5) // izy
        {
          subtract(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F2: *KIL (*) // TODO imm?
        {
          crash();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F3: *INS ($XX),Y // izy
        {
          ins(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F4: *NOP $XX,X (4) // zpx
        {
          read(readAbsoluteZeropageAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F5: SBC $XX,X (4) // zpx
        {
          subtract(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F6: INC $XX,X (6) // zpx
        {
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(increment(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F7: *INS $XX,X // zpx
        {
          ins(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F8: SED (2) // no
        {
          idleRead(); // during operation
          _state.D = true;
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $F9: SBC $XXXX,Y (4)
        {
          subtract(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $FA: *NOP (2) // no
        {
          idleRead();
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $FB: *INS $XXXX,Y
        {
          ins(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $FC: *NOP $XXXX,X (5) // abx
        {
          read(readAbsoluteAddressPC(_state.X));
          nop();
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $FD: SBC $XXXX,X (4)
        {
          subtract(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $FE: INC $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(increment(read(addr)), addr);
        }
      },

      new Opcode() {
        @Override
        @Interruptible
        public final void execute() // $FF: *INS $XXXX,X
        {
          ins(readAbsoluteAddressPC(_state.X));
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
  // commands
  //

  /**
   * Execute interrupt (IRQ or NMI)
   *
   * @param addr interrupt vector
   */
  @Interruptible
  protected final void interrupt(int addr) {
    // TODO ticks?
    pushWord(_state.PC);
    pushByte(_state.getP());
    _state.I = true;
    _state.PC = readAbsoluteAddress(addr);
  }

  /**
   * Shift left.
   *
   * @param value value
   */
  @Interruptible
  protected final int shiftLeft(int value) {
    idleRead(); // during operation

    int result = value << 1;
    _state.setCarryZeroNegativeP(result, (result & 0x100) != 0);
    return result & 0xFF;
  }

  /**
   * Shift right.
   * (1)
   *
   * @param value value
   */
  @Interruptible
  protected final int shiftRight(int value) {
    idleRead(); // during operation

    int result = value >> 1;
    _state.setCarryZeroNegativeP(result, (value & 0x01) != 0);
    return result;
  }

  /**
   * Rotate left.
   *
   * @param value value
   */
  @Interruptible
  protected final int rotateLeft(int value) {
    idleRead(); // during operation

    int result = value << 1;
    if (_state.C) {
      result |= 0x01;
    }
    _state.setCarryZeroNegativeP(result, (result & 0x100) != 0);
    return result & 0xFF;
  }

  /**
   * Rotate right.
   * (1)
   *
   * @param value value
   */
  @Interruptible
  protected final int rotateRight(int value) {
    idleRead(); // during operation

    int result = value >> 1;
    if (_state.C) {
      result |= 0x80;
    }
    _state.setCarryZeroNegativeP(result, (value & 0x01) != 0);
    return result;
  }

  /**
   * And value with A temporary.
   *
   * @param value value
   */
  protected final void bit(int value) {
    _state.setZeroOverflowNegativeP(value, (_state.A & value) == 0);
  }

  /**
   * And value with A.
   *
   * @param value value
   */
  protected final void and(int value) {
    int a = _state.A & value;
    _state.setZeroNegativeP(a);
    _state.A = a;
  }

  /**
   * Or value with A.
   *
   * @param value value
   */
  protected final void or(int value) {
    int a = _state.A | value;
    _state.setZeroNegativeP(a);
    _state.A = a;
  }

  /**
   * Exclusive or value with A.
   *
   * @param value value
   */
  protected final void xor(int value) {
    int a = _state.A ^ value;
    _state.setZeroNegativeP(a);
    _state.A = a;
  }

  /**
   * Add value to A.
   *
   * @param value value
   */
  protected final void add(int value) {
    if (_state.D) {
      addDecimal(value);
    } else {
      addBinary(value);
    }
  }

  /**
   * Add value to A (binary mode).
   *
   * @param value value
   */
  private void addBinary(int value) {
    int a = _state.A + value;
    if (_state.C) {
      a++;
    }
    _state.setCarryZeroOverflowNegativeP(_state.A, value, a);
    _state.A = a & 0xFF;
  }

  /**
   * Add value to A (binary mode).
   *
   * @param value value
   */
  private void addDecimal(int value) {
    // add low nibbles
    int lo = (_state.A & 0x0F) + (value & 0x0F);
    if (_state.C) {
      lo += 0x01;
    }
    boolean clo = lo > 0x09;

    // add high nibbles
    int hi = (_state.A & 0xF0) + (value & 0xF0);
    if (clo) {
      hi += 0x10;
    }
    boolean chi = hi > 0x90;

    _state.setCarryZeroOverflowNegativeP(_state.A, value, lo & 0x0F | hi);

    if (clo) {
      lo -= 0x0A;
    }
    if (chi) {
      hi -= 0xA0;
    }

    _state.A = lo & 0x0F | hi & 0xF0;
    _state.C = chi;
  }

  /**
   * Subtract value from A.
   *
   * @param value value
   */
  protected final void subtract(int value) {
    if (_state.D) {
      subtractDecimal(value ^ 0xFF);
    } else {
      addBinary(value ^ 0xFF);
    }
  }

  /**
   * Add value to A (binary mode).
   *
   * @param value value
   */
  private void subtractDecimal(int value) {
    // add low nibbles
    int lo = (_state.A & 0x0F) + (value & 0x0F);
    if (_state.C) {
      lo += 0x01;
    }
    boolean clo = lo <= 0x0F;

    // add high nibbles
    int hi = (_state.A & 0xF0) + (value & 0xF0);
    if (!clo) {
      hi += 0x10;
    }
    boolean chi = hi <= 0xF0;

    _state.setCarryZeroOverflowNegativeP(_state.A, value, lo & 0x0F | hi);

    if (clo) {
      lo -= 0x06;
    }
    if (chi) {
      hi -= 0x60;
    }

    _state.A = lo & 0x0F | hi & 0xF0;
    _state.C = !chi;
  }

  /**
   * Load.
   *
   * @param value value
   */
  @Interruptible
  protected final int load(int value) {
    _state.setZeroNegativeP(value);
    return value;
  }

  /**
   * Decrement.
   * (1)
   *
   * @param value value
   */
  @Interruptible
  protected final int decrement(int value) {
    idleRead(); // during operation

    int result = (value - 1) & 0xFF;
    _state.setZeroNegativeP(result);
    return result;
  }

  /**
   * Increment.
   * (1)
   *
   * @param value value
   */
  @Interruptible
  protected final int increment(int value) {
    idleRead(); // during operation

    int result = (value + 1) & 0xFF;
    _state.setZeroNegativeP(result);
    return result;
  }

  /**
   * Compare value with value at addr.
   * (0)
   */
  protected final void compare(int value, int value2) {
    int result = value - value2;
    _state.setCarryZeroNegativeP(result, result >= 0);
  }

  /**
   * Branch to relative addr if flag is set in P.
   * (2/3 if condition)
   *
   * @param condition condition to branch
   */
  @Interruptible
  protected final void branchIf(boolean condition) {
    int addr = readRelativeAddressPC();
    if (condition) {
      _state.PC = addr;
      _tick.waitForTick();
    }
  }

  /**
   * Push byte onto stack.
   */
  @Interruptible
  protected final void pushByte(int value) {
    if (DEBUG && _state.S == 0x00) {
      throw new IllegalArgumentException("Stack overflow");
    }
    write(value, _state.decS());
  }

  /**
   * Push word onto stack.
   */
  @Interruptible
  protected final void pushWord(int value) {
    pushByte(value >> 8);
    pushByte(value & 0xFF);
  }

  /**
   * Pop byte from stack.
   */
  @Interruptible
  protected final int popByte() {
    if (DEBUG && _state.S == 0xFF) {
      throw new IllegalArgumentException("Stack underflow");
    }
    return read(_state.incS());
  }

  /**
   * Pop word from stack.
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
    // TODO 3 ticks
    _state.PC = (popWord() + 1) & 0xFFFF;
  }

  //
  // illegal commands
  //

  /**
   * Report illegal opcode.
   */
  protected final void reportIllegalOpcode() {
    if (logger.isDebugEnabled()) {
      logger.debug(Monitor.state(_state));
      logger.debug(Monitor.disassemble(_state.PC, _bus));
    }
    int pc = (_state.PC - 1) & 0xFFFF;
    throw new IllegalArgumentException("Illegal opcode " + HexUtil.hexByte(_bus.read(pc)) + " at " + HexUtil.hexWord(pc));
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
    for (;;) {
      _tick.waitForTick();
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
   * @param value argument
   */
  @Interruptible
  protected void alr(int value) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    int result = _state.A & value;
    boolean c = (result & 0x01) != 0;
    result = result >> 1;
    _state.setCarryZeroNegativeP(result, c);
    _state.A = result;
  }

  /**
   * *ALR: AND with moving bit 7 to carry (logic from ASL/ROL).
   *
   * @param value argument
   */
  @Interruptible
  protected void anc(int value) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    int result = _state.A & value;
    _state.setCarryZeroNegativeP(result, (result & 0x80) != 0);
    _state.A = result;
  }

  /**
   * *AHX: A and X and (Highbyte of address + 1).
   *
   * @param value argument
   * @param addr address
   */
  @Interruptible
  protected void ahx(int value, int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    int result = _state.A & _state.X & ((addr >> 8) + 1);
    // no state change!
    write(result, addr);
  }

  /**
   * *ARR: AND with decimal mode corrections of ADC and with exchanging bit 7 with carry (logic from ROR).
   *
   * @param value argument
   */
  @Interruptible
  protected void arr(int value) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    if (_state.D) {
      int result = _state.A & value;
      _state.V = ((result ^ (result << 1)) & 0x80) != 0;
      result = result >> 1;
      if (_state.C) {
        result |= 0x80;
      }
      if ((result & 0x0F) > 0x09) {
        result -= 0x0A;
      }
      if ((result & 0xF0) > 0x90) {
        result -= 0xA0;
      }

      _state.setZeroNegativeP(result);
      _state.C = (result & 0x40) != 0;
      _state.A = result;

    } else {
      int result = _state.A & value;
      _state.V = ((result ^ (result << 1)) & 0x80) != 0;
      result = result >> 1;
      if (_state.C) {
        result |= 0x80;
      }

      _state.setZeroNegativeP(result);
      _state.C = (result & 0x40) != 0;
      _state.A = result;
    }
  }

  /**
   * *AXS: A and X, store result.
   * (?)
   *
   * @param addr address
   */
  @Interruptible
  protected final void axs(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    // no state change!
    write(_state.A & _state.X, addr);
  }

  /**
   * *DCM (*DCP): Decrement and compare.
   * (?)
   * <p/>
   * TODO mh: add value as a parameter instead of reading it here?
   *
   * @param addr address
   */
  @Interruptible
  protected final void dcm(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int value = decrement(read(addr));
    write(value, addr);
    compare(_state.A, value);
  }

  /**
   * *INS (*ISC): increment address, subtract value from A.
   * (?)
   * <p/>
   * TODO mh: add value as a parameter instead of reading it here?
   *
   * @param addr address
   */
  @Interruptible
  protected final void ins(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int value = increment(read(addr));
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
    int result = value & _state.S;
    _state.setZeroNegativeP(result);
    _state.A = result;
    _state.X = result;
    _state.S = result;
  }

  /**
   * *LAX: Load A and X.
   * (?)
   *
   * @param value argument
   */
  @Interruptible
  protected final void lax(int value) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    _state.setZeroNegativeP(value);
    _state.A = value;
    _state.X = value;
  }

  /**
   * *LSE: Logical shift right and eor.
   * (?)
   * <p/>
   * TODO mh: add value as a parameter instead of reading it here?
   *
   * @param addr address
   */
  @Interruptible
  protected final void lse(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int value = shiftRight(read(addr));
    write(value, addr);
    xor(value);
  }

  /**
   * *LXA: Load A and X.
   * (?)
   *
   * @param value argument
   */
  @Interruptible
  protected final void lxa(int value) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int result = (_state.A | 0xEE) & value;
    _state.setZeroNegativeP(result);
    _state.A = result;
    _state.X = result;
  }

  /**
   * *RLA: Rotate left and and with A, store result.
   * (3)
   * <p/>
   * TODO mh: add value as a parameter instead of reading it here?
   *
   * @param addr address
   */
  @Interruptible
  protected final void rla(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int value = (read(addr) << 1) | (_state.C ? 1 : 0);
    _tick.waitForTick(); // << 1 needs 1 tick
    _state.setCarryZeroNegativeP(value, (value & 0x100) != 0);
    write(value & 0xFF, addr);

    _state.A &= value & 0xFF;
    _state.setZeroNegativeP(_state.A);
  }

  /**
   * *RRA: ROR and ADC.
   * (?)
   * <p/>
   * TODO mh: add value as a parameter instead of reading it here?
   *
   * @param addr address
   */
  @Interruptible
  protected final void rra(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int value = rotateRight(read(addr));
    write(value, addr);
    add(value);
  }

  /**
   * *SLO: Shift left and or with A.
   * (3)
   * <p/>
   * TODO mh: add value as a parameter instead of reading it here?
   *
   * @param addr address
   */
  @Interruptible
  protected final void slo(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int value = read(addr) << 1;
    _state.setCarryZeroNegativeP(value, (value & 0x100) != 0);
    _tick.waitForTick(); // internal operation
    write(value & 0xFF, addr);

    _state.A |= value & 0xFF;
    _state.setZeroNegativeP(_state.A);
  }

  /**
   * *AXS: A and X, store result.
   * (?)
   *
   * @param value argument
   */
  @Interruptible
  protected final void sax(int value) {
    if (DEBUG) {
      reportIllegalOpcode();
    }
    int result = (_state.A & _state.X) - value;
    _state.setCarryZeroNegativeP(result, result >= 0);
    _state.X = result & 0xFF;
  }

  /**
   * *SHX: X and (Highbyte of address + 1).
   *
   * @param value argument
   * @param addr address
   */
  @Interruptible
  protected void shx(int value, int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    int result = _state.X & ((addr >> 8) + 1);
    write(result, addr);
  }

  /**
   * *SHY: Y and (Highbyte of address + 1).
   *
   * @param value argument
   * @param addr address
   */
  @Interruptible
  protected void shy(int value, int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    int result = _state.Y & ((addr >> 8) + 1);
    write(result, addr);
  }

  /**
   * *TAS: S = A amd X, write S and (Highbyte of address + 1).
   *
   * @param addr address
   */
  @Interruptible
  protected void tas(int addr) {
    if (DEBUG) {
      reportIllegalOpcode();
    }

    _state.S = _state.A & _state.X;
    int result = _state.S & ((addr >> 8) + 1);
    // no state change!
    write(result, addr);
  }

  /**
   * *XAA (*ANE): TXA and AND.
   *
   * @param value argument
   */
  @Interruptible
  protected final void xaa(int value) {
    // TODO 0xEE is cpu dependant
    int result = (_state.A | 0xEE) & _state.X & value;
    _state.setZeroNegativeP(result);
    _state.A = result;
  }

  //
  // addressing modes
  //

  /**
   * Dummy read at PC for implied addressing modes.
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
    return ((byte) readBytePC() + _state.PC) & 0xFFFF;
  }

  /**
   * Read absolute zeropage address at PC.
   * (1)
   */
  @Interruptible
  protected final int readAbsoluteZeropageAddressPC() {
    return readBytePC();
  }

  /**
   * Read absolute zeropage address at PC indexed by index.
   * (2)
   */
  @Interruptible
  protected final int readAbsoluteZeropageAddressPC(int index) {
    int addr = readBytePC();
    // dummy read, while adding index to addr
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
   * Read absolute address at PC indexed by index.
   * (3)
   */
  @Interruptible
  protected final int readAbsoluteAddressPC(int index) {
    int addr = readWordPC();
    int result = (addr + index) & 0xFFFF;
    // dummy read with just the low byte indexed, while the high byte is being added
    read(addr & 0xFF00 | result & 0x00FF);
    return result;
  }

  /**
   * Read absolute address at addr. (AAY64)
   * (2)
   *
   * @param addr address
   */
  @Interruptible
  protected final int readAbsoluteAddress(int addr) {
    return read(addr) | (read((addr + 1) & 0xFFFF) << 8);
  }

  /**
   * Read absolute address at addr for indirect addressing modes. (AAY64)
   * Processor bug: Incrementing address does not increment page.
   * (2)
   *
   * @param addr address
   */
  @Interruptible
  protected final int readAbsoluteAddressForIndirect(int addr) {
    return read(addr) | (read(addr & 0xFF00 | ((addr + 1) & 0xFF)) << 8);
  }

  /**
   * Read address zeropage indexed by X indirect.
   * (4)
   */
  @Interruptible
  protected final int readZeropageIndirectXAddressPC() {
    _tick.waitForTick(); // + X needs 1 tick, TODO read address first?
    return readAbsoluteAddressForIndirect((readAbsoluteZeropageAddressPC() + _state.X) & 0xFF);
  }

  /**
   * Read address zeropage indirect indexed by Y.
   * (3)
   */
  @Interruptible
  protected final int readZeropageIndirectYAddressPC() {
    return (readAbsoluteAddressForIndirect(readAbsoluteZeropageAddressPC()) + _state.Y) & 0xFFFF;
  }

  /**
   * Read indirect address at PC.
   * (4)
   */
  @Interruptible
  protected final int readIndirectAddress() {
    return readAbsoluteAddressForIndirect(readAbsoluteAddressPC());
  }

  //
  // read / write interface
  //

  /**
   * Read byte at PC. Does NOT Increment PC.
   * (1)
   */
  @Interruptible
  protected final void idleRead() {
    read(_state.PC);
  }

  /**
   * Read byte at PC. Increment PC.
   * (1)
   */
  @Interruptible
  protected final int readBytePC() {
    int pc = _state.incPC();
    int result = read(pc);

    return result;
  }

  /**
   * Read word at PC. Increment PC.
   * (2)
   */
  @Interruptible
  protected final int readWordPC() {
    int pc = _state.incPC();
    int result = read(pc);
    pc = _state.incPC();
    result |= read(pc) << 8;

    return result;
  }

  /**
   * Write value (byte) to addr.
   * (1)
   *
   * @param value value
   * @param addr address
   */
  @Interruptible
  protected final void write(int value, int addr) {
    // always write to bus!
    _bus.write(value, addr);
    if (addr <= 1) {
      writePort(value, addr);
    }
    _tick.waitForTick();
  }

  /**
   * Write value to io port.
   *
   * @param value value
   * @param addr port address (0 or 1)
   */
  final void writePort(int value, int addr) {
    if (addr == 0) {
      _portOut.setOutputMask(value);
    } else if (addr == 1) {
      _portOut.setOutputData(value);
    } else {
      throw new IllegalArgumentException("unreachable code");
    }

    _portIn.setOutputData(_portOut.data() & 0xDF | 0x17);
  }

  /**
   * Read byte from addr.
   * (1)
   *
   * @param addr address
   * @return byte from bus
   */
  @Interruptible
  protected final int read(int addr) {
    // always read from bus!
    int result = _bus.read(addr);
    if (addr <= 1) {
      result = readPort(addr);
    }
    _tick.waitForTick();

    return result;
  }

  /**
   * Read byte from io port.
   *
   * @param addr Address of port
   * @return byte from port
   */
  private int readPort(int addr) {
    if (addr == 0) {
      return _portOut.outputMask();
    } else if (addr == 1) {
      return _portOut.data();
    } else {
      throw new IllegalArgumentException("unreachable code");
    }
  }

  //
  // External debugging support
  //

  /**
   * Get cpu state.
   */
  public synchronized CPU6510State getState() {
    return _state;
  }

  //
  // inner classes
  //

  /**
   * Interface for opcode implementations.
   */
  private interface Opcode {
    @Interruptible
    void execute();
  }
}
