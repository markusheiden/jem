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
     * Initialize the RAM with the pattern of a C64 after power-up:
     * Blocks of 4 bytes $00 and 4 bytes $FF, starting with 2 bytes $00, inverted every 16 KB.
     * Some programs depend on that pattern. It is the default of VICE without random bits,
     * that passes all tests of the VICE testprogs C64/raminitpattern. Real C64s show similar patterns.
     */
    public RAM powerUpPattern() {
        for (var address = 0; address < memory.length; address++) {
            var value = ((address + 2) / 4 & 1) != 0 ? 0xFF : 0x00;
            var pattern = (address / 0x4000 & 1) != 0 ? 0xFF : 0x00;
            memory[address] = value ^ pattern;
        }
        return this;
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
        var result = memory[address & mask];
        assert result >= 0 && result < 0x100 : "result >= 0 && result < 0x100";
        return result;
    }
}
