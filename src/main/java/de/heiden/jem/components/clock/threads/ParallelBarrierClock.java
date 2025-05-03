package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ManualAbort;
import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Clock implemented with synchronization, executing component threads in parallel.
 */
public final class ParallelBarrierClock extends AbstractSynchronizedClock {
  /**
   * Barrier for synchronizing all component threads.
   */
  private CyclicBarrier barrier;

  /**
   * Start threads of all components.
   */
  protected void doSynchronizedInit() {
    logger.debug("starting components");

    // Create threads.
    var components = clockedComponents();
    barrier = new CyclicBarrier(components.length, this::startTick);
    for (var component : components) {
      var tick = new BarrierTick(barrier);
      component.setTick(tick);
      createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
    }
  }

  @Override
  protected void doClose() {
    super.doClose();
    barrier.reset();
  }

  /**
   * Special tick, waiting for the barrier.
   */
  private static final class BarrierTick implements Tick {
    /**
     * Barrier for synchronizing all component threads.
     */
    private final CyclicBarrier barrier;

    /**
     * Constructor.
     *
     * @param barrier Barrier for synchronizing all component threads.
     */
    public BarrierTick(CyclicBarrier barrier) {
      this.barrier = barrier;
    }

    @Override
    public void waitForTick() {
      try {
        barrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new ManualAbort();
      }
    }
  }
}
