package de.heiden.jem.components.bus;

/**
 * Bus device for an "open" bus, when no bus device has been selected.
 */
public class NoBusDevice implements BusDevice
{
  @Override
  public int read(int address)
  {
    return 0xFF;
  }

  @Override
  public void write(int value, int address)
  {
    // ignored
  }
}
