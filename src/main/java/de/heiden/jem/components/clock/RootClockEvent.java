package de.heiden.jem.components.clock;

/**
 * Dummy event, just for easy implementation of the clock event list.
 */
class RootClockEvent extends ClockEvent {
  /**
   * Constructor.
   */
  RootClockEvent() {
    tick = Long.MAX_VALUE;
  }

  @Override
  public void execute(long tick) {
    throw new UnsupportedOperationException("Root clock event may not be executed");
  }
}
