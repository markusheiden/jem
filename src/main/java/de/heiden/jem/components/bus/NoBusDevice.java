package de.heiden.jem.components.bus;

/**
 * Bus device for an "open" bus, when no bus device has been selected.
 */
public final class NoBusDevice implements BusDevice
{
  @Override
  public final int read(int address)
  {
    return 0xFF;
  }

  @Override
  public final void write(int value, int address)
  {
    // ignored
  }
}
