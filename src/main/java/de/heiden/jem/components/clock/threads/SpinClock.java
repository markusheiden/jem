package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.List;

/**
 * Clock implemented without synchronization by using spin locks (busy wait).
 */
public class SpinClock extends AbstractSynchronizedClock {
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

    _componentThreads = new ArrayList<>(_componentMap.size());
    int i = 0;
    for (ClockedComponent component : _componentMap.values()) {
      final int number = i++;
      Tick tick = () -> waitForTick(number);
      component.setTick(tick);

      // Start component.
      _componentThreads.add(createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick)));
      // Wait for component to reach first tick.
      do {
        Thread.yield();
      } while (_state != i);
    }

    _tickThread = createStartedDaemonThread("Tick", this::executeTicks);
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Tick.
   */
  private void waitForTick(final int number) {
    // Execute next component thread.
    _state = number + 1;
    // Wait for next turn.
    do {
      Thread.yield();
    } while (_state != number);
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks() {
    final int numComponents = _componentMap.size();
    for (;;) {
      startTick();
      // Execute component threads.
      _state = 0;
      // Wait for component threads to finish tick.
      do {
        Thread.yield();
      } while (_state != numComponents);
    }
  }

  @Override
  protected final void doRun(int ticks) {
    addClockEvent(_tick.get() + ticks, _suspendEvent);
    doRun();
  }

  @Override
  protected final void doRun() {
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
