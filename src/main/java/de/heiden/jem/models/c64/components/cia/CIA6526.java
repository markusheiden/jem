package de.heiden.jem.models.c64.components.cia;

/**
 * CIA 6526 (old CIA), as in the original C64.
 * <p>
 * The IRQ follows an interrupt flag 1 cycle later.
 */
public class CIA6526 extends AbstractCIA {
    /**
     * Interrupt flags of the last cycle, which are enabled.
     * The IRQ follows them one cycle later.
     */
    private int irqSource = 0;

    /**
     * Timer B interrupt, whose flag has been lost in the current cycle.
     */
    private int lost = 0;

    /**
     * Constructor for a PAL C64.
     */
    public CIA6526() {
        this(TodInput.PAL);
    }

    /**
     * Constructor.
     *
     * @param todInput
     *         TOD input.
     */
    public CIA6526(TodInput todInput) {
        super(todInput);
    }

    @Override
    protected void timerBUnderflow() {
        if (!icrRead) {
            super.timerBUnderflow();
        } else {
            // Timer B bug of the old CIA: If the ICR is read in the cycle of the underflow,
            // the interrupt flag is lost, but the IRQ still occurs.
            lost = ICR_UNDERFLOW_TIMER_B;
        }
    }

    @Override
    protected void updateIrq() {
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
        lost = 0;
    }
}
