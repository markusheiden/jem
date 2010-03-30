package de.heiden.jem.models.c64.components.cpu;

import de.heiden.jem.models.c64.components.CIA6526;
import de.heiden.jem.models.c64.components.ColorRAM;
import de.heiden.jem.models.c64.components.RAM;
import de.heiden.jem.models.c64.components.ROM;
import de.heiden.jem.models.c64.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.bus.NoBusDevice;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;

/**
 * C64 bus.
 *
 * TODO evaluate loram, hiram, charen, game, exrom
 * TODO SID, IO1, IO2
 */
public class C64Bus implements BusDevice
{
  /**
   * Constructor.
   *
   * @require ram != null
   * @require basic != null
   * @require vic != null
   * @require colorRam != null
   * @require cia1 != null
   * @require cia2 != null
   * @require kernel != null
   */
  public C64Bus(RAM ram, ROM basic, VIC6569PAL vic, ColorRAM colorRam, CIA6526 cia1, CIA6526 cia2, ROM kernel)
  {
    assert ram != null : "ram != null";
    assert basic != null : "basic != null";
    assert vic != null : "vic != null";
    assert colorRam != null : "colorRam != null";
    assert cia1 != null : "cia1 != null";
    assert cia2 != null : "cia2 != null";
    assert kernel != null : "kernel != null";

    _ram = ram;
    _basic = basic;
    _vic = vic;
    _colorRam = colorRam;
    _cia1 = cia1;
    _cia2 = cia2;
    _kernel = kernel;
    _noBusDevice = new NoBusDevice();

    _ioMap = new BusDevice[]
      {
        _vic, _vic, _vic, _vic,
        _noBusDevice, _noBusDevice, _noBusDevice, _noBusDevice,
        _colorRam, _colorRam, _colorRam, _colorRam,
        _cia1, _cia2,
        _noBusDevice, _noBusDevice
      };
  }

  /**
   * Constructor for testing purposes only.
   *
   * @require ram != null
   */
  protected C64Bus(RAM ram)
  {
    assert ram != null : "ram != null";

    _ram = ram;
    _basic = null;
    _vic = null;
    _colorRam = null;
    _cia1 = null;
    _cia2 = null;
    _kernel = null;
    _noBusDevice = null;

    _ioMap = null;
  }

  //
  // public
  //

  /**
   * Read byte from bus device.
   *
   * @param address address to read byte from
   * @require address >= 0x0000 && address < 0x10000
   * @ensure result >= 0x00 && result < 0x100
   */
  public int read(int address)
  {
    assert address >= 0x0000 && address < 0x10000 : "address >= 0x0000 && address < 0x10000";

    switch (address >> 12)
    {
      case 0x0A:
      case 0x0B:
        return _basic.read(address);
      case 0x0D:
        return _ioMap[(address >> 8) & 0x0F].read(address);
      case 0x0E:
      case 0x0F:
        return _kernel.read(address);
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
    assert value >= 0x00 && value < 0x100 : "value >= 0x00 && value < 0x100";
    assert address >= 0x0000 && address < 0x10000 : "address >= 0x0000 && address < 0x10000";

    switch (address >> 12)
    {
      case 0x0D:
        _ioMap[(address >> 8) & 0x0F].write(value, address);
        break;
      default:
        _ram.write(value, address);
        break;
    }
  }


  //
  // private attributes
  //

  private final RAM _ram;
  private final ROM _basic;
  private final VIC6569PAL _vic;
  private final ColorRAM _colorRam;
  private final CIA6526 _cia1;
  private final CIA6526 _cia2;
  private final ROM _kernel;
  private final BusDevice _noBusDevice;

  private final BusDevice[] _ioMap;
}
