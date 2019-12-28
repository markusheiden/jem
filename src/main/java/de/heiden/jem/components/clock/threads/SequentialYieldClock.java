package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.List;

/**
 * Clock implemented without synchronization sequentially executing component threads by using yield locks (busy wait).
 */
public final class SequentialYieldClock extends AbstractSynchronizedClock {
  /**
   * Threads.
   */
  private List<Thread> threads = new ArrayList<>();

  /**
   * Ordinal of component thread to execute.
   * Package visible to avoid synthetic accessors.
   */
  volatile int _state = 0;

  @Override
  protected void doSynchronizedInit() {
    var components = new ArrayList<>(_componentMap.values());
    for (int state = 0; state < components.size(); state++) {
      var component = components.get(state);
      var tick = new SequentialSpinTick(state);
      component.setTick(tick);

      // Start component.
      threads.add(createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick)));
      // Wait for component to reach first tick.
      while (_state != state + 1) {
        Thread.yield();
      }
    }

    // Start tick manager.
    threads.add(createStartedDaemonThread("Tick", () -> executeTicks(components.size())));
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks(final int finalState) {
    //noinspection InfiniteLoopStatement
    while (true) {
      startTick();
      // Execute all component threads.
      _state = 0;
      // Wait for all component threads to finish tick.
      do {
        Thread.yield();
      } while (_state != finalState);
    }
  }

  @Override
  protected void doClose() {
    threads.forEach(Thread::interrupt);
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
     * @param state Ordinal of component thread.
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
      do {
        Thread.yield();
      } while (_state != tickState);
    }
  }
}
