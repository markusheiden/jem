package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.assembler.AbstractCodeBuffer;
import de.heiden.jem.components.bus.BusDevice;

/**
 * Code buffer based on a bus.
 */
public class BusCodeBuffer extends AbstractCodeBuffer
{
  private final BusDevice _bus;

  /**
   * Constructor.
   *
   * @param position start address of the code disassemble
   * @param bus bus
   */
  public BusCodeBuffer(int position, BusDevice bus)
  {
    super(0x10000);

    this.position = position;
    this._bus = bus;
  }

  @Override
  public int readByte()
  {
    return _bus.read(position++);
  }
}
