package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clock implemented with synchronization.
 * <p/>
 * TODO add / remove clocked components operation could not be execute while clock is running
 */
public class SerializedClock
  extends AbstractSynchronizedClock<SerializedClockEntry>
  implements Tick {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * All clocked components sorted by their position.
   */
  protected SerializedClockEntry[] _entries;

  /**
   * Index of currently excecute entry / clocked component.
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

  @Override
  public void dispose() {
    // TODO
  }

  @Override
  protected SerializedClockEntry createClockEntry(ClockedComponent component) {
    // TODO 2009-04-27 mh: use own Tick instance for every thread
    SerializedClockEntry entry = new SerializedClockEntry(component, this);
    entry.start();

    return entry;
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
    assert _entries.length > 0 : "Precondition: _entries.length > 0";

    int currentIndex = _currentIndex;
//    _logger.debug("wait for tick (" + currentIndex + ")");
    Lock currentLock = _entries[currentIndex].lock;
    currentLock.setTicksToSleep(ticks);

    int nextIndex = currentIndex + 1;
    boolean started = false;
    while (!started && nextIndex < _entries.length) {
      // trigger next component
//      _logger.debug("wakeup next (" + nextIndex + ")");
      _currentIndex = nextIndex;
      started = _entries[nextIndex++].lock.wakeup();
    }

    if (!started) {
      // trigger next tick
//      _logger.debug("wakeup for next tick");
      _finishedTickLock.wakeup();
    }

    // go to sleep
//    _logger.debug("wait (" + currentIndex + ")");
    try {
      currentLock.sleep();
    } catch (InterruptedException e) {
      throw new RuntimeException("Thread has been stopped", e);
    }
  }

  /**
   * Run this clock as master clock.
   */
  @Override
  public void run() {
    logger.debug("run clock");

    // only run this thread if no one else is able to run
    Thread.currentThread().setPriority(Thread.currentThread().getPriority() - 1);

    try {
      while (true) {
        tick();
      }
    } catch (InterruptedException e) {
      logger.debug("run interrupted");
    }
  }

  /**
   * Start debugging run.
   *
   * @param ticks number of ticks to execute.
   */
  @Override
  public void run(int ticks) {
    logger.debug("run clock for " + ticks + " ticks");

    // only run this thread if no one else is able to run
    Thread.currentThread().setPriority(Thread.currentThread().getPriority() - 1);

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
    if (logger.isDebugEnabled()) {
      logger.debug("tick " + _tick);
    }

    executeEvent(_tick);

    // init finished tick lock
    _finishedTickLock.setTicksToSleep(1);

    // execute first component
    int nextIndex = 0;
    boolean started = false;
    while (!started && nextIndex < _entries.length) {
      _currentIndex = nextIndex;
      started = _entries[nextIndex++].lock.wakeup();
    }

    // wait for end of tick
    if (started) {
      _finishedTickLock.sleep();
    }

    // increment ticks / time
    _tick++;
  }
}
