package de.heiden.jem.components.clock;

/**
 * Bean holding registration information for one clocked component.
 */
public class ClockEntry {
  /**
   * Component.
   */
  protected final ClockedComponent component;

  /**
   * Tick.
   */
  protected final Tick tick;

  /**
   * Constructor.
   *
   * @param component clocked component
   * @param tick clock tick
   * @require component != null
   * @require tick != null
   */
  public ClockEntry(ClockedComponent component, Tick tick) {
    assert component != null : "Precondition: component != null";
    assert tick != null : "Precondition: tick != null";

    this.component = component;
    this.tick = tick;
  }

  /**
   * Component.
   */
  public ClockedComponent getComponent() {
    return component;
  }

  /**
   * Tick.
   */
  public Tick getTick() {
    return tick;
  }
}
