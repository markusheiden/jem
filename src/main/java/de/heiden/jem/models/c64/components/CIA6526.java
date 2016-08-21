package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.InputOutputPortImpl;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;

/**
 * CIA 6526.
 * <p/>
 * TODO PC handling for port a & b
 * TODO serial port handling!
 */
public class CIA6526 implements BusDevice {
  public static final int PB6 = 0x40;
  public static final int PB6_MASK = 0xFF - 0x40;
  public static final int PB7 = 0x80;
  public static final int PB7_MASK = 0xFF - 0x80;

  private static final int PORTS_CNT = 1 << 1;

  public static final int ICR_UNDERFLOW_TIMER_A = 0x01;
  public static final int ICR_UNDERFLOW_TIMER_B = 0x02;
  public static final int ICR_ALARM = 0x04;
  public static final int ICR_SDR = 0x08;
  public static final int ICR_FLAG = 0x10;
  public static final int ICR_IRQ = 0x80;

  public static final int CR_START = 0x01;
  public static final int CR_UNDERFLOW_PB = 0x02;
  public static final int CR_UNDERFLOW_PB_TOGGLE = 0x04;
  public static final int CR_ONE_SHOT = 0x08;
  public static final int CR_LOAD = 0x10;

  public static final int CR_MODE_O2 = 0x00;
  public static final int CR_MODE_CNT = 0x20;

  public static final int CRA_MODE_MASK = 0x20;
  public static final int CRA_SP_OUTPUT = 0x40;
  public static final int CRA_50HZ = 0x80;

  public static final int CRB_MODE_MASK = 0x60;
  public static final int CRB_MODE_TIMER_A = 0x40;
  public static final int CRB_MODE_TIMER_A_CNT = 0x60;
  public static final int CRB_SET_ALARM = 0x80;

  // system clock
  private final Clock _clock;
  private final ClockEvent _timerAUnderflowEvent;
  private final ClockEvent _timerBUnderflowEvent;

  // address mask
  private final int _mask;

  // Port A
  private int _controlA; // 0x0E
  private InputOutputPortImpl _portA;

  // Port B
  private int _controlB; // 0x0F
  private InputOutputPortImpl _portB;

  // serial shift reg
  private int _sdr; // 0x0C
  private InputOutputPortImpl _portSerial;

  // timer A
  private boolean _timerAIsRunning;
  private int _timerA; // LO: 0x04, HI 0x05
  private int _timerAInit; // LO: 0x04, HI 0x05
  private boolean _timerACLK; // increment timer A with clock?
  private long _timerABase; // base tick for count clocking
  private boolean _timerACNT; // increment timer A when raising edge at CNT?

  // timer B
  private boolean _timerBIsRunning;
  private int _timerB; // LO: 0x06, HI 0x07
  private int _timerBInit; // LO: 0x04, HI 0x05
  private boolean _timerBCLK; // increment timer B with clock?
  private long _timerBBase; // base tick for count clocking
  private boolean _timerBCNT; // increment timer B when raising edge at CNT?

  // real time
  private long _timeBase;
  private int _timeTenth; // 0x08
  private int _timeSec; // 0x09
  private int _timeMin; // 0x0A
  private int _timeHour; // 0x0B
  private boolean _timeIsRunning;

  private boolean _timeLock;
  private int _timeTenthTemp; // 0x08
  private int _timeSecTemp; // 0x09
  private int _timeMinTemp; // 0x0A
  private int _timeHourTemp; // 0x0B

  private int _alarmTenth; // 0x08
  private int _alarmSec; // 0x09
  private int _alarmMin; // 0x0A
  private int _alarmHour; // 0x0B

  // irq control
  private int _irq; // 0x0D
  private int _irqMask; // 0x0D

  private final OutputPortImpl _irqPort;

  /**
   * Constructor.
   *
   * @param clock system clock
   * @require clock != null
   */
  public CIA6526(Clock clock) {
    assert clock != null : "clock != null";

    _clock = clock;

    // event for timer a finished clock counting
    _timerAUnderflowEvent = new ClockEvent("Timer A underflow") {
      @Override
      public void execute(long tick) {
        timerAUnderflow();
        _timerABase = tick;
        if ((_controlA & CR_ONE_SHOT) == 0) {
          // continuous mode -> restart timer.
          _clock.addClockEvent(_timerABase + _timerA, _timerAUnderflowEvent);
        } else {
          // one shot -> stop timer.
          _timerACLK = false;
        }
      }
    };

    // event for timer b finished clock counting
    _timerBUnderflowEvent = new ClockEvent("Timer B underflow") {
      @Override
      public void execute(long tick) {
        timerBUnderflow();
        _timerBBase = tick;
        if ((_controlB & CR_ONE_SHOT) == 0) {
          // continuous mode -> restart timer.
          _clock.addClockEvent(_timerBBase + _timerB, _timerBUnderflowEvent);
        } else {
          // one shot -> stop timer.
          _timerBCLK = false;
        }
      }
    };

    // address mask
    _mask = 0x0F;

    _portA = new InputOutputPortImpl();
    // TODO add listener for events
    _portB = new InputOutputPortImpl();
    // TODO add listener for events

    _portSerial = new InputOutputPortImpl();
    _portSerial.addInputPortListener((value, mask) -> {
      // receiveSerial(value);
      if (_timerACNT) { // TODO check for raising edge of cnt
        countTimerA();
      }
      if (_timerBCNT) { // TODO check for raising edge of cnt
        countTimerA();
      }
    });

    _irqPort = new OutputPortImpl();
    _irqPort.setOutputMask(0x01);
    _irqPort.setOutputData(0x01);
  }

  /**
   * Reset.
   */
  public void reset() {
    // TODO
    _timerAIsRunning = false;

    _timerBIsRunning = false;

    _timeIsRunning = true;
    _timeLock = false;
  }

  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Port A.
   *
   * @ensure result != null
   */
  public InputOutputPort portA() {
    assert _portA != null : "result != null";
    return _portA;
  }

  /**
   * Port B.
   *
   * @ensure result != null
   */
  public InputOutputPort portB() {
    assert _portB != null : "result != null";
    return _portB;
  }

  /**
   * Serial port.
   *
   * @ensure result != null
   */
  public InputOutputPort portSerial() {
    assert _portSerial != null : "result != null";
    return _portSerial;
  }

  /**
   * IRQ output signal.
   */
  public OutputPort getIRQ() {
    return _irqPort;
  }

  /**
   * Address mask.
   *
   * @ensure result >= 0 && result < 0x10000
   */
  public int mask() {
    assert _mask >= 0 && _mask < 0x10000 : "result >= 0 && result < 0x10000";
    return _mask;
  }

  /**
   * Write byte to bus device.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0 && value < 0x100
   */
  @Override
  public void write(int value, int address) {
    assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

    switch (address & _mask) {
      case 0x00: {
        _portA.setOutputData(value);
        break;
      }
      case 0x01: {
        _portB.setOutputData(value);
        break;
      }
      case 0x02: {
        _portA.setOutputMask(value);
        break;
      }
      case 0x03: {
        _portB.setOutputMask(value);
        break;
      }
      case 0x04: {
        _timerAInit = _timerAInit & 0xFF00 | value;
        updateTimerAWrite();
        break;
      }
      case 0x05: {
        _timerAInit = _timerAInit & 0x00FF | (value << 8);
        updateTimerAWrite();
        break;
      }
      case 0x06: {
        _timerBInit = _timerBInit & 0xFF00 | value;
        updateTimerBWrite();
        break;
      }
      case 0x07: {
        _timerBInit = _timerBInit & 0x00FF | (value << 8);
        updateTimerBWrite();
        break;
      }
      case 0x08: {
        if ((_controlB & CRB_SET_ALARM) == 0) {
          _timeTenth = value & 0x0F;
          _timeIsRunning = true;
        } else {
          _alarmTenth = value & 0x0F;
        }
        break;
      }
      case 0x09: {
        if ((_controlB & CRB_SET_ALARM) == 0) {
          _timeSec = value & 0x7F;
        } else {
          _alarmSec = value & 0x7F;
        }
        break;
      }
      case 0x0A: {
        if ((_controlB & CRB_SET_ALARM) == 0) {
          _timeMin = value & 0x7F;
        } else {
          _alarmMin = value & 0x7F;
        }
        break;
      }
      case 0x0B: {
        if ((_controlB & CRB_SET_ALARM) == 0) {
          _timeHour = value & 0x9F;
          _timeIsRunning = false;
        } else {
          _alarmHour = value & 0x9F;
        }
        break;
      }
      case 0x0C: {
        _sdr = value;
        break;
      }
      case 0x0D: {
        if ((value & 0x80) == 0) {
          _irqMask = _irqMask & (0xFF - value);
        } else {
          _irqMask = _irqMask | (value & 0x7F);
        }
        break;
      }
      case 0x0E: {
        // load timer A
        if ((value & CR_LOAD) != 0) {
          _timerA = _timerAInit;
          updateTimerAWrite();
        }
        _controlA = value & (0xFF - CR_LOAD); // TODO correct?
        updateTimerAMode();
        updatePortSerial();
        break;
      }
      case 0x0F: {
        // load timer B
        if ((value & CR_LOAD) != 0) {
          _timerB = _timerBInit;
          updateTimerBWrite();
        }
        _controlB = value & (0xFF - CR_LOAD); // TODO correct?
        updateTimerBMode();
        break;
      }
      default: {
        // ignore
        break;
      }
    }
  }

  /**
   * Read byte from bus device.
   *
   * @param address address to read byte from
   * @ensure result >= 0 && result < 0x100
   */
  @Override
  public int read(int address) {
    int result;
    switch (address & _mask) {
      case 0x00: {
        result = _portA.outputData();
        break;
      }
      case 0x01: {
        result = _portB.outputData();
        break;
      }
      case 0x02: {
        result = _portA.outputMask();
        break;
      }
      case 0x03: {
        result = _portB.outputMask();
        break;
      }
      case 0x04: {
        updateTimerARead();
        result = _timerA & 0x00FF;
        break;
      }
      case 0x05: {
        updateTimerARead();
        result = _timerA >> 8;
        break;
      }
      case 0x06: {
        updateTimerBRead();
        result = _timerB & 0x00FF;
        break;
      }
      case 0x07: {
        updateTimerBRead();
        result = _timerB >> 8;
        break;
      }
      case 0x08: {
        updateTime();
        result = _timeLock ? _timeTenthTemp : _timeTenth;

        // unlock current time
        _timeLock = false;

        break;
      }
      case 0x09: {
        updateTime();
        result = _timeLock ? _timeSecTemp : _timeSec;
        break;
      }
      case 0x0A: {
        updateTime();
        result = _timeLock ? _timeMinTemp : _timeMin;
        break;
      }
      case 0x0B: {
        updateTime();
        result = _timeHour;

        // lock current time in temp registers
        _timeTenthTemp = _timeTenth;
        _timeSecTemp = _timeSec;
        _timeMinTemp = _timeMin;
        _timeHourTemp = _timeHour;
        _timeLock = true;

        break;
      }
      case 0x0C: {
        result = _sdr;
        break;
      }
      case 0x0D: {
        result = _irq;
        _irq = 0;
        _irqPort.setOutputData(0x1);
        break;
      }
      case 0x0E: {
        result = _controlA;
        updateTimerAMode();
        break;
      }
      case 0x0F: {
        result = _controlB;
        updateTimerBMode();
        break;
      }
      default: {
        result = 0xFF; // TODO correct?
        break;
      }
    }

    assert result >= 0 && result < 0x100 : "result >= 0 && result < 0x100";
    return result;
  }

  //
  // Port handling
  //

  /**
   * Update serial port.
   */
  protected void updatePortSerial() {
    // TODO implement
  }

  //
  // Timer handling
  //

  /**
   * Update timer A mode relevant settings.
   */
  protected void updateTimerAMode() {
    if ((_controlA & CR_START) != 0) {
      int mode = _controlA & CRA_MODE_MASK;
      if (mode == CR_MODE_O2) {
        // timer A enabled
        // start clock counting, if not already counting
        if (!_timerACLK && _timerA > 0) {
          // count ticks
          _timerABase = _clock.getTick();
          _clock.addClockEvent(_timerABase + _timerA, _timerAUnderflowEvent);
          _timerACLK = true;
        }
        _timerACNT = false;
      } else if (mode == CR_MODE_CNT) {
        // count CNT raising edges
        if (_timerACLK) {
          _clock.removeClockEvent(_timerAUnderflowEvent);
          _timerACLK = false;
        }
        _timerACNT = true;
      }
    } else {
      // timer A disabled
      // stop clock counting, if counting
      if (_timerACLK) {
        _clock.removeClockEvent(_timerAUnderflowEvent);
        _timerACLK = false;
      }
      _timerACNT = false;
    }
  }

  /**
   * Update timer A for read access.
   */
  public void updateTimerARead() {
    if (_timerACLK) {
      long tick = _clock.getTick();
      _timerA -= (int) (tick - _timerABase);
      _timerABase = tick;
    }
  }

  /**
   * Update timer A after write access.
   */
  public void updateTimerAWrite() {
    if (_timerACLK) {
      _clock.updateClockEvent(_clock.getTick() + _timerA, _timerAUnderflowEvent);
    }
  }

  /**
   * Count timer A.
   */
  public void countTimerA() {
    if (--_timerA == 0) {
      timerAUnderflow();
    }
  }

  /**
   * Timer A underflow.
   */
  protected void timerAUnderflow() {
    // automatic reload
    _timerA = _timerAInit;

    // irq
    _irq |= ICR_UNDERFLOW_TIMER_A;
    if ((_irqMask & ICR_UNDERFLOW_TIMER_A) != 0) {
      _irq |= ICR_IRQ;
      _irqPort.setOutputData(0x0);
    }

    // PB6 signalling
    if ((_controlA & CR_UNDERFLOW_PB) != 0) {
      if ((_controlA & CR_UNDERFLOW_PB_TOGGLE) != 0) {
        // toggle PB6
        _portB.setOutputData(_portB.outputData() ^ PB6);
      } else {
        // hi strobe
        _portB.setOutputData(_portB.outputData() | PB6);
        // TODO wait for 1 tick
        _portB.setOutputData(_portB.outputData() & PB6_MASK);
      }
    }
    // trigger timer B
    if ((_controlB & CRA_SP_OUTPUT) != 0) {
      // count or count if CNT == 1
      if ((_controlB & (1 << 5)) == 0 || (_portSerial.inputData() & PORTS_CNT) != 0) {
        countTimerB();
      }
    }
  }

  /**
   * Update timer B mode relevant settings.
   */
  public void updateTimerBMode() {
    if ((_controlB & 0x41) == 0x01) // Bit 0 set, Bit 6 cleared
    {
      if ((_controlB & 0x20) == 0) // Bit 5 cleared
      {
        // timer B enabled and not counting timer A underflows
        // count ticks
        if (!_timerBCLK && _timerB > 0) {
          _timerBBase = _clock.getTick();
          _clock.addClockEvent(_timerBBase + _timerB, _timerBUnderflowEvent);
          _timerBCLK = true;
          _timerBCNT = false;
        }
      } else {
        // count CNT raising edges
        if (_timerBCLK) {
          _clock.removeClockEvent(_timerBUnderflowEvent);
        }
        _timerBCLK = false;
        _timerBCNT = true;
      }
    } else {
      // timer B disabled or counting timer A underflows
      // count CNT raising edges
      if (_timerBCLK) {
        _clock.removeClockEvent(_timerBUnderflowEvent);
      }
      _timerBCLK = false;
      _timerBCNT = false;
    }
  }

  /**
   * Update timer B for read access.
   */
  public void updateTimerBRead() {
    if (_timerBCLK) {
      long tick = _clock.getTick();
      _timerB -= (int) (tick - _timerBBase);
      _timerBBase = tick;
    }
  }

  /**
   * Update timer B after write access.
   */
  public void updateTimerBWrite() {
    if (_timerBCLK) {
      _clock.updateClockEvent(_clock.getTick() + _timerB, _timerBUnderflowEvent);
    }
  }

  /**
   * Count timer B.
   */
  protected void countTimerB() {
    if (--_timerB == 0) {
      timerBUnderflow();
    }
  }

  protected void timerBUnderflow() {
    // automatic reload
    _timerB = _timerBInit;

    // irq
    _irq |= ICR_UNDERFLOW_TIMER_B;
    if ((_irqMask & ICR_UNDERFLOW_TIMER_B) != 0) {
      _irq |= ICR_IRQ;
      _irqPort.setOutputData(0x0);
    }

    if ((_controlB & CR_UNDERFLOW_PB) != 0) {
      // PB7 signalling
      if ((_controlB & CR_UNDERFLOW_PB_TOGGLE) != 0) {
        // toggle PB7
        _portB.setOutputData(_portB.outputData() ^ PB7);
      } else {
        // hi strobe
        _portB.setOutputData(_portB.outputData() | PB7);
        // TODO wait for 1 tick
        _portB.setOutputData(_portB.outputData() & PB7_MASK);
      }
    }
  }

  /**
   * Update time.
   */
  public void updateTime() {
    // TODO implement
  }
}
