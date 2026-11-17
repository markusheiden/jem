package de.heiden.jem.components.clock;

import de.heiden.jem.components.clock.loom.ContinuationClock;
import de.heiden.jem.components.clock.loom.SequentialFiberLatchClock;
import de.heiden.jem.components.clock.loom.SequentialFiberParkClock;
import de.heiden.jem.components.clock.loom.SequentialFiberQueueClock;
import de.heiden.jem.components.clock.loom.SequentialFiberYieldClock;
import de.heiden.jem.components.clock.threads.ParallelBarrierClock;
import de.heiden.jem.components.clock.threads.ParallelNotifyClock;
import de.heiden.jem.components.clock.threads.ParallelSpinClock;
import de.heiden.jem.components.clock.threads.ParallelYieldClock;
import de.heiden.jem.components.clock.threads.SequentialClock;
import de.heiden.jem.components.clock.threads.SequentialSpinClock;
import de.heiden.jem.components.clock.threads.SequentialYieldClock;
import org.serialthreads.Interruptible;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the overhead per tick of the thread, fiber and continuation based clocks.
 * The components just count, so only the handover between the components is measured.
 * <p>
 * Without arguments, each clock is benchmarked in its own JVM. The clocks executing their components sequentially
 * are additionally benchmarked with a single carrier thread, as far as they support it.
 * With the class name of a clock as argument, just this clock is benchmarked in this JVM.
 * The number of components may be set via the system property "components".
 * <p>
 * The serial clock is not part of this benchmark, because it needs byte code transformation.
 */
public final class ClockBenchmark {
    /**
     * Thread based clocks.
     */
    private static final List<Class<? extends Clock>> THREAD_CLOCKS = List.of(
            SequentialClock.class,
            SequentialSpinClock.class,
            SequentialYieldClock.class,
            ParallelBarrierClock.class,
            ParallelNotifyClock.class,
            ParallelSpinClock.class,
            ParallelYieldClock.class);

    /**
     * Fiber based clocks.
     */
    private static final List<Class<? extends Clock>> FIBER_CLOCKS = List.of(
            SequentialFiberLatchClock.class,
            SequentialFiberParkClock.class,
            SequentialFiberQueueClock.class,
            SequentialFiberYieldClock.class);

    /**
     * Clocks executing all components and events with the same thread, if there is just a single carrier thread.
     */
    private static final List<Class<? extends Clock>> SINGLE_CARRIER_CLOCKS = List.of(
            SequentialClock.class,
            SequentialYieldClock.class,
            SequentialFiberLatchClock.class,
            SequentialFiberParkClock.class,
            SequentialFiberQueueClock.class,
            SequentialFiberYieldClock.class);

    /**
     * JVM arguments to execute all fibers on a single carrier thread.
     */
    private static final List<String> SINGLE_CARRIER = List.of(
            "-Djdk.virtualThreadScheduler.parallelism=1",
            "-Djdk.virtualThreadScheduler.maxPoolSize=1");

    /**
     * Number of components. A C64 has about 4 clocked components.
     */
    private static final int COMPONENTS = Integer.getInteger("components", 4);

    /**
     * Number of ticks per run of the clock.
     */
    private static final int TICKS = 10_000;

    /**
     * Duration of the warm-up in nanoseconds.
     */
    private static final long WARM_UP = TimeUnit.SECONDS.toNanos(2);

    /**
     * Duration of the measurement in nanoseconds.
     */
    private static final long MEASUREMENT = TimeUnit.SECONDS.toNanos(5);

    /**
     * Timeout for the benchmark of a single clock in seconds.
     */
    private static final long TIMEOUT = 60;

    /**
     * Start the benchmark.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            for (var clock : THREAD_CLOCKS) {
                fork(clock, List.of());
            }
            for (var clock : FIBER_CLOCKS) {
                fork(clock, List.of());
            }
            fork(ContinuationClock.class, List.of());
            System.out.println("Single carrier thread:");
            for (var clock : SINGLE_CARRIER_CLOCKS) {
                fork(clock, SINGLE_CARRIER);
            }
        } else {
            benchmark(Class.forName(args[0]).asSubclass(Clock.class));
        }
    }

    /**
     * Benchmark the clock in its own JVM.
     */
    private static void fork(Class<? extends Clock> clock, List<String> jvmArgs) throws Exception {
        var command = new ArrayList<String>();
        command.add(System.getProperty("java.home") + "/bin/java");
        command.add("-Dcomponents=" + COMPONENTS);
        // For the continuation clock.
        command.add("--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED");
        command.addAll(jvmArgs);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(ClockBenchmark.class.getName());
        command.add(clock.getName());

        var process = new ProcessBuilder(command).inheritIO().start();
        if (!process.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            System.out.printf("%-28s timed out%n", clock.getSimpleName());
        } else if (process.exitValue() != 0) {
            System.out.printf("%-28s failed%n", clock.getSimpleName());
        }
    }

    /**
     * Benchmark the clock.
     */
    private static void benchmark(Class<? extends Clock> clockClass) throws Exception {
        try (var clock = clockClass.getConstructor().newInstance()) {
            for (var i = 0; i < COMPONENTS; i++) {
                clock.addClockedComponent(i, new CountingComponent());
            }

            for (var end = System.nanoTime() + WARM_UP; System.nanoTime() < end; ) {
                clock.run(TICKS);
            }

            var ticks = 0L;
            var start = System.nanoTime();
            long elapsed;
            do {
                clock.run(TICKS);
                ticks += TICKS;
                elapsed = System.nanoTime() - start;
            } while (elapsed < MEASUREMENT);

            System.out.printf("%-28s %,12d ticks/s %,10.1f ns/tick%n",
                    clockClass.getSimpleName(), ticks * TimeUnit.SECONDS.toNanos(1) / elapsed, (double) elapsed / ticks);
        }
    }

    /**
     * Component that just counts its ticks.
     */
    private static final class CountingComponent implements ClockedComponent {
        /**
         * Tick.
         */
        private Tick tick;

        /**
         * Count.
         */
        private long count;

        @Override
        public String getName() {
            return "Counter";
        }

        @Override
        public void setTick(Tick tick) {
            this.tick = tick;
        }

        @Override
        @Interruptible
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                count++;
                tick.waitForTick();
            }
        }
    }

    private ClockBenchmark() {
        // Utility class.
    }
}
