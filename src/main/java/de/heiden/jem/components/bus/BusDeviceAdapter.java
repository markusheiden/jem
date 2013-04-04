package de.heiden.jem.components.bus;

/**
 * Adapter for {@link BusDevice}.
 * Delegates all methods to a given bus device.
 */
public class BusDeviceAdapter implements BusDevice {
  /**
   * Bus to which will be delegated.
   */
  protected final BusDevice _bus;

  /**
   * Constructor.
   *
   * @param bus Bus to delegate to
   * @require bus != null
   */
  public BusDeviceAdapter(BusDevice bus) {
    assert bus != null : "bus != null";

    _bus = bus;
  }

  @Override
  public void write(int value, int address) {
    _bus.write(value, address);
  }

  @Override
  public int read(int address) {
    return _bus.read(address);
  }
}
