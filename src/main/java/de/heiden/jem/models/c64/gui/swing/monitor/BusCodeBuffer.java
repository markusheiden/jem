package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.c64dt.assembler.AbstractCodeBuffer;
import de.heiden.jem.components.bus.BusDevice;

/**
 * Code buffer based on a bus.
 */
public class BusCodeBuffer extends AbstractCodeBuffer {
  private final BusDevice _bus;

  /**
   * Constructor.
   *
   * @param index start address of the code to disassemble
   * @param bus bus
   */
  public BusCodeBuffer(int index, BusDevice bus) {
    super(0x0000, 0x10000);

    this._bus = bus;
    setCurrentIndex(index);
  }

  @Override
  protected int readByteAt(int index) {
    return _bus.read(index);
  }
}
