package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;

/**
 * Debug cartridge of VICE in the SID area: Test programs write their result to $D7FF.
 * Only writes with I/O banked in reach it, writes with I/O banked out go to RAM.
 * Otherwise the SID area is mapped to the SID.
 */
public class DebugCartridge implements BusDevice {
    /**
     * Address of the result register.
     */
    public static final int RESULT = 0xD7FF;

    /**
     * SID.
     */
    private final BusDevice sid;

    /**
     * Result written by the test program, -1 if none.
     */
    private volatile int result = -1;

    /**
     * Constructor.
     *
     * @param sid
     *         SID
     */
    public DebugCartridge(BusDevice sid) {
        this.sid = sid;
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
        sid.write(value, address);
    }

    @Override
    public int read(int address) {
        return sid.read(address);
    }
}
