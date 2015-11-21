package de.heiden.jem.components.clock.synchronization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockEvent;
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
   * Has the run been suspended?.
   */
  private final AtomicBoolean _suspended = new AtomicBoolean(false);

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
    assert isStarted() : "Precondition: isStarted()";

    try {
      _barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  /**
   * Start threads of all components.
   */
  protected void doInit() {
    logger.debug("starting components");

    // Suspend execution at start of first tick.
    addClockEvent(0, new SuspendEvent());

    // Create threads.
    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    _threads = new Thread[components.size()];
    for (int i = 0; i < _threads.length; i++) {
      ClockedComponent component = components.get(i);
      _threads[i] = new Thread(() -> {
        logger.debug("starting {}", component.getName());
        ParallelClock.this.waitForTick();
        logger.debug("started {}", component.getName());
        component.run();
      }, component.getName());
      _threads[i].setDaemon(true);
    }

    // Start threads.
    _barrier = new CyclicBarrier(_componentMap.size(), this::tick);
    for (Thread thread : _threads) {
      thread.start();
    }
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
    // Third execute components.
  }

  @Override
  protected final void doRun(int ticks) {
    addClockEvent(_tick.get() + ticks, new SuspendEvent());
    doRun();
  }

  @Override
  protected final void doRun() {
    try {
      synchronized (_suspended) {
        // Resume, if suspended.
        if (_suspended.get()) {
          _suspended.set(false);
          _suspended.notifyAll();
        }

        while (!_suspended.get()) {
          _suspended.wait();
        }
      }
    } catch (InterruptedException e) {
      close();
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  protected void doClose() {
    for (Thread thread : _threads) {
      thread.interrupt();
    }
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
      logger.info("Suspend at {}.", tick);
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
        logger.info("Resume at {}.", tick);
      }
    }
  }
}
