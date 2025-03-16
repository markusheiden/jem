package de.heiden.jem.components;

import de.heiden.jem.components.clock.ManualAbort;
import de.heiden.jem.components.clock.Tick;

/**
 * Tick that {@link ManualAbort aborts}.
 */
public class ManualAbortTick implements Tick {
    @Override
    public void waitForTick() {
        throw new ManualAbort();
    }
}
