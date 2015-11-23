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
   * Constructor.
   */
  public SuspendEvent() {
    super("Suspend");
  }

  @Override
  public synchronized void execute(long tick) throws ManualAbort {
    logger.info("Suspend at {}.", tick);
    _suspended = true;
    notifyAll();
    try {
      while (_suspended) {
        wait();
      }
    } catch (InterruptedException e) {
      throw new ManualAbort();
    }
    logger.info("Resume at {}.", tick);
  }

  /**
   * Resume execution, if suspended.
   */
  public synchronized void resume() {
    if (_suspended) {
      _suspended = false;
      notifyAll();
    }
  }

  /**
   * Wait for suspend.
   */
  public synchronized void waitForSuspend() throws ManualAbort {
    try {
      while (!_suspended) {
        wait();
      }
    } catch (InterruptedException e) {
      throw new ManualAbort();
    }
  }
}
