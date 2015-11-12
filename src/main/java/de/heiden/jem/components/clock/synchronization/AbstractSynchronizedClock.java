package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockEvent;

/**
 * Base implementation for all clocks using synchronization.
 */
public abstract class AbstractSynchronizedClock<E extends ClockEntry> extends AbstractClock<E> {
  @Override
  public synchronized void addClockEvent(long tick, ClockEvent event) {
    super.addClockEvent(tick, event);
  }

  @Override
  public synchronized void removeClockEvent(ClockEvent event) {
    super.removeClockEvent(event);
  }
}
