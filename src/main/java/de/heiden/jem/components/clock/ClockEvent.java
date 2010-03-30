package de.heiden.jem.components.clock;

/**
 * Clock event.
 */
public interface ClockEvent
{
  /**
   * Execute event.
   */
  public void execute(long tick);
}
