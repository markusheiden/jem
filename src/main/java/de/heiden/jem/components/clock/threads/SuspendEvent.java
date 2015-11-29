package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event to suspend clock run.
 */
public final class SuspendEvent extends ClockEvent {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Has the run been suspended?.
   */
  private boolean _suspended = false;

  /**
   * Monitor for synchronization.
   */
  private final Object _monitor;

  /**
   * Constructor.
   *
   * @param monitor Monitor for synchronization.
   */
  public SuspendEvent(Object monitor) {
    super("Suspend");

    this._monitor = monitor;
  }

  @Override
  public void execute(long tick) throws ManualAbort {
    synchronized (_monitor) {
      logger.info("Suspend at {}.", tick);
      _suspended = true;
      _monitor.notifyAll();
      try {
        while (_suspended) {
          _monitor.wait();
        }
      } catch (InterruptedException e) {
        throw new ManualAbort();
      }
      logger.info("Resume at {}.", tick);
    }
  }

  /**
   * Resume execution, if suspended.
   */
  public void resume() {
    synchronized (_monitor) {
      if (_suspended) {
        _suspended = false;
        _monitor.notifyAll();
      }
    }
  }

  /**
   * Wait for suspend.
   */
  public void waitForSuspend() throws ManualAbort {
    try {
      synchronized (_monitor) {
        while (!_suspended) {
          _monitor.wait();
        }
      }
    } catch (InterruptedException e) {
      throw new ManualAbort();
    }
  }
}
