package de.heiden.jem.models.c64.components.cia;

import de.heiden.jem.components.clock.Clock;
import jakarta.annotation.Nonnull;

/**
 * CIA 6526A (new CIA), as in the C64C.
 * <p>
 * The IRQ follows an interrupt flag in the same cycle, one cycle earlier than in the old CIA.
 */
public class CIA6526A extends AbstractCIA {
    /**
     * Has the interrupt control register been read in the previous cycle?
     */
    private boolean icrReadBefore = false;

    /**
     * Value of the last read of the interrupt control register.
     */
    private int icrReadValue = 0;

    /**
     * Constructor for a PAL C64.
     *
     * @param clock
     *         clock
     * @param position
     *         position of the CIA in the clock, after the CPU
     */
    public CIA6526A(@Nonnull Clock clock, int position) {
        this(clock, position, TodInput.PAL);
    }

    /**
     * Constructor.
     *
     * @param clock
     *         clock
     * @param position
     *         position of the CIA in the clock, after the CPU
     * @param todInput
     *         TOD input
     */
    public CIA6526A(@Nonnull Clock clock, int position, @Nonnull TodInput todInput) {
        super(clock, position, todInput);
    }

    @Override
    protected int readICR() {
        var result = super.readICR();
        if (icrReadBefore) {
            // A read directly after a read returns the value of that read again.
            result |= icrReadValue;
        }
        icrReadValue = result;
        return result;
    }

    @Override
    protected void updateIrq() {
        // The IRQ follows the enabled interrupt flags in the same cycle.
        // A read in the same cycle suppresses the IRQ output.
        if ((icr & icrMask) != 0) {
            icr |= ICR_IRQ;
            if (!icrRead) {
                setIrq(true);
            }
        }
        icrReadBefore = icrRead;
    }
}
