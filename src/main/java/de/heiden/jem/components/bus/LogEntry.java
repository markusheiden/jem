package de.heiden.jem.components.bus;

import static de.heiden.c64dt.bytes.HexUtil.hexByte;
import static de.heiden.c64dt.bytes.HexUtil.hexWord;

/**
 * Represents a single access to the bus.
 */
public class LogEntry {
    /**
     * Read (true) or write (false)?.
     */
    private final boolean read;

    /**
     * Accessed address.
     */
    private final int address;

    /**
     * Value read from or written to the accessed address.
     */
    private final int value;

    /**
     * Constructor.
     *
     * @param read
     *         is access a read access?.
     * @param address
     *         accessed address.
     * @param value
     *         value which has been written / read.
     */
    public LogEntry(boolean read, int address, int value) {
        this.read = read;
        this.address = address;
        this.value = value;
    }

    /**
     * Is access a read access?.
     */
    public boolean isReadAccess() {
        return read;
    }

    /**
     * Get accessed address.
     */
    public int getAddress() {
        return address;
    }

    /**
     * Get value which has been written / read.
     */
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LogEntry e &&
               read == e.read &&
               address == e.address &&
               value == e.value;
    }

    @Override
    public int hashCode() {
        return address;
    }

    @Override
    public String toString() {
        return "%s %s value %s.".formatted(read ? "Read from " : "Write to ", hexWord(address), hexByte(value));
    }
}
