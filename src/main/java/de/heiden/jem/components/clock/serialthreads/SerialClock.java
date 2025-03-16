package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import org.serialthreads.Executor;
import org.serialthreads.context.ThreadFinishedException;

/**
 * Clock using serial threads.
 */
public final class SerialClock extends AbstractSimpleClock {
  @Override
  @Executor
  protected void doRun() {
    var components = clockedComponents();
    try {
      //noinspection InfiniteLoopStatement
      while (true) {
        startTick();
        for (var runnable : components) {
          runnable.run();
        }
      }
    } catch (ThreadFinishedException e) {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }

  @Override
  @Executor
  protected void doRun(int ticks) {
    assert ticks >= 0 : "Precondition: ticks >= 0";

    var components = clockedComponents();
    try {
      for (final long stop = getTick() + ticks; getTick() < stop;) {
        startTick();
        for (var runnable : components) {
          runnable.run();
        }
      }
    } catch (ThreadFinishedException e) {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }
}
