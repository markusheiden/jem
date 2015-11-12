package de.heiden.jem.components.clock.synchronization;

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
  protected ParallelClockEntry[] _entries;

  /**
   * Number of waiting components.
   */
  private int _waiting = 0;

  //
  // public
  //

  @Override
  public void dispose() {
    for (ParallelClockEntry entry : _entries) {
      entry.thread.interrupt();
    }
  }

  @Override
  protected ParallelClockEntry createClockEntry(final ClockedComponent component) {
    Thread thread = new Thread(() -> {
      logger.debug("starting {}", component.getName());
      waitForTick();
      logger.debug("started {}", component.getName());
      component.run();
    }, component.getName());

    return new ParallelClockEntry(component, this, thread);
  }

  protected void waitForStart() throws InterruptedException {
    synchronized (_lock) {
      _waiting++;
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
        if (_waiting < _entries.length) {
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
    logger.debug("going to sleep at {}", _tick);

    long tick = _tick.get();
    _waiting++;
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
    logger.debug("tick {}", _tick);

    executeEvent(_tick.get());

    // next tick
    _waiting = 1;
    // TODO 2015-11-11 markus: Increment at top to avoid double access to _tick?
    _tick.incrementAndGet();
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
        entry.thread.start();
      }
      Thread.yield();

      // wait for all components
      while (_waiting < _entries.length) {
        logger.debug("waiting for {} / {} components", _entries.length - _waiting, _entries.length);
        _lock.wait(1);
      }

      // start clock
      _started = true;
      tick();
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
    _waiting--;
    logger.debug("paused");
  }

  /**
   * Resume run.
   * Call needs to be synchronized by _lock;
   */
  private void resume() {
    logger.debug("resume");
    _waiting++;
    if (_waiting > _entries.length) {
      tick();
    }
  }
}
