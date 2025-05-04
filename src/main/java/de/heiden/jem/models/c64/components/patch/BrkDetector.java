package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

/**
 * Detects when a runs into a BRK opcode.
 */
public class BrkDetector extends Patch {
    /**
     * Has the program run into a BRK?.
     */
    private boolean brk = false;

    /**
     * Constructor.
     */
    public BrkDetector() {
        super(0xE37B);
    }

    /**
     * Has the program run into a BRK? Resets the BRK flag.
     */
    public boolean hasBrk() {
        boolean result = brk;
        brk = false;
        return result;
    }

    @Override
    protected int execute(CPU6510State state, BusDevice bus) {
        brk = true;
        return replaced;
    }
}
