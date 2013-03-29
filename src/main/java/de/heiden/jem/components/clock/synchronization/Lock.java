package de.heiden.jem.components.clock.synchronization;

/**
 * A lock encapsulates the wait / notify mechanism to avoid missed
 * notifications.
 */
public final class Lock {
  /**
   * Constructor using default name.
   */
  public Lock() {
    this("lock");
  }

  /**
   * Constructor.
   *
   * @param name informal name of lock.
   * @require name != null
   */
  public Lock(String name) {
    assert name != null : "name != null";

    _name = name;
    _ticks = 0;
    _waiting = false;
    _noWakeup = true;
    _lock = new Object();
  }

  /**
   * Set number cycles to sleep.
   *
   * @param ticks ticks to sleep
   * @require ticks > 0
   */
  public void setTicksToSleep(int ticks) {
    assert ticks > 0 : "ticks > 0";

    _ticks = ticks;
  }

  /**
   * Sleeps until waked up.
   * Should only be used after setting ticks to sleep!
   */
  public void sleep() throws InterruptedException {
    if (_noWakeup && _ticks == 1) {
      // give other threads chance to run to avoid wait of this thread
      Thread.yield();
    }
    synchronized (_lock) {
      if (_noWakeup) {
        _waiting = true;
        _lock.wait();
        _waiting = false;
      } else {
        _noWakeup = true;
      }
    }
  }

  /**
   * Wakeup.
   *
   * @return Lock has been waked up.
   */
  public boolean wakeup() {
    int ticks = _ticks;
    if (--ticks == 0) {
      // really wakeup
      synchronized (_lock) {
        if (_waiting) {
          _lock.notify();
        } else {
          _noWakeup = false;
        }
        return true;
      }
    } else {
      // sleep farther
      _ticks = ticks;
      return false;
    }
  }

  /**
   * toString.
   */
  public String toString() {
    return _name;
  }

  //
  // private attributes
  //

  /**
   * Informal name of lock.
   */
  private final String _name;

  /**
   * Numer of ticks to sleep.
   */
  private volatile int _ticks;

  /**
   * Remembers if thread to sleep is already waiting.
   */
  private volatile boolean _waiting;

  /**
   * Remembers if thread has not already been waked up.
   */
  private volatile boolean _noWakeup;

  /**
   * Object used as lock.
   */
  private final Object _lock;
}