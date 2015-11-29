package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.LockSupport;

/**
 * Clock implemented with synchronization, executing component threads sequentially.
 *
 * TODO 2015-11-23 markus: Spurious wakeups are not handled.
 */
public class SequentialClock extends AbstractSynchronizedClock {
  /**
   * Tick thread.
   */
  private Thread _tickThread;

  @Override
  protected void doInit() {
    // Suspend execution at start of first tick.
    addClockEvent(0, _suspendEvent);

    Collection<ClockedComponent> components = _componentMap.values();
    _componentThreads = new ArrayList<>(components.size());
    SerializedTick previousTick = null;
    for (ClockedComponent component : components) {
      SerializedTick tick = new SerializedTick();
      component.setTick(tick);
      Thread thread = createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
      _componentThreads.add(thread);
      if (previousTick != null) {
        previousTick.nextThread = thread;
      }
      // Wait for component to reach first tick.
      do {
        Thread.yield();
      } while (!thread.getState().equals(Thread.State.WAITING));

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
      LockSupport.park(this);
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
  static final class SerializedTick implements Tick {
    /**
     * Thread of next component.
     */
    private Thread nextThread;

    @Override
    public void waitForTick() {
      // Execute next component thread.
      LockSupport.unpark(nextThread);
      // Wait for next tick.
      // There is no problem, if this thread is not parked when the previous threads unparks it:
      // In this case this park will not block, see LockSupport.unpark() javadoc.
      LockSupport.park(this);
    }
  }
}
