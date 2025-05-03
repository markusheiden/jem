package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.bytes.HexUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.bus.NoBusDevice;
import de.heiden.jem.components.ports.InputPort;
import de.heiden.jem.components.ports.InputPortImpl;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.models.c64.components.memory.Patchable;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 bus.
 * <p>
 * TODO evaluate loram, hiram, charen, game, exrom
 * TODO SID, IO1, IO2
 */
public class C64Bus implements BusDevice, Patchable {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final InputPort cpu = new InputPortImpl();

  private final BusDevice ram;
  private final BusDevice basic;
  private final BusDevice vic;
  private final BusDevice colorRam;
  private final BusDevice cia1;
  private final BusDevice cia2;
  private final BusDevice charset;
  private final BusDevice kernel;

  private BusDevice cartridgeL;
  private BusDevice cartridgeH;
  private BusDevice cartridge;

  private final BusDevice noBusDevice;

  private BusDevice[] ioModeRead;
  private BusDevice[] ioModeWrite;

  private final BusDevice[][] ioModesRead;
  private final BusDevice[][] ioModesWrite;

  /**
   * Constructor.
   *
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
    @Nonnull BusDevice ram,
    @Nonnull BusDevice basic,
    @Nonnull BusDevice vic,
    @Nonnull BusDevice colorRam,
    @Nonnull BusDevice cia1,
    @Nonnull BusDevice cia2,
    @Nonnull BusDevice charset,
    @Nonnull BusDevice kernel) {
    this.ram = ram;
    this.basic = basic;
    this.vic = vic;
    this.colorRam = colorRam;
    this.cia1 = cia1;
    this.cia2 = cia2;
    this.charset = charset;
    this.kernel = kernel;
    noBusDevice = new NoBusDevice();

    cartridge = new NoBusDevice(); // TODO
    cartridgeL = new NoBusDevice(); // TODO
    cartridgeH = new NoBusDevice(); // TODO

    var ioModeRead01 = computeIoModeRead(this.ram, this.ram, this.basic, null, this.kernel); // io
    var ioModeRead02 = computeIoModeRead(this.ram, this.ram, this.basic, this.charset, this.kernel); // ram
    var ioModeRead03 = computeIoModeRead(this.ram, this.ram, this.ram, null, this.ram); // io
    var ioModeRead04 = computeIoModeRead(this.ram, this.ram, this.ram, this.charset, this.ram); // ram
    var ioModeRead05 = computeIoModeRead(this.ram, this.ram, this.ram, this.ram, this.ram); // ram
    var ioModeRead06 = computeIoModeRead(this.ram, this.ram, this.ram, null, this.kernel); // io
    var ioModeRead07 = computeIoModeRead(this.ram, this.ram, this.basic, this.charset, this.kernel); // ram
    var ioModeRead08 = computeIoModeRead(this.ram, cartridgeL, this.basic, null, this.kernel); // io
    var ioModeRead09 = computeIoModeRead(this.ram, cartridgeL, this.basic, this.charset, this.kernel); // ram
    var ioModeRead10 = computeIoModeRead(this.ram, this.ram, cartridgeH, null, this.kernel); // io
    var ioModeRead11 = computeIoModeRead(this.ram, this.ram, cartridgeH, this.charset, this.kernel); // ram
    var ioModeRead12 = computeIoModeRead(this.ram, cartridgeL, cartridgeH, null, this.kernel); // io
    var ioModeRead13 = computeIoModeRead(this.ram, cartridgeL, cartridgeH, this.charset, this.kernel); // ram
    var ioModeRead14 = computeIoModeRead(this.ram, cartridgeL, this.ram, null, cartridgeH); // cartridge / io

    var ioModeWriteIo = computeIoModeWrite(this.ram, true);
    var ioModeWriteRam = computeIoModeWrite(this.ram, false);
    var ioModeWriteOpen = computeIoModeWrite(cartridge, true);

    ioModeRead = ioModeRead01;
    ioModesRead = new BusDevice[][]
      {
        ioModeRead07, // 00000 000x0
        ioModeRead14, // 00001 xxx01
        ioModeRead07, // 00010 000x0
        ioModeRead05, // 00011 x001x
        ioModeRead05, // 00100 00100
        ioModeRead14, // 00101 xxx01
        ioModeRead04, // 00110 0011x
        ioModeRead04, // 00111 0011x

        ioModeRead11, // 01000 01000
        ioModeRead14, // 01001 xxx01
        ioModeRead07, // 01010 0101x
        ioModeRead07, // 01011 0101x
        ioModeRead13, // 01100 01100 Mode13 OK?
        ioModeRead14, // 01101 xxx01
        ioModeRead09, // 01110 01110
        ioModeRead02, // 01111 01111

        ioModeRead06, // 10000 100x0
        ioModeRead14, // 10001 xxx01
        ioModeRead06, // 10010 100x0
        ioModeRead05, // 10011 x001x
        ioModeRead03, // 10100 10100
        ioModeRead14, // 10101 xxx01
        ioModeRead03, // 10110 1011x
        ioModeRead03, // 10111 1011x

        ioModeRead10, // 11000 11000
        ioModeRead14, // 11001 xxx01
        ioModeRead06, // 11010 1101x
        ioModeRead06, // 11011 1101x
        ioModeRead12, // 11100 11100 Mode12 OK???
        ioModeRead14, // 11101 xxx01
        ioModeRead08, // 11110 11110
        ioModeRead01  // 11111 11111
      };

    ioModeWrite = ioModeWriteIo;
    ioModesWrite = new BusDevice[][]
      {
        ioModeWriteRam,  // 07 00000 000x0
        ioModeWriteOpen, // 14 00001 xxx01
        ioModeWriteRam,  // 07 00010 000x0
        ioModeWriteRam,  // 0500011 x001x
        ioModeWriteRam,  // 05 00100 00100
        ioModeWriteOpen, // 14 00101 xxx01
        ioModeWriteRam,  // 04 00110 0011x
        ioModeWriteRam,  // 04 00111 0011x

        ioModeWriteRam,  // 11 01000 01000
        ioModeWriteOpen, // 14 01001 xxx01
        ioModeWriteRam,  // 07 01010 0101x
        ioModeWriteRam,  // 07 01011 0101x
        ioModeWriteRam,  // 13 01100 01100 Mode13 OK?
        ioModeWriteOpen, // 14 01101 xxx01
        ioModeWriteRam,  // 09 01110 01110
        ioModeWriteRam,  // 02 01111 01111

        ioModeWriteIo,   // 06 10000 100x0
        ioModeWriteOpen, // 14 10001 xxx01
        ioModeWriteIo,   // 06 10010 100x0
        ioModeWriteRam,  // 05 10011 x001x
        ioModeWriteIo,   // 03 10100 10100
        ioModeWriteOpen, // 14 10101 xxx01
        ioModeWriteIo,   // 03 10110 1011x
        ioModeWriteIo,   // 03 10111 1011x

        ioModeWriteIo,   // 10 11000 11000
        ioModeWriteOpen, // 14 11001 xxx01
        ioModeWriteIo,   // 06 11010 1101x
        ioModeWriteIo,   // 06 11011 1101x
        ioModeWriteIo,   // 12 11100 11100 Mode12 OK???
        ioModeWriteOpen, // 14 11101 xxx01
        ioModeWriteIo,   // 08 11110 11110
        ioModeWriteIo,   // 01 11111 11111
      };

    cpu.addInputPortListener((value, mask) -> {
      // TODO 2010-10-08 mh: consider signals from expansion port
      int mode = ((value << 2) | 0x03) & 0x1f;

      BusDevice[] oldIoModeRead = ioModeRead;
      ioModeRead = ioModesRead[mode];
      ioModeWrite = ioModesWrite[mode];
      if (logger.isDebugEnabled() && oldIoModeRead != ioModeRead) {
        logger.debug("Changed bus mode to {}", HexUtil.hexBytePlain(mode));
      }
    });
  }

  private BusDevice[] computeIoModeRead(BusDevice ram, BusDevice x8000, BusDevice xA000, BusDevice xD000, BusDevice xE000) {
    var result = new BusDevice[256];
    for (int i = 0x00; i <= 0x7F; i++) {
      result[i] = ram;
    }
    for (int i = 0x80; i <= 0x9F; i++) {
      result[i] = x8000;
    }
    for (int i = 0xA0; i <= 0xBF; i++) {
      result[i] = xA000;
    }
    for (int i = 0xC0; i <= 0xCF; i++) {
      result[i] = ram;
    }
    if (xD000 != null) {
      for (int i = 0xD0; i <= 0xDF; i++) {
        result[i] = xD000;
      }
    } else {
      setIo(result);
    }
    for (int i = 0xE0; i <= 0xFF; i++) {
      result[i] = xE000;
    }

    return result;
  }

  private BusDevice[] computeIoModeWrite(BusDevice ram, boolean io) {
    var result = new BusDevice[256];
    for (int i = 0x00; i <= 0xFF; i++) {
      result[i] = ram;
    }
    if (io) {
      setIo(result);
    }
    return result;
  }

  private void setIo(BusDevice[] result) {
    for (int i = 0xD0; i <= 0xD3; i++) {
      result[i] = vic;
    }
    // TODO markus 2016-08-16: Add SID...
    for (int i = 0xD4; i <= 0xD7; i++) {
      result[i] = ram;
    }
    for (int i = 0xD8; i <= 0xDB; i++) {
      result[i] = colorRam;
    }
    result[0xDC] = cia1;
    result[0xDD] = cia2;
    result[0xDE] = noBusDevice;
    result[0xDF] = noBusDevice;
  }

  //
  // public
  //

  /**
   * Connect to cpu port.
   */
  public void connect(@Nonnull OutputPort cpu) {
    this.cpu.connect(cpu);
  }

  /**
   * Read a byte from the bus device.
   *
   * @param address address to read byte from
   * @require address >= 0x0000 && address < 0x10000
   * @ensure result >= 0x00 && result < 0x100
   */
  @Override
  public final int read(int address) {
    assert address >= 0x0000 && address < 0x10000 : "address >= 0x0000 && address < 0x10000";

    return ioModeRead[address >> 8].read(address);
  }

  /**
   * Patch a byte in the ROM.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   */
  @Override
  public void patch(int value, int address) {
    // 0x100 is used to escape emulation in the cpu
    assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

    ((Patchable) ioModeRead[address >> 8]).patch(value, address);
  }

  /**
   * Write the byte to the bus device.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   * @require address >= 0x0000 && address < 0x10000
   */
  @Override
  public final void write(int value, int address) {
    assert value >= 0x00 && value < 0x100 : "value >= 0x00 && value < 0x100";
    assert address >= 0x0000 && address < 0x10000 : "address >= 0x0000 && address < 0x10000";

    ioModeWrite[address >> 8].write(value, address);
  }
}
