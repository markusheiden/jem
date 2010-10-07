package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.InputOutputPortImpl;
import de.heiden.jem.components.ports.InputPort;
import de.heiden.jem.components.ports.InputPortImpl;
import de.heiden.jem.components.ports.InputPortListener;
import de.heiden.jem.models.c64.monitor.Monitor;
import org.apache.log4j.Logger;
import org.serialthreads.Interruptible;

/**
 * CPU.
 */
public class CPU6510 implements ClockedComponent
{
  /**
   * Constructor.
   *
   * @param clock system clock
   * @require clock != null
   */
  public CPU6510(Clock clock)
  {
    assert clock != null : "clock != null";

    _state = new CPU6510State();

    _port = new InputOutputPortImpl();

    _irq = new InputPortImpl();
    _irq.addInputPortListener(new InputPortListener()
    {
      private boolean _irq = false;

      @Override
      public void inputPortChanged(int value, int mask)
      {
        // irq is low active
        boolean irq = (value & 0x01) == 0;
        if (irq && !_irq)
        {
          _state.IRQ = true;
        }
        else if (!irq)
        {
          // normally irq will be reset when it is about to be executed,
          // but if the irq is not being handled due to the I flag,
          // then it will be reset when the interrupt request is cleared
          _state.IRQ = false;
        }
        _irq = irq;
      }
    });

    _nmi = new InputPortImpl();
    _nmi.addInputPortListener(new InputPortListener()
    {
      private boolean _nmi = false;

      @Override
      public void inputPortChanged(int value, int mask)
      {
        // nmi is low active
        boolean nmi = (value & 0x01) == 0;
        if (nmi && !_nmi)
        {
          _state.NMI = true;
        }
        else if (!nmi)
        {
          _state.NMI = false;
        }
        _nmi = nmi;
      }
    });

    _tick = clock.addClockedComponent(Clock.CPU, this);

    _logger.debug("start cpu");
  }

  /**
   * Connect to bus.
   *
   * @param bus cpu bus
   * @require bus != null
   */
  public void connect(C64Bus bus)
  {
    assert bus != null : "bus != null";
    
    _bus = bus;
  }

  /**
   * Reset CPU.
   * <p/>
   * TODO should be protected?
   */
  @Interruptible
  public void reset()
  {
    _logger.debug("reset");

    // TODO init something else?
    _state.S = 0x01FF;
    _state.setP(0x00); // TODO correct?
    _state.PC = readAbsoluteAddress(0xFFFC);
  }

  @Override
  public String getName()
  {
    return getClass().getSimpleName();
  }

  /**
   * CPU port.
   */
  public InputOutputPort getPort()
  {
    return _port;
  }

  /**
   * IRQ input signal.
   */
  public InputPort getIRQ()
  {
    return _irq;
  }

  /**
   * NMI input signal.
   */
  public InputPort getNMI()
  {
    return _nmi;
  }

  @Interruptible
  public void run()
  {
    reset();

    final CPU6510State state = _state;
    while (true)
    {
      if (state.NMI)
      {
        state.NMI = false;
        interrupt(0xFFFA);
      }
      else if (state.IRQ && !state.I)
      {
        state.IRQ = false;
        interrupt(0xFFFE);
      }
      else
      {
        preExecute();
        int b = readBytePC();
        Opcode opcode = OPCODES[b];
        opcode.execute();
//        OPCODES[readBytePC()].execute();
      }
    }
  }

  private boolean trace = false;
  private int count = 1000;

  protected void preExecute()
  {
    if (_state.PC == 0x0000)
    {
      trace = true;
    }

    if (trace)
    {
      if (count-- == 0)
      {
        System.out.println("STOP");
      }
      if (_logger.isDebugEnabled())
      {
        if (_state.NMI || _state.IRQ && !_state.I)
        {
          _logger.debug(Monitor.state(_state));
        }
        else
        {
          _logger.debug(Monitor.state(_state));
          _logger.debug(Monitor.disassemble(_state.PC, _bus));
        }
      }
    }
  }

  private final Opcode[] OPCODES =
    {
      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $00: BRK (7)
        {
          _state.B = true;
          interrupt(0xFFFA);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $01: ORA ($XX,X) (6)
        {
          or(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $02: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $03: *SLO ($XX,X) (8)
        {
          shiftLeftOr(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $04: *NOP (3)
        {
          nop13();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $05: ORA $XX (3)
        {
          or(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $06: ASL $XX (5)
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $07: *SLO $XX (5)
        {
          shiftLeftOr(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $08: PHP (3)
        {
          pushByte(_state.getP());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $09: ORA #$XX (2)
        {
          or(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $0A: ASL (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.A = shiftLeft(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $0B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $0C: *NOP (4)
        {
          nop24();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $0D: ORA $XXXX (4)
        {
          or(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $0E: ASL $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $0F: *SLO $XXXX (6)
        {
          shiftLeftOr(readAbsoluteAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $10: BPL $XXXX (2/3)
        {
          branchIf(!_state.N);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $11: ORA ($XX),Y (5)
        {
          or(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $12: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $13: *SLO ($XX),Y (8)
        {
          // TODO 1 tick
          shiftLeftOr(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $14: *NOP (4)
        {
          nop14();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $15: ORA $XX,X (4)
        {
          or(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $16: ASL $XX,X (6)
        {
          // TODO 1 tick
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $17: *SLO $XX,X (6)
        {
          // TODO 1 tick
          shiftLeftOr(readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $18: CLC (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.C = false;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $19: ORA $XXXX,Y (4)
        {
          or(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $1A: *NOP (2)
        {
          nop2();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $1B: *SLO $XXXX,Y (7)
        {
          // TODO 1 tick
          shiftLeftOr(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $1C: *NOP (5)
        {
          nop25();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $1D: ORA $XXXX,X (4)
        {
          or(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $1E: ASL $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(shiftLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $1F: *SLO $XXXX,X (7)
        {
          // TODO 1 tick
          shiftLeftOr(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $20: JSR $XXXX (6) (TODO rework: see AAY64)
        {
          int addr = readAbsoluteAddressPC();
          int returnAddr = (_state.PC - 1) & 0xFFFF;
          _tick.waitForTick(); // internal operation
          pushWord(returnAddr);
          _state.PC = addr;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $21: AND ($XX,X) (6)
        {
          and(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $22: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $23: *RLA ($XX,X) (8)
        {
          rotateLeftAnd(readZeropageIndirectXAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $24: BIT $XX (4)
        {
          bit(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $25: AND $XX (3)
        {
          and(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $26: ROL $XX (5)
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $27: *RLA $XX (5)
        {
          rotateLeftAnd(readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $28: PLP (4)
        {
          _state.setP(popByte());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $29: AND #$XX (2)
        {
          and(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $2A: ROL (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.A = rotateLeft(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public void execute() // $2B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $2C: BIT $XXXX (4)
        {
          bit(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $2D: AND $XXXX (4)
        {
          and(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $2E: ROL $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $2F: *RLA $XXXX (6)
        {
          rotateLeftAnd(readAbsoluteAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $30: BMI $XXXX (2/3)
        {
          branchIf(_state.N);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $31: AND ($XX),Y (5)
        {
          and(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $32: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $33: *RLA ($XX),Y (8)
        {
          // TODO 1 tick
          rotateLeftAnd(readZeropageIndirectYAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $34: *NOP (4)
        {
          nop14();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $35:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $36: ROL $XX,X (6)
        {
          // TODO 1 tick
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $37: *RLA $XX,X (6)
        {
          rotateLeftAnd(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $38: SEC (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.C = true;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $39: AND $XXXX,Y (4)
        {
          and(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // §3A: *NOP (2)
        {
          nop2();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $3B: *RLA $XXXX,Y (7)
        {
          // TODO 1 tick
          rotateLeftAnd(readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $3C: *NOP (5)
        {
          nop25();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $3D: AND $XXXX,X (4)
        {
          and(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $3E: ROL $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(rotateLeft(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $3F: *RLA $XXXX,X (7)
        {
          // TODO 1 tick
          rotateLeftAnd(readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $40: RTI (6)
        {
          // TODO 2 ticks
          _state.setP(popByte());
          _state.PC = popWord();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $41: EOR ($XX,X) (6)
        {
          xor(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $42: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $43:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $44: *NOP (3)
        {
          nop13();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $45: EOR $XX (3)
        {
          xor(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $46: LSR $XX (5)
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $47:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $48: PHA (3)
        {
          pushByte(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $49: EOR #$XX (2)
        {
          xor(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $4A: LSR (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.A = shiftRight(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $4B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $4C: JMP $XXXX (3) (AAY64)
        {
          _state.PC = readAbsoluteAddressPC();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $4D: EOR $XXXX (4)
        {
          xor(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $4E: LSR $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $4F:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $50: BVC $XXXX (2/3)
        {
          branchIf(!_state.V);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $51: EOR ($XX),Y (5)
        {
          xor(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $52: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $53:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $54: *NOP (4)
        {
          nop14();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $55: EOR $XX,X (4)
        {
          xor(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $56: LSR $XX,X (6)
        {
          // TODO 1 tick
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $57:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $58: CLI (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.I = false;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $59: EOR $XXXX,Y (4)
        {
          xor(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $5A: *NOP (2)
        {
          nop2();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $5B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $5C: *NOP (5)
        {
          nop25();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $5D: EOR $XXXX,X (4)
        {
          xor(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $5E: LSR $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(shiftRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $5F:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $60: RTS (6)
        {
          // TODO 3 ticks
          _state.PC = (popWord() + 1) & 0xFFFF;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $61: ADC ($XX,X) (6)
        {
          add(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $62: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $63:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $64: *NOP (3)
        {
          nop13();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $65: ADC $XX (3)
        {
          add(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $66: ROR $XX (5)
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $67:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $68: PLA (4)
        {
          int a = popByte();
          _state.setZeroNegativeP(a);
          _state.A = a;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $69: ADC #$XX (2)
        {
          add(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $6A: ROR (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.A = rotateRight(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $6B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $6C: JMP ($XXXX) (5)
        {
          _state.PC = readIndirectAddress();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $6D: ADC $XXXX (4)
        {
          add(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $6E: ROR $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $6F:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $70: BVS $XXXX (2/3)
        {
          branchIf(_state.V);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $71: ADC ($XX),Y (4)
        {
          add(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $72: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $73:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $74: *NOP (4)
        {
          nop14();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $75: ADC $XX,X (4)
        {
          // TODO 1 tick
          add(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $76: ROR $XX,X (6)
        {
          // TODO 1 tick
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $77:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $78: SEI (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.I = true;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $79: ADC $XXXX,Y (5)
        {
          add(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $7A: *NOP (2)
        {
          nop2();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $7B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $7C: *NOP (5)2
        {
          nop25();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $7D: ADC $XXXX,X (4)
        {
          add(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $7E: ROR $XXXX,X (7)
        {
          // TODO 1 ticks
          int addr = readAbsoluteAddressPC(_state.X);
          write(rotateRight(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $7F:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $80: *NOP (2)
        {
          nop12();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $81: STA ($XX,X) (6)
        {
          write(_state.A, readZeropageIndirectXAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $82: *NOP (2)
        {
          nop12();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $83:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $84: STY $XX (3)
        {
          write(_state.Y, readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $85: STA $XX (3)
        {
          write(_state.A, readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $86: STX $XX (3)
        {
          write(_state.X, readAbsoluteZeropageAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $87:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $88: DEY (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.Y = decrement(_state.Y);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $89: *NOP (2)
        {
          nop12();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $8A: TXA (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.A = load(_state.X);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $8B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $8C: STY $XXXX (4)
        {
          write(_state.Y, readAbsoluteAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $8D: STA $XXXX (4)
        {
          write(_state.A, readAbsoluteAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $8E: STX $XXXX (4)
        {
          write(_state.X, readAbsoluteAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $8F:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $90: BCC $XXXX (2/3)
        {
          branchIf(!_state.C);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $91: STA ($XX),Y (6)
        {
          write(_state.A, readZeropageIndirectYAddressPC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $92: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $93:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $94: STY $XX,X (4)
        {
          // TODO 1 tick
          write(_state.Y, readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $95: STA $XX,X (4)
        {
          // TODO 1 tick
          write(_state.A, readAbsoluteZeropageAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $96: STX $XX,Y (4)
        {
          // TODO 1 tick
          write(_state.X, readAbsoluteZeropageAddressPC(_state.Y));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $97:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $98: TYA (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.A = load(_state.Y);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $99: STA $XXXX,Y (5)
        {
          // TODO 1 tick
          write(_state.A, readAbsoluteAddressPC(_state.Y));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $9A: TXS (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.S = STACK + _state.X; // no update of P !!!
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $9B:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $9C:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $9D: STA $XXXX,X (5)
        {
          write(_state.A, readAbsoluteAddressPC(_state.X));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $9E:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $9F:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A0: LDY #$XX (2)
        {
          _state.Y = load(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A1: LDA ($XX,X) (6)
        {
          _state.A = load(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A2: LDX #$XX (2)
        {
          _state.X = load(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A3:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A4: LDY $XX (3)
        {
          _state.Y = load(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A5: LDA $XX (3)
        {
          _state.A = load(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A6: LDX $XX (3)
        {
          _state.X = load(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A7:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A8: TAY (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.Y = load(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $A9: LDA #$XX (2)
        {
          _state.A = load(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $AA: TAX (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.X = load(_state.A);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $AB:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $AC: LDY $XXXX (4)
        {
          _state.Y = load(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $AD: LDA $XXXX (4)
        {
          _state.A = load(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $AE: LDX $XXXX (4)
        {
          _state.X = load(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $AF:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B0: BCS $XXXX (2/3)
        {
          branchIf(_state.C);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B1: LDA ($XX),Y (5)
        {
          _state.A = load(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B2: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B3:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B4: LDY $XX,X (4)
        {
          // TODO 1 tick
          _state.Y = load(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B5: LDA $XX,X (4)
        {
          // TODO 1 tick
          _state.A = load(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B6: LDX $XX,Y (4)
        {
          // TODO 1 tick
          _state.X = load(read(readAbsoluteZeropageAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B7:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B8: CLV (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.V = false;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $B9: LDA $XXXX,Y (4)
        {
          _state.A = load(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $BA: TSX (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.X = load(_state.S & 0xFF);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $BB:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $BC: LDY $XXXX,X (4)
        {
          _state.Y = load(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $BD: LDA $XXXX,X (4)
        {
          _state.A = load(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $BE: LDX $XXXX,Y (4)
        {
          _state.X = load(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $BF:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C0: CPY #$XX (2)
        {
          compare(_state.Y, readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C1: CMP ($XX,X) (6)
        {
          compare(_state.A, read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C2: *NOP (2)
        {
          nop12();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C3:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C4: CPY $XX (3)
        {
          compare(_state.Y, read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C5: CMP $XX (3)
        {
          compare(_state.A, read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C6: DEC $XX (5)
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C7:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C8: INY (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.Y = increment(_state.Y);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $C9: CMP #$XX (2)
        {
          compare(_state.A, readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $CA: DEX (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.X = decrement(_state.X);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $CB:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $CC: CPY $XXXX (4)
        {
          compare(_state.Y, read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $CD: CMP $XXXX (4)
        {
          compare(_state.A, read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $CE: DEC $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $CF:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D0: BNE $XXXX (2/3)
        {
          branchIf(!_state.Z);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D1: CMP ($XX),Y (5)
        {
          compare(_state.A, read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D2: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D3:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D4: *NOP (4)
        {
          nop14();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D5: CMP $XX,X (4)
        {
          // TODO 1 tick
          compare(_state.A, read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D6: DEC $XX,X (6)
        {
          // TODO 1 tick
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D7:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D8: CLD (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.D = false;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $D9: CMP $XXXX,Y (4)
        {
          compare(_state.A, read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $DA: *NOP (2)
        {
          nop2();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $DB:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $DC: *NOP (5)
        {
          nop25();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $DD: CMP $XXXX,X (4)
        {
          compare(_state.A, read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $DE: DEC $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(decrement(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $DF:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E0: CPX #$XX (2)
        {
          compare(_state.X, readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E1: SBC ($XX,X) (6)
        {
          subtract(read(readZeropageIndirectXAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E2: *NOP (2)
        {
          nop12();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E3:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E4: CPX $XX (3)
        {
          compare(_state.X, read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E5: SBC $XX (3)
        {
          subtract(read(readAbsoluteZeropageAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E6: INC $XX (5)
        {
          int addr = readAbsoluteZeropageAddressPC();
          write(increment(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E7:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E8: INX (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.X = increment(_state.X);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $E9: SBC #$XX (2)
        {
          subtract(readImmediatePC());
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $EA: NOP (2)
        {
          _tick.waitForTick();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $EB:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $EC: CPX $XXXX (4)
        {
          compare(_state.X, read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $ED: SBC $XXXX (4)
        {
          subtract(read(readAbsoluteAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $EE: INC $XXXX (6)
        {
          int addr = readAbsoluteAddressPC();
          write(increment(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $EF:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F0: BEQ $XXXX (2/3)
        {
          branchIf(_state.Z);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F1: SBC ($XX),Y (5)
        {
          subtract(read(readZeropageIndirectYAddressPC()));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F2: *KIL (*)
        {
          crash();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F3:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F4: *NOP (4)
        {
          nop14();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F5: SBC $XX,X (4)
        {
          subtract(read(readAbsoluteZeropageAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F6: INC $XX,X (6)
        {
          // TODO 1 ticks
          int addr = readAbsoluteZeropageAddressPC(_state.X);
          write(increment(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F7:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F8: SED (2)
        {
          _tick.waitForTick(); // minimum operation time: 1 tick
          _state.D = true;
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $F9: SBC $XXXX,Y (4)
        {
          subtract(read(readAbsoluteAddressPC(_state.Y)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $FA: *NOP (2)
        {
          nop2();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $FB:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $FC: *NOP (5)
        {
          nop25();
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $FD: SBC $XXXX,X (4)
        {
          subtract(read(readAbsoluteAddressPC(_state.X)));
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $FE: INC $XXXX,X (7)
        {
          // TODO 1 tick
          int addr = readAbsoluteAddressPC(_state.X);
          write(increment(read(addr)), addr);
        }
      },

      new Opcode()
      {
        @Override
        @Interruptible
        public final void execute() // $FF:
        {
          // TODO implement opcode
          notImplementedYet();
        }
      }
    };

  private void notImplementedYet()
  {
    if (DEBUG)
    {
      throw new UnsupportedOperationException();
    }
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
  protected final void interrupt(int addr)
  {
    // TODO ticks?
    int returnAddr = _state.PC;
    pushWord(returnAddr);
    pushByte(_state.getP());
    _state.PC = readAbsoluteAddress(addr);
  }

  /**
   * Shift left.
   *
   * @param value value
   */
  protected final int shiftLeft(int value)
  {
    int result = value << 1;
    _state.setCarryZeroNegativeP(result, (result & 0x100) != 0);
    return result & 0xFF;
  }

  /**
   * Shift right.
   *
   * @param value value
   */
  protected final int shiftRight(int value)
  {
    int result = value >> 1;
    _state.setCarryZeroNegativeP(result, (value & 0x01) != 0);
    return result;
  }

  /**
   * Rotate left.
   *
   * @param value value
   */
  protected final int rotateLeft(int value)
  {
    int result = value << 1;
    if (_state.C)
    {
      result |= 0x01;
    }
    _state.setCarryZeroNegativeP(result, (result & 0x100) != 0);
    return result & 0xFF;
  }

  /**
   * Rotate right.
   *
   * @param value value
   */
  protected final int rotateRight(int value)
  {
    int result = value >> 1;
    if (_state.C)
    {
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
  protected final void bit(int value)
  {
    _state.setZeroOverflowNegativeP(value, (_state.A & value) == 0);
  }

  /**
   * And value with A.
   *
   * @param value value
   */
  protected final void and(int value)
  {
    int a = _state.A & value;
    _state.setZeroNegativeP(a);
    _state.A = a;
  }

  /**
   * Or value with A.
   *
   * @param value value
   */
  protected final void or(int value)
  {
    int a = _state.A | value;
    _state.setZeroNegativeP(a);
    _state.A = a;
  }

  /**
   * Exclusive or value with A.
   *
   * @param value value
   */
  protected final void xor(int value)
  {
    int a = _state.A ^ value;
    _state.setZeroNegativeP(a);
    _state.A = a;
  }

  /**
   * Add value to A.
   *
   * @param value value
   */
  protected final void add(int value)
  {
    int a = _state.A + value;
    if (_state.C)
    {
      a++;
    }
    _state.setCarryZeroOverflowNegativeP(_state.A, a, a >= 0x100);
    _state.A = a & 0xFF;
  }

  /**
   * Subtract value from A.
   *
   * @param value value
   */
  protected final void subtract(int value)
  {
    int a = _state.A - value;
    if (!_state.C)
    {
      a--;
    }
    _state.setCarryZeroOverflowNegativeP(_state.A, a, a >= 0);
    _state.A = a & 0xFF;
  }

  /**
   * Load.
   *
   * @param value value
   */
  protected final int load(int value)
  {
    _state.setZeroNegativeP(value);
    return value;
  }

  /**
   * Decrement.
   *
   * @param value value
   */
  protected final int decrement(int value)
  {
    int result = (value - 1) & 0xFF;
    _state.setZeroNegativeP(result);
    return result;
  }

  /**
   * Increment.
   *
   * @param value value
   */
  protected final int increment(int value)
  {
    int result = (value + 1) & 0xFF;
    _state.setZeroNegativeP(result);
    return result;
  }

  /**
   * Compare value with value at addr.
   * (0)
   */
  protected final void compare(int value, int value2)
  {
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
  protected final void branchIf(boolean condition)
  {
    int addr = readRelativeAddressPC();
    if (condition)
    {
      _state.PC = addr;
      _tick.waitForTick();
    }
  }

  /**
   * Push byte onto stack.
   */
  @Interruptible
  protected final void pushByte(int value)
  {
    int s = _state.S;
    write(value, s);
    s--;
    if (s < STACK)
    {
      if (DEBUG)
      {
        throw new IllegalArgumentException("Stack overflow");
      }
      s = STACK + 0xFF; // TODO overflow OK?
    }
    _state.S = s;
  }

  /**
   * Push word onto stack.
   */
  @Interruptible
  protected final void pushWord(int value)
  {
    int s = _state.S;
    write(value >> 8, s);
    s--;
    if (s < STACK)
    {
      if (DEBUG)
      {
        throw new IllegalArgumentException("Stack overflow");
      }
      s = STACK + 0xFF; // TODO overflow OK?
    }
    write(value & 0xFF, s);
    s--;
    if (s < STACK)
    {
      if (DEBUG)
      {
        throw new IllegalArgumentException("Stack overflow");
      }
      s = STACK + 0xFF; // TODO overflow OK?
    }
    _state.S = s;
  }

  /**
   * Pop byte from stack.
   */
  @Interruptible
  protected final int popByte()
  {
    int s = _state.S + 1;
    if (s >= STACK + 0x0100)
    {
      if (DEBUG)
      {
        throw new IllegalArgumentException("Stack underflow");
      }
      s = STACK; // TODO underflow OK?
    }
    _state.S = s;
    return read(s);
  }

  /**
   * Pop word from stack.
   */
  @Interruptible
  protected final int popWord()
  {
    int s = _state.S + 1;
    if (s >= STACK + 0x0100)
    {
      if (DEBUG)
      {
        throw new IllegalArgumentException("Stack underflow");
      }
      s = STACK; // TODO underflow OK?
    }
    int result = read(s);
    s++;
    if (s >= STACK + 0x0100)
    {
      if (DEBUG)
      {
        throw new IllegalArgumentException("Stack underflow");
      }
      s = STACK; // TODO underflow OK?
    }
    _state.S = s;
    return result | read(s) << 8;
  }

  //
  // illegal commands
  //

  /**
   * Report illegal opcode.
   */
  protected final void reportIllegalOpcode()
  {
    if (_logger.isDebugEnabled())
    {
      _logger.debug(Monitor.state(_state));
      _logger.debug(Monitor.disassemble(_state.PC, _bus));
    }
    int pc = (_state.PC - 1) & 0xFFFF;
    throw new IllegalArgumentException("Illegal opcode " + HexUtil.hexByte(_bus.read(pc)) + " at " + HexUtil.hexWord(pc));
  }

  /**
   * Crash.
   */
  @Interruptible
  protected final void crash()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    while (true)
    {
      _tick.waitForTick();
    }
  }

  /**
   * NOP. 2 ticks.
   * (1)
   */
  @Interruptible
  protected final void nop2()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    _tick.waitForTick();
  }

  /**
   * NOP with 1 byte extra. 2 ticks.
   * (1)
   */
  @Interruptible
  protected final void nop12()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    _state.PC = (_state.PC + 1) & 0xFFFF; // TODO read?
    _tick.waitForTick();
  }

  /**
   * NOP with 1 byte extra. 3 ticks.
   * (2)
   */
  @Interruptible
  protected final void nop13()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    _state.PC = (_state.PC + 1) & 0xFFFF; // TODO read?
    _tick.waitForTick();
    _tick.waitForTick();
  }

  /**
   * NOP with 1 byte extra. 4 ticks.
   * (3)
   */
  @Interruptible
  protected final void nop14()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    _state.PC = (_state.PC + 1) & 0xFFFF; // TODO read?
    _tick.waitForTick();
    _tick.waitForTick();
    _tick.waitForTick();
  }

  /**
   * NOP with 2 bytes extra. 4 ticks.
   * (3)
   */
  @Interruptible
  protected final void nop24()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    _state.PC = (_state.PC + 2) & 0xFFFF; // TODO read?
    _tick.waitForTick();
    _tick.waitForTick();
    _tick.waitForTick();
  }

  /**
   * NOP with 2 bytes extra. 5 ticks.
   * (4)
   */
  @Interruptible
  protected final void nop25()
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    _state.PC = (_state.PC + 2) & 0xFFFF; // TODO read?
    _tick.waitForTick();
    _tick.waitForTick();
    _tick.waitForTick();
    _tick.waitForTick();
  }

  /**
   * *SLO: Shift left and or with A.
   * (3)
   *
   * @param addr address
   */
  @Interruptible
  protected final void shiftLeftOr(int addr)
  {
    if (DEBUG)
    {
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
   * *RLA: Rotate left and and with A, store result.
   * (3)
   *
   * @param addr address
   */
  @Interruptible
  public final void rotateLeftAnd(int addr)
  {
    if (DEBUG)
    {
      reportIllegalOpcode();
    }
    int value = (read(addr) << 1) | (_state.C ? 1 : 0);
    _tick.waitForTick(); // << 1 needs 1 tick
    _state.setCarryZeroNegativeP(value, (value & 0x100) != 0);
    write(value & 0xFF, addr);

    _state.A &= value & 0xFF;
    _state.setZeroNegativeP(_state.A);
  }

  //
  // addressing modes
  //

  /**
   * Read immediate at PC.
   * (1)
   */
  @Interruptible
  protected final int readImmediatePC()
  {
    return readBytePC();
  }

  /**
   * Read relative address at PC.
   * (1)
   */
  @Interruptible
  protected final int readRelativeAddressPC()
  {
    return ((byte) readBytePC() + _state.PC) & 0xFFFF;
  }

  /**
   * Read absolute zeropage address at PC.
   * (1)
   */
  @Interruptible
  protected final int readAbsoluteZeropageAddressPC()
  {
    return readBytePC();
  }

  /**
   * Read absolute zeropage address at PC indexed by index.
   * (1)
   */
  @Interruptible
  protected final int readAbsoluteZeropageAddressPC(int index)
  {
    return (readBytePC() + index) & 0xFF;
  }

  /**
   * Read absolute address at PC. (AAY64)
   * (2)
   */
  @Interruptible
  protected final int readAbsoluteAddressPC()
  {
    return readWordPC();
  }

  /**
   * Read absolute address at PC indexed by index.
   * (2)
   */
  @Interruptible
  protected final int readAbsoluteAddressPC(int index)
  {
    return (readWordPC() + index) & 0xFFFF;
  }

  /**
   * Read absolute address at addr. (AAY64)
   * (2)
   *
   * @param addr address
   */
  @Interruptible
  protected final int readAbsoluteAddress(int addr)
  {
    return read(addr) + (read((addr + 1) & 0xFFFF) << 8);
  }

  /**
   * Read address zeropage indexed by X indirect.
   * (4)
   */
  @Interruptible
  protected final int readZeropageIndirectXAddressPC()
  {
    _tick.waitForTick(); // + X needs 1 tick, TODO read address first?
    return readAbsoluteAddress((readAbsoluteZeropageAddressPC() + _state.X) & 0xFF);
  }

  /**
   * Read address zeropage indirect indexed by Y.
   * (3)
   */
  @Interruptible
  protected final int readZeropageIndirectYAddressPC()
  {
    return (readAbsoluteAddress(readAbsoluteZeropageAddressPC()) + _state.Y) & 0xFFFF;
  }

  /**
   * Read indirect address at PC.
   * (4)
   */
  @Interruptible
  protected final int readIndirectAddress()
  {
    return readAbsoluteAddress(readAbsoluteAddressPC());
  }

  //
  // read / write interface
  //

  /**
   * Read byte at PC. Increment PC.
   * (1)
   */
  @Interruptible
  protected final int readBytePC()
  {
    int pc = _state.PC;
    int result = read(pc++);
    _state.PC = pc & 0xFFFF;

    return result;
  }

  /**
   * Read word at PC. Increment PC.
   * (1)
   */
  @Interruptible
  protected final int readWordPC()
  {
    int pc = _state.PC;
    int result = read(pc++);
    pc = pc & 0xFFFF;
    result |= read(pc++) << 8;
    _state.PC = pc & 0xFFFF;

    return result;
  }

  /**
   * Write value (byte) to addr.
   * (1)
   */
  @Interruptible
  protected final void write(int value, int addr)
  {
    if (addr == 0)
    {
      _port.setOutputMask(value);
    }
    else if (addr == 1) 
    {
      _port.setOutputMask(value);
    }
    _bus.write(value, addr);

    _tick.waitForTick();
  }

  /**
   * Read byte from addr.
   * (1)
   *
   * @param addr address
   */
  @Interruptible
  protected final int read(int addr)
  {
    int result;
    if (addr == 0)
    {
      result = _port.outputMask();
    }
    else if (addr == 1)
    {
      result = _port.inputData();
    }
    else
    {
      result = _bus.read(addr);
    }

    _tick.waitForTick();

    return result;
  }

  //
  // External debugging support
  //

  /**
   * Get cpu state.
   */
  public synchronized CPU6510State getState()
  {
    return _state;
  }

  //
  // inner classes
  //

  /**
   * Interface for opcode implementations.
   */
  private static interface Opcode
  {
    @Interruptible
    public void execute();
  }


  //
  // private attributes
  //

  protected static final boolean DEBUG = true;

  // stack
  protected static final int STACK = 0x0100;

  final CPU6510State _state;

  final Tick _tick;
  private C64Bus _bus;
  private final InputOutputPortImpl _port;
  private final InputPort _irq;
  private final InputPort _nmi;

  /**
   * Logger.
   */
  private static final Logger _logger = Logger.getLogger(CPU6510.class);
}
