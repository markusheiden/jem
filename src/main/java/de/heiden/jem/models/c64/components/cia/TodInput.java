package de.heiden.jem.models.c64.components.cia;

/**
 * TOD input of the CIA: The frequency of the power line relative to the CPU clock.
 * <p>
 * The CPU clock is derived from the crystal, so its frequency is the fraction {@code cpuClock / cpuDivider} Hz.
 * The power line is independent of the CPU clock, so the ticks of the TOD input are distributed exactly over the cycles.
 *
 * @param cpuClock
 *         frequency of the crystal in Hz
 * @param cpuDivider
 *         divider of the crystal frequency to the CPU clock
 * @param frequency
 *         frequency of the power line in Hz
 */
public record TodInput(int cpuClock, int cpuDivider, int frequency) {
    /**
     * PAL C64: Crystal 17.734475 MHz / 18 = 985248.6 Hz, power line 50 Hz.
     */
    public static final TodInput PAL = new TodInput(17734475, 18, 50);

    /**
     * NTSC C64: Crystal 14.318181 MHz (315 / 22 MHz) / 14 = 1022727.3 Hz, power line 60 Hz.
     */
    public static final TodInput NTSC = new TodInput(315000000, 22 * 14, 60);

    public TodInput {
        assert cpuClock > 0 : "cpuClock > 0";
        assert cpuDivider > 0 : "cpuDivider > 0";
        assert frequency > 0 : "frequency > 0";
        assert (long) cpuDivider * frequency < cpuClock : "Less than one tick per cycle";
    }

    /**
     * Increment of the accumulator per cycle: A tick of the TOD input happens every {@link #cpuClock()} increments.
     */
    int increment() {
        return cpuDivider * frequency;
    }
}
