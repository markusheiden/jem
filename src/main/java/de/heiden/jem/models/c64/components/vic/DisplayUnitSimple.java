package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
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
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - vic._lastVBlank,
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - vic._lastVBlank);
  }

  @Override
  @Interruptible
  public final void run()
  {
    _vic.reset();

    final int lastX = _vic._lastX;
    final int pixelPerLine = _vic._lastX - _vic._firstVisibleX + _vic._lastVisibleX;

    //noinspection InfiniteLoopStatement
    while (true)
    {
      byte[] screen = _screenRender;
      int ptr = 0;

      int raster = 0;

      // top vblank
      for (; raster < _vic._lastVBlank; raster++)
      {
        _vic.setRasterLine(raster);
        for (int x = 0; x < lastX; x++)
        {
          _tick.waitForTick();
        }
      }

      // top border
      for (; raster < _vic._firstLine_25; raster++)
      {
        _vic.setRasterLine(raster);
        for (int x = 0; x < lastX; x++)
        {
          _tick.waitForTick();
        }
        Arrays.fill(screen, ptr, (ptr += pixelPerLine), _vic._regExteriorColor);
      }

      // visible area
      for (int y = 0; raster < _vic._lastLine_25; raster++, y++)
      {
        _vic.setRasterLine(raster);

        for (int x = 0; x < lastX; x++)
        {
          _tick.waitForTick();
        }

        Arrays.fill(screen, ptr, (ptr += lastX - _vic._firstVisibleX + _vic._firstX_25), _vic._regExteriorColor);
        int newPtr = renderTextLine(screen, ptr , y);
        renderSprites(screen, ptr, raster);
        ptr = newPtr;
        Arrays.fill(screen, ptr, (ptr += _vic._lastVisibleX - _vic._lastX_25), _vic._regExteriorColor);
      }

      // bottom border
      for (; raster < _vic._firstVBlank; raster++)
      {
        _vic.setRasterLine(raster);
        for (int x = 0; x < lastX; x++)
        {
          _tick.waitForTick();
        }
        Arrays.fill(screen, ptr, (ptr += pixelPerLine), _vic._regExteriorColor);
      }

      // bottom vblank
      for (; raster < _vic._linesPerScreen; raster++)
      {
        _vic.setRasterLine(raster);
        for (int x = 0; x < lastX; x++)
        {
          _tick.waitForTick();
        }
      }

      rendered(screen);
    }
  }

  /**
   * Render sprites for the given line.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param raster raster line
   */
  private void renderSprites(byte[] screen, int ptr, int raster)
  {
    VICBus bus = _vic._bus;

    int spritePointer = _vic._baseCharacterMode + 0x03F8 + 7;

    for (int i = 7; i >= 0; i--, spritePointer--)
    {
      Sprite sprite = _vic._sprites[i];
      if (sprite.enabled)
      {
        int y = sprite.y;
        int endY = y + (sprite.expandY? 42 : 21);
        if (y <= raster && raster < endY)
        {
          int spritePtr = ptr - _vic._firstX_25  + sprite.x;
          int spriteRow = raster - y;
          if (sprite.expandY)
          {
            spriteRow >>= 1;
          }
          int baseSprite = (bus.read(spritePointer) << 6) + spriteRow * 3;
          spritePtr = renderSpriteByte(screen, spritePtr, bus.read(baseSprite++), sprite);
          spritePtr = renderSpriteByte(screen, spritePtr, bus.read(baseSprite++), sprite);
          spritePtr = renderSpriteByte(screen, spritePtr, bus.read(baseSprite++), sprite);
        }
      }
    }
  }

  private int renderSpriteByte(byte[] screen, int spritePtr, int bitmap, Sprite sprite)
  {
    byte color = sprite.color;
    boolean expandX = sprite.expandX;
    int incX = expandX? 2 : 1;

    if (sprite.multicolor)
    {
      incX <<= 1;

      byte pixel = getSpriteColor((bitmap & 0xC0) >> 6, color);
      if (pixel >= 0)
      {
        screen[spritePtr++] = pixel;
        screen[spritePtr++] = pixel;
        if (expandX)
        {
          screen[spritePtr++] = pixel;
          screen[spritePtr++] = pixel;
        }
      }
      else
      {
        spritePtr += incX;
      }

      pixel = getSpriteColor((bitmap & 0x30) >> 4, color);
      if (pixel >= 0)
      {
        screen[spritePtr++] = pixel;
        screen[spritePtr++] = pixel;
        if (expandX)
        {
          screen[spritePtr++] = pixel;
          screen[spritePtr++] = pixel;
        }
      }
      else
      {
        spritePtr += incX;
      }

      pixel = getSpriteColor((bitmap & 0x0C) >> 2, color);
      if (pixel >= 0)
      {
        screen[spritePtr++] = pixel;
        screen[spritePtr++] = pixel;
        if (expandX)
        {
          screen[spritePtr++] = pixel;
          screen[spritePtr++] = pixel;
        }
      }
      else
      {
        spritePtr += incX;
      }

      pixel = getSpriteColor((bitmap & 0x03) >> 0, color);
      if (pixel >= 0)
      {
        screen[spritePtr++] = pixel;
        screen[spritePtr++] = pixel;
        if (expandX)
        {
          screen[spritePtr++] = pixel;
          screen[spritePtr++] = pixel;
        }
      }
      else
      {
        spritePtr += incX;
      }
    }
    else
    {
      for (int mask = 0x80; mask != 0; mask >>= 1)
      {
        if ((bitmap & mask) != 0)
        {
          screen[spritePtr++] = color;
          if (expandX)
          {
            screen[spritePtr++] = color;
          }
        }
        else
        {
          spritePtr += incX;
        }
      }
    }

    return spritePtr;
  }

  private byte getSpriteColor(int bitmap, byte color)
  {
    switch (bitmap)
    {
      case 0x01: return _vic._regSpritesMulticolor0;
      case 0x02: return color;
      case 0x03: return _vic._regSpritesMulticolor1;
      default: return -1;
    }
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

    byte regBackGroundColor0 = _vic._regBackgroundColor0;

    // compute character row
    int charRow = y & 0x0007; // optimization for y % 8
    // compute text row
    int screenRow = (y & 0xFFF8) * 5; // optimization for (y / 8) * 40
    // address of row in video ram
    int screenAddress = screenBaseAddress + screenRow;
    // pre-add character row
    int charsetAddress = charsetBaseAddress + charRow;

    // render line
    for (int x = 0; x < 320; screenAddress++, x += 8)
    {
      byte color = (byte) colorRam.read(screenAddress); // color ram masks address itself
      int character = bus.read(screenAddress);
      int bitmap = bus.read(charsetAddress + (character << 3));

      if (multiColor && ((color & 0x08) != 0))
      {
        color &= 0x07;
        byte pixel = getMultiColor((bitmap & 0xC0) >> 6, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
        pixel = getMultiColor((bitmap & 0x30) >> 4, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
        pixel = getMultiColor((bitmap & 0x0C) >> 2, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
        pixel = getMultiColor((bitmap & 0x03) >> 0, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
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
      case 0x00: return _vic._regBackgroundColor0;
      case 0x01: return _vic._regBackgroundColor1;
      case 0x02: return _vic._regBackgroundColor2;
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
