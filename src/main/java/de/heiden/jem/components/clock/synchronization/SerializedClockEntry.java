package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bean holding registration information for one clocked component.
 */
public class SerializedClockEntry extends ClockEntry {
  /**
   * Logger.
   */
  private final Log logger = LogFactory.getLog(getClass());

  private final Thread _thread;

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

    _thread = new Thread(new Runnable() {
      @Override
      public void run() {
        component.run();
      }
    }, component.getName());
    this.lock = new Lock(component.getName());
  }

  public void run() {
    try {
      logger.debug("wait for start of clock");
      lock.setTicksToSleep(1);
      lock.sleep();

      component.run();
    } catch (InterruptedException e) {
      logger.debug("interrupted");
    }
  }

  /**
   * Start associated thread.
   */
  public void start() {
    _thread.start();
  }

  /**
   * Dispose.
   */
  public void dispose() {
    _thread.interrupt();
  }
}