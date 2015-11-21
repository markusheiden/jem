package de.heiden.jem.components.clock.synchronization;

/**
 * A lock encapsulates the wait / notify mechanism to avoid missed
 * notifications.
 */
public final class Lock {
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

  //
  //
  //

  /**
   * Constructor using a default name.
   */
  public Lock() {
    this("Lock");
  }

  /**
   * Constructor.
   *
   * @param name Informal name of the lock.
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
   * Sleeps until waked up.
   */
  public void sleep(int ticks) throws InterruptedException {
    _ticks = ticks;

    if (_noWakeup && ticks == 1) {
      // give other threads the chance to run to avoid wait of this thread
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
   * Wait for the lock being sleeping.
   */
  public void waitForLock() throws InterruptedException {
    synchronized (_lock) {
      while (!_waiting) {
        // Wait for sleep() being called
        _lock.wait(1);
      }
    }
  }

  /**
   * toString.
   */
  public String toString() {
    return _name;
  }
}