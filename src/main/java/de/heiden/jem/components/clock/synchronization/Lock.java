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
   * Remembers if thread to sleep is already waiting.
   * Synchronized by {@link #_lock}.
   */
  private boolean _waiting;

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
   * Constructor.
   *
   * @param name Informal name of the lock.
   * @require name != null
   */
  public Lock(String name) {
    assert name != null : "name != null";

    _name = name;
    _waiting = false;
    _noWakeup = true;
    _lock = new Object();
  }

  /**
   * Sleeps until waked up.
   */
  public void sleep() throws InterruptedException {
    if (_noWakeup) {
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
  public void wakeup() {
    synchronized (_lock) {
      if (_waiting) {
        _lock.notify();
      } else {
        _noWakeup = false;
      }
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