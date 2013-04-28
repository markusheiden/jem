package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean holding registration information for one clocked component.
 */
public class SerializedClockEntry extends ClockEntry {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Thread used to execute the component.
   */
  private final Thread thread;

  /**
   * Lock for controlling the execution of the thread.
   */
  public final Lock lock;

  /**
   * Constructor.
   *
   * @param component clocked component
   * @param tick clock tick
   * @require component != null
   * @require tick != null
   */
  public SerializedClockEntry(final ClockedComponent component, Tick tick) {
    super(component, tick);

    thread = new Thread(component::run, component.getName());
    this.lock = new Lock(component.getName());
  }

  /**
   * Run the component for one tick.
   */
  public void run() {
    try {
      logger.debug("wait for start of clock");
      lock.setTicksToSleep(1);
      lock.sleep();

      component.run();
    } catch (InterruptedException e) {
      logger.debug("Execution has been interrupted");
    }
  }

  /**
   * Start associated thread.
   */
  public void start() {
    try {
      thread.start();
      // Wait for the thread to reach the first sleep()
      lock.waitForLock();
    } catch (InterruptedException e) {
      logger.debug("Execution has been interrupted");
      // T
    }
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      dispose();
    } finally {
      super.finalize();
    }
  }

  /**
   * Dispose.
   */
  public void dispose() {
    thread.interrupt();
  }
}