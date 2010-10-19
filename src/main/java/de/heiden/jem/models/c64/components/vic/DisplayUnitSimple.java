package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import org.apache.log4j.Logger;
import org.serialthreads.Interruptible;

import java.util.Arrays;

/**
 * Display unit of vic.
 *
 * TODO refactor dependencies
 */
public class DisplayUnitSimple extends AbstractDisplayUnit
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  /**
   * Hidden constructor.
   *
   * @param vic vic this display unit belongs to
   * @param clock clock
   */
  DisplayUnitSimple(VIC vic, Clock clock)
  {
    super(vic, clock,
      0,
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._linesPerScreen,
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._linesPerScreen);
  }

  @Override
  @Interruptible
  public final void run()
  {
    _vic.reset();

    int lines = _vic._linesPerScreen;
    int lineLength = _vic._cyclesPerLine * 8;

    //noinspection InfiniteLoopStatement
    while (true)
    {
      for (int line = 0; line < lines; line++)
      {
        _vic.setRasterLine(line);

        for (int x = 0; x < lineLength; x++)
        {
          _tick.waitForTick();
        }
      }
      renderScreenAtOnce();
    }
  }

  public void renderScreenAtOnce()
  {
    byte[] screen = _screenRender;

    int linesPerScreen = getHeight();
    int pixelPerLine = getWidth();

    // TODO 2010-10-18 mh: use correct border values
    int top = (linesPerScreen - 200) / 2;
    int left = (pixelPerLine - 320) / 2;

    byte regExteriorColor = (byte) _vic._regExteriorColor;

    // top border
    int end = top * pixelPerLine;
    Arrays.fill(screen, 0, end, regExteriorColor);
    int ptr = end;

    for (int y = 0; y < 200; y++)
    {
      // left border
      end = ptr + left;
      Arrays.fill(screen, ptr, end, regExteriorColor);
      ptr = end;

      ptr = renderTextLine(screen, ptr , y);

      // right border
      end = ptr + (pixelPerLine - left - 320);
      Arrays.fill(screen, ptr, end, regExteriorColor);
      ptr = end;
    }

    // bottom border
    Arrays.fill(screen, ptr, screen.length, regExteriorColor);

    rendered(screen);
  }

  /**
   * Render text line.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param y y position in visible screen area
   * @return next index in screen data
   */
  private int renderTextLine(byte[] screen, int ptr, int y)
  {
    VICBus bus = _vic._bus;
    BusDevice colorRam = _vic._colorRam;

    int screenBaseAddress = _vic._baseCharacterMode;
    int charsetBaseAddress = _vic._baseCharacterSet;

    boolean multiColor = (_vic._regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;

    byte regBackGroundColor0 = (byte) _vic._regBackgroundColor[0];
    byte regBackGroundColor1 = (byte) _vic._regBackgroundColor[1];
    byte regBackGroundColor2 = (byte) _vic._regBackgroundColor[2];

    // compute character row
    int charRow = y & 0x0007; // optimization for y % 8
    // compute text row
    int screenRow = (y & 0xFFF8) * 5; // optimization for (y / 8) * 40
    // address of row in vid
    // eo ram
    int screenAddress = screenBaseAddress + screenRow;
    // pre-add character row
    int charsetAddress = charsetBaseAddress + charRow;

    // render line
    for (int x = 0; x < 320; screenAddress++, x += 8)
    {
      byte color = (byte) colorRam.read(screenAddress); // color ram masks address itself
      int character = bus.read(screenAddress);
      int bitmap = bus.read(charsetAddress + (character << 3));

      if (multiColor)
      {
        if ((color & 0x08) != 0)
        {
          color &= 0x07;
          byte pixel = getMultiColor(bitmap & 0xC0 >> 6, color);
          screen[ptr++] = pixel;
          screen[ptr++] = pixel;
          color = getMultiColor(bitmap & 0x30 >> 4, color);
          screen[ptr++] = pixel;
          screen[ptr++] = pixel;
          color = getMultiColor(bitmap & 0x0C >> 2, color);
          screen[ptr++] = pixel;
          screen[ptr++] = pixel;
          color = getMultiColor(bitmap & 0x03 >> 0, color);
          screen[ptr++] = pixel;
          screen[ptr++] = pixel;
        }
        else
        {
          color &= 0x07;
          for (int mask = 0x80; mask != 0; mask >>= 1)
          {
            screen[ptr++] = (bitmap & mask) == 0 ? regBackGroundColor0 : color;
          }
        }
      }
      else
      {
        for (int mask = 0x80; mask != 0; mask >>= 1)
        {
          screen[ptr++] = (bitmap & mask) == 0 ? regBackGroundColor0 : color;
        }
      }
    }

    return ptr;
  }

  private byte getMultiColor(int bitmap, byte color)
  {
    switch (bitmap)
    {
      case 0x00: return (byte) _vic._regBackgroundColor[0];
      case 0x01: return (byte) _vic._regBackgroundColor[1];
      case 0x02: return (byte) _vic._regBackgroundColor[2];
      default: return color; // case 0x03
    }

  }

  /**
   * Render bitmap line.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param y y position in visible screen area
   * @return next index in screen data
   */
  private int renderBitmapLine(byte[] screen, int ptr, int y)
  {
    VICBus bus = _vic._bus;

    int screenBaseAddress = _vic._baseCharacterMode;
    int bitmapBaseAddress = _vic._baseBitmapMode;

    // compute character row
    int charRow = y & 0x0007; // optimization for y % 8
    // compute text row
    int screenRow = (y & 0xFFF8) * 5; // optimization for (y / 8) * 40

    // address of row in video ram
    int screenAddress = screenBaseAddress + screenRow;
    // pre-add bitmap row
    int bitmapAddress = bitmapBaseAddress + (y & 0xFFF8) * 40 + charRow;

    // render line
    for (int x = 0; x < 320; screenAddress++, x += 8)
    {
      int character = bus.read(screenAddress);
      byte foreground = (byte) (character >> 4);
      byte background = (byte) (character & 0x0F);
      int bitmap = bus.read(bitmapAddress + x);
      for (int mask = 0x80; mask != 0; mask >>= 1)
      {
        screen[ptr++] = (bitmap & mask) == 0 ? background : foreground;
      }
    }

    return ptr;
  }
}
