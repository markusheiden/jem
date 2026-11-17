package de.heiden.jem.components.clock.loom;

import jdk.management.VirtualThreadSchedulerMXBean;

import java.lang.management.ManagementFactory;

/**
 * Configuration of the scheduler of the {@link Thread fibers} of project loom.
 * The configuration applies to all fibers of this JVM.
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
     * Called before the first fiber is created, there never will be more than one carrier thread.
     * Called later, the scheduler stops using its other carrier threads.
     * Do not use with clocks that solely wait busily:
     * These keep the carrier thread busy, so the fiber being waited for never gets executed.
     */
    public static synchronized void useSingleCarrier() {
        // Only has an effect, if the scheduler has not been created yet.
        System.setProperty(PARALLELISM, "1");
        System.setProperty(MAX_POOL_SIZE, "1");

        var scheduler = scheduler();
        if (scheduler.getParallelism() != 1) {
            scheduler.setParallelism(1);
        }
    }

    /**
     * Are all fibers of this JVM executed on a single carrier thread?
     */
    public static boolean isSingleCarrier() {
        return scheduler().getParallelism() == 1;
    }

    /**
     * Number of carrier threads of this JVM.
     */
    public static int carrierThreads() {
        return scheduler().getPoolSize();
    }

    private static VirtualThreadSchedulerMXBean scheduler() {
        return ManagementFactory.getPlatformMXBean(VirtualThreadSchedulerMXBean.class);
    }

    private FiberScheduler() {
        // Utility class.
    }
}
