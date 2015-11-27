package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.List;

/**
 * Clock implemented without synchronization sequentially executing components by using spin locks (busy wait).
 */
public class SequentialSpinClock extends AbstractSynchronizedClock {
  /**
   * Component threads.
   */
  private List<Thread> _componentThreads;

  /**
   * Tick thread.
   */
  private Thread _tickThread;

  /**
   * Ordinal of thread to execute.
   */
  private volatile int _state = 0;

  /**
   * Event for suspending execution.
   */
  private final SuspendEvent _suspendEvent = new SuspendEvent();

  @Override
  protected void doInit() {
    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    _componentThreads = new ArrayList<>(components.size());
    for (int i = 0; i < components.size(); i++) {
      final int state = i;
      final int nextState = i + 1;

      ClockedComponent component = components.get(state);
      Tick tick = () -> waitForTick(state);
      component.setTick(tick);

      // Start component.
      _componentThreads.add(createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick)));
      // Wait for component to reach first tick.
      waitForState(nextState);
    }

    // Start thread manager.
    _tickThread = createStartedDaemonThread("Tick", this::executeTicks);
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Tick.
   */
  private void waitForTick(final int state) {
    // Execute next component thread.
    _state = state + 1;
    // Wait for next turn.
    waitForState(state);
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks() {
    final int lastState = _componentMap.size();
    for (;;) {
      startTick();
      // Execute component threads.
      _state = 0;
      // Wait for component threads to finish tick.
      waitForState(lastState);
    }
  }

  /**
   * Busy wait until state is reached.
   *
   * @param state State to reach.
   */
  private void waitForState(final int state) {
    do {
      Thread.yield();
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
    _componentThreads.forEach(Thread::interrupt);
    _tickThread.interrupt();
    Thread.yield();
  }
}
