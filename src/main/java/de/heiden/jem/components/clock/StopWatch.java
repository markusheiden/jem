package de.heiden.jem.components.clock;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stop watch for execution performance.
 * Measures time each simulated second.
 */
public class StopWatch extends ClockEvent {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Clock clock;
    private final long freq;

    private long last;

    /**
     * Constructor.
     *
     * @param clock
     *         clock to slow down
     * @param freq
     *         frequency in Hz (clock ticks per second)
     */
    public StopWatch(@Nonnull Clock clock, long freq) {
        super("Stop watch");

        assert freq > 0 : "Precondition: freq > 0";

        this.clock = clock;
        this.freq = freq;
        last = System.nanoTime();

        this.clock.addClockEvent(this.freq, this);
    }

    @Override
    public void execute(long tick) {
        long now = System.nanoTime();
        long elapsed = (now - last) / 1000000;
        logger.info("1 simulated second took {} ms", elapsed);
        last = now;

        clock.addClockEvent(tick + freq, this);
    }
}
