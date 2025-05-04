package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

/**
 * Detects when a (basic) program ends.
 */
public class ProgramEndDetector extends Patch {
    /**
     * Has the program ended?.
     */
    private boolean end = false;

    /**
     * Error code at the program end.
     */
    private int error = 0;

    /**
     * Constructor.
     */
    public ProgramEndDetector() {
        super(0xE38B);
    }

    /**
     * Has the program ended? Resets end flag.
     */
    public boolean hasEnded() {
        boolean result = end;
        end = false;
        return result;
    }

    /**
     * Error code at the program end.
     */
    public int getError() {
        return error;
    }

    @Override
    protected int execute(CPU6510State state, BusDevice bus) {
        end = true;
        error = state.X;
        return replaced;
    }
}
