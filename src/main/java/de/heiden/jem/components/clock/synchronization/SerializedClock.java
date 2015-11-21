package de.heiden.jem.components.clock.synchronization;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented with synchronization, executing component threads sequentially.
 * <p/>
 * TODO add / remove clocked components operation could not be execute while clock is running
 */
public class SerializedClock extends AbstractSynchronizedClock {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Component threads.
   */
  private Thread[] _threads;

  /**
   * Component thread locks.
   */
  private Lock[] _locks;

  /**
   * Lock to wait for last component to be executed.
   */
  private final Lock _finishedTickLock = new Lock("Finish tick");

  //
  // public
  //

  @Override
  protected Tick createTick(ClockedComponent component) {
    return new SerializedTick();
  }

  @Override
  protected void doInit() {
    // Create threads.
    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    SerializedTick[] ticks = new SerializedTick[components.size()];
    _locks = new Lock[components.size()];
    _threads = new Thread[components.size()];
    for (int i = 0; i < _threads.length; i++) {
      ClockedComponent component = components.get(i);
      SerializedTick tick = (SerializedTick) _tickMap.get(component);
      ticks[i] = tick;

      Lock lock = new Lock(component.getName());
      tick._lock = _locks[i];
      _locks[i] = lock;

      _threads[i] = new Thread(() -> {
        try {
          logger.debug("wait for start of clock");
          lock.sleep(1);

          component.run();
        } catch (InterruptedException e) {
          logger.debug("Execution has been interrupted");
        }
      }, component.getName());
      _threads[i].setDaemon(true);
    }

    for (int i = 0; i < ticks.length; i++) {
      ticks[i]._lock = _locks[i];
    }
    for (int i = 0; i < ticks.length - 1; i++) {
      ticks[i]._nextLock = _locks[i + 1];
    }
    ticks[components.size() - 1]._nextLock = _finishedTickLock;

    // Just run this thread if no one else is able to run.
    Thread.currentThread().setPriority(Thread.currentThread().getPriority() - 1);

    // Start threads.
    for (int i = 0; i < _threads.length; i++) {
      try {
        _threads[i].start();
        // Wait for the thread to reach the first sleep()
        _locks[i].waitForLock();
      } catch (InterruptedException e) {
        logger.debug("Execution has been interrupted");
      }
    }
    Thread.yield();
  }

  @Override
  protected final void doRun() {
    try {
      while (true) {
        tick();
      }
    } catch (InterruptedException e) {
      logger.debug("run interrupted");
    }
  }

  @Override
  protected final void doRun(int ticks) {
    try {
      while (--ticks >= 0) {
        tick();
      }
    } catch (InterruptedException e) {
      logger.debug("debug run interrupted");
    }
  }

  /**
   * Execute 1 tick.
   */
  protected final void tick() throws InterruptedException {
    startTick();
    _locks[0].wakeup();
    _finishedTickLock.sleep(1);
  }

  @Override
  protected void doClose() {
    for (Thread thread : _threads) {
      thread.interrupt();
    }
    Thread.yield();
  }

  private static class SerializedTick implements Tick {
    private Lock _lock;

    private Lock _nextLock;

    @Override
    public void waitForTick() {
      try {
        _nextLock.wakeup();
        _lock.sleep(1);
      } catch (InterruptedException e) {
        throw new RuntimeException("Thread has been stopped", e);
      }
    }
  }
}
