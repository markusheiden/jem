package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.apache.log4j.Logger;

/**
 * Bean holding registration information for one clocked component.
 */
public class SerializedClockEntry extends ClockEntry
{
  /**
   * Constructor.
   *
   * @param component clocked component
   * @param tick clock tick
   * @require component != null
   * @require tick != null
   */
  public SerializedClockEntry(final ClockedComponent component, Tick tick)
  {
    super(component, tick);

    _thread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        component.run();
      }
    }, component.getName());
    this.lock = new Lock(component.getName());
  }

  public void run()
  {
    try
    {
      _logger.debug("wait for start of clock");
      lock.setTicksToSleep(1);
      lock.sleep();

      component.run();
    }
    catch (InterruptedException e)
    {
      _logger.debug("interrupted");
    }
  }

  /**
   * Start associated thread.
   */
  public void start()
  {
    _thread.start();
  }

  /**
   * Dispose.
   */
  public void dispose()
  {
    _thread.interrupt();
  }

  //
  // attributes
  //

  private final Thread _thread;

  public final Lock lock;

  /**
   * Logger.
   */
  private static final Logger _logger = Logger.getLogger(SerializedClockEntry.class);
}