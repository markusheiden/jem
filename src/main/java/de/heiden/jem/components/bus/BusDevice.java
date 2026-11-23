package de.heiden.jem.components.bus;

/**
 * Device (slave) connected to bus.
 */
public interface BusDevice {
    /**
     * Write byte to the bus device.
     *
     * @param value
     *         Byte to write.
     * @param address
     *         Address to write byte to.
     * @require value >= 0 && value < 0x100
     * @require address >= 0 && address < size()
     */
    void write(int value, int address);

    /**
     * Read byte from the bus device.
     *
     * @param address
     *         Address to read byte from.
     * @require address >= 0 && address < size()
     * @ensure result >= 0 && result < 0x100
     */
    int read(int address);
}
