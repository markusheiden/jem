package de.heiden.jem.components.clock.synchronization;

import java.util.concurrent.atomic.AtomicInteger;

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
   * All clocked components sorted by their position.
   */
  private ParallelClockEntry[] _entries;

  /**
   * Number of waiting components.
   */
  private final AtomicInteger _waiting = new AtomicInteger(0);

  //
  // public
  //

  @Override
  public void close() {
    for (ParallelClockEntry entry : _entries) {
      entry.close();
    }
  }

  @Override
  protected ParallelClockEntry createClockEntry(final ClockedComponent component) {
    return new ParallelClockEntry(component, this);
  }

  protected void waitForStart() throws InterruptedException {
    synchronized (_lock) {
      _waiting.incrementAndGet();
      while (_tick.get() < 0) {
        _lock.wait();
      }
    }
  }

  @Override
  public final void waitForTick() {
    assert isStarted() : "Precondition: isStarted()";

    try {
      synchronized (_lock) {
        if (_waiting.get() < _entries.length) {
          sleep();
        } else {
          tick();
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  /**
   * Sleep until the other components finish the current tick.
   * Call needs to be synchronized by _lock;
   */
  private void sleep() throws InterruptedException {
    long tick = _tick.get();

    logger.debug("going to sleep at {}", tick);

    _waiting.incrementAndGet();
    do {
      _lock.wait();
      logger.debug("wake up at {}", _tick.get());
    } while (tick == _tick.get());
  }

  /**
   * All other components have finished the tick, continue with the next tick.
   * Call needs to be synchronized by _lock;
   */
  private void tick() {
    long tick = _tick.incrementAndGet();

    logger.debug("tick {}", tick);

    // First execute events
    executeEvent(tick);

    // Second execute all entries
    _waiting.set(1);
    _lock.notifyAll();
  }

  /**
   * Start threads of all components.
   */
  protected void doInit() {
    try {
      logger.debug("starting components");

      _entries = _entryMap.values().toArray(new ParallelClockEntry[_entryMap.size()]);
      for (ParallelClockEntry entry : _entries) {
        entry.start();
      }
      Thread.yield();

      synchronized (_lock) {
        // wait for all components
        while (_waiting.get() <  _entries.length) {
          logger.debug("waiting for {} / {} components", _entries.length - _waiting.get(), _entries.length);
          _lock.wait(1);
        }

        // start clock
        _started = true;
        tick();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  protected final void doRun() {
    try {
      synchronized (this) {
        wait();
      }
    } catch (InterruptedException e) {
      close();
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  protected final void doRun(int ticks) {
    try {
      synchronized (_lock) {
        if (_started) {
          resume();
        }

        long end = _tick.get() + ticks;
        do {
          _lock.wait();
        } while (_tick.get() < end);

        pause();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  /**
   * Pause run.
   * Call needs to be synchronized by _lock;
   */
  private void pause() {
    _waiting.decrementAndGet();
    logger.debug("paused");
  }

  /**
   * Resume run.
   * Call needs to be synchronized by _lock;
   */
  private void resume() {
    logger.debug("resume");
    if (_waiting.incrementAndGet() > _entries.length) {
      tick();
    }
  }
}
