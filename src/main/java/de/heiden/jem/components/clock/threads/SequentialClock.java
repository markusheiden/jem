package de.heiden.jem.components.clock.threads;

import java.util.concurrent.locks.LockSupport;

import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented with synchronization sequentially executing component threads.
 */
public final class SequentialClock extends AbstractSynchronizedClock {
  /**
   * Ordinal of component thread to execute.
   * Package visible to avoid synthetic accessors.
   */
  volatile int _state = 0;

  @Override
  protected void doSynchronizedInit() {
    var components = clockedComponents();
    Thread firstThread = null;
    SequentialTick previousTick = null;
    for (int i = 0; i < components.length; i++) {
      final int state = i;
      final int nextState = i + 1;

      var component = components[state];
      var tick = new SequentialTick(state);
      component.setTick(tick);

      // Start component.
      var thread = createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
      if (firstThread == null) {
        firstThread = thread;
      }
      if (previousTick != null) {
        previousTick._nextThread = thread;
      }
      // Wait for component to reach first tick.
      waitForState(nextState);

      previousTick = tick;
    }

    // Start tick manager.
    var finalFirstThread = firstThread;
    var tickThread = createDaemonThread("Tick", () -> executeTicks(components.length, finalFirstThread));
    previousTick._nextThread = tickThread;
    tickThread.start();
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks(final int state, final Thread nextThread) {
    //noinspection InfiniteLoopStatement
    while (true) {
      startTick();
      // Execute all component threads.
      _state = 0;
      LockSupport.unpark(nextThread);
      // Wait for all component threads to finish tick.
      waitForState(state);
    }
  }

  /**
   * Busy wait until state is reached.
   * Package visible to avoid synthetic accessors.
   *
   * @param state State to reach.
   */
  final void waitForState(final int state) {
    do {
      // There is no problem, if this thread is not parked when the previous threads unparks it:
      // In this case this park will not block, see LockSupport.unpark() javadoc.
      LockSupport.park(this);
    } while (_state != state);
  }

  /**
   * Special tick, waiting for its state and parking its thread but unparking the next thread before.
   */
  private final class SequentialTick implements Tick {
    /**
     * Ordinal of component thread.
     */
    private final int _tickState;

    /**
     * Thread of next component.
     * First this is the current thread to avoid parking the thread executing {@link #doInit()}.
     */
    private Thread _nextThread = Thread.currentThread();

    /**
     * Constructor.
     *
     * @param state Ordinal of thread to execute.
     */
    private SequentialTick(int state) {
      this._tickState = state;
    }

    @Override
    public void waitForTick() {
      final int tickState = _tickState;
      // Execute next component thread.
      _state = tickState + 1;
      LockSupport.unpark(_nextThread);
      // Wait for next tick.
      waitForState(tickState);
    }
  }
}
