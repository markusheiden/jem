package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

/**
 * Patch which inserts an RTS to immediately return from a subroutine.
 */
public class Return extends Patch {
    /**
     * Constructor.
     *
     * @param addr
     *         Address to patch
     */
    public Return(int addr) {
        super(addr);
    }

    @Override
    protected int execute(CPU6510State state, BusDevice bus) {
        return RTS;
    }
}
