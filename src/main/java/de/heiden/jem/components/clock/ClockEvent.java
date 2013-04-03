package de.heiden.jem.components.clock;

/**
 * Clock event.
 */
public abstract class ClockEvent {
  /**
   * Name of event.
   */
  private final String name;

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
   * Constructor.
   */
  public ClockEvent(String name) {
    assert name != null : "name != null";

    this.name = name;
  }

  /**
   * Execute event.
   */
  public abstract void execute(long tick);

  @Override
  public String toString() {
    return name;
  }
}
