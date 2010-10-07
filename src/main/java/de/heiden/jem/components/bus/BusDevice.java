package de.heiden.jem.components.bus;

/**
 * Device (slave) connected to bus.
 */
public interface BusDevice
{
  /**
   * Write byte to bus device.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0 && value < 0x100
   * @require address >= 0 && address < size()
   */
  public void write(int value, int address);

  /**
   * Read byte from bus device.
   *
   * @param address address to read byte from
   * @require address >= 0 && address < size()
   * @ensure result >= 0 && result < 0x100
   */
  public int read(int address);
}
