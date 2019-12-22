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
   * Previous clock event.
   * Used for efficient list implementation in clocks.
   */
  ClockEvent previous;

  /**
   * Next clock event.
   * Used for efficient list implementation in clocks.
   */
  ClockEvent next;

  /**
   * Constructor.
   */
  protected ClockEvent(String name) {
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
