package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.Tick;
import org.serialthreads.Interrupt;

/**
 * Tick for serial clocks.
 */
class SerialClockTick implements Tick {
  @Override
  @Interrupt
  public final void waitForTick() {
  }
}
