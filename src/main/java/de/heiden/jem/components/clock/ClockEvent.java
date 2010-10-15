package de.heiden.jem.components.clock;

/**
 * Clock event.
 */
public abstract class ClockEvent
{
  /**
   * Tick of clock event.
   * Used for efficient list implementation in clocks.
   */
  long tick;
  /**
   * Next clock event.
   * Used for efficient list implementation in clocks.
   */
  ClockEvent next;

  /**
   * Execute event.
   */
  public abstract void execute(long tick);
}
