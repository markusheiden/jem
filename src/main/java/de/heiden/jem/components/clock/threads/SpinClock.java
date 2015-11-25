package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clock implemented without synchronization by using spin locks (busy wait).
 */
public class SpinClock extends AbstractSynchronizedClock {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

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
  private final AtomicInteger _state = new AtomicInteger(-1);

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
      SpinTick tick = new SpinTick(_state);
      component.setTick(tick);
      tick.number = i++;
      tick.nextNumber = i;

      _componentThreads.add(createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick)));
      Thread.yield();
    }

    _tickThread = createStartedDaemonThread("Tick", this::executeTicks);
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Execution of component.
   */
  private void executeComponent(ClockedComponent component, SpinTick tick) {
    logger.debug("Starting {}.", component.getName());
    // Wait for the first tick.
    tick.waitForTick();
    logger.debug("Started {}.", component.getName());
    component.run();
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks() {
    final int numComponents = _componentMap.size();
    for (;;) {
      startTick();
      // Execute component threads.
      _state.set(0);
      // Wait for component threads to finish tick.
      do {
        Thread.yield();
      } while (_state.get() != numComponents);
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

  /**
   * Special tick, which busy wait on the next ticke.
   */
  private static class SpinTick implements Tick {
    /**
     * Ordinal of next component.
     */
    private int nextNumber;

    /**
     * Ordinal of component.
     */
    private int number;

    /**
     * Ordinal of thread to execute.
     */
    private final AtomicInteger _state;

    /**
     * Constructor.
     *
     * @param state State.
     */
    public SpinTick(AtomicInteger state) {
      this._state = state;
    }

    @Override
    public final void waitForTick() {
      // Execute next component thread.
      _state.set(nextNumber);
      // Wait for next turn.
      do {
        Thread.yield();
      } while (_state.get() != number);
    }
  }
}
