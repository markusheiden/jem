package de.heiden.jem.models.c64.components.memory;

/**
 * 8 Bit-RAM.
 */
public class RAM extends AbstractMemory {
    /**
     * Constructor.
     *
     * @param size
     *         size in bytes
     * @require size >= 0 && size <= 0x10000
     */
    public RAM(int size) {
        super(size);
    }

    /**
     * Constructor.
     *
     * @param content
     *         ROM content
     * @require content.length >= 0 && content.length <= 0x10000
     */
    public RAM(byte[] content) {
        super(content);
    }

    /**
     * Write byte to RAM.
     *
     * @param value
     *         byte to write
     * @param address
     *         address to write byte to
     * @require value >= 0 && value < 0x100
     */
    @Override
    public final void write(int value, int address) {
        assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

        memory[address & mask] = value;
    }

    /**
     * Read byte from RAM.
     *
     * @param address
     *         address to read byte from
     * @ensure result >= 0 && result < 0x100
     */
    @Override
    public final int read(int address) {
        int result = memory[address & mask];
        assert result >= 0 && result < 0x100 : "result >= 0 && result < 0x100";
        return result;
    }
}
