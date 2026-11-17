package de.heiden.jem.components.clock.loom;

/**
 * Configuration of the scheduler of the {@link Thread fibers} of project loom.
 */
public final class FiberScheduler {
    /**
     * Execute all fibers of this JVM on a single carrier thread.
     * Handing over execution between fibers is much faster then,
     * because it becomes a switch on the same thread instead of waking up another thread.
     * <p>
     * Has to be called before the first fiber is created.
     * Do not use with clocks that execute busy waiting or yielding fibers:
     * These keep the carrier thread busy, so fibers woken up from blocking or sleeping never get executed again.
     */
    public static void useSingleCarrier() {
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "1");
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "1");
    }

    private FiberScheduler() {
        // Utility class.
    }
}
