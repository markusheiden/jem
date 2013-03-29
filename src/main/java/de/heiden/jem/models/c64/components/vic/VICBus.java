package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortListener;

/**
 * VIC bus.
 */
public class VICBus implements BusDevice {
  /**
   * 64kB RAM.
   */
  private final BusDevice _ram;

  /**
   * Character ROM.
   */
  private final BusDevice _character;

  /**
   * VIC base address in cpu address space.
   */
  private int _vicBase;

  /**
   * Constructor.
   *
   * @param cia2PortA Port A of CIA 2 for VIC base address
   * @param ram RAM
   * @param character char rom
   * @require cia2PortA != null
   * @require ram != null
   * @require character != null
   */
  public VICBus(OutputPort cia2PortA, BusDevice ram, BusDevice character) {
    assert cia2PortA != null : "cia2PortA != null";
    assert ram != null : "ram != null";
    assert character != null : "character != null";

    _ram = ram;
    _character = character;

    cia2PortA.addOutputPortListener(new OutputPortListener() {
      @Override
      public void outputPortChanged(int value, int mask) {
        _vicBase = ((value ^ 0xFF) & 0x03) << 14;
      }
    });
  }

  /**
   * Read byte from bus device.
   *
   * @param address address to read byte from
   * @require address >= 0x0000 && address < 0x4000
   * @ensure result >= 0x00 && result < 0x100
   */
  @Override
  public final int read(int address) {
    assert address >= 0x0000 && address < 0x4000 : "address >= 0x0000 && address < 0x4000";

    int cpuAddress = _vicBase + address;
    return ((cpuAddress & 0x7000) == 0x1000 ? _character : _ram).read(cpuAddress);
  }

  /**
   * Write byte to bus device.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   * @require address >= 0x0000 && address < 0x10000
   */
  @Override
  public final void write(int value, int address) {
    assert false : "VIC does not write anything";
  }
}
