package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
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
  private CyclicBarrier _barrier;

  /**
   * Start threads of all components.
   */
  protected void doSynchronizedInit() {
    logger.debug("starting components");

    // Create threads.
    var components = _componentMap.values();
    _barrier = new CyclicBarrier(components.size(), this::startTick);
    for (ClockedComponent component : components) {
      Tick tick = new BarrierTick(_barrier);
      component.setTick(tick);
      createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
    }
  }

  @Override
  protected void doClose() {
    super.doClose();
    _barrier.reset();
  }

  /**
   * Special tick, waiting for the barrier.
   */
  private static final class BarrierTick implements Tick {
    /**
     * Barrier for synchronizing all component threads.
     */
    private final CyclicBarrier _barrier;

    /**
     * Constructor.
     *
     * @param barrier Barrier for synchronizing all component threads.
     */
    public BarrierTick(CyclicBarrier barrier) {
      this._barrier = barrier;
    }

    @Override
    public final void waitForTick() {
      try {
        _barrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new ManualAbort();
      }
    }
  }
}
