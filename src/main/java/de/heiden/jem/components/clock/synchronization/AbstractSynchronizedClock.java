package de.heiden.jem.components.clock.synchronization;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockEvent;

/**
 * Base implementation for all clocks using synchronization.
 */
public abstract class AbstractSynchronizedClock<E extends ClockEntry> extends AbstractClock<E> {
  //
  // private attributes
  //

  /**
   * Lock for instance variables.
   */
  protected final Object _lock = new Object();

  //
  //
  //

  @Override
  public boolean isStarted() {
    synchronized (_lock) {
      return super.isStarted();
    }
  }

  @Override
  public void addClockEvent(long tick, ClockEvent event) {
    synchronized (_lock) {
      super.addClockEvent(tick, event);
    }
  }

  @Override
  public void removeClockEvent(ClockEvent event) {
    synchronized (_lock) {
      super.removeClockEvent(event);
    }
  }
}
