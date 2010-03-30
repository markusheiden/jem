package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.ColorRAM;
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
        _vic._regRaster = line;

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

    int top = (linesPerScreen - 200) / 2;
    int left = (pixelPerLine - 320) / 2;

    byte regExteriorColor = (byte) _vic._regExteriorColor;
    byte regBackGroundColor0 = (byte) _vic._regBackgroundColor[0];

    VICBus bus = _vic._bus;
    int screenBaseAddress = _vic._baseCharacterMode;
    int charsetBaseAddress = _vic._baseCharacterSet;
    ColorRAM colorRam = _vic._colorRam;

    int end = top * pixelPerLine;
    Arrays.fill(screen, 0, end, regExteriorColor);
    int ptr = end;
    for (int y = top, row = 0, lastY = top + 200; y < lastY; row++, y++)
    {
      end = ptr + left;
      Arrays.fill(screen, ptr, end, regExteriorColor);
      ptr = end;

      int index = (row / 8) * 40;
      int screenAddress = screenBaseAddress + index;
      int charsetAddress = charsetBaseAddress + row % 8;
      int colorAddress = index;
      for (int x = left, lastX = left + 320; x < lastX; screenAddress++, colorAddress++, x += 8)
      {
        int character = bus.read(screenAddress);
        int bitmap = bus.read(charsetAddress + (character << 3));
        byte foreground = (byte) colorRam.read(colorAddress);
        screen[ptr++] = (bitmap & 0x80) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x40) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x20) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x10) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x08) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x04) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x02) == 0 ? regBackGroundColor0 : foreground;
        screen[ptr++] = (bitmap & 0x01) == 0 ? regBackGroundColor0 : foreground;
      }

      end = ptr + (pixelPerLine - left - 320);
      Arrays.fill(screen, ptr, end, regExteriorColor);
      ptr = end;
    }
    Arrays.fill(screen, ptr, screen.length, regExteriorColor);

    rendered(screen);
  }
}
