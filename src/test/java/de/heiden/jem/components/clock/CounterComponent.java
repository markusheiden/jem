package de.heiden.jem.components.clock;

import org.serialthreads.Interruptible;

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
  @Interruptible
  public void run() {
      //noinspection InfiniteLoopStatement
      while (true) {
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

  @Override
  public String toString() {
    return Long.toString(count);
  }
}
