package de.heiden.jem.components.clock;

/**
 * Clock event.
 */
public abstract class ClockEvent implements Comparable<ClockEvent> {
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
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final int compareTo(ClockEvent o) {
    return Long.compare(tick, o.tick);
  }

  @Override
  public String toString() {
    return name;
  }
}
