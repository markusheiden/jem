package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
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

    boolean bitmapMode = (_vic._regControl1 & VIC.CONTROL1_BITMAP) != 0;
    boolean extColorMode = (_vic._regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
    byte regExteriorColor = (byte) _vic._regExteriorColor;

    System.out.println("video mode: " + bitmapMode + "/" + extColorMode);

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
   * @param y y position
   * @return
   */
  private int renderTextLine(byte[] screen, int ptr, int y)
  {
    VICBus bus = _vic._bus;
    BusDevice colorRam = _vic._colorRam;

    int screenBaseAddress = _vic._baseCharacterMode;
    int charsetBaseAddress = _vic._baseCharacterSet;

    byte regBackGroundColor0 = (byte) _vic._regBackgroundColor[0];

    // compute character row
    int charRow = y % 8;
    // compute text row
    int screenRow = (y - charRow) * 5; // optimization for (y / 8) * 40

    // address of row in video ram
    int screenAddress = screenBaseAddress + screenRow;
    // pre-add character row
    int charsetAddress = charsetBaseAddress + charRow;

    // render line
    for (int x = 0; x < 320; screenAddress++, x += 8)
    {
      int character = bus.read(screenAddress);
      byte color = (byte) colorRam.read(screenAddress);
      int bitmap = bus.read(charsetAddress + (character << 3));
      for (int mask = 0x80; mask != 0; mask >>= 1)
      {
        screen[ptr++] = (bitmap & mask) == 0 ? regBackGroundColor0 : color;
      }
    }

    return ptr;
  }
}
