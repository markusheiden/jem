package de.heiden.jem.components.clock;

import org.serialthreads.Interrupt;

/**
 * Clock tick.
 */
public interface Tick {
  /**
   * Wait for next tick.
   * Called by clocked components.
   */
  @Interrupt
  void waitForTick();
}
