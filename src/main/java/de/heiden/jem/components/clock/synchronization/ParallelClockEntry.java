package de.heiden.jem.components.clock.synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Bean holding registration information for one clocked component.
 */
public class ParallelClockEntry extends ClockEntry implements AutoCloseable {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Thread to execute component.
   */
  private final Thread thread;

  /**
   * Constructor.
   *
   * @param component clocked component
   * @param tick clock tick
   * @require component != null
   * @require tick != null
   */
  public ParallelClockEntry(ClockedComponent component, Tick tick) {
    super(component, tick);

    thread = new Thread(() -> {
      logger.debug("starting {}", component.getName());
      tick.waitForTick();
      logger.debug("started {}", component.getName());
      component.run();
    }, component.getName());
    thread.setDaemon(true);
  }

  /**
   * Start underlying thread.
   */
  public void start() {
    thread.start();
  }

  /**
   * Interrupt underlying thread.
   */
  @Override
  public void close() {
    thread.interrupt();
  }
}