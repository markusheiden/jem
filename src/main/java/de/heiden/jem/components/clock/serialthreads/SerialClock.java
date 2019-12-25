package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;
import org.serialthreads.Executor;
import org.serialthreads.context.IRunnable;
import org.serialthreads.context.ThreadFinishedException;

/**
 * Clock using serial threads.
 */
public final class SerialClock extends AbstractSimpleClock {
  @Override
  @Executor
  protected final void doRun() {
    final ClockedComponent[] components =
            _componentMap.values().toArray(new ClockedComponent[0]);

    try {
      //noinspection InfiniteLoopStatement
      while (true) {
        startTick();
        for (final IRunnable runnable : components) {
          runnable.run();
        }
      }
    } catch (ThreadFinishedException e) {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }

  @Override
  @Executor
  protected final void doRun(int ticks) {
    assert ticks >= 0 : "Precondition: ticks >= 0";

    final ClockedComponent[] components =
            _componentMap.values().toArray(new ClockedComponent[0]);

    try {
      for (final long stop = getTick() + ticks; getTick() < stop;) {
        startTick();
        for (final IRunnable runnable : components) {
          runnable.run();
        }
      }
    } catch (ThreadFinishedException e) {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }
}
