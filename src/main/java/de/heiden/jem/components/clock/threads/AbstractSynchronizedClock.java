package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Base implementation for all clocks using synchronization.
 */
public abstract class AbstractSynchronizedClock extends AbstractClock {
  /**
   * Component threads.
   */
  private final ThreadGroup _componentThreads = new ThreadGroup(getClass().getSimpleName());
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

  /**
   * Current tick.
   * Start at tick -1, because the first action when running is to increment the tick.
   */
  private final AtomicLong _tick = new AtomicLong(-1);

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

  protected final void executeComponent(ClockedComponent component, Tick tick) {
//    logger.debug("starting {}", component.getName());
    tick.waitForTick();
//    logger.debug("started {}", component.getName());
    component.run();
  }

  @Override
  protected void doClose() {
    _componentThreads.interrupt();
    Thread.yield();
  }

  /**
   * Start a new tick.
   */
  protected final void startTick() {
    // First increment tick.
    // Second execute events.
    executeEvents(_tick.incrementAndGet());
    // Third execute components.
  }

  @Override
  public long getTick() {
    return _tick.get();
  }
}
