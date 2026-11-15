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

/**
 * CIA 6526 (old CIA), executed cycle by cycle after the CPU:
 * A CPU read sees the state at the end of the previous cycle, a CPU write is processed in the same cycle.
 * <p>
 * The internal delays of the 6526 are modeled as pipelines of bits, which are shifted each cycle:
 * The first decrement happens 2 cycles after a timer has been started,
 * a forced load happens 1 cycle after it has been requested,
 * the IRQ follows an interrupt flag 1 cycle later.
 * <p>
 * Not implemented: Time of day clock, serial shift register, counting CNT edges.
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

    public static final int CRB_MODE_MASK = 0x60;
    public static final int CRB_MODE_TIMER_A = 0x40;
    public static final int CRB_SET_ALARM = 0x80;

    /**
     * Pipeline: Interrupt flag set, IRQ follows in the next cycle.
     */
    private static final int INT0 = 1 << 0;
    private static final int INT1 = 1 << 1;

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
     * Serial shift register.
     */
    private int sdr; // 0x0C

    /**
     * Timers.
     */
    private final Timer timerA = new Timer(CIA6526.ICR_UNDERFLOW_TIMER_A, 0x40);
    private final Timer timerB = new Timer(CIA6526.ICR_UNDERFLOW_TIMER_B, 0x80);

    /**
     * Time of day.
     */
    private int timeTenth; // 0x08
    private int timeSec; // 0x09
    private int timeMin; // 0x0A
    private int timeHour; // 0x0B
    private boolean timeLock;
    private int timeTenthTemp;
    private int timeSecTemp;
    private int timeMinTemp;
    private int alarmTenth;
    private int alarmSec;
    private int alarmMin;
    private int alarmHour;

    /**
     * Interrupt control: Flags and mask.
     */
    private int icr; // 0x0D
    private int icrMask; // 0x0D

    /**
     * Interrupt pipeline.
     */
    private int intDelay = 0;

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

        clock.addClockedComponent(position, this);
    }

    public void reset() {
        // TODO
        timeLock = false;
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
            case 0x08 -> {
                if ((timerB.control & CRB_SET_ALARM) == 0) {
                    timeTenth = value & 0x0F;
                } else {
                    alarmTenth = value & 0x0F;
                }
            }
            case 0x09 -> {
                if ((timerB.control & CRB_SET_ALARM) == 0) {
                    timeSec = value & 0x7F;
                } else {
                    alarmSec = value & 0x7F;
                }
            }
            case 0x0A -> {
                if ((timerB.control & CRB_SET_ALARM) == 0) {
                    timeMin = value & 0x7F;
                } else {
                    alarmMin = value & 0x7F;
                }
            }
            case 0x0B -> {
                if ((timerB.control & CRB_SET_ALARM) == 0) {
                    timeHour = value & 0x9F;
                } else {
                    alarmHour = value & 0x9F;
                }
            }
            case 0x0C -> sdr = value;
            case 0x0D -> {
                if ((value & 0x80) == 0) {
                    icrMask &= ~value & ICR_FLAGS;
                } else {
                    icrMask |= value & ICR_FLAGS;
                }
                // Pending interrupts trigger an IRQ, if they are enabled now.
                if ((icr & icrMask) != 0) {
                    intDelay |= INT0;
                }
            }
            case 0x0E -> timerA.writeControl(value);
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
            case 0x08 -> {
                var value = timeLock ? timeTenthTemp : timeTenth;
                // unlock current time
                timeLock = false;
                yield value;
            }
            case 0x09 -> timeLock ? timeSecTemp : timeSec;
            case 0x0A -> timeLock ? timeMinTemp : timeMin;
            case 0x0B -> {
                // lock current time in temp registers
                timeTenthTemp = timeTenth;
                timeSecTemp = timeSec;
                timeMinTemp = timeMin;
                timeLock = true;
                yield timeHour;
            }
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
     * Read the interrupt control register: Clears all flags and releases the IRQ.
     */
    private int readICR() {
        var result = icr;
        icr = 0;
        intDelay = 0;
        irqPort.setOutputData(0x01);
        return result;
    }

    /**
     * Execute one cycle.
     */
    private void cycle() {
        var underflowA = timerA.cycle();
        var underflowB = timerB.cycle();
        if (underflowA && (timerB.control & CRB_MODE_TIMER_A) != 0) {
            // Timer B counts underflows of timer A one cycle later. CNT is not emulated, so it is considered high.
            timerB.count();
        }

        if (underflowA) {
            setInterrupt(ICR_UNDERFLOW_TIMER_A);
        }
        if (underflowB) {
            setInterrupt(ICR_UNDERFLOW_TIMER_B);
        }

        if ((intDelay & INT1) != 0) {
            icr |= ICR_IRQ;
            irqPort.setOutputData(0x00);
        }
        intDelay = intDelay << 1 & INT1;
    }

    /**
     * Set interrupt flag. If it is enabled, the IRQ follows in the next cycle.
     */
    private void setInterrupt(int flag) {
        icr |= flag;
        if ((icrMask & flag) != 0) {
            intDelay |= INT0;
        }
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
