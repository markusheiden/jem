package de.heiden.jem.components.clock;

/**
 * Counter for clock tests.
 */
public class CounterComponent implements ClockedComponent {
  /**
   * Tick.
   */
  private Tick _tick;

  /**
   * Count.
   */
  private volatile long count;

  @Override
  public String getName() {
    return "Test counter";
  }

  @Override
  public void setTick(Tick tick) {
    this._tick = tick;
  }

  @Override
  public void run() {
    for (;;) {
      count++;
      _tick.waitForTick();
    }
  }

  /**
   * Count.
   */
  public long getCount() {
    return count;
  }
}
