package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockEvent;

/**
 * Base implementation for all clocks using synchronization.
 */
public abstract class AbstractSynchronizedClock extends AbstractClock {
  /**
   * Component threads.
   */
  protected final ThreadGroup _componentThreads = new ThreadGroup(getClass().getSimpleName());
  {
    _componentThreads.setDaemon(false);
  }

  /**
   * Monitor for synchronization.
   */
  private final Object _monitor = new Object();

  /**
   * Event for suspending execution.
   */
  protected final SuspendEvent _suspendEvent = new SuspendEvent(_monitor);

  @Override
  public void addClockEvent(long tick, ClockEvent event) {
    synchronized (_monitor) {
      super.addClockEvent(tick, event);
    }
  }

  @Override
  public void updateClockEvent(long tick, ClockEvent event) {
    synchronized (_monitor) {
      super.updateClockEvent(tick, event);
    }
  }

  @Override
  public void removeClockEvent(ClockEvent event) {
    synchronized (_monitor) {
      super.removeClockEvent(event);
    }
  }

  @Override
  protected void executeEvents(long tick) {
    synchronized (_monitor) {
      super.executeEvents(tick);
    }
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
    Thread thread = new Thread(_componentThreads, () -> {
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
    _componentThreads.interrupt();
    Thread.yield();
  }
}
