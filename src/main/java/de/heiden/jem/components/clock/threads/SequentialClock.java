package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * Clock implemented with synchronization sequentially executing component threads.
 */
public class SequentialClock extends AbstractSynchronizedClock {
  /**
   * Tick thread.
   */
  private Thread _tickThread;

  /**
   * Ordinal of component thread to execute.
   * Package visible to avoid synthetic accessors.
   */
  volatile int _state = 0;

  @Override
  protected void doInit() {
    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    Thread firstThread = null;
    SequentialTick previousTick = null;
    for (int i = 0; i < components.size(); i++) {
      final int state = i;
      final int nextState = i + 1;

      ClockedComponent component = components.get(state);
      SequentialTick tick = new SequentialTick(state);
      component.setTick(tick);

      // Start component.
      Thread thread = createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
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
    final Thread finalFirstThread = firstThread;
    _tickThread = createDaemonThread("Tick", () -> executeTicks(components.size(), finalFirstThread));
    previousTick._nextThread = _tickThread;
    _tickThread.start();
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks(final int state, final Thread nextThread) {
    for (;;) {
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

  @Override
  protected void doRun(int ticks) {
    addClockEvent(_tick.get() + ticks, _suspendEvent);
    doRun();
  }

  @Override
  protected void doRun() {
    _suspendEvent.resume();
    _suspendEvent.waitForSuspend();
  }

  @Override
  protected void doClose() {
    _tickThread.interrupt();
    super.doClose();
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
