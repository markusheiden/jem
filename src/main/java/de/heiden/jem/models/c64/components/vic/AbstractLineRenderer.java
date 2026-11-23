package de.heiden.jem.models.c64.components.vic;

/**
 * Base for {@link LineRenderer}s with helpers to read graphics data and to render pixels.
 */
abstract class AbstractLineRenderer implements LineRenderer {
    /**
     * VIC.
     */
    protected final VIC vic;

    /**
     * Constructor.
     *
     * @param vic
     *         VIC.
     */
    AbstractLineRenderer(VIC vic) {
        this.vic = vic;
    }

    /**
     * Read the given line of a character from the character set (g-access in text modes).
     */
    protected final int readCharacter(int character, int rc) {
        return vic.bus.read(vic.baseCharacterSet + (character << 3) + rc);
    }

    /**
     * Read the given line of a column from the bitmap (g-access in bitmap modes).
     */
    protected final int readBitmap(int vc, int rc) {
        return vic.bus.read(vic.baseBitmapMode + (vc << 3) + rc);
    }

    /**
     * Render 8 pixels with 1 bit per pixel. Set bits are foreground.
     */
    protected static void renderHires(byte[] pixels, boolean[] foreground, int x, int bitmap, byte color0, byte color1) {
        for (var mask = 0x80; mask != 0; mask >>= 1, x++) {
            var set = (bitmap & mask) != 0;
            pixels[x] = set ? color1 : color0;
            foreground[x] = set;
        }
    }

    /**
     * Render 4 double width pixels with 2 bits per pixel. Bit pairs 10 and 11 are foreground.
     */
    protected static void renderMultiColor(byte[] pixels, boolean[] foreground, int x, int bitmap, byte color0, byte color1, byte color2, byte color3) {
        for (var shift = 6; shift >= 0; shift -= 2) {
            var index = (bitmap >> shift) & 0x03;
            var pixel = switch (index) {
                case 0 -> color0;
                case 1 -> color1;
                case 2 -> color2;
                default -> color3;
            };
            var isForeground = index >= 2;
            pixels[x] = pixel;
            foreground[x++] = isForeground;
            pixels[x] = pixel;
            foreground[x++] = isForeground;
        }
    }
}
