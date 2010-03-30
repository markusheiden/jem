package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortListener;
import de.heiden.jem.models.c64.components.RAM;
import de.heiden.jem.models.c64.components.ROM;
import de.heiden.jem.models.c64.components.bus.BusDevice;

/**
 * VIC CPUBus.
 *
 * TODO implement setting of VIC area.
 */
public class VICBus implements BusDevice
{
  /**
   * Constructor.
   *
   * @param cia2PortA Port A of CIA 2 for VIC base address
   * @param ram RAM
   * @param character char rom
   * @require cia2PortA != null
   * @require ram != null
   * @require character != null
   * @require kernel != null
   */
  public VICBus(OutputPort cia2PortA, RAM ram, ROM character)
  {
    assert cia2PortA != null : "cia2PortA != null";
    assert ram != null : "ram != null";
    assert character != null : "character != null";

    _ram = ram;
    _character = character;

    cia2PortA.addOutputPortListener(new OutputPortListener()
    {
      @Override
      public void outputPortChanged(int value, int mask)
      {
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
  public int read(int address)
  {
    assert address >= 0x0000 && address < 0x4000 : "address >= 0x0000 && address < 0x4000";

    switch ((_vicBase + address) >> 12)
    {
      case 0x01:
      case 0x09:
        return _character.read(address);
      default:
        return _ram.read(address);
    }
  }

  /**
   * Write byte to bus device.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   * @require address >= 0x0000 && address < 0x10000
   */
  public void write(int value, int address)
  {
    assert false : "VIC does not write anything";
  }

  //
  // private attributes
  //

  private int _vicBase;
  private final RAM _ram;
  private final ROM _character;
}
