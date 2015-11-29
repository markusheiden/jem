package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockEvent;

import java.util.List;

/**
 * Base implementation for all clocks using synchronization.
 */
public abstract class AbstractSynchronizedClock extends AbstractClock {
  /**
   * Component threads.
   */
  protected List<Thread> _componentThreads;

  /**
   * Event for suspending execution.
   */
  protected final SuspendEvent _suspendEvent = new SuspendEvent();

  @Override
  public synchronized void addClockEvent(long tick, ClockEvent event) {
    super.addClockEvent(tick, event);
  }

  @Override
  public synchronized void updateClockEvent(long tick, ClockEvent event) {
    super.updateClockEvent(tick, event);
  }

  @Override
  public synchronized void removeClockEvent(ClockEvent event) {
    super.removeClockEvent(event);
  }

  @Override
  protected synchronized void executeEvent(long tick) {
    super.executeEvent(tick);
  }

  /**
   * Create started daemon thread.
   */
  protected Thread createStartedDaemonThread(String name, Runnable runnable) {
    Thread thread = createDaemonThread(name, runnable);
    thread.start();
    return thread;
  }

  /**
   * Create daemon thread.
   */
  protected Thread createDaemonThread(String name, Runnable runnable) {
    Thread thread = new Thread(() -> {
      try {
        runnable.run();
      } catch (ManualAbort e) {
        // ignore
      } catch (Exception e) {
        logger.error("Component failed.", e);
      }
    }, name);
    thread.setDaemon(true);
    return thread;
  }

  @Override
  protected void doClose() {
    _componentThreads.forEach(Thread::interrupt);
    Thread.yield();
  }
}
