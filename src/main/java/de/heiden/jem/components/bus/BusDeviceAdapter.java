package de.heiden.jem.components.bus;

/**
 * Adapter for {@link BusDevice}.
 * Delegates all methods to a given bus device.
 */
public class BusDeviceAdapter implements BusDevice {
  /**
   * Bus to which will be delegated.
   */
  protected final BusDevice bus;

  /**
   * Constructor.
   *
   * @param bus Bus to delegate to
   * @require bus != null
   */
  public BusDeviceAdapter(BusDevice bus) {
    assert bus != null : "bus != null";

    this.bus = bus;
  }

  @Override
  public void write(int value, int address) {
    bus.write(value, address);
  }

  @Override
  public int read(int address) {
    return bus.read(address);
  }
}
