package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.models.c64.components.RAM;

import java.util.ArrayList;
import java.util.List;

/**
 * C64 bus for testing purposes. This bus logs all access to it.
 */
public class CPUTestBus extends C64Bus
{
  /**
   * Constructor.
   *
   * @param ram ram
   * @require ram != null
   */
  public CPUTestBus(RAM ram)
  {
    super(ram);

    _log = new ArrayList<LogEntry>();
  }

  /**
   * Protocol all write accesses.
   */
  public void write(int value, int address)
  {
    _log.add(new LogEntry(false, address, value));
    super.write(value, address);
  }

  /**
   * Protocol all read accesses
   */
  public int read(int address)
  {
    int value = super.read(address);
    _log.add(new LogEntry(true, address, value));

    return value;
  }

  /**
   * Get last log entry.
   *
   * @require getLog().length > 0
   * @ensure result != null
   */
  public LogEntry getLastLogEntry()
  {
    assert getLog().length > 0 : "Precondition: getLog().length > 0";

    LogEntry result = _log.get(_log.size() - 1);

    assert result != null : "Postcondition: result != null";
    return result;
  }

  /**
   * Get log.
   */
  public LogEntry[] getLog()
  {
    return _log.toArray(new LogEntry[_log.size()]);
  }

  /**
   * Reset log.
   *
   * @ensure getLog().length == 0
   */
  public void resetLog()
  {
    _log.clear();

    assert getLog().length == 0 : "Postcondition: getLog().length == 0";
  }

  //
  // private attributes
  //

  private List<LogEntry> _log;

  //
  // inner class
  //

  /**
   * LogEntry represents an single access to the bus.
   */
  public static class LogEntry
  {
    /**
     * Constructor.
     *
     * @param read is access a read access?.
     * @param address accessed address.
     * @param value value which has been written / read.
     */
    public LogEntry(boolean read, int address, int value)
    {
      _read = read;
      _address = address;
      _value = value;
    }

    /**
     * Is access a read access?.
     */
    public boolean isReadAccess()
    {
      return _read;
    }

    /**
     * Get accessed address.
     */
    public int getAddress()
    {
      return _address;
    }

    /**
     * Get value which has been writen / read.
     */
    public int getValue()
    {
      return _value;
    }

    /**
     * toString.
     */
    public String toString()
    {
      return (_read ? "read from " : "write to ") + HexUtil.hexWord(_address) +
        " value " + HexUtil.hexByte(_value);

    }

    //
    // private attributes
    //

    private boolean _read;
    private int _address;
    private int _value;
  }
}
