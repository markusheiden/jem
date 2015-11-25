package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * Clock implemented with synchronization, executing component threads sequentially.
 *
 * TODO 2015-11-23 markus: Spurious wakeups are not handled.
 */
public class SequentialClock extends AbstractSynchronizedClock {
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
   * Blocker for parking of threads.
   */
  private final Object _blocker = new Object();

  /**
   * Event for suspending execution.
   */
  private final SuspendEvent _suspendEvent = new SuspendEvent();

  @Override
  protected void doInit() {
    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    _componentThreads = new ArrayList<>(_componentMap.size());
    SerializedTick previousTick = null;
    for (ClockedComponent component : _componentMap.values()) {
      SerializedTick tick = new SerializedTick(_blocker);
      component.setTick(tick);
      Thread componentThread = createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
      _componentThreads.add(componentThread);
      if (previousTick != null) {
        previousTick.nextThread = componentThread;
      }
      Thread.yield();

      previousTick = tick;
    }

    _tickThread = createDaemonThread("Tick", this::executeTicks);
    previousTick.nextThread = _tickThread;
    _tickThread.start();
    Thread.yield();

    _suspendEvent.waitForSuspend();
  }

  /**
   * Execution of ticks.
   */
  private void executeTicks() {
    final Thread firstThread = _componentThreads.get(0);
    for (;;) {
      startTick();
      // Execute component threads.
      LockSupport.unpark(firstThread);
      // Wait for component threads to finish.
      // There is no problem, if this thread is not parked when the last threads unparks it:
      // In this case this park will not block, see LockSupport.unpark() javadoc.
      LockSupport.park(_blocker);
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
   * Special tick, parking its thread but unparking the next thread before.
   */
  private static class SerializedTick implements Tick {
    /**
     * Thread of next component.
     */
    private Thread nextThread;

    /**
     * Blocker.
     */
    private final Object _blocker;

    /**
     * Constructor.
     *
     * @param blocker Blocker.
     */
    public SerializedTick(Object blocker) {
      this._blocker = blocker;
    }

    @Override
    public void waitForTick() {
      // Execute next component thread.
      LockSupport.unpark(nextThread);
      // Wait for next tick.
      // There is no problem, if this thread is not parked when the previous threads unparks it:
      // In this case this park will not block, see LockSupport.unpark() javadoc.
      LockSupport.park(_blocker);
    }
  }
}
