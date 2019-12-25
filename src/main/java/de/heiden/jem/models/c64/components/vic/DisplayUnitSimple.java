package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import org.serialthreads.Interruptible;

import java.util.Arrays;

/**
 * Display unit of vic.
 * <p/>
 * TODO refactor dependencies
 */
public class DisplayUnitSimple extends AbstractDisplayUnit {
  /**
   * Hidden constructor.
   *
   * @param vic vic this display unit belongs to
   */
  DisplayUnitSimple(VIC vic) {
    super(vic,
      0,
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - vic._lastVBlank,
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - vic._lastVBlank);
  }

  @Override
  @Interruptible
  public final void run() {
    _vic.reset();

    //noinspection InfiniteLoopStatement
    while (true) {
      int ptr = 0;

      int raster = 0;

      // top vblank
      for (; raster < _vic._lastVBlank; raster++) {
        _vic.setRasterLine(raster);
        for (int x = 0; x < _vic._lastX; x++) {
          _tick.waitForTick();
        }
      }

      // top border
      for (; raster < _vic._firstLine_25; raster++) {
        _vic.setRasterLine(raster);
        for (int x = 0; x < _vic._lastX; x++) {
          _tick.waitForTick();
        }

        final int pixelPerLine = _vic._lastX - _vic._firstVisibleX + _vic._lastVisibleX;
        Arrays.fill(_screenRender, ptr, ptr += pixelPerLine, _vic._regExteriorColor);
      }

      // visible area
      for (int y = 0; raster < _vic._lastLine_25; raster++, y++) {
        _vic.setRasterLine(raster);

        for (int x = 0; x < _vic._lastX; x++) {
          _tick.waitForTick();
        }

        final int leftBorder = _vic._lastX - _vic._firstVisibleX + _vic._firstX_25;
        Arrays.fill(_screenRender, ptr, ptr += leftBorder, _vic._regExteriorColor);
        int newPtr = renderTextLine(_screenRender, ptr, y);
        renderSprites(_screenRender, ptr, raster);
        ptr = newPtr;
        final int rightBorder = _vic._lastVisibleX - _vic._lastX_25;
        Arrays.fill(_screenRender, ptr, ptr += rightBorder, _vic._regExteriorColor);
      }

      // bottom border
      for (; raster < _vic._firstVBlank; raster++) {
        _vic.setRasterLine(raster);
        for (int x = 0; x < _vic._lastX; x++) {
          _tick.waitForTick();
        }

        final int pixelPerLine = _vic._lastX - _vic._firstVisibleX + _vic._lastVisibleX;
        Arrays.fill(_screenRender, ptr, ptr += pixelPerLine, _vic._regExteriorColor);
      }

      // bottom vblank
      for (; raster < _vic._linesPerScreen; raster++) {
        _vic.setRasterLine(raster);
        for (int x = 0; x < _vic._lastX; x++) {
          _tick.waitForTick();
        }
      }

      rendered(_screenRender);
    }
  }

  /**
   * Render sprites for the given line.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param raster raster line
   */
  private void renderSprites(byte[] screen, int ptr, int raster) {
    VICBus bus = _vic._bus;

    int spritePointer = _vic._baseCharacterMode + 0x03F8 + 7;
    for (int i = 7; i >= 0; i--, spritePointer--) {
      Sprite sprite = _vic._sprites[i];
      if (sprite.enabled) {
        int y = sprite.y;
        boolean expandY = sprite.expandY;
        if (y <= raster && raster < y + (expandY ? 42 : 21)) {
          int spritePtr = ptr - _vic._firstX_25 + sprite.x;
          int spriteRow = raster - y;
          if (expandY) {
            spriteRow >>= 1;
          }
          int baseSprite = (bus.read(spritePointer) << 6) + spriteRow * 3;
          spritePtr = renderSpriteByte(screen, spritePtr, bus.read(baseSprite++), sprite);
          spritePtr = renderSpriteByte(screen, spritePtr, bus.read(baseSprite++), sprite);
          renderSpriteByte(screen, spritePtr, bus.read(baseSprite), sprite);
        }
      }
    }
  }

  /**
   * Render a single byte of sprite data.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param bitmap sprite data byte to render
   * @param sprite sprite
   * @return next index in screen data
   */
  private int renderSpriteByte(byte[] screen, int ptr, int bitmap, Sprite sprite) {
    byte color = sprite.color;
    boolean expandX = sprite.expandX;

    if (sprite.multicolor) {
      ptr = renderSpriteMultiColorPixel(screen, ptr, bitmap >> 6, color, expandX);
      ptr = renderSpriteMultiColorPixel(screen, ptr, (bitmap & 0x30) >> 4, color, expandX);
      ptr = renderSpriteMultiColorPixel(screen, ptr, (bitmap & 0x0C) >> 2, color, expandX);
      ptr = renderSpriteMultiColorPixel(screen, ptr, bitmap & 0x03, color, expandX);
    } else {
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x80), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x40), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x20), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x10), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x08), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x04), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x02), color, expandX);
      ptr = renderSpriteSingleColorPixel(screen, ptr, (bitmap & 0x01), color, expandX);
    }

    return ptr;
  }

  /**
   * Render one pixel of a single color sprite.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param colorIndex index of color to set: 0: background, otherwise: sprite color
   * @param color sprite color
   * @param expandX expand sprite in x?
   * @return next index in screen data
   */
  private int renderSpriteSingleColorPixel(byte[] screen, int ptr, int colorIndex, byte color, boolean expandX) {
    screen[ptr++] = colorIndex != 0 ? color : screen[ptr];
    if (expandX) {
      screen[ptr++] = colorIndex != 0 ? color : screen[ptr];
    }

    return ptr;
  }

  /**
   * Render one pixel of a multi color sprite.
   *
   * @param screen screen data
   * @param ptr current index in screen data
   * @param colorIndex index of color to set: 0: background, 1: color 1, 2: sprite color, 3: color 2
   * @param color sprite color
   * @param expandX expand sprite in x?
   * @return next index in screen data
   */
  private int renderSpriteMultiColorPixel(byte[] screen, int ptr, int colorIndex, byte color, boolean expandX) {
    screen[ptr++] = getSpriteMultiColor(colorIndex, color, screen[ptr]);
    screen[ptr++] = getSpriteMultiColor(colorIndex, color, screen[ptr]);
    if (expandX) {
      screen[ptr++] = getSpriteMultiColor(colorIndex, color, screen[ptr]);
      screen[ptr++] = getSpriteMultiColor(colorIndex, color, screen[ptr]);
    }

    return ptr;
  }

  /**
   * Get sprite color for color index.
   *
   * @param colorIndex index of color to set
   * @param color sprite color
   * @param background background color
   */
  private byte getSpriteMultiColor(int colorIndex, byte color, byte background) {
    switch (colorIndex) {
      case 0x01:
        return _vic._regSpritesMulticolor0;
      case 0x02:
        return color;
      case 0x03:
        return _vic._regSpritesMulticolor1;
      default:
        return background;
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
  private int renderTextLine(byte[] screen, int ptr, int y) {
    VICBus bus = _vic._bus;
    BusDevice colorRam = _vic._colorRam;

    int screenBaseAddress = _vic._baseCharacterMode;
    int charsetBaseAddress = _vic._baseCharacterSet;

    boolean multiColor = (_vic._regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;

    byte backgroundColor = _vic._regBackgroundColor0;

    // compute character row
    int charRow = y & 0x0007; // optimization for y % 8
    // compute text row
    int screenRow = (y & 0xFFF8) * 5; // optimization for (y / 8) * 40
    // address of row in video ram
    int screenAddress = screenBaseAddress + screenRow;
    // pre-add character row
    int charsetAddress = charsetBaseAddress + charRow;

    // render line
    for (int x = 0; x < 320; screenAddress++, x += 8) {
      byte color = (byte) colorRam.read(screenAddress); // color ram masks address itself
      int character = bus.read(screenAddress);
      int bitmap = bus.read(charsetAddress + (character << 3));

      if (multiColor && ((color & 0x08) != 0)) {
        color &= 0x07;
        byte pixel = getTextMultiColor(bitmap & 0xC0, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
        pixel = getTextMultiColor((bitmap & 0x30) >> 4, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
        pixel = getTextMultiColor((bitmap & 0x0C) >> 2, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
        pixel = getTextMultiColor(bitmap & 0x03, color);
        screen[ptr++] = pixel;
        screen[ptr++] = pixel;
      } else {
        screen[ptr++] = (bitmap & 0x80) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x40) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x20) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x10) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x08) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x04) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x02) == 0 ? backgroundColor : color;
        screen[ptr++] = (bitmap & 0x01) == 0 ? backgroundColor : color;
      }
    }

    return ptr;
  }


  /**
   * Get color of multi color text pixel.
   *
   * @param colorIndex index of color to set
   * @param color foreground color
   */
  private byte getTextMultiColor(int colorIndex, byte color) {
    switch (colorIndex) {
      case 0x00:
        return _vic._regBackgroundColor0;
      case 0x01:
        return _vic._regBackgroundColor1;
      case 0x02:
        return _vic._regBackgroundColor2;
      default:
        return color; // case 0x03
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
  private int renderBitmapLine(byte[] screen, int ptr, int y) {
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
    for (int x = 0; x < 320; screenAddress++, x += 8) {
      int character = bus.read(screenAddress);
      byte foreground = (byte) (character >> 4);
      byte background = (byte) (character & 0x0F);
      int bitmap = bus.read(bitmapAddress + x);
      for (int mask = 0x80; mask != 0; mask >>= 1) {
        screen[ptr++] = (bitmap & mask) == 0 ? background : foreground;
      }
    }

    return ptr;
  }
}
