package de.heiden.jem.components.clock;

import de.heiden.jem.components.clock.loom.FiberScheduler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.time.Duration;

import static java.lang.Runtime.getRuntime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test base for {@link Clock}s.
 * <p>
 * The tests are executed isolated: Most clocks share the carrier threads of the fibers of this JVM,
 * and the busy waiting ones occupy them, so the clocks of concurrently executed tests would block each other.
 */
@Tag("clock")
@Isolated
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
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> run(runs, cycles, numCounters()));
    }

    /**
     * Test for {@link Clock#run(int)} with fewer counters than processors.
     * Some clocks wait differently for ticks in this case.
     */
    @Test
    void runFewCounters() {
        var runs = 10;
        var cycles = 1000;
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> run(runs, cycles, 2));
    }

    private void run(int runs, int cycles, int num) throws Exception {
        for (var i = 0; i < runs; i++) {
            System.out.println("Run " + i);
            try (var clock = createClock()) {
                run(clock, cycles, num);
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
     * Does the clock execute its components sequentially by fibers?
     * In this case all components and events are executed by the same thread, if there is a single carrier thread.
     */
    protected boolean isSequential() {
        return false;
    }

    /**
     * Test run clock.
     */
    private void run(Clock clock, int cycles, int num) {
        var counters = new CounterComponent[num];
        for (var i = 0; i < num; i++) {
            var counter = new CounterComponent();
            clock.addClockedComponent(i, counter);
            counters[i] = counter;
        }

        clock.run(cycles);

        // Other tests executed concurrently may need multiple carrier threads,
        // so the carrier threads are only limited when testing the sequential clocks separately.
        if (isSequential() && FiberScheduler.isSingleCarrier()) {
            assertEquals(1, FiberScheduler.carrierThreads(), "All components are executed by the same carrier thread");
        }

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
