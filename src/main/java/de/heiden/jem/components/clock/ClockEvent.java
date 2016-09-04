package de.heiden.jem.components.clock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clock event.
 */
public abstract class ClockEvent implements Comparable<ClockEvent> {
  /**
   * Id sequence.
   */
  private static final AtomicInteger IDS = new AtomicInteger();

  /**
   * Id.
   */
  private final int id;

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

    this.id = IDS.incrementAndGet();
    this.name = name;
  }

  /**
   * Execute event.
   */
  public abstract void execute(long tick);

  @Override
  public final boolean equals(Object o) {
    return o instanceof ClockEvent && id == ((ClockEvent) o).id;
  }

  @Override
  public final int hashCode() {
    return id;
  }

  @Override
  public final int compareTo(ClockEvent o) {
    int result = Long.compare(tick, o.tick);
    return result != 0? result : Integer.compare(id, o.id);
  }

  @Override
  public String toString() {
    return name;
  }
}
