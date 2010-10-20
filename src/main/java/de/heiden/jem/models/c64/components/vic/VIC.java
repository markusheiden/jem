package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.components.bus.BusDevice;
import org.apache.log4j.Logger;

/**
 * VIC.
 */
public abstract class VIC implements BusDevice
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  /**
   * VIC parameters.
   */
  protected final int _linesPerScreen;
  protected final int _firstLine_25 = 51;
  protected final int _firstLine_24 = 55;
  protected final int _lastLine_25 = 250 + 1;
  protected final int _lasttLine_24 = 246 + 1;
  protected final int _firstVBlank;
  protected final int _lastVBlank;

  protected final int _cyclesPerLine;
  protected final int _firstX_25 = 24;
  protected final int _firstX_24 = 31;
  protected final int _lastX_25 = 343 + 1;
  protected final int _lastX_24 = 334 + 1;
  protected final int _firstVisibleX;
  protected final int _lastVisibleX;
  protected final int _lastX;


  // mem access unit
  private final MemAccessUnit _memAccessUnit;

  // display unit
  public final AbstractDisplayUnit _displayUnit;

  // address mask
  private final int _mask;

  // components
  protected final Clock _clock;
  public final VICBus _bus;
  public final ColorRAM _colorRam;

  private final OutputPortImpl _irqPort;

  // sprite register
  protected Sprite[] _sprites;

  private int _regSpritesEnable; // 0x15
  private int _regSpritesMSBX; // 0x11
  private int _regSpritesExpandX; // 0x17
  private int _regSpritesExpandY; // 0x1D
  private int _regSpritesMulticolorMode; // 0x1C
  private int _regSpritesMulticolor0; // 0x15
  private int _regSpritesMulticolor1; // 0x16

  private int _regSpritesSpriteCollision; // 0x1E
  private int _regSpritesBackgroundCollision; // 0x1F
  private int _regSpritesBackgroundPriority; // 0x1B

  // control
  protected int _regControl1; // 0x11
  protected static final int CONTROL1_BITMAP = 1 << 5;
  protected static final int CONTROL1_EXT_COLOR = 1 << 6;
  protected int _regRaster; // read: 0x11 bit 7, 0x12
  protected int _regRasterIRQ; // write: 0x11 bit 7, 0x12
  protected int _regControl2; // 0x16
  protected static final int CONTROL2_MULTI_COLOR = 1 << 4;

  // base addresses
  private int _regBase; // 0x18
  protected int _baseCharacterMode; // base address of text screen
  protected int _baseBitmapMode; // base address of bitmap screen
  protected int _baseCharacterSet; // base address of character set

  // strobe
  private int _regStrobeX; // 0x13
  private int _regStrobeY; // 0x14

  // irq register
  protected int _regInterruptRequest; // 0x19
  protected int _regInterruptMask; // 0x1A
  protected static final int INTERRUPT_MASK = 0x0F;
  protected static final int INTERRUPT_RASTER = 1 << 0;
  protected static final int INTERRUPT_ANY = 1 << 7;
  // color register
  protected int _regExteriorColor; // 0x20
  protected int[] _regBackgroundColor = new int[4]; // 0x21-0x24

  // C128 register
  private int _regKeyboard; // 0x2F
  private int _regFastMode; // 0x30

  /**
   * Constructor.
   *
   * @param clock system clock (1 MHz).
   * @param bus vic memory bus.
   * @param colorRam color ram.
   * @param cyclesPerLine system clock cycles per line
   * @param linesPerScreen lines per screen
   * @param firstVBlank
   * @param lastVBlank
   * @param firstVisibleX
   * @param lastVisibleX last x value
   * @param lastX
   * @require clock != null
   * @require bus != null
   * @require colorRam != null
   */
  public VIC(Clock clock, VICBus bus, ColorRAM colorRam,
    int cyclesPerLine, int linesPerScreen, int firstVBlank, int lastVBlank,
    int firstVisibleX, int lastVisibleX, int lastX)
  {
    assert clock != null : "clock != null";
    assert bus != null : "bus != null";
    assert colorRam != null : "colorRam != null";

    _clock = clock;
    _bus = bus;
    _colorRam = colorRam;

    _cyclesPerLine = cyclesPerLine;
    _linesPerScreen = linesPerScreen;
    _firstVBlank = firstVBlank;
    _lastVBlank = lastVBlank;
    _firstVisibleX = (firstVisibleX + 0x007) & 0x01F8;
    _lastVisibleX = lastVisibleX & 0x01F8;
    _lastX = lastX;

    // address mask
    _mask = 0x3F;

    // prepare sprites
    _sprites = new Sprite[8];
    for (int i = 0; i < _sprites.length; i++)
    {
      _sprites[i] = new Sprite(i);
    }

    _logger.debug("start vic mem access unit");
    _memAccessUnit = new MemAccessUnit(this, clock);

    _logger.debug("start vic display unit");
    _displayUnit = new DisplayUnitSimple(this, clock);

    _irqPort = new OutputPortImpl();
    _irqPort.setOutputMask(0x01);
    _irqPort.setOutputData(0x01);
  }

  /**
   * Reset VIC.
   */
  public void reset()
  {
    // TODO what to init?
  }

  /**
   * Address mask.
   *
   * @ensure result >= 0 && result < 0x10000
   */
  public int mask()
  {
    assert _mask >= 0 && _mask < 0x10000 : "result >= 0 && result < 0x10000";
    return _mask;
  }

  /**
   * IRQ output signal.
   */
  public OutputPort getIRQ()
  {
    return _irqPort;
  }

  /**
   * Write byte to bus device.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0 && value < 0x100
   */
  public void write(int value, int address)
  {
    assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

    switch (address & _mask)
    {
      case 0x00:
      {
        _sprites[0].setXLSB(value);
        break;
      }
      case 0x01:
      {
        _sprites[0].y = value;
        break;
      }
      case 0x02:
      {
        _sprites[1].setXLSB(value);
        break;
      }
      case 0x03:
      {
        _sprites[1].y = value;
        break;
      }
      case 0x04:
      {
        _sprites[2].setXLSB(value);
        break;
      }
      case 0x05:
      {
        _sprites[2].y = value;
        break;
      }
      case 0x06:
      {
        _sprites[3].setXLSB(value);
        break;
      }
      case 0x07:
      {
        _sprites[3].y = value;
        break;
      }
      case 0x08:
      {
        _sprites[4].setXLSB(value);
        break;
      }
      case 0x09:
      {
        _sprites[4].y = value;
        break;
      }
      case 0x0A:
      {
        _sprites[5].setXLSB(value);
        break;
      }
      case 0x0B:
      {
        _sprites[5].y = value;
        break;
      }
      case 0x0C:
      {
        _sprites[6].setXLSB(value);
        break;
      }
      case 0x0D:
      {
        _sprites[6].y = value;
        break;
      }
      case 0x0E:
      {
        _sprites[7].setXLSB(value);
        break;
      }
      case 0x0F:
      {
        _sprites[7].y = value;
        break;
      }
      case 0x10:
      {
        _regSpritesMSBX = value;
        for (Sprite sprite : _sprites)
        {
          sprite.setXMSB(value);
        }
        break;
      }
      case 0x11:
      {
        _regControl1 = value;
        // bit 7 is high bit of raster irq line
        _regRasterIRQ = _regRasterIRQ & 0xFF | (value & 0x80) << 1;

        if (_logger.isDebugEnabled())
        {
          boolean bitmapMode = (_regControl1 & VIC.CONTROL1_BITMAP) != 0;
          boolean extColorMode = (_regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
          boolean multiColorMode = (_regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;
          _logger.debug("bitmap: " + bitmapMode + ", extended color: " + extColorMode + ", multi color: " + multiColorMode);
        }

        break;
      }
      case 0x12:
      {
        _regRasterIRQ = _regRasterIRQ & 0x0100 | value;
        break;
      }
      case 0x13:
      {
        _regStrobeX = value;
        break;
      }
      case 0x14:
      {
        _regStrobeY = value;
        break;
      }
      case 0x15:
      {
        _regSpritesEnable = value;
        for (Sprite sprite : _sprites)
        {
          sprite.enable(value);
        }
        break;
      }
      case 0x16:
      {
        _regControl2 = value;

        if (_logger.isDebugEnabled())
        {
          boolean bitmapMode = (_regControl1 & VIC.CONTROL1_BITMAP) != 0;
          boolean extColorMode = (_regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
          boolean multiColorMode = (_regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;
          _logger.debug("bitmap: " + bitmapMode + ", extended color: " + extColorMode + ", multi color: " + multiColorMode);
        }

        break;
      }
      case 0x17:
      {
        _regSpritesExpandX = value;
        for (Sprite sprite : _sprites)
        {
          sprite.setExpandX(value);
        }
        break;
      }
      case 0x18:
      {
        _regBase = value | 0x01; // set unused bits to 1;
        updateBaseAddresses();
        break;
      }
      case 0x19:
      {
        // 1-bits in value disable the corresponding interrupt bits in the irq register
        _regInterruptRequest &= ~(value & INTERRUPT_MASK);
        if ((_regInterruptRequest & INTERRUPT_MASK) == 0)
        {
          // no pending irq anymore:

          // clear "master" interrupt bit
          _regInterruptRequest = 0x00;
          // reset irq
          _irqPort.setOutputData(0x1);
        }
        break;
      }
      case 0x1A:
      {
        _regInterruptMask = value;
        break;
      }
      case 0x1B:
      {
        _regSpritesBackgroundPriority = value;
        break;
      }
      case 0x1C:
      {
        _regSpritesMulticolorMode = value;
        for (Sprite sprite : _sprites)
        {
          sprite.setMulticolor(value);
        }
        break;
      }
      case 0x1D:
      {
        _regSpritesExpandY = value;
        for (Sprite sprite : _sprites)
        {
          sprite.setExpandY(value);
        }
        break;
      }
      case 0x1E:
      {
        _regSpritesSpriteCollision = value;
        break;
      }
      case 0x1F:
      {
        _regSpritesBackgroundCollision = value;
        break;
      }
      case 0x20:
      {
        _regExteriorColor = value;
        break;
      }
      case 0x21:
      {
        _regBackgroundColor[0] = value;
        break;
      }
      case 0x22:
      {
        _regBackgroundColor[1] = value;
        break;
      }
      case 0x23:
      {
        _regBackgroundColor[2] = value;
        break;
      }
      case 0x24:
      {
        _regBackgroundColor[3] = value;
        break;
      }
      case 0x25:
      {
        _regSpritesMulticolor0 = value;
        for (Sprite sprite : _sprites)
        {
          sprite.multicolor1 = value;
        }
        break;
      }
      case 0x26:
      {
        _regSpritesMulticolor1 = value;
        for (Sprite sprite : _sprites)
        {
          sprite.multicolor2 = value;
        }
        break;
      }
      case 0x27:
      {
        _sprites[0].color = value;
        break;
      }
      case 0x28:
      {
        _sprites[1].color = value;
        break;
      }
      case 0x29:
      {
        _sprites[2].color = value;
        break;
      }
      case 0x2A:
      {
        _sprites[3].color = value;
        break;
      }
      case 0x2B:
      {
        _sprites[4].color = value;
        break;
      }
      case 0x2C:
      {
        _sprites[5].color = value;
        break;
      }
      case 0x2D:
      {
        _sprites[6].color = value;
        break;
      }
      case 0x2E:
      {
        _sprites[7].color = value;
        break;
      }
      case 0x2F:
      {
        _regKeyboard = value;
        break;
      }
      case 0x30:
      {
        _regFastMode = value;
        break;
      }
      default:
      {
        // ignore
        break;
      }
    }
  }

  /**
   * Read byte from bus device.
   *
   * @param address address to read byte from
   * @ensure result >= 0 && result < 0x100
   */
  public int read(int address)
  {
    int result;
    switch (address & _mask)
    {
      case 0x00:
      {
        result = _sprites[0].getXLSB();
        break;
      }
      case 0x01:
      {
        result = _sprites[0].y;
        break;
      }
      case 0x02:
      {
        result = _sprites[1].getXLSB();
        break;
      }
      case 0x03:
      {
        result = _sprites[1].y;
        break;
      }
      case 0x04:
      {
        result = _sprites[2].getXLSB();
        break;
      }
      case 0x05:
      {
        result = _sprites[2].y;
        break;
      }
      case 0x06:
      {
        result = _sprites[3].getXLSB();
        break;
      }
      case 0x07:
      {
        result = _sprites[3].y;
        break;
      }
      case 0x08:
      {
        result = _sprites[4].getXLSB();
        break;
      }
      case 0x09:
      {
        result = _sprites[4].y;
        break;
      }
      case 0x0A:
      {
        result = _sprites[5].getXLSB();
        break;
      }
      case 0x0B:
      {
        result = _sprites[5].y;
        break;
      }
      case 0x0C:
      {
        result = _sprites[6].getXLSB();
        break;
      }
      case 0x0D:
      {
        result = _sprites[6].y;
        break;
      }
      case 0x0E:
      {
        result = _sprites[7].getXLSB();
        break;
      }
      case 0x0F:
      {
        result = _sprites[7].y;
        break;
      }
      case 0x10:
      {
        result = _regSpritesMSBX;
        break;
      }
      case 0x11:
      {
        // TODO 2010-03-17 mh: correct?
        result = _regControl1 & 0x7F;
        // bit 7 is high bit of current raster line
        result |= (_regRaster & 0x0100) >> 1;
        break;
      }
      case 0x12:
      {
        result = _regRaster & 0xFF;
        break;
      }
      case 0x13:
      {
        result = _regStrobeX;
        break;
      }
      case 0x14:
      {
        result = _regStrobeY;
        break;
      }
      case 0x15:
      {
        result = _regSpritesEnable;
        break;
      }
      case 0x16:
      {
        result = _regControl2;
        break;
      }
      case 0x17:
      {
        result = _regSpritesExpandX;
        break;
      }
      case 0x18:
      {
        result = _regBase;
        break;
      }
      case 0x19:
      {
        result = _regInterruptRequest;
        break;
      }
      case 0x1A:
      {
        result = _regInterruptMask;
        break;
      }
      case 0x1B:
      {
        result = _regSpritesBackgroundPriority;
        break;
      }
      case 0x1C:
      {
        result = _regSpritesMulticolorMode;
        break;
      }
      case 0x1D:
      {
        result = _regSpritesExpandY;
        break;
      }
      case 0x1E:
      {
        result = _regSpritesSpriteCollision;
        break;
      }
      case 0x1F:
      {
        result = _regSpritesBackgroundCollision;
        break;
      }
      case 0x20:
      {
        result = _regExteriorColor;
        break;
      }
      case 0x21:
      {
        result = _regBackgroundColor[0];
        break;
      }
      case 0x22:
      {
        result = _regBackgroundColor[1];
        break;
      }
      case 0x23:
      {
        result = _regBackgroundColor[2];
        break;
      }
      case 0x24:
      {
        result = _regBackgroundColor[3];
        break;
      }
      case 0x25:
      {
        result = _regSpritesMulticolor0;
        break;
      }
      case 0x26:
      {
        result = _regSpritesMulticolor1;
        break;
      }
      case 0x27:
      {
        result = _sprites[0].color;
        break;
      }
      case 0x28:
      {
        result = _sprites[1].color;
        break;
      }
      case 0x29:
      {
        result = _sprites[2].color;
        break;
      }
      case 0x2A:
      {
        result = _sprites[3].color;
        break;
      }
      case 0x2B:
      {
        result = _sprites[4].color;
        break;
      }
      case 0x2C:
      {
        result = _sprites[5].color;
        break;
      }
      case 0x2D:
      {
        result = _sprites[6].color;
        break;
      }
      case 0x2E:
      {
        result = _sprites[7].color;
        break;
      }
      case 0x2F:
      {
        result = _regKeyboard;
        break;
      }
      case 0x30:
      {
        result = _regFastMode;
        break;
      }
      default:
      {
        result = 0xFF; // TODO correct?
        break;
      }
    }

    assert result >= 0 && result < 0x100 : "result >= 0 && result < 0x100";
    return result;
  }

  //
  // protected
  //

  /**
   * Sets to current raster line.
   *
   * @param line raster line
   */
  protected void setRasterLine(int line)
  {
    _regRaster = line;
    if (line == _regRasterIRQ && (_regInterruptMask & INTERRUPT_RASTER) != 0)
    {
      _regInterruptRequest |= INTERRUPT_RASTER | INTERRUPT_ANY;
      _irqPort.setOutputData(0x0);

      if (_logger.isDebugEnabled())
      {
        _logger.debug("Raster irq at line " + _regRaster + " at tick " + _clock.getTick());
      }
    }
  }

  /**
   * Set base register values.
   */
  protected void updateBaseAddresses()
  {
    _baseCharacterMode = (_regBase & 0xF0) << 6;
    _baseBitmapMode = (_regBase & 0x04) << 10;
    _baseCharacterSet = (_regBase & 0x0E) << 10; // TODO what about bit 1?
  }
}
