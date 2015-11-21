package de.heiden.jem.components.clock.synchronization;

import java.util.concurrent.atomic.AtomicBoolean;

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
  private boolean _waiting = false;

  /**
   * Remembers if thread has not already been waked up.
   */
  private final AtomicBoolean _noWakeup = new AtomicBoolean(true);

  /**
   * Object used as lock.
   */
  private final Object _lock = new Object();

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
  }

  /**
   * Sleeps until waked up.
   */
  public void sleep() throws InterruptedException {
    synchronized (_lock) {
      if (_noWakeup.getAndSet(true)) {
        _waiting = true;
        _lock.wait();
        _waiting = false;
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
        _noWakeup.set(false);
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
        _lock.wait(0, 100);
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