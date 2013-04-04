package de.heiden.jem.components.bus;

import java.util.ArrayList;
import java.util.List;

/**
 * Bus for testing purposes.
 * This bus delegates to a given bus and logs all accesses to it.
 */
public class LoggingBus extends BusDeviceAdapter {
  /**
   * Log entries.
   * <p/>
   * TODO 2010-10-12 mh: use array with cyclic pointer?
   */
  private final List<LogEntry> _log;

  /**
   * Constructor.
   *
   * @param bus Bus to delegate to
   * @require bus != null
   */
  public LoggingBus(BusDevice bus) {
    super(bus);

    _log = new ArrayList<>(1024);
  }

  /**
   * Protocol all write accesses.
   */
  @Override
  public synchronized final void write(int value, int address) {
    _log.add(new LogEntry(false, address, value));
    super.write(value, address);
  }

  /**
   * Protocol all read accesses
   */
  @Override
  public synchronized final int read(int address) {
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
  public synchronized LogEntry getLastLogEntry() {
    assert !getLog().isEmpty() : "Precondition: !getLog().isEmpty()";

    LogEntry result = _log.get(_log.size() - 1);

    assert result != null : "Postcondition: result != null";
    return result;
  }

  /**
   * Get log.
   */
  public synchronized List<LogEntry> getLog() {
    return _log;
  }

  /**
   * Reset log.
   *
   * @ensure getLog().length == 0
   */
  public synchronized void resetLog() {
    _log.clear();

    assert getLog().isEmpty() : "Postcondition: getLog().isEmpty()";
  }
}
