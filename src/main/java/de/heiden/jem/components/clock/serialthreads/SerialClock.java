package de.heiden.jem.components.clock.serialthreads;

import org.serialthreads.Executor;
import org.serialthreads.Interrupt;
import org.serialthreads.context.IRunnable;
import org.serialthreads.context.ThreadFinishedException;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Clock using serial threads.
 */
public final class SerialClock extends AbstractClock {
  @Override
  protected Tick createTick(ClockedComponent component) {
    // Serial threads do not support lambdas by now, so we are using an inner class here.
    return new Tick() {
      @Override
      @Interrupt
      public void waitForTick() {
      }
    };
  }

  @Override
  protected final void doRun() {
    run(this::startTick);
  }

  @Override
  protected final void doRun(int ticks) {
    assert ticks >= 0 : "Precondition: ticks >= 0";

    final long stop = _tick.get() + 1 + ticks;
    run(() -> {
      if (_tick.get() == stop) {
        throw new ThreadFinishedException("Stop");
      }
      startTick();
    });
  }

  /**
   * Execute runnables.
   *
   * @param startTick Runnable executed to start a new tick.
   */
  @Executor
  private void run(final Runnable startTick) {
    final ClockedComponent[] components =
      _componentMap.values().toArray(new ClockedComponent[_componentMap.size()]);

    try {
      //noinspection InfiniteLoopStatement
      while (true) {
        startTick.run();
        for (IRunnable runnable : components) {
          runnable.run();
        }
      }
    } catch (ThreadFinishedException e) {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }
}
