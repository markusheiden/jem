package de.heiden.jem.components.bus;

import de.heiden.c64dt.util.HexUtil;

/**
 * LogEntry represents an single access to the bus.
 */
public class LogEntry {
  /**
   * Read (true) or write (false)?.
   */
  private final boolean _read;

  /**
   * Accessed address.
   */
  private final int _address;

  /**
   * Value read from or written to the accessed address.
   */
  private final int _value;

  /**
   * Constructor.
   *
   * @param read is access a read access?.
   * @param address accessed address.
   * @param value value which has been written / read.
   */
  public LogEntry(boolean read, int address, int value) {
    _read = read;
    _address = address;
    _value = value;
  }

  /**
   * Is access a read access?.
   */
  public boolean isReadAccess() {
    return _read;
  }

  /**
   * Get accessed address.
   */
  public int getAddress() {
    return _address;
  }

  /**
   * Get value which has been written / read.
   */
  public int getValue() {
    return _value;
  }

  @Override
  public String toString() {
    return (_read ? "read from " : "write to ") + HexUtil.hexWord(_address) +
      " value " + HexUtil.hexByte(_value);
  }
}
