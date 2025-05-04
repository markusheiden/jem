package de.heiden.jem.components.clock;

import org.serialthreads.Interruptible;

import static java.lang.Thread.interrupted;

/**
 * Counter for clock tests.
 */
public final class CounterComponent implements ClockedComponent {
    /**
     * Tick.
     */
    private Tick tick;

    /**
     * Count.
     */
    private volatile long count;

    @Override
    public String getName() {
        return "Test counter";
    }

    @Override
    public void setTick(Tick tick) {
        this.tick = tick;
    }

    @Override
    @Interruptible
    public void run() {
        while (!interrupted()) {
            count++;
            if (count % 10 == 0) {
                System.out.print(".");
            }
            tick.waitForTick();
        }
    }

    /**
     * Count.
     */
    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return Long.toString(count);
    }
}
