package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;

/**
 * Clock implemented without synchronization sequentially executing components by using spin locks (busy wait).
 */
public final class ParallelSpinClock extends AbstractSynchronizedClock {
  /**
   * Tick thread.
   */
  private Thread _tickThread;

  @Override
  protected void doInit() {
    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    var components = new ArrayList<>(_componentMap.values()).toArray(new ClockedComponent[0]);
    var ticks = new ParallelSpinTick[components.length];
    for (int i = 0; i < components.length; i++) {
      var component = components[i];
      var tick = new ParallelSpinTick();
      component.setTick(tick);
      ticks[i] = tick;

      // Start component.
      createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
      // Wait for component to reach first tick.
      tick.waitForTickEnd();
    }

    // Start tick manager.
    _tickThread = createStartedDaemonThread("Tick", () -> executeTicks(ticks));
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks(final ParallelSpinTick[] ticks) {
    //noinspection InfiniteLoopStatement
    while (true) {
      // Start new tick.
      startTick();
      // Execute all component threads.
      for (ParallelSpinTick tick : ticks) {
        tick.startTick();
      }
      // Wait for all components threads to finish tick.
      for (ParallelSpinTick tick : ticks) {
        tick.waitForTickEnd();
      }
    }
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
  private static final class ParallelSpinTick implements Tick {
    /**
     * State of tick.
     * True: Current tick finished, waiting for next tick.
     * False: Start next tick.
     */
    private volatile boolean _state = false;

    @Override
    public void waitForTick() {
      _state = true;
      do {
        Thread.onSpinWait();
      } while (_state);
    }

    /**
     * Start next tick.
     */
    void startTick() {
      _state = false;
    }

    /**
     * Wait for tick to finish.
     */
    void waitForTickEnd() {
      while (!_state) {
        Thread.onSpinWait();
      }
    }
  }
}
