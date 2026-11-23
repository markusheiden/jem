package de.heiden.jem.models.c64.components.memory;

/**
 * Patchable device.
 */
public interface Patchable {
    /**
     * Patch byte in ROM.
     *
     * @param value
     *         Byte to write.
     * @param address
     *         Address to write byte to.
     * @require value >= 0x00 && value < 0x100
     */
    void patch(int value, int address);
}
