package de.heiden.jem.components.clock;

import static java.lang.Thread.currentThread;

/**
 * Encapsulates {@link InterruptedException}.
 */
public class ManualAbort extends RuntimeException {
    /**
     * Constructor.
     */
    public ManualAbort() {
        super("Thread has been interrupted.");
    }

    /**
     * Throw {@link ManualAbort}, if the current thread has been interrupted.
     * Does not clear the interrupted flag, so subsequent checks abort too.
     */
    public static void abortIfInterrupted() {
        if (currentThread().isInterrupted()) {
            throw new ManualAbort();
        }
    }
}
