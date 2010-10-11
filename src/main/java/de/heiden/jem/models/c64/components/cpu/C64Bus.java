package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.bus.NoBusDevice;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortListener;
import de.heiden.jem.models.c64.components.memory.RAM;
import org.apache.log4j.Logger;

/**
 * C64 bus.
 * <p/>
 * TODO evaluate loram, hiram, charen, game, exrom
 * TODO SID, IO1, IO2
 */
public class C64Bus implements BusDevice
{
  /**
   * Constructor.
   *
   * @param cpu CPU port
   * @param ram RAM
   * @param basic Basic ROM
   * @param vic VIC
   * @param colorRam Color RAM
   * @param cia1 CIA1
   * @param cia2 CIA2
   * @param charset Charset ROM
   * @param kernel Kernel ROM
   * @require cpu != null
   * @require ram != null
   * @require basic != null
   * @require vic != null
   * @require colorRam != null
   * @require cia1 != null
   * @require cia2 != null
   * @require charset != null
   * @require kernel != null
   */
  public C64Bus(
    OutputPort cpu,
    BusDevice ram,
    BusDevice basic,
    BusDevice vic,
    BusDevice colorRam,
    BusDevice cia1,
    BusDevice cia2,
    BusDevice charset,
    BusDevice kernel)
  {
    assert cpu != null : "cpu != null";
    assert ram != null : "ram != null";
    assert basic != null : "basic != null";
    assert vic != null : "vic != null";
    assert colorRam != null : "colorRam != null";
    assert cia1 != null : "cia1 != null";
    assert cia2 != null : "cia2 != null";
    assert charset != null : "charset != null";
    assert kernel != null : "kernel != null";

    _cpu = cpu;
    _cpu.addOutputPortListener(new OutputPortListener()
    {
      @Override
      public final void outputPortChanged(int value, int mask)
      {
        BusDevice oldIoMode = _ioMode;
        // TODO 2010-10-08 mh: consider signals from expansion port
        int mode = ((value << 2) | 0x03) & 0x1f;
        BusDevice newIoMode = _ioModes[mode];
        _ioMode = newIoMode;
        if (_logger.isDebugEnabled() && oldIoMode != _ioMode)
        {
          _logger.debug("Change bus mode to " + HexUtil.hexBytePlain(mode));
        }
      }
    });

    _ram = ram;
    _basic = basic;
    _vic = vic;
    _colorRam = colorRam;
    _cia1 = cia1;
    _cia2 = cia2;
    _charset = charset;
    _kernel = kernel;
    _noBusDevice = new NoBusDevice();

    _cartridge = new NoBusDevice(); // TODO
    _cartridgeL = new NoBusDevice(); // TODO
    _cartridgeH = new NoBusDevice(); // TODO

    _ioMode = new Mode1();
    _ioModes = new BusDevice[]
      {
        new Mode7(),  // 00000 000x0
        new Mode14(), // 00001 xxx01
        new Mode7(),  // 00010 000x0
        new Mode5(),  // 00011 x001x
        new Mode5(),  // 00100 00100
        new Mode14(), // 00101 xxx01
        new Mode4(),  // 00110 0011x
        new Mode4(),  // 00111 0011x

        new Mode11(), // 01000 01000
        new Mode14(), // 01001 xxx01
        new Mode7(),  // 01010 0101x
        new Mode7(),  // 01011 0101x
        new Mode13(), // 01100 01100 Mode13 OK?
        new Mode14(), // 01101 xxx01
        new Mode9(),  // 01110 01110
        new Mode2(),  // 01111 01111

        new Mode6(),  // 10000 100x0
        new Mode14(), // 10001 xxx01
        new Mode6(),  // 10010 100x0
        new Mode5(),  // 10011 x001x
        new Mode3(),  // 10100 10100
        new Mode14(), // 10101 xxx01
        new Mode3(),  // 10110 1011x
        new Mode3(),  // 10111 1011x

        new Mode10(), // 11000 11000
        new Mode14(), // 11001 xxx01
        new Mode6(),  // 11010 1101x
        new Mode6(),  // 11011 1101x
        new Mode12(), // 11100 11100 Mode12 OK???
        new Mode14(), // 11101 xxx01
        new Mode8(),  // 11110 11110
        new Mode1(),  // 11111 11111

      };

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

    _cpu = null;

    _ram = ram;
    _basic = null;
    _vic = null;
    _colorRam = null;
    _cia1 = null;
    _cia2 = null;
    _charset = null;
    _kernel = null;
    _noBusDevice = null;

    _ioModes = null;
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

    return _ioMode.read(address);
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

    _ioMode.write(value, address);
  }


  //
  // private attributes
  //

  private final OutputPort _cpu;

  private final BusDevice _ram;
  private final BusDevice _basic;
  private final BusDevice _vic;
  private final BusDevice _colorRam;
  private final BusDevice _cia1;
  private final BusDevice _cia2;
  private final BusDevice _charset;
  private final BusDevice _kernel;

  private BusDevice _cartridgeL;
  private BusDevice _cartridgeH;
  private BusDevice _cartridge;

  private final BusDevice _noBusDevice;

  private BusDevice _ioMode;
  private final BusDevice[] _ioModes;
  private final BusDevice[] _ioMap;

  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  //
  // Inner classes for different address configurations
  //

  private abstract class IOBusDevice implements BusDevice
  {
    @Override
    public final void write(int value, int address)
    {
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
  }

  private abstract class RAMBusDevice implements BusDevice
  {
    @Override
    public final void write(int value, int address)
    {
      _ram.write(value, address);
    }
  }

  /**
   * C64 bus mode 1 "default".
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: Basic
   * $C000-$CFFF: RAM
   * $D000-$DFFF: I/O
   * $E000-$FFFF: Kernel
   */
  private final class Mode1 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
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
  }

  /**
   * C64 bus mode 2.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: Basic
   * $C000-$CFFF: RAM
   * $D000-$DFFF: Charset
   * $E000-$FFFF: Kernal
   */
  private final class Mode2 extends RAMBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0A:
        case 0x0B:
          return _basic.read(address);
        case 0x0D:
          return _charset.read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 3.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: RAM
   * $C000-$CFFF: RAM
   * $D000-$DFFF: I/O
   * $E000-$FFFF: RAM
   */
  private final class Mode3 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0D:
          return _ioMap[(address >> 8) & 0x0F].read(address);
        default:
          return _ram.read(address);
      }
    }
  }


  /**
   * C64 bus mode 4.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: RAM
   * $C000-$CFFF: RAM
   * $D000-$DFFF: Charset
   * $E000-$FFFF: RAM
   */
  private final class Mode4 extends RAMBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0D:
          return _charset.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 5.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: RAM
   * $C000-$CFFF: RAM
   * $D000-$DFFF: RAM
   * $E000-$FFFF: RAM
   */
  private final class Mode5 extends RAMBusDevice
  {
    @Override
    public int read(int address)
    {
      return _ram.read(address);
    }
  }

  /**
   * C64 bus mode 6.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: RAM
   * $C000-$CFFF: RAM
   * $D000-$DFFF: I/O
   * $E000-$FFFF: Kernal
   */
  private final class Mode6 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0D:
          return _ioMap[(address >> 8) & 0x0F].read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 6.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: RAM
   * $C000-$CFFF: RAM
   * $D000-$DFFF: Charset
   * $E000-$FFFF: Kernal
   */
  private final class Mode7 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0D:
          return _charset.read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 8.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: ROM L
   * $A000-$BFFF: Basic
   * $C000-$CFFF: RAM
   * $D000-$DFFF: I/O
   * $E000-$FFFF: Kernal
   */
  private final class Mode8 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x08:
        case 0x09:
          return _cartridgeL.read(address);
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
  }

  /**
   * C64 bus mode 9.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: ROM L
   * $A000-$BFFF: Basic
   * $C000-$CFFF: RAM
   * $D000-$DFFF: Charset
   * $E000-$FFFF: Kernal
   */
  private final class Mode9 extends RAMBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x08:
        case 0x09:
          return _cartridgeL.read(address);
        case 0x0A:
        case 0x0B:
          return _basic.read(address);
        case 0x0D:
          return _charset.read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 10.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: ROM H
   * $C000-$CFFF: RAM
   * $D000-$DFFF: I/O
   * $E000-$FFFF: Kernal
   */
  private final class Mode10 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0A:
        case 0x0B:
          return _cartridgeH.read(address);
        case 0x0D:
          return _ioMap[(address >> 8) & 0x0F].read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 11.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: RAM
   * $A000-$BFFF: ROM H
   * $C000-$CFFF: RAM
   * $D000-$DFFF: Charset
   * $E000-$FFFF: Kernal
   */
  private final class Mode11 extends RAMBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x0A:
        case 0x0B:
          return _cartridgeH.read(address);
        case 0x0D:
          return _charset.read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 12.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: ROM L
   * $A000-$BFFF: ROM H
   * $C000-$CFFF: RAM
   * $D000-$DFFF: I/O
   * $E000-$FFFF: Kernal
   */
  private final class Mode12 extends IOBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x08:
        case 0x09:
          return _cartridgeL.read(address);
        case 0x0A:
        case 0x0B:
          return _cartridgeH.read(address);
        case 0x0D:
          return _ioMap[(address >> 8) & 0x0F].read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 13.
   * $1000-$1FFF: RAM
   * $8000-$9FFF: ROM L
   * $A000-$BFFF: ROM H
   * $C000-$CFFF: RAM
   * $D000-$DFFF: Charset
   * $E000-$FFFF: Kernal
   */
  private final class Mode13 extends RAMBusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x08:
        case 0x09:
          return _cartridgeL.read(address);
        case 0x0A:
        case 0x0B:
          return _cartridgeH.read(address);
        case 0x0D:
          return _charset.read(address);
        case 0x0E:
        case 0x0F:
          return _kernel.read(address);
        default:
          return _ram.read(address);
      }
    }
  }

  /**
   * C64 bus mode 14 "open".
   * $1000-$1FFF: open
   * $8000-$9FFF: ROM L
   * $A000-$BFFF: open
   * $C000-$CFFF: open
   * $D000-$DFFF: I/O
   * $E000-$FFFF: ROM H
   */
  private final class Mode14 implements BusDevice
  {
    @Override
    public int read(int address)
    {
      switch (address >> 12)
      {
        case 0x08:
        case 0x09:
          return _cartridgeL.read(address);
        case 0x0D:
          return _ioMap[(address >> 8) & 0x0F].read(address);
        case 0x0E:
        case 0x0F:
          return _cartridgeH.read(address);
        default:
          return _cartridge.read(address);
      }
    }

    @Override
    public void write(int value, int address)
    {
      switch (address >> 12)
      {
        case 0x0D:
          _ioMap[(address >> 8) & 0x0F].write(value, address);
          break;
        default:
          _cartridge.write(value, address);
          break;
      }
    }
  }
}
