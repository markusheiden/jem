package de.heiden.jem.components.clock.synchronization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented with synchronization, executing component threads sequentially.
 * <p/>
 * TODO add / remove clocked components operation could not be execute while clock is running
 */
public class SerializedClock extends AbstractSynchronizedClock {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Component threads.
   */
  private Thread[] _threads;

  /**
   * Fair singleton semaphore for sequential execution of component threads.
   */
  private final Semaphore _semaphore = new Semaphore(1, true);

  /**
   * Event for suspending execution.
   */
  private final SuspendEvent _suspendEvent = new SuspendEvent();

  //
  // public
  //

  @Override
  protected Tick createTick(ClockedComponent component) {
    return this::waitForTick;
  }

  /**
   * Wait for next tick. Called by clocked components.
   */
  private void waitForTick() {
    try {
      // Let other components run.
      _semaphore.release();
      // Wait for next tick.
      _semaphore.acquire();
      // Run again.
    } catch (InterruptedException e) {
      throw new ManualAbort();
    }
  }

  @Override
  protected void doInit() {
    try {
      // Suspend execution at start of first tick.
      addClockEvent(0, _suspendEvent);

      // Block semaphore for first.
      _semaphore.acquire();

      List<ClockedComponent> components = new ArrayList<>(_componentMap.values());
      _threads = new Thread[1 + components.size()];
      for (int i = 1; i < _threads.length; i++) {
        ClockedComponent component = components.get(i - 1);

        _threads[i] = createStartedDaemonThread(component.getName(), () -> {
          logger.debug("starting {}", component.getName());
          try {
            // Wait for first tick.
            _semaphore.acquire();
          } catch (InterruptedException e) {
            throw new ManualAbort();
          }
          logger.debug("started {}", component.getName());
          component.run();
        });
        Thread.yield();

        // Wait for thread to await permit.
        while (_semaphore.getQueueLength() != i) {
          Thread.sleep(1);
        }
      }

      _threads[0] = createStartedDaemonThread("Tick", () -> {
        while (true) {
          startTick();
          // Execute component threads.
          _semaphore.release();
          // Wait for component threads to finish.
          try {
            _semaphore.acquire();
          } catch (InterruptedException e) {
            throw new ManualAbort();
          }
        }
      });
      Thread.yield();

      _suspendEvent.waitForSuspend();

    } catch (InterruptedException e) {
      throw new ManualAbort();
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
    for (Thread thread : _threads) {
      thread.interrupt();
    }
    Thread.yield();
  }
}
