package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Bean holding registration information for one clocked component.
 */
public class ParallelClockEntry extends ClockEntry {
  /**
   * Constructor.
   *
   * @param component clocked component
   * @param tick clock tick
   * @require component != null
   * @require tick != null
   */
  public ParallelClockEntry(ClockedComponent component, Tick tick, Thread thread) {
    super(component, tick);

    this.thread = thread;
  }

  //
  // attributes
  //

  public final Thread thread;
}