package de.heiden.jem.components.clock.synchronization;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented with synchronization.
 */
public class ParallelClock extends AbstractSynchronizedClock<ParallelClockEntry> implements Tick {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Barrier for synchronizing all component threads.
   */
  private CyclicBarrier _barrier;

  //
  // public
  //

  @Override
  protected ParallelClockEntry createClockEntry(ClockedComponent component) {
    return new ParallelClockEntry(component, this);
  }

  /**
   * Start threads of all components.
   */
  protected void doInit() {
    logger.debug("starting components");

    _barrier = new CyclicBarrier(_entryMap.size(), this::tick);
    _entryMap.values().forEach(ParallelClockEntry::start);
    Thread.yield();
  }

  /**
   * Start tick.
   */
  private void tick() {
    // First increment tick.
    // Second execute events.
    executeEvent(_tick.incrementAndGet());
    // Thirds execute components.
  }

  @Override
  protected synchronized final void doRun() {
    try {
      wait();
    } catch (InterruptedException e) {
      close();
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  protected synchronized final void doRun(int ticks) {
    try {
      final long end = _tick.get() + ticks;
      do {
        wait();
      } while (_tick.get() < end);
    } catch (InterruptedException e) {
      close();
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  public final void waitForTick() {
    assert isStarted() : "Precondition: isStarted()";

    try {
      _barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  protected void doClose() {
    _entryMap.values().forEach(ParallelClockEntry::close);
    Thread.yield();
    _barrier.reset();
  }
}
