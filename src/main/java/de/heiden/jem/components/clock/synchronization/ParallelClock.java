package de.heiden.jem.components.clock.synchronization;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockEvent;
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

  /**
   * Has the run been suspended?.
   */
  private AtomicBoolean _suspended = new AtomicBoolean(false);

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

    // Suspend execution at start of first tick.
    addClockEvent(0, new SuspendEvent());

    // Start threads.
    _barrier = new CyclicBarrier(_entryMap.size(), this::tick);
    _entryMap.values().forEach(ParallelClockEntry::start);
    Thread.yield();

    // Wait until all threads are at start of first click.
    try {
      synchronized (_suspended) {
        while (!_suspended.get()) {
          _suspended.wait();
        }
      }
    } catch (InterruptedException e) {
      close();
      throw new RuntimeException("Thread has been stopped", e);
    }
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
  protected final void doRun(int ticks) {
    addClockEvent(_tick.get() + ticks, new SuspendEvent());
    doRun();
  }

  @Override
  protected final void doRun() {
    try {
      resume();
      synchronized (_suspended) {
        while (!_suspended.get()) {
          _suspended.wait();
        }
      }
    } catch (InterruptedException e) {
      close();
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  /**
   * Resume clock run.
   */
  private void resume() {
    synchronized (_suspended) {
      if (_suspended.get()) {
        logger.info("Resuming at {}.", _tick.get());
        _suspended.set(false);
        _suspended.notifyAll();
      }
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

  /**
   * Event to suspend clock run.
   */
  private class SuspendEvent extends ClockEvent {
    public SuspendEvent() {
      super("Suspend");
    }

    @Override
    public void execute(long tick) {
      logger.info("Suspended at {}.", tick);
      synchronized (_suspended) {
        _suspended.set(true);
        _suspended.notifyAll();
        while (_suspended.get()) {
          try {
            _suspended.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException("Thread has been stopped", e);
          }
        }
      }
    }
  }
}
