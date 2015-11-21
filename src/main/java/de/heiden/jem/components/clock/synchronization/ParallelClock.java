package de.heiden.jem.components.clock.synchronization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented with synchronization, executing component threads in parallel.
 */
public class ParallelClock extends AbstractSynchronizedClock {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Component threads.
   */
  private Thread[] _threads;

  /**
   * Barrier for synchronizing all component threads.
   */
  private CyclicBarrier _barrier;

  /**
   * Event for suspending execution.
   */
  private final SuspendEvent _suspendEvent = new SuspendEvent();

  //
  // public
  //

  @Override
  protected Tick createTick(ClockedComponent component) {
    return this::waitForTick;
  }

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
    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    _barrier = new CyclicBarrier(components.size(), this::startTick);
    _threads = new Thread[components.size()];
    for (int i = 0; i < _threads.length; i++) {
      ClockedComponent component = components.get(i);
      _threads[i] = createStartedDaemonThread(component.getName(), () -> {
        logger.debug("starting {}", component.getName());
        ParallelClock.this.waitForTick();
        logger.debug("started {}", component.getName());
        component.run();
      });
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
    for (Thread thread : _threads) {
      thread.interrupt();
    }
    Thread.yield();
    _barrier.reset();
  }

}
