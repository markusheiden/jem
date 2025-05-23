package de.heiden.jem.models.c64.components.memory;

/**
 * 8 Bit-ROM.
 */
public final class ROM extends AbstractMemory implements Patchable {
    /**
     * Constructor.
     *
     * @param content
     *         ROM content
     * @require content.length >= 0 && content.length <= 0x10000
     */
    public ROM(byte... content) {
        super(content);
    }

    /**
     * Write byte to ROM.
     *
     * @param value
     *         byte to write
     * @param address
     *         address to write byte to
     * @require value >= 0x00 && value < 0x100
     */
    @Override
    public void write(int value, int address) {
        assert false : "Write to ROM not possible";
    }

    /**
     * Patch byte in ROM.
     *
     * @param value
     *         byte to write
     * @param address
     *         address to write byte to
     * @require value >= 0x00 && value < 0x100
     */
    @Override
    public void patch(int value, int address) {
        // 0x100 is used to escape emulation in the cpu
        assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

        memory[address & mask] = value;
    }

    /**
     * Read byte from ROM.
     *
     * @param address
     *         address to read byte from
     * @ensure result >= 0x00 && result < 0x100
     */
    @Override
    public int read(int address) {
        int result = memory[address & mask];
        // 0x100 is used to escape emulation in the cpu
        assert result >= 0x00 && result < 0x100 : "result >= 0x00 && result < 0x100";
        return result;
    }
}
