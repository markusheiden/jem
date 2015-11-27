package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.List;

/**
 * Clock implemented without synchronization sequentially executing components by using spin locks (busy wait).
 */
public class ParallelSpinClock extends AbstractSynchronizedClock {
  /**
   * Tick thread.
   */
  private Thread _tickThread;

  @Override
  protected void doInit() {
    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
    _componentThreads = new ArrayList<>(components.size());
    ParallelSpinTick[] ticks = new ParallelSpinTick[components.size()];
    for (int i = 0; i < components.size(); i++) {
      ClockedComponent component = components.get(i);
      ParallelSpinTick tick = new ParallelSpinTick();
      component.setTick(tick);
      ticks[i] = tick;

      // Start component.
      _componentThreads.add(createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick)));
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
  private void executeTicks(ParallelSpinTick[] ticks) {
    for (;;) {
      // Start new tick.
      startTick();
      // Start all component threads.
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

  /**
   * Special tick.
   */
  static final class ParallelSpinTick implements Tick {
    /**
     * State of tick.
     * True: Current tick finished, waiting for next tick.
     * False: Start next tick.
     */
    private volatile boolean _state = false;

    @Override
    public final void waitForTick() {
      _state = true;
      while (_state) {
        Thread.yield();
      }
    }

    /**
     * Start next tick.
     */
    final void startTick() {
      _state = false;
    }

    /**
     * Wait for tick to finish.
     */
    final void waitForTickEnd() {
      while (!_state) {
        Thread.yield();
      }
    }
  }
}
