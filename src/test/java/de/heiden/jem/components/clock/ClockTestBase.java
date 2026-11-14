package de.heiden.jem.components.clock;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.lang.Runtime.getRuntime;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test base for {@link Clock}s.
 */
public abstract class ClockTestBase {
    /**
     * Create the clock.
     */
    protected abstract Clock createClock() throws Exception;

    /**
     * Test for {@link Clock#run(int)}.
     */
    @Test
    void run() {
        var runs = 10;
        var cycles = 1000;
        // @Timeout(10) does not work if synchronization ignores interrupts.
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> run(runs, cycles));
    }

    private void run(int runs, int cycles) throws Exception {
        for (var i = 0; i < runs; i++) {
            System.out.println("Run " + i);
            try (var clock = createClock()) {
                run(clock, cycles);
            }
            System.out.println();
            System.out.flush();
        }
    }

    /**
     * Number of counters to test with.
     * Per default three times the number of processors to test context switch performance.
     */
    protected int numCounters() {
        return getRuntime().availableProcessors() * 3;
    }

    /**
     * Test run clock.
     */
    private void run(Clock clock, int cycles) {
        var num = numCounters();
        var counters = new CounterComponent[num];
        for (var i = 0; i < num; i++) {
            var counter = new CounterComponent();
            clock.addClockedComponent(i, counter);
            counters[i] = counter;
        }

        clock.run(cycles);

        // Check that all components are executed exactly the specified amount of cycles.
        var failure = false;
        for (var counter : counters) {
            failure |= counter.getCount() != cycles;
        }

        if (failure) {
            for (var i = 0; i < counters.length; i++) {
                System.out.printf("Counter %d: %d%n", i, counters[i].getCount());
            }
            fail("Not all counters are at " + cycles);
        }
    }
}
