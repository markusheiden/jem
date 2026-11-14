package de.heiden.jem.models.c64.components.vic;

/**
 * Renders the sprites of one line over the graphics.
 */
class SpriteRenderer {
    /**
     * Width of the display window in pixels.
     */
    private static final int WIDTH = 320;

    /**
     * VIC.
     */
    private final VIC vic;

    /**
     * Screen data of the line being rendered.
     */
    private byte[] screen;

    /**
     * Foreground flags of the line being rendered.
     */
    private boolean[] foreground;

    /**
     * Index of the first pixel of the display window of the line being rendered.
     */
    private int window;

    /**
     * Is the sprite being rendered behind the foreground graphics?
     */
    private boolean behind;

    /**
     * Constructor.
     *
     * @param vic
     *         VIC
     */
    SpriteRenderer(VIC vic) {
        this.vic = vic;
    }

    /**
     * Render sprites for the given line. Sprite 0 has the highest priority, so it is rendered last.
     * Sprites are clipped to the display window, because the border covers them.
     * Sprites with background priority are hidden behind foreground graphics.
     *
     * @param screen
     *         screen data
     * @param foreground
     *         foreground flags parallel to screen data
     * @param ptr
     *         index of the first pixel of the display window in screen data
     * @param raster
     *         raster line
     */
    void render(byte[] screen, boolean[] foreground, int ptr, int raster) {
        this.screen = screen;
        this.foreground = foreground;
        this.window = ptr;
        var spritePointer = vic.baseCharacterMode + 0x03F8 + 7;
        for (var i = 7; i >= 0; i--, spritePointer--) {
            var sprite = vic.sprites[i];
            var height = sprite.expandY ? 42 : 21;
            if (sprite.enabled && sprite.y <= raster && raster < sprite.y + height) {
                var spriteRow = sprite.expandY ? (raster - sprite.y) >> 1 : raster - sprite.y;
                var dataAddress = (vic.bus.read(spritePointer) << 6) + spriteRow * 3;
                behind = (vic.regSpritesBackgroundPriority & sprite.bitmask) != 0;
                var spritePtr = ptr - vic.firstX_25 + sprite.x;
                for (var b = 0; b < 3; b++) {
                    spritePtr = renderByte(spritePtr, vic.bus.read(dataAddress + b), sprite);
                }
            }
        }
    }

    /**
     * Render a single byte of sprite data.
     *
     * @return next index in screen data
     */
    private int renderByte(int ptr, int bitmap, Sprite sprite) {
        var width = sprite.expandX ? 2 : 1;
        if (sprite.multicolor) {
            for (var shift = 6; shift >= 0; shift -= 2) {
                ptr = renderPixel(ptr, 2 * width, multiColor((bitmap >> shift) & 0x03, sprite.color));
            }
        } else {
            for (var mask = 0x80; mask != 0; mask >>= 1) {
                ptr = renderPixel(ptr, width, (bitmap & mask) == 0 ? -1 : sprite.color);
            }
        }
        return ptr;
    }

    /**
     * Render one sprite pixel, clipped to the display window.
     * Transparent pixels and pixels behind foreground graphics keep the graphics.
     *
     * @param color
     *         color or -1 for transparent
     * @return next index in screen data
     */
    private int renderPixel(int ptr, int width, int color) {
        if (color >= 0) {
            for (int i = Math.max(ptr, window), to = Math.min(ptr + width, window + WIDTH); i < to; i++) {
                if (!(behind && foreground[i])) {
                    screen[i] = (byte) color;
                }
            }
        }
        return ptr + width;
    }

    /**
     * Color of a multi color sprite pixel or -1 for transparent.
     */
    private int multiColor(int colorIndex, byte color) {
        return switch (colorIndex) {
            case 0 -> -1;
            case 1 -> vic.regSpritesMulticolor0;
            case 2 -> color;
            default -> vic.regSpritesMulticolor1;
        };
    }
}
