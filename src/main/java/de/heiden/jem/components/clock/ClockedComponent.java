package de.heiden.jem.components.clock;

import org.serialthreads.context.IRunnable;

/**
 * Clocked component.
 */
public interface ClockedComponent extends IRunnable {
    /**
     * Name of the component.
     */
    String getName();

    /**
     * Inject tick.
     */
    void setTick(Tick tick);
}
