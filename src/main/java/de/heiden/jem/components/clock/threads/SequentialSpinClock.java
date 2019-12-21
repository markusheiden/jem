package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.List;

/**
 * Clock implemented without synchronization sequentially executing component threads by using spin locks (busy wait).
 */
public final class SequentialSpinClock extends AbstractSynchronizedClock {
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
    for (int i = 0; i < components.size(); i++) {
      final int state = i;
      final int nextState = i + 1;

      ClockedComponent component = components.get(state);
      SequentialSpinTick tick =  new SequentialSpinTick(state);
      component.setTick(tick);

      // Start component.
      createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
      // Wait for component to reach first tick.
      waitForState(nextState);
    }

    // Start tick manager.
    _tickThread = createStartedDaemonThread("Tick", () -> executeTicks(components.size()));
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks(final int state) {
    for (;;) {
      startTick();
      // Execute all component threads.
      _state = 0;
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
      Thread.yield();
    } while (_state != state);
  }

  @Override
  protected void doRun(int ticks) {
    addClockEvent(getTick() + ticks, _suspendEvent);
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
   * Special tick, waiting for its state.
   */
  private final class SequentialSpinTick implements Tick {
    /**
     * Ordinal of component thread.
     */
    private final int _tickState;

    /**
     * Constructor.
     *
     * @param state Ordinal of thread to execute.
     */
    private SequentialSpinTick(int state) {
      this._tickState = state;
    }

    @Override
    public void waitForTick() {
      final int tickState = _tickState;
      // Execute next component thread.
      _state = tickState + 1;
      // Wait for next tick.
      waitForState(tickState);
    }
  }
}
