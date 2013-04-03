package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockEntry;
import de.heiden.jem.components.clock.ClockedComponent;
import org.serialthreads.Executor;
import org.serialthreads.context.ChainedRunnable;
import org.serialthreads.context.IRunnable;
import org.serialthreads.context.ThreadFinishedException;

/**
 * Clock using serial threads.
 */
public final class SerialClock extends AbstractClock<ClockEntry> {
  /**
   * Current tick.
   */
  private long _tick;

  /**
   * Constructor.
   */
  public SerialClock() {
    // Start at tick -1, because the first action when running is to increment the tick
    _tick = -1;
  }

  @Override
  public long getTick() {
    return _tick;
  }

  @Override
  protected ClockEntry createClockEntry(ClockedComponent component) {
    // every components needs its own Tick instance, because the instances cache its serial thread
    return new ClockEntry(component, new SerialClockTick());
  }

  @Override
  public void run() {
    Counter counter = new Counter();
    createChain(counter);
    run(counter);
  }

  @Override
  public void run(int ticks) {
    assert ticks >= 0 : "Precondition: ticks >= 0";

    StopCounter counter = new StopCounter(ticks);
    createChain(counter);
    run(counter);
  }

  /**
   * Create a chain of runnables from the component.
   * This method needs no return value, because first is (the start of) the chain.
   *
   * @param first First runnable
   */
  private void createChain(ChainedRunnable first) {
    IRunnable[] runnables = new IRunnable[_entryMap.size()];
    int i = 0;
    for (ClockEntry entry : _entryMap.values()) {
      runnables[i++] = entry.component;
    }

    // prepend first to chain
    ChainedRunnable[] chain = ChainedRunnable.chain(runnables);
    first.next = chain[0];
    chain[chain.length - 1].next = first;
  }

  /**
   * Execute runnables.
   *
   * @param chain Runnables as chain
   */
  @Executor
  private void run(ChainedRunnable chain) {
    ChainedRunnable runnable = chain;
    try {
      //noinspection InfiniteLoopStatement
      while (true) {
        runnable = runnable.run();
      }
    } catch (ThreadFinishedException e) {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }

  /**
   * Runnable which increments the tick and executes the events for the new tick.
   */
  private class Counter extends ChainedRunnable {
    @Override
    public final ChainedRunnable run() {
      final long tick = _tick + 1;
      _tick = tick;

      // execute events first, if any
      executeEvent(tick);

      return next;
    }
  }

  /**
   * Runnable which increments the tick and executes the events for the new tick.
   * Stops after a given number of ticks.
   */
  private class StopCounter extends ChainedRunnable {
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
      stop = _tick + 1 + ticks;
    }

    @Override
    public final ChainedRunnable run() {
      final long tick = _tick + 1;

      if (tick == stop) {
        // TODO throw another kind of exception?
        throw new ThreadFinishedException("Stop");
      }

      _tick = tick;

      // execute events first, if any
      executeEvent(tick);

      return next;
    }
  }
}
