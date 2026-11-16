package de.heiden.jem.models.c64.components.sid;

import de.heiden.jem.components.bus.BusDevice;

/**
 * Placeholder for the SID until it is emulated.
 * Keeps the written registers, so reads return the last value written to a register.
 * Does not write through to the RAM below, because programs may store code or data there.
 */
public class SIDStub implements BusDevice {
    /**
     * Address mask: The registers are mirrored every $20 bytes.
     */
    private static final int MASK = 0x1F;

    /**
     * Registers.
     */
    private final int[] registers = new int[MASK + 1];

    @Override
    public void write(int value, int address) {
        registers[address & MASK] = value;
    }

    @Override
    public int read(int address) {
        return registers[address & MASK];
    }
}
