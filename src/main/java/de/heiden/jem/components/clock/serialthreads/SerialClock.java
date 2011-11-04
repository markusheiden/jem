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
public class SerialClock extends AbstractClock<ClockEntry>
{
  /**
   * Current tick.
   */
  private long _tick;

  /**
   * Constructor.
   */
  public SerialClock()
  {
    _tick = -1;
  }

  @Override
  public long getTick()
  {
    return _tick;
  }

  @Override
  protected ClockEntry createClockEntry(ClockedComponent component)
  {
    // every components needs its own Tick instance, because the instances cache its serial thread
    return new ClockEntry(component, new SerialClockTick());
  }

  @Override
  @Executor
  public final void run()
  {
    final ChainedRunnable first = createChain()[0];
    ChainedRunnable chain = first;
    long tick = _tick;

    try
    {
      //noinspection InfiniteLoopStatement
      while (true)
      {
        // execute events first, if any
        if (_nextEventTick <= tick)
        {
          executeEvent(tick);
        }

        // execute chain one tick
        do
        {
          chain.runnable.run();
          chain = chain.next;
        } while (chain != first);

        _tick = ++tick;
      }
    }
    catch (ThreadFinishedException e)
    {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }

  @Override
  @Executor
  public void run(int ticks)
  {
    assert ticks >= 0 : "Precondition: ticks >= 0";

    if (ticks <= 0)
    {
      return;
    }

    final ChainedRunnable first = createChain()[0];
    ChainedRunnable chain = first;
    long tick = _tick;

    try
    {
      for (; ticks != 0; ticks--)
      {
        // execute events first, if any
        if (_nextEventTick <= tick)
        {
          executeEvent(tick);
        }

        // execute chain one tick
        do
        {
          chain.runnable.run();
          chain = chain.next;
        } while (chain != first);

        _tick = ++tick;
      }
    }
    catch (ThreadFinishedException e)
    {
      // TODO 2009-12-11 mh: should not happen!!!
    }
  }

  /**
   * Create a chain of runnables from the component.
   */
  private ChainedRunnable[] createChain()
  {
    IRunnable[] runnables = new IRunnable[_entryMap.size()];
    int i = 0;
    for (ClockEntry entry : _entryMap.values())
    {
      runnables[i++] = entry.component;
    }

    return ChainedRunnable.chain(runnables);
  }
}
