package de.heiden.jem.models.c64.components.cia;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.InputOutputPortImpl;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import jakarta.annotation.Nonnull;

/**
 * CIA 6526.
 * <p>
 * TODO PC handling for port a & b
 * TODO serial port handling!
 */
public class CIA6526 implements BusDevice {
    public static final int PB6 = 0x40;
    public static final int PB6_MASK = 0xFF - PB6;
    public static final int PB7 = 0x80;
    public static final int PB7_MASK = 0xFF - PB7;

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
    public static final int CR_LOAD_MASK = 0xFF - CR_LOAD;

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
    private final Clock clock;
    private final ClockEvent timerALoadEvent;
    private final ClockEvent timerAUnderflowEvent;
    private final ClockEvent timerBLoadEvent;
    private final ClockEvent timerBUnderflowEvent;

    // address mask
    private final int mask;

    // Port A
    private int controlA; // 0x0E
    private InputOutputPortImpl portA;

    // Port B
    private int controlB; // 0x0F
    private InputOutputPortImpl portB;

    // serial shift reg
    private int sdr; // 0x0C
    private InputOutputPortImpl portSerial;

    // timer A
    private int timerA; // LO: 0x04, HI 0x05
    private int timerAInit; // LO: 0x04, HI 0x05
    private boolean timerACLK; // increment timer A with clock?
    private long timerABase; // base tick for count clocking
    private boolean timerACNT; // increment timer A when raising edge at CNT?

    // timer B
    private int timerB; // LO: 0x06, HI 0x07
    private int timerBInit; // LO: 0x04, HI 0x05
    private boolean timerBCLK; // increment timer B with clock?
    private long timerBBase; // base tick for count clocking
    private boolean timerBCNT; // increment timer B when raising edge at CNT?

    // real time
    private long timeBase;
    private int timeTenth; // 0x08
    private int timeSec; // 0x09
    private int timeMin; // 0x0A
    private int timeHour; // 0x0B
    private boolean timeIsRunning;

    private boolean timeLock;
    private int timeTenthTemp; // 0x08
    private int timeSecTemp; // 0x09
    private int timeMinTemp; // 0x0A
    private int timeHourTemp; // 0x0B

    private int alarmTenth; // 0x08
    private int alarmSec; // 0x09
    private int alarmMin; // 0x0A
    private int alarmHour; // 0x0B

    // irq control
    private int irq; // 0x0D
    private int irqMask; // 0x0D

    private final OutputPortImpl irqPort;

    /**
     * Constructor.
     *
     * @param clock
     *         system clock
     * @require clock != null
     */
    public CIA6526(@Nonnull Clock clock) {
        this.clock = clock;

        timerALoadEvent = new ClockEvent("Timer A load") {
            @Override
            public void execute(long tick) {
                timerA = timerAInit;
                updateTimerAWrite();
            }
        };

        // event for timer a finished clock counting
        timerAUnderflowEvent = new ClockEvent("Timer A underflow") {
            @Override
            public void execute(long tick) {
                timerAUnderflow();
                timerABase = tick;
                if ((controlA & CR_ONE_SHOT) == 0 && timerA > 0) {
                    // continuous mode -> restart timer.
                    CIA6526.this.clock.addClockEvent(timerABase + timerA, timerAUnderflowEvent);
                } else {
                    // one shot -> stop timer.
                    timerACLK = false;
                }
            }
        };

        timerBLoadEvent = new ClockEvent("Timer B load") {
            @Override
            public void execute(long tick) {
                timerB = timerBInit;
                updateTimerBWrite();
            }
        };

        // event for timer b finished clock counting
        timerBUnderflowEvent = new ClockEvent("Timer B underflow") {
            @Override
            public void execute(long tick) {
                timerBUnderflow();
                timerBBase = tick;
                if ((controlB & CR_ONE_SHOT) == 0 && timerB > 0) {
                    // continuous mode -> restart timer.
                    CIA6526.this.clock.addClockEvent(timerBBase + timerB, timerBUnderflowEvent);
                } else {
                    // one shot -> stop timer.
                    timerBCLK = false;
                }
            }
        };

        // address mask
        mask = 0x0F;

        portA = new InputOutputPortImpl();
        // TODO add listener for events
        portB = new InputOutputPortImpl();
        // TODO add listener for events

        portSerial = new InputOutputPortImpl();
        portSerial.addInputPortListener((value, mask) -> {
            // receiveSerial(value);
            if (timerACNT) { // TODO check for raising edge of cnt
                countTimerA();
            }
            if (timerBCNT) { // TODO check for raising edge of cnt
                countTimerA();
            }
        });

        irqPort = new OutputPortImpl();
        irqPort.setOutputMask(0x01);
        irqPort.setOutputData(0x01);
    }

    /**
     * Reset.
     */
    public void reset() {
        // TODO
        timeIsRunning = true;
        timeLock = false;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Port A.
     *
     * @ensure result != null
     */
    public @Nonnull InputOutputPort portA() {
        return portA;
    }

    /**
     * Port B.
     *
     * @ensure result != null
     */
    public @Nonnull InputOutputPort portB() {
        return portB;
    }

    /**
     * Serial port.
     *
     * @ensure result != null
     */
    public @Nonnull InputOutputPort portSerial() {
        return portSerial;
    }

    /**
     * IRQ output signal.
     */
    public OutputPort getIRQ() {
        return irqPort;
    }

    /**
     * Address mask.
     *
     * @ensure result >= 0 && result < 0x10000
     */
    public int mask() {
        assert mask >= 0 && mask < 0x10000 : "result >= 0 && result < 0x10000";
        return mask;
    }

    /**
     * Write the byte to the bus device.
     *
     * @param value
     *         byte to write
     * @param address
     *         address to write byte to
     * @require value >= 0 && value < 0x100
     */
    @Override
    public void write(int value, int address) {
        assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

        switch (address & mask) {
            case 0x00: {
                portA.setOutputData(value);
                break;
            }
            case 0x01: {
                portB.setOutputData(value);
                break;
            }
            case 0x02: {
                portA.setOutputMask(value);
                break;
            }
            case 0x03: {
                portB.setOutputMask(value);
                break;
            }
            case 0x04: {
                timerAInit = timerAInit & 0xFF00 | value;
                updateTimerAWrite();
                break;
            }
            case 0x05: {
                timerAInit = timerAInit & 0x00FF | (value << 8);
                if ((controlA & CR_START) == 0) {
                    timerA = timerAInit;
                }
                updateTimerAWrite();
                break;
            }
            case 0x06: {
                timerBInit = timerBInit & 0xFF00 | value;
                updateTimerBWrite();
                break;
            }
            case 0x07: {
                timerBInit = timerBInit & 0x00FF | (value << 8);
                if ((controlB & CR_START) == 0) {
                    timerB = timerBInit;
                }
                updateTimerBWrite();
                break;
            }
            case 0x08: {
                if ((controlB & CRB_SET_ALARM) == 0) {
                    timeTenth = value & 0x0F;
                    timeIsRunning = true;
                } else {
                    alarmTenth = value & 0x0F;
                }
                break;
            }
            case 0x09: {
                if ((controlB & CRB_SET_ALARM) == 0) {
                    timeSec = value & 0x7F;
                } else {
                    alarmSec = value & 0x7F;
                }
                break;
            }
            case 0x0A: {
                if ((controlB & CRB_SET_ALARM) == 0) {
                    timeMin = value & 0x7F;
                } else {
                    alarmMin = value & 0x7F;
                }
                break;
            }
            case 0x0B: {
                if ((controlB & CRB_SET_ALARM) == 0) {
                    timeHour = value & 0x9F;
                    timeIsRunning = false;
                } else {
                    alarmHour = value & 0x9F;
                }
                break;
            }
            case 0x0C: {
                sdr = value;
                break;
            }
            case 0x0D: {
                if ((value & 0x80) == 0) {
                    irqMask = irqMask & (0xFF - value);
                } else {
                    irqMask = irqMask | (value & 0x7F);
                }
                break;
            }
            case 0x0E: {
                // load timer A
                if ((value & CR_LOAD) != 0) {
                    clock.addClockEvent(clock.getTick() + 2, timerALoadEvent);
                }
                controlA = value & CR_LOAD_MASK; // TODO correct?
                updateTimerAMode();
                updatePortSerial();
                break;
            }
            case 0x0F: {
                // load timer B
                if ((value & CR_LOAD) != 0) {
                    clock.addClockEvent(clock.getTick() + 2, timerBLoadEvent);
                }
                controlB = value & CR_LOAD_MASK; // TODO correct?
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
     * Read a byte from the bus device.
     *
     * @param address
     *         address to read byte from
     * @ensure result >= 0 && result < 0x100
     */
    @Override
    public int read(int address) {
        int result;
        switch (address & mask) {
            case 0x00: {
                result = portA.outputData();
                break;
            }
            case 0x01: {
                result = portB.outputData();
                break;
            }
            case 0x02: {
                result = portA.outputMask();
                break;
            }
            case 0x03: {
                result = portB.outputMask();
                break;
            }
            case 0x04: {
                updateTimerARead();
                result = timerA & 0x00FF;
                break;
            }
            case 0x05: {
                updateTimerARead();
                result = timerA >> 8;
                break;
            }
            case 0x06: {
                updateTimerBRead();
                result = timerB & 0x00FF;
                break;
            }
            case 0x07: {
                updateTimerBRead();
                result = timerB >> 8;
                break;
            }
            case 0x08: {
                updateTime();
                result = timeLock ? timeTenthTemp : timeTenth;

                // unlock current time
                timeLock = false;

                break;
            }
            case 0x09: {
                updateTime();
                result = timeLock ? timeSecTemp : timeSec;
                break;
            }
            case 0x0A: {
                updateTime();
                result = timeLock ? timeMinTemp : timeMin;
                break;
            }
            case 0x0B: {
                updateTime();
                result = timeHour;

                // lock current time in temp registers
                timeTenthTemp = timeTenth;
                timeSecTemp = timeSec;
                timeMinTemp = timeMin;
                timeHourTemp = timeHour;
                timeLock = true;

                break;
            }
            case 0x0C: {
                result = sdr;
                break;
            }
            case 0x0D: {
                result = irq;
                irq = 0;
                irqPort.setOutputData(0x1);
                break;
            }
            case 0x0E: {
                result = controlA;
                updateTimerAMode();
                break;
            }
            case 0x0F: {
                result = controlB;
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
        if ((controlA & CR_START) != 0) {
            int mode = controlA & CRA_MODE_MASK;
            if (mode == CR_MODE_O2) {
                // timer A enabled
                // start clock counting, if not already counting
                startTimerACLK();
                timerACNT = false;
            } else if (mode == CR_MODE_CNT) {
                // count CNT raising edges
                stopTimerACLK();
                timerACNT = true;
            }
        } else {
            // timer A disabled
            // stop clock counting, if counting
            stopTimerACLK();
            timerACNT = false;
        }
    }

    /**
     * Start timer A CLK counting.
     */
    private void startTimerACLK() {
        if (!timerACLK && timerA > 0) {
            // Count ticks, starting at the next tick.
            timerABase = clock.getTick() + 2;
            clock.addClockEvent(timerABase + timerA, timerAUnderflowEvent);
            timerACLK = true;
        }
    }

    /**
     * Stop timer A CLK counting.
     */
    private void stopTimerACLK() {
        if (timerACLK) {
            clock.removeClockEvent(timerAUnderflowEvent);
            timerACLK = false;
        }
    }

    /**
     * Update timer A for read access.
     */
    public void updateTimerARead() {
        if (timerACLK) {
            long tick = clock.getTick();
            int diff = (int) (tick - timerABase);
            if (diff > 0) {
                timerA -= diff;
                timerABase = tick;
            }
        }
    }

    /**
     * Update timer A after write access.
     */
    public void updateTimerAWrite() {
        if (timerACLK) {
            if (timerA > 0) {
                clock.updateClockEvent(clock.getTick() + timerA, timerAUnderflowEvent);
            } else {
                stopTimerACLK();
            }
        }
    }

    /**
     * Count timer A.
     */
    public void countTimerA() {
        if (--timerA == 0) {
            timerAUnderflow();
        }
    }

    /**
     * Timer A underflow.
     */
    protected void timerAUnderflow() {
        // automatic reload
        timerA = timerAInit;

        // irq
        irq |= ICR_UNDERFLOW_TIMER_A;
        if ((irqMask & ICR_UNDERFLOW_TIMER_A) != 0) {
            irq |= ICR_IRQ;
            irqPort.setOutputData(0x0);
        }

        // PB6 signalling
        if ((controlA & CR_UNDERFLOW_PB) != 0) {
            if ((controlA & CR_UNDERFLOW_PB_TOGGLE) != 0) {
                // toggle PB6
                portB.setOutputData(portB.outputData() ^ PB6);
            } else {
                // hi strobe
                portB.setOutputData(portB.outputData() | PB6);
                // TODO wait for 1 tick
                portB.setOutputData(portB.outputData() & PB6_MASK);
            }
        }
        // trigger timer B
        if ((controlB & CRA_SP_OUTPUT) != 0) {
            // count or count if CNT == 1
            if ((controlB & (1 << 5)) == 0 || (portSerial.inputData() & PORTS_CNT) != 0) {
                countTimerB();
            }
        }
    }

    /**
     * Update timer B mode relevant settings.
     */
    public void updateTimerBMode() {
        if ((controlB & 0x41) == 0x01) // Bit 0 set, Bit 6 cleared
        {
            if ((controlB & 0x20) == 0) // Bit 5 cleared
            {
                // timer B enabled and not counting timer A underflows.
                // Count ticks, starting at the next tick.
                startTimerBCLK();
                timerBCNT = false;
            } else {
                // count CNT raising edges
                stopTimerBCLK();
                timerBCNT = true;
            }
        } else {
            // timer B disabled or counting timer A underflows.
            // count CNT raising edges.
            stopTimerBCLK();
            timerBCNT = false;
        }
    }

    /**
     * Start timer B CLK counting.
     */
    private void startTimerBCLK() {
        if (!timerBCLK && timerB > 0) {
            // Count ticks starting at the next tick,
            timerBBase = clock.getTick() + 2;
            clock.addClockEvent(timerBBase + timerB, timerBUnderflowEvent);
            timerBCLK = true;
        }
    }

    /**
     * Stop timer B CLK counting.
     */
    private void stopTimerBCLK() {
        if (timerBCLK) {
            clock.removeClockEvent(timerBUnderflowEvent);
            timerBCLK = false;
        }
    }

    /**
     * Update timer B for read access.
     */
    public void updateTimerBRead() {
        if (timerBCLK) {
            long tick = clock.getTick();
            int diff = (int) (tick - timerBBase);
            if (diff > 0) {
                timerB -= diff;
                timerBBase = tick;
            }
        }
    }

    /**
     * Update timer B after write access.
     */
    public void updateTimerBWrite() {
        if (timerBCLK) {
            if (timerB > 0) {
                clock.updateClockEvent(clock.getTick() + timerB, timerBUnderflowEvent);
            } else {
                stopTimerBCLK();
            }
        }
    }

    /**
     * Count timer B.
     */
    protected void countTimerB() {
        if (--timerB == 0) {
            timerBUnderflow();
        }
    }

    protected void timerBUnderflow() {
        // automatic reload
        timerB = timerBInit;

        // irq
        irq |= ICR_UNDERFLOW_TIMER_B;
        if ((irqMask & ICR_UNDERFLOW_TIMER_B) != 0) {
            irq |= ICR_IRQ;
            irqPort.setOutputData(0x0);
        }

        if ((controlB & CR_UNDERFLOW_PB) != 0) {
            // PB7 signalling
            if ((controlB & CR_UNDERFLOW_PB_TOGGLE) != 0) {
                // toggle PB7
                portB.setOutputData(portB.outputData() ^ PB7);
            } else {
                // hi strobe
                portB.setOutputData(portB.outputData() | PB7);
                // TODO wait for 1 tick
                portB.setOutputData(portB.outputData() & PB7_MASK);
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
