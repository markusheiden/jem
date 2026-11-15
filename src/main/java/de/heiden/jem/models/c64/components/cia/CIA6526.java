package de.heiden.jem.models.c64.components.cia;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.InputOutputPortImpl;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import jakarta.annotation.Nonnull;
import org.serialthreads.Interruptible;

import java.util.Arrays;

/**
 * CIA 6526 (old CIA), executed cycle by cycle after the CPU:
 * A CPU read sees the state at the end of the previous cycle, a CPU write is processed in the same cycle.
 * <p>
 * The internal delays of the 6526 are modeled as pipelines of bits, which are shifted each cycle:
 * The first decrement happens 2 cycles after a timer has been started,
 * a forced load happens 1 cycle after it has been requested,
 * the IRQ follows an interrupt flag 1 cycle later.
 */
public class CIA6526 implements BusDevice, ClockedComponent {
    public static final int ICR_UNDERFLOW_TIMER_A = 0x01;
    public static final int ICR_UNDERFLOW_TIMER_B = 0x02;
    public static final int ICR_ALARM = 0x04;
    public static final int ICR_SDR = 0x08;
    public static final int ICR_FLAG = 0x10;
    public static final int ICR_IRQ = 0x80;
    private static final int ICR_FLAGS = 0x1F;

    public static final int CR_START = 0x01;
    public static final int CR_PB_ON = 0x02;
    public static final int CR_PB_TOGGLE = 0x04;
    public static final int CR_ONE_SHOT = 0x08;
    public static final int CR_LOAD = 0x10;
    public static final int CR_COUNT_CNT = 0x20;
    public static final int CRA_SERIAL_OUT = 0x40;
    public static final int CRA_TOD_50HZ = 0x80;

    public static final int CRB_MODE_MASK = 0x60;
    public static final int CRB_MODE_CNT = 0x20;
    public static final int CRB_MODE_TIMER_A = 0x40;
    public static final int CRB_MODE_TIMER_A_CNT = 0x60;
    public static final int CRB_SET_ALARM = 0x80;

    /**
     * Serial port: Bits of SP and CNT.
     */
    public static final int SERIAL_SP = 0x01;
    public static final int SERIAL_CNT = 0x02;

    /**
     * Cycles per tick of the TOD input: The 50 Hz of the power line of a PAL C64 (985248 Hz).
     */
    private static final int TOD_CYCLES = 985248 / 50;

    /**
     * Address mask.
     */
    private static final int MASK = 0x0F;

    /**
     * Clock tick.
     */
    private Tick tick;

    /**
     * Ports.
     */
    private final InputOutputPortImpl portA = new InputOutputPortImpl();
    private final InputOutputPortImpl portB = new InputOutputPortImpl();
    private final InputOutputPortImpl portSerial = new InputOutputPortImpl();

    /**
     * Serial data register.
     */
    private int sdr; // 0x0C

    /**
     * Serial shift register.
     */
    private int shift = 0;

    /**
     * Output mode: Remaining timer A underflows (2 per bit) of the current transfer, 0 if idle.
     * Input mode: Number of bits shifted in.
     */
    private int shiftCount = 0;

    /**
     * Output mode: Has the serial data register been written during a transfer?
     */
    private boolean sdrPending = false;

    /**
     * Level of the CNT input.
     */
    private boolean cnt = true;

    /**
     * Timers.
     */
    private final Timer timerA = new Timer(CIA6526.ICR_UNDERFLOW_TIMER_A, 0x40);
    private final Timer timerB = new Timer(CIA6526.ICR_UNDERFLOW_TIMER_B, 0x80);

    /**
     * Time of day: Tenths, seconds, minutes, hours (0x08-0x0B). At power up the time is 01:00:00.0.
     */
    private final int[] tod = {0x00, 0x00, 0x00, 0x01};

    /**
     * Alarm: Tenths, seconds, minutes, hours.
     */
    private final int[] alarm = new int[4];

    /**
     * Time of day latched by a read of the hours, until the tenths are read.
     */
    private final int[] todLatch = new int[4];

    /**
     * Has the time of day been latched?
     */
    private boolean todLatched = false;

    /**
     * Has the time of day been stopped by a write to the hours?
     * At power up the time of day is stopped until the tenths are written.
     */
    private boolean todStopped = true;

    /**
     * Counter of the ticks of the TOD input.
     */
    private int todTicks = 0;

    /**
     * Counter of the cycles until the next tick of the TOD input.
     */
    private int todCycles = 0;

    /**
     * Interrupt control: Flags and mask.
     */
    private int icr; // 0x0D
    private int icrMask; // 0x0D

    /**
     * Interrupt flags of the last cycle, which are enabled.
     * The IRQ follows them one cycle later.
     */
    private int irqSource = 0;

    /**
     * Serial interrupt of the current cycle, whose IRQ follows without delay.
     */
    private int irqImmediate = 0;

    /**
     * Interrupt flags set in the current cycle.
     */
    private int newFlags = 0;

    /**
     * Has the interrupt control register been read in the current cycle?
     */
    private boolean icrRead = false;

    /**
     * Has the interrupt control register been written in the current cycle?
     */
    private boolean icrWritten = false;

    /**
     * Is the IRQ output active?
     */
    private boolean irq = false;

    /**
     * IRQ output, low active.
     */
    private final OutputPortImpl irqPort = new OutputPortImpl();

    /**
     * Constructor.
     *
     * @param clock
     *         clock
     * @param position
     *         position of the CIA in the clock, after the CPU
     */
    public CIA6526(@Nonnull Clock clock, int position) {
        irqPort.setOutputMask(0x01);
        irqPort.setOutputData(0x01);

        portSerial.addInputPortListener((value, mask) -> {
            var high = (value & SERIAL_CNT) != 0;
            if (high && !cnt) {
                cntRisingEdge();
            }
            cnt = high;
        });

        clock.addClockedComponent(position, this);
    }

    public void reset() {
        todLatched = false;
        todStopped = true;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void setTick(Tick tick) {
        this.tick = tick;
    }

    @Override
    @Interruptible
    public final void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            cycle();
            tick.waitForTick();
        }
    }

    public @Nonnull InputOutputPort portA() {
        return portA;
    }

    public @Nonnull InputOutputPort portB() {
        return portB;
    }

    public @Nonnull InputOutputPort portSerial() {
        return portSerial;
    }

    /**
     * IRQ output signal.
     */
    public OutputPort getIRQ() {
        return irqPort;
    }

    @Override
    public void write(int value, int address) {
        assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

        switch (address & MASK) {
            case 0x00 -> portA.setOutputData(value);
            case 0x01 -> portB.setOutputData(value);
            case 0x02 -> portA.setOutputMask(value);
            case 0x03 -> portB.setOutputMask(value);
            case 0x04 -> timerA.writeLatchLow(value);
            case 0x05 -> timerA.writeLatchHigh(value);
            case 0x06 -> timerB.writeLatchLow(value);
            case 0x07 -> timerB.writeLatchHigh(value);
            case 0x08 -> writeTod(0, value & 0x0F);
            case 0x09 -> writeTod(1, value & 0x7F);
            case 0x0A -> writeTod(2, value & 0x7F);
            case 0x0B -> writeTod(3, value & 0x9F);
            case 0x0C -> writeSdr(value);
            case 0x0D -> {
                icrWritten = true;
                if ((value & 0x80) == 0) {
                    icrMask &= ~value & ICR_FLAGS;
                } else {
                    icrMask |= value & ICR_FLAGS;
                }
            }
            case 0x0E -> {
                if (((value ^ timerA.control) & CRA_SERIAL_OUT) != 0) {
                    changeSerialMode(value);
                }
                timerA.writeControl(value);
            }
            case 0x0F -> timerB.writeControl(value);
            default -> {
                // ignore
            }
        }
    }

    @Override
    public int read(int address) {
        var result = switch (address & MASK) {
            case 0x00 -> portA.pinData();
            case 0x01 -> timerB.pbOutput(timerA.pbOutput(portB.pinData()));
            case 0x02 -> portA.outputMask();
            case 0x03 -> portB.outputMask();
            case 0x04 -> timerA.counter & 0xFF;
            case 0x05 -> timerA.counter >> 8;
            case 0x06 -> timerB.counter & 0xFF;
            case 0x07 -> timerB.counter >> 8;
            case 0x08, 0x09, 0x0A, 0x0B -> readTod(address & 0x03);
            case 0x0C -> sdr;
            case 0x0D -> readICR();
            case 0x0E -> timerA.control;
            case 0x0F -> timerB.control;
            default -> 0xFF;
        };
        assert result >= 0 && result < 0x100 : "result >= 0 && result < 0x100";
        return result;
    }

    /**
     * Write the serial data register. In output mode this starts a transfer, if no transfer is running.
     */
    private void writeSdr(int value) {
        sdr = value;
        if ((timerA.control & CRA_SERIAL_OUT) == 0) {
            return;
        }
        if (shiftCount == 0) {
            shift = value;
            shiftCount = 16;
        } else {
            sdrPending = true;
        }
    }

    /**
     * Change of the serial port mode: Aborts a running transfer.
     * In output mode SP and CNT are driven by the CIA, idle CNT is high.
     *
     * @param control
     *         new value of the control register A
     */
    private void changeSerialMode(int control) {
        shiftCount = 0;
        sdrPending = false;
        if ((control & CRA_SERIAL_OUT) != 0) {
            portSerial.setOutputData(0xFF);
            portSerial.setOutputMask(SERIAL_SP | SERIAL_CNT);
        } else {
            portSerial.setOutputMask(0x00);
            portSerial.setOutputData(0xFF);
        }
    }

    /**
     * Output mode: Timer A underflow, which toggles CNT.
     * CNT goes low and the next bit is put to SP, then CNT goes high again.
     * After the 8th bit the serial interrupt is set and a pending byte is transferred.
     */
    private void shiftOut() {
        if (shiftCount == 0) {
            return;
        }
        shiftCount--;
        if ((shiftCount & 0x01) != 0) {
            var sp = (shift & 0x80) != 0 ? SERIAL_SP : 0;
            shift = shift << 1 & 0xFF;
            portSerial.setOutputData(0xFF & ~SERIAL_CNT & ~SERIAL_SP | sp);
        } else {
            portSerial.setOutputData(portSerial.outputData() | SERIAL_CNT);
        }

        if (shiftCount == 0) {
            // The IRQ of the serial interrupt follows without delay.
            setInterrupt(ICR_SDR);
            irqImmediate = ICR_SDR;
            if (sdrPending) {
                sdrPending = false;
                shift = sdr;
                shiftCount = 16;
            }
        }
    }

    /**
     * Rising edge of the CNT input: Counts for the timers and shifts SP in, in input mode.
     * After the 8th bit the shift register is copied to the serial data register and the serial interrupt is set.
     */
    private void cntRisingEdge() {
        if ((timerA.control & CR_COUNT_CNT) != 0) {
            timerA.count();
        }
        if ((timerB.control & CRB_MODE_MASK) == CRB_MODE_CNT) {
            timerB.count();
        }

        if ((timerA.control & CRA_SERIAL_OUT) == 0) {
            shift = (shift << 1 | portSerial.inputData() & SERIAL_SP) & 0xFF;
            if (++shiftCount == 8) {
                shiftCount = 0;
                sdr = shift;
                setInterrupt(ICR_SDR);
            }
        }
    }

    /**
     * Write a time of day register or, if selected by CRB, an alarm register.
     *
     * @param index
     *         index of the register: 0 = tenths, 1 = seconds, 2 = minutes, 3 = hours
     * @param value
     *         value without undefined bits
     */
    private void writeTod(int index, int value) {
        var setAlarm = (timerB.control & CRB_SET_ALARM) != 0;
        if (!setAlarm) {
            if (index == 3) {
                // Writing hour 12 flips AM/PM.
                if ((value & 0x1F) == 0x12) {
                    value ^= 0x80;
                }
                // Writing the hours stops the time of day.
                todStopped = true;
            } else if (index == 0) {
                // Writing the tenths (re)starts the time of day. A restart resets the counter of the TOD input.
                if (todStopped) {
                    todTicks = 0;
                }
                todStopped = false;
            }
        }

        var registers = setAlarm ? alarm : tod;
        if (registers[index] != value) {
            registers[index] = value;
            checkAlarm();
        }
    }

    /**
     * Read a time of day register.
     * Reading the hours latches the time of day, reading the tenths releases it.
     *
     * @param index
     *         index of the register: 0 = tenths, 1 = seconds, 2 = minutes, 3 = hours
     */
    private int readTod(int index) {
        if (!todLatched) {
            System.arraycopy(tod, 0, todLatch, 0, tod.length);
        }
        if (index == 0) {
            todLatched = false;
        } else if (index == 3) {
            todLatched = true;
        }
        return todLatch[index];
    }

    /**
     * Tick of the TOD input: Counts the time of day every 5 (50 Hz) or 6 (60 Hz) ticks.
     */
    private void todTick() {
        if (todStopped) {
            return;
        }
        todTicks = (todTicks + 1) & 0x07;
        if (todTicks != ((timerA.control & CRA_TOD_50HZ) != 0 ? 5 : 6)) {
            return;
        }
        todTicks = 0;

        // The digits are binary counters, which carry over by comparison of their values.
        var tenths = (tod[0] + 1) & 0x0F;
        var secLow = tod[1] & 0x0F;
        var secHigh = tod[1] >> 4;
        var minLow = tod[2] & 0x0F;
        var minHigh = tod[2] >> 4;
        var hourLow = tod[3] & 0x0F;
        var hourHigh = tod[3] >> 4 & 0x01;
        var pm = tod[3] & 0x80;
        if (tenths == 10) {
            tenths = 0;
            secLow = (secLow + 1) & 0x0F;
            if (secLow == 10) {
                secLow = 0;
                secHigh = (secHigh + 1) & 0x07;
                if (secHigh == 6) {
                    secHigh = 0;
                    minLow = (minLow + 1) & 0x0F;
                    if (minLow == 10) {
                        minLow = 0;
                        minHigh = (minHigh + 1) & 0x07;
                        if (minHigh == 6) {
                            minHigh = 0;
                            hourLow = (hourLow + 1) & 0x0F;
                            if (hourHigh != 0) {
                                if (hourLow == 2) {
                                    // From 11 to 12 AM/PM flips.
                                    pm ^= 0x80;
                                } else if (hourLow == 3) {
                                    // From 12 to 1.
                                    hourLow = 1;
                                    hourHigh = 0;
                                }
                            } else if (hourLow == 10) {
                                hourLow = 0;
                                hourHigh = 1;
                            }
                        }
                    }
                }
            }
        }
        tod[0] = tenths;
        tod[1] = secHigh << 4 | secLow;
        tod[2] = minHigh << 4 | minLow;
        tod[3] = pm | hourHigh << 4 | hourLow;

        checkAlarm();
    }

    /**
     * Set the alarm interrupt flag, if the time of day equals the alarm.
     */
    private void checkAlarm() {
        if (Arrays.equals(tod, alarm)) {
            setInterrupt(ICR_ALARM);
        }
    }

    /**
     * Read the interrupt control register: Clears all flags and releases the IRQ.
     */
    private int readICR() {
        var result = icr;
        icr = 0;
        icrRead = true;
        setIrq(false);
        return result;
    }

    /**
     * Execute one cycle.
     */
    private void cycle() {
        if (++todCycles == TOD_CYCLES) {
            todCycles = 0;
            todTick();
        }

        var underflowA = timerA.cycle();
        var underflowB = timerB.cycle();
        if (underflowA) {
            // Timer B counts underflows of timer A one cycle later, optionally only while CNT is high.
            var modeB = timerB.control & CRB_MODE_MASK;
            if (modeB == CRB_MODE_TIMER_A || modeB == CRB_MODE_TIMER_A_CNT && cnt) {
                timerB.count();
            }
            if ((timerA.control & CRA_SERIAL_OUT) != 0) {
                shiftOut();
            }
            setInterrupt(ICR_UNDERFLOW_TIMER_A);
        }
        var lost = 0;
        if (underflowB) {
            if (!icrRead) {
                setInterrupt(ICR_UNDERFLOW_TIMER_B);
            } else {
                // Timer B bug of the old CIA: If the ICR is read in the cycle of the underflow,
                // the interrupt flag is lost, but the IRQ still occurs.
                lost = ICR_UNDERFLOW_TIMER_B;
            }
        }

        // The IRQ bit is set by the enabled interrupt flags of the last cycle.
        // So it is set again by flags, which have just been cleared by a read, but a read in the same cycle
        // suppresses the IRQ output. Such an IRQ bit without IRQ output is cleared, when no flags are left.
        if ((irqSource | irqImmediate & icrMask) != 0) {
            icr |= ICR_IRQ;
            if (!icrRead) {
                setIrq(true);
            }
        } else if ((icr & ICR_FLAGS) == 0 && !irq) {
            icr = 0;
        }
        // A write to the ICR suppresses the IRQ of the interrupt flags set in the same cycle.
        irqSource = (icr | lost) & icrMask & ~(icrWritten ? newFlags | lost : 0);
        irqImmediate = 0;
        newFlags = 0;
        icrRead = false;
        icrWritten = false;
    }

    /**
     * Set the IRQ output.
     *
     * @param irq
     *         IRQ active?
     */
    private void setIrq(boolean irq) {
        if (this.irq != irq) {
            this.irq = irq;
            irqPort.setOutputData(irq ? 0x00 : 0x01);
        }
    }

    /**
     * Set interrupt flag. If it is enabled, the IRQ follows in the next cycle.
     */
    private void setInterrupt(int flag) {
        icr |= flag;
        newFlags |= flag;
    }

    /**
     * Timer of the CIA.
     */
    private final class Timer {
        /**
         * Pipeline: Count. The counter is decremented in stage 1 and checked for underflow in stage 0.
         */
        private static final int COUNT0 = 1 << 0;
        private static final int COUNT1 = 1 << 1;

        /**
         * Pipeline: Load the counter from the latch in stage 1.
         */
        private static final int LOAD0 = 1 << 4;
        private static final int LOAD1 = 1 << 5;

        /**
         * Pipeline: One shot mode.
         */
        private static final int ONE_SHOT0 = 1 << 8;

        /**
         * Shifting keeps the stages but the first ones, which come from the feed or from register writes.
         */
        private static final int DELAY_MASK = COUNT1 | LOAD1;

        /**
         * Interrupt flag of this timer.
         */
        private final int interruptFlag;

        /**
         * Bit of port B for the timer output.
         */
        private final int pbBit;

        /**
         * Control register.
         */
        private int control = 0;

        /**
         * Counter.
         */
        private int counter = 0;

        /**
         * Latch.
         */
        private int latch = 0xFFFF;

        /**
         * Pipeline.
         */
        private int delay = 0;

        /**
         * Bits fed into the pipeline each cycle.
         */
        private int feed = 0;

        /**
         * Timer output in toggle mode.
         */
        private boolean toggle = false;

        /**
         * Timer output in pulse mode: High for the cycle after an underflow.
         */
        private boolean pulse = false;

        /**
         * Has the timer underflowed in the last cycle?
         * In the cycle after an underflow, the counter still follows the latch.
         */
        private boolean underflowed = false;

        /**
         * Constructor.
         *
         * @param interruptFlag
         *         interrupt flag of this timer
         * @param pbBit
         *         bit of port B for the timer output
         */
        private Timer(int interruptFlag, int pbBit) {
            this.interruptFlag = interruptFlag;
            this.pbBit = pbBit;
        }

        private void writeLatchLow(int value) {
            latch = latch & 0xFF00 | value;
            if (underflowed) {
                counter = latch;
            }
        }

        private void writeLatchHigh(int value) {
            latch = latch & 0x00FF | value << 8;
            if (underflowed) {
                counter = latch;
            }
            // A stopped timer is loaded, when the high byte of the latch is written.
            if ((control & CR_START) == 0) {
                delay |= LOAD0;
            }
        }

        private void writeControl(int value) {
            if ((value & CR_START) != 0 && (control & CR_START) == 0) {
                // The toggle output is set, when the timer is started.
                toggle = true;
            }
            if ((value & CR_LOAD) != 0) {
                delay |= LOAD0;
            }
            control = value & ~CR_LOAD;
            updateFeed();
        }

        /**
         * Update the bits fed into the pipeline: Count each cycle, if started and counting φ2.
         */
        private void updateFeed() {
            var countsClock = (control & (interruptFlag == ICR_UNDERFLOW_TIMER_A ? CR_COUNT_CNT : CRB_MODE_MASK)) == 0;
            feed = (control & CR_START) != 0 && countsClock ? COUNT0 : 0;
            if ((control & CR_ONE_SHOT) != 0) {
                feed |= ONE_SHOT0;
            }
        }

        /**
         * Count one external event, e.g. an underflow of timer A.
         */
        private void count() {
            if ((control & CR_START) != 0) {
                delay |= COUNT0;
            }
        }

        /**
         * Execute one cycle.
         *
         * @return whether the timer underflowed
         */
        private boolean cycle() {
            pulse = false;
            var current = delay;
            if ((current & COUNT1) != 0) {
                counter = (counter - 1) & 0xFFFF;
            }

            var underflow = counter == 0 && (current & COUNT0) != 0;
            underflowed = underflow;
            if (underflow) {
                if (((current | feed) & ONE_SHOT0) != 0) {
                    // One shot: Stop the timer.
                    control &= ~CR_START;
                    feed &= ~COUNT0;
                    delay &= ~COUNT0;
                }
                delay |= LOAD1;
                toggle = !toggle;
                pulse = true;
            }

            if ((delay & LOAD1) != 0) {
                counter = latch;
                // No decrement in the cycle after a load.
                delay &= ~COUNT0;
            }

            delay = (delay << 1 & DELAY_MASK) | feed;
            return underflow;
        }

        /**
         * Apply the timer output to port B, if enabled.
         */
        private int pbOutput(int value) {
            if ((control & CR_PB_ON) == 0) {
                return value;
            }
            var high = (control & CR_PB_TOGGLE) != 0 ? toggle : pulse;
            return high ? value | pbBit : value & ~pbBit;
        }
    }
}
