package de.heiden.jem.models.c64.components.vic;

import java.util.Arrays;

/**
 * Renders the sprites of one line into a line buffer, indexed by X coordinate.
 * The display unit combines it with the graphics and the border.
 */
class SpriteRenderer {
    /**
     * No sprite pixel.
     */
    static final byte NONE = -1;

    /**
     * VIC.
     */
    private final VIC vic;

    /**
     * Sprite pixel colors of the line, indexed by X coordinate, {@link #NONE} for no sprite pixel.
     */
    final byte[] pixels;

    /**
     * Is the sprite pixel behind the foreground graphics?
     */
    final boolean[] behind;

    /**
     * Constructor.
     *
     * @param vic
     *         VIC
     * @param width
     *         number of X coordinates
     */
    SpriteRenderer(VIC vic, int width) {
        this.vic = vic;
        this.pixels = new byte[width];
        this.behind = new boolean[width];
    }

    /**
     * Start of a line: Render the displayed sprites with the data the sprite sequencer read for the line.
     */
    void startLine() {
        for (var sprite : vic.sprites) {
            // X coordinates beyond the end of the line are never reached.
            sprite.lineX = sprite.display && sprite.x < vic.lastX ? sprite.x : -1;
            sprite.lineData = sprite.data;
        }
        render();
    }

    /**
     * The CPU changed X coordinates of sprites: Sprites not yet reached by the raster beam are displayed
     * at their new X coordinate, if the raster beam still reaches it in this line.
     *
     * @param position
     *         position of the change in pixels from the start of the line
     * @param firstX
     *         X coordinate of the first pixel of the line
     */
    void xChanged(int position, int firstX) {
        var changed = false;
        for (var sprite : vic.sprites) {
            var lineX = sprite.lineX;
            if (sprite.x != lineX && sprite.display && (lineX < 0 || position(lineX, firstX) >= position)) {
                sprite.lineX = sprite.x < vic.lastX && position(sprite.x, firstX) >= position ? sprite.x : -1;
                changed |= sprite.lineX != lineX;
            }
        }
        if (changed) {
            render();
        }
    }

    /**
     * Position of an X coordinate in pixels from the start of the line.
     */
    private int position(int x, int firstX) {
        return (x - firstX + vic.lastX) % vic.lastX;
    }

    /**
     * Render the sprites of the line. Sprite 0 has the highest priority, so it is rendered last.
     */
    private void render() {
        Arrays.fill(pixels, NONE);

        for (var i = 7; i >= 0; i--) {
            var sprite = vic.sprites[i];
            if (sprite.lineX >= 0) {
                var isBehind = (vic.regSpritesBackgroundPriority & sprite.bitmask) != 0;
                var x = sprite.lineX;
                for (var shift = 16; shift >= 0; shift -= 8) {
                    x = renderByte(x, (sprite.lineData >> shift) & 0xFF, sprite, isBehind);
                }
            }
        }
    }

    /**
     * Render a single byte of sprite data.
     *
     * @return X coordinate of the next pixel
     */
    private int renderByte(int x, int bitmap, Sprite sprite, boolean isBehind) {
        var width = sprite.expandX ? 2 : 1;
        if (sprite.multicolor) {
            for (var shift = 6; shift >= 0; shift -= 2) {
                x = renderPixel(x, 2 * width, multiColor((bitmap >> shift) & 0x03, sprite.color), isBehind);
            }
        } else {
            for (var mask = 0x80; mask != 0; mask >>= 1) {
                x = renderPixel(x, width, (bitmap & mask) == 0 ? NONE : sprite.color, isBehind);
            }
        }
        return x;
    }

    /**
     * Render one sprite pixel. Transparent pixels keep the pixels of sprites with lower priority.
     * Sprites wrap around at the end of the line.
     *
     * @param color
     *         color or {@link #NONE} for transparent
     * @return X coordinate of the next pixel
     */
    private int renderPixel(int x, int width, int color, boolean isBehind) {
        if (color != NONE) {
            for (int i = x, to = x + width; i < to; i++) {
                var index = i % vic.lastX;
                pixels[index] = (byte) color;
                behind[index] = isBehind;
            }
        }
        return x + width;
    }

    /**
     * Color of a multi color sprite pixel or {@link #NONE} for transparent.
     */
    private int multiColor(int colorIndex, byte color) {
        return switch (colorIndex) {
            case 0 -> NONE;
            case 1 -> vic.regSpritesMulticolor0;
            case 2 -> color;
            default -> vic.regSpritesMulticolor1;
        };
    }
}
