package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;

/**
 * Debug cartridge of VICE in the SID area: Test programs write their result to $D7FF.
 * Only writes with I/O banked in reach it, writes with I/O banked out go to RAM.
 * Otherwise the SID area is mapped to RAM, like without this cartridge.
 */
public class DebugCartridge implements BusDevice {
    /**
     * Address of the result register.
     */
    public static final int RESULT = 0xD7FF;

    /**
     * RAM.
     */
    private final BusDevice ram;

    /**
     * Result written by the test program, -1 if none.
     */
    private volatile int result = -1;

    /**
     * Constructor.
     *
     * @param ram
     *         RAM
     */
    public DebugCartridge(BusDevice ram) {
        this.ram = ram;
    }

    /**
     * Result written by the test program, -1 if none.
     */
    public int getResult() {
        return result;
    }

    @Override
    public void write(int value, int address) {
        if (address == RESULT) {
            result = value;
        }
        ram.write(value, address);
    }

    @Override
    public int read(int address) {
        return ram.read(address);
    }
}
