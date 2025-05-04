package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

/**
 * Replaces standard C64 "System.in" at $FFE4.
 * Stops emulation.
 */
public class StopAtSystemIn extends Patch {
    /**
     * Constructor.
     */
    public StopAtSystemIn() {
        super(0xFFE4);
    }

    @Override
    protected int execute(CPU6510State state, BusDevice bus) {
        throw new IllegalArgumentException("Stop");
    }
}
