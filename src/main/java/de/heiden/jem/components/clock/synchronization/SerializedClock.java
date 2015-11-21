package de.heiden.jem.components.clock.synchronization;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented with synchronization.
 * <p/>
 * TODO add / remove clocked components operation could not be execute while clock is running
 */
public class SerializedClock extends AbstractSynchronizedClock implements Tick {
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
   * Index of currently executed entry / clocked component.
   */
  protected int _currentIndex;

  /**
   * Lock for sleeping until tick is finished.
   */
  protected final Lock _finishedTickLock;

  //
  // public
  //

  /**
   * Constructor.
   */
  public SerializedClock() {
    _currentIndex = 0;
    _finishedTickLock = new Lock();
  }

  /**
   * Wait for next tick. Called by clocked components.
   */
  @Override
  public void waitForTick() {
    waitForTick(1);
  }

  /**
   * Wait for next tick. Called by clocked components.
   */
  public void waitForTick(int ticks) {
    int currentIndex = _currentIndex;
//    _logger.debug("wait for tick ({})", currentIndex);
    Lock currentLock = _locks[currentIndex];
    currentLock.setTicksToSleep(ticks);

    int nextIndex = currentIndex + 1;
    boolean started = false;
    while (!started && nextIndex < _locks.length) {
      // trigger next component
//      _logger.debug("wakeup next ({})", nextIndex);
      _currentIndex = nextIndex;
      started = _locks[nextIndex++].wakeup();
    }

    if (!started) {
      // trigger next tick
//      _logger.debug("wakeup for next tick");
      _finishedTickLock.wakeup();
    }

    // go to sleep
//    _logger.debug("wait ({})", currentIndex);
    try {
      currentLock.sleep();
    } catch (InterruptedException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  @Override
  protected Tick createTick(ClockedComponent component) {
    // TODO 2009-04-27 mh: use own Tick instance for every thread
    return this;
  }

  @Override
  protected void doInit() {
    // Create threads.
    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    _locks = new Lock[components.size()];
    _threads = new Thread[components.size()];
    for (int i = 0; i < _threads.length; i++) {
      ClockedComponent component = components.get(i);

      Lock lock = new Lock(component.getName());
      _locks[i] = lock;

      _threads[i] = new Thread(() -> {
        try {
          logger.debug("wait for start of clock");
          lock.setTicksToSleep(1);
          lock.sleep();

          component.run();
        } catch (InterruptedException e) {
          logger.debug("Execution has been interrupted");
        }
      }, component.getName());
      _threads[i].setDaemon(true);
    }

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
    // First increment tick.
    long tick = _tick.incrementAndGet();
    logger.debug("tick {}", tick);

    // Second execute events.
    executeEvent(tick);

    // init finished tick lock
    _finishedTickLock.setTicksToSleep(1);

    // Third execute components.
    int nextIndex = 0;
    boolean started = false;
    while (!started && nextIndex < _locks.length) {
      _currentIndex = nextIndex;
      started = _locks[nextIndex++].wakeup();
    }

    // wait for end of tick
    if (started) {
      _finishedTickLock.sleep();
    }
  }

  @Override
  protected void doClose() {
    for (Thread thread : _threads) {
      thread.interrupt();
    }
    Thread.yield();
  }
}
