package de.heiden.jem.models.c64.components.vic;

import java.util.Arrays;

/**
 * Renders the sprites into a line buffer, indexed by X coordinate, when the raster beam reaches them.
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
     * Bit masks of the sprites with a pixel at the X coordinates, for collisions.
     */
    final byte[] sprites;

    /**
     * Number of pixels of the currently rendered sprite, until its data is read.
     */
    private int remaining;

    /**
     * Color of the last displayed pixel of the currently rendered sprite.
     */
    private int lastColor;

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
        this.sprites = new byte[width];
        Arrays.fill(pixels, NONE);
    }

    /**
     * Start to display the sprites, whose X coordinate is reached by the raster beam.
     * A sprite displays the data, which has been read last, once.
     * A sprite is not displayed while its data is read:
     * From 1 pixel before its 2 access cycles up to 3 pixels into its second access cycle.
     *
     * @param x
     *         X coordinate of the first pixel
     * @param count
     *         number of pixels
     */
    void trigger(int x, int count) {
        var lastX = vic.lastX;
        for (var sprite : vic.sprites) {
            if (sprite.armed && sprite.display && (sprite.x - x + lastX) % lastX < count && sprite.x < lastX) {
                sprite.armed = false;

                var access = vic.x(vic.spriteAccessCycle(sprite.number)) + lastX - 1;
                if ((sprite.x - access + 2 * lastX) % lastX >= 12) {
                    // The display stops, when the data is read: The last pixel is displayed until then.
                    remaining = (access - sprite.x + 2 * lastX) % lastX;
                    render(sprite);
                    if (remaining <= 0 && lastColor != NONE) {
                        remaining = 12;
                        renderPixel(access, 12, lastColor, sprite, (vic.regSpritesBackgroundPriority & sprite.bitmask) != 0);
                    }
                }
            }
        }
    }

    /**
     * The pixels have been output: Remove them.
     *
     * @param x
     *         X coordinate of the first pixel
     * @param count
     *         number of pixels
     */
    void clear(int x, int count) {
        for (var i = 0; i < count; i++) {
            pixels[x] = NONE;
            sprites[x] = 0;
            if (++x == vic.lastX) {
                x = 0;
            }
        }
    }

    /**
     * Render the current line of a sprite.
     */
    private void render(Sprite sprite) {
        var isBehind = (vic.regSpritesBackgroundPriority & sprite.bitmask) != 0;
        var x = sprite.x;
        for (var shift = 16; shift >= 0; shift -= 8) {
            x = renderByte(x, (sprite.data >> shift) & 0xFF, sprite, isBehind);
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
                x = renderPixel(x, 2 * width, multiColor((bitmap >> shift) & 0x03, sprite.color), sprite, isBehind);
            }
        } else {
            for (var mask = 0x80; mask != 0; mask >>= 1) {
                x = renderPixel(x, width, (bitmap & mask) == 0 ? NONE : sprite.color, sprite, isBehind);
            }
        }
        return x;
    }

    /**
     * Render one sprite pixel. Sprites with a lower number have priority.
     * Sprites wrap around at the end of the line.
     *
     * @param color
     *         color or {@link #NONE} for transparent
     * @return X coordinate of the next pixel
     */
    private int renderPixel(int x, int width, int color, Sprite sprite, boolean isBehind) {
        var visible = Math.min(width, remaining);
        remaining -= width;
        if (visible > 0) {
            lastColor = color;
        }
        if (color != NONE) {
            for (int i = x, to = x + visible; i < to; i++) {
                var index = i % vic.lastX;
                if ((sprites[index] & (sprite.bitmask - 1)) == 0) {
                    pixels[index] = (byte) color;
                    behind[index] = isBehind;
                }
                sprites[index] |= (byte) sprite.bitmask;
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
