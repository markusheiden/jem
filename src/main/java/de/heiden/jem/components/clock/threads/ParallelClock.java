package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Clock implemented with synchronization, executing component threads in parallel.
 */
public class ParallelClock extends AbstractSynchronizedClock {
  /**
   * Barrier for synchronizing all component threads.
   */
  private CyclicBarrier _barrier;

  /**
   * Wait for next tick. Called by clocked components.
   */
  private void waitForTick() {
    try {
      _barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      throw new ManualAbort();
    }
  }

  /**
   * Start threads of all components.
   */
  protected void doInit() {
    logger.debug("starting components");

    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    // Create threads.
    Collection<ClockedComponent> components = _componentMap.values();
    _barrier = new CyclicBarrier(components.size(), this::startTick);
    _componentThreads = new ArrayList<>(components.size());
    for (ClockedComponent component : components) {
      Tick tick = this::waitForTick;
      component.setTick(tick);
      _componentThreads.add(createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick)));
    }
    Thread.yield();

    // Wait until all threads are at start of first click.
    _suspendEvent.waitForSuspend();
  }

  @Override
  protected final void doRun(int ticks) {
    addClockEvent(_tick.get() + ticks, _suspendEvent);
    doRun();
  }

  @Override
  protected final void doRun() {
    _suspendEvent.resume();
    _suspendEvent.waitForSuspend();
  }

  @Override
  protected void doClose() {
    _barrier.reset();
    super.doClose();
  }
}
