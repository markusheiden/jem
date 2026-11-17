package de.heiden.jem.components.clock.loom;

/**
 * Configuration of the scheduler of the {@link Thread fibers} of project loom.
 */
public final class FiberScheduler {
    /**
     * System property for the number of carrier threads.
     */
    private static final String PARALLELISM = "jdk.virtualThreadScheduler.parallelism";

    /**
     * System property for the maximum number of carrier threads.
     */
    private static final String MAX_POOL_SIZE = "jdk.virtualThreadScheduler.maxPoolSize";

    /**
     * Execute all fibers of this JVM on a single carrier thread.
     * So all components and events of a clock executing its components sequentially are executed by the same thread.
     * Additionally, handing over execution between fibers is much faster then,
     * because it becomes a switch on the same thread instead of waking up another thread.
     * <p>
     * Has to be called before the first fiber is created.
     * Do not use with clocks that solely wait busily:
     * These keep the carrier thread busy, so the fiber being waited for never gets executed.
     */
    public static void useSingleCarrier() {
        System.setProperty(PARALLELISM, "1");
        System.setProperty(MAX_POOL_SIZE, "1");
    }

    /**
     * Are all fibers of this JVM executed on a single carrier thread?
     */
    public static boolean isSingleCarrier() {
        return Integer.getInteger(PARALLELISM, 0) == 1 && Integer.getInteger(MAX_POOL_SIZE, 0) == 1;
    }

    private FiberScheduler() {
        // Utility class.
    }
}
