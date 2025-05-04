package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.clock.ManualAbort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event to suspend clock run.
 */
public final class SuspendEvent extends ClockEvent {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SuspendEvent.class);

    /**
     * Has the run been suspended?.
     */
    private boolean suspended = false;

    /**
     * Monitor for synchronization.
     */
    private final Object monitor;

    /**
     * Constructor.
     *
     * @param monitor
     *         Monitor for synchronization.
     */
    public SuspendEvent(Object monitor) {
        super("Suspend");

        this.monitor = monitor;
    }

    @Override
    public void execute(long tick) throws ManualAbort {
        synchronized (monitor) {
            logger.info("Suspend at {}.", tick);
            suspended = true;
            monitor.notifyAll();
            try {
                while (suspended) {
                    monitor.wait();
                }
            } catch (InterruptedException e) {
                throw new ManualAbort();
            }
            logger.info("Resume at {}.", tick);
        }
    }

    /**
     * Resume execution if suspended.
     */
    public void resume() {
        synchronized (monitor) {
            if (suspended) {
                suspended = false;
                monitor.notifyAll();
            }
        }
    }

    /**
     * Wait for suspend.
     */
    public void waitForSuspend() throws ManualAbort {
        try {
            synchronized (monitor) {
                while (!suspended) {
                    monitor.wait();
                }
            }
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
    }
}
