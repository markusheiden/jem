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
    run(new Counter());
  }

  @Override
  protected final void doRun(int ticks) {
    assert ticks >= 0 : "Precondition: ticks >= 0";

    run(new StopCounter(ticks));
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

  /**
   * Runnable which increments the tick and executes the events for the new tick.
   */
  private class Counter implements Runnable {
    @Override
    public final void run() {
      final long tick = _tick.incrementAndGet();

      // execute events first, if any
      executeEvent(tick);
    }
  }

  /**
   * Runnable which increments the tick and executes the events for the new tick.
   * Stops after a given number of ticks.
   */
  private class StopCounter implements Runnable {
    /**
     * Tick to stop at.
     */
    private final long stop;

    /**
     * Constructor.
     *
     * @param ticks Stop after executing this number of ticks
     */
    public StopCounter(long ticks) {
      stop = _tick.get() + 1 + ticks;
    }

    @Override
    public final void run() {
      final long tick = _tick.incrementAndGet();

      if (tick == stop) {
        // TODO throw another kind of exception?
        throw new ThreadFinishedException("Stop");
      }

      // execute events first, if any
      executeEvent(tick);
    }
  }
}
