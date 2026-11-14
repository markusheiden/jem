package de.heiden.jem.models.c64.components.vic;

/**
 * Base for {@link LineRenderer}s with helpers to read graphics data and to render pixels.
 */
abstract class AbstractLineRenderer implements LineRenderer {
    /**
     * Columns per line.
     */
    protected static final int COLUMNS = 40;

    /**
     * VIC.
     */
    protected final VIC vic;

    /**
     * Constructor.
     *
     * @param vic
     *         VIC
     */
    AbstractLineRenderer(VIC vic) {
        this.vic = vic;
    }

    /**
     * Address of the first column of the text row containing the line in video ram.
     */
    protected final int screenAddress(int line) {
        return vic.baseCharacterMode + (line >> 3) * COLUMNS;
    }

    /**
     * Address of the given line of the first character in the character set.
     */
    protected final int charsetAddress(int line) {
        return vic.baseCharacterSet + (line & 0x07);
    }

    /**
     * Address of the given line of the first column in the bitmap.
     */
    protected final int bitmapAddress(int line) {
        return vic.baseBitmapMode + (line >> 3) * COLUMNS * 8 + (line & 0x07);
    }

    /**
     * Color of a column from color ram.
     */
    protected final byte color(int screenAddress) {
        return (byte) (vic.colorRam.read(screenAddress) & 0x0F); // color ram masks address itself
    }

    /**
     * Render 8 pixels with 1 bit per pixel. Set bits are foreground.
     *
     * @return next index in screen data
     */
    protected static int renderHires(byte[] screen, boolean[] foreground, int ptr, int bitmap, byte color0, byte color1) {
        for (var mask = 0x80; mask != 0; mask >>= 1, ptr++) {
            var set = (bitmap & mask) != 0;
            screen[ptr] = set ? color1 : color0;
            foreground[ptr] = set;
        }
        return ptr;
    }

    /**
     * Render 4 double width pixels with 2 bits per pixel. Bit pairs 10 and 11 are foreground.
     *
     * @return next index in screen data
     */
    protected static int renderMultiColor(byte[] screen, boolean[] foreground, int ptr, int bitmap, byte color0, byte color1, byte color2, byte color3) {
        for (var shift = 6; shift >= 0; shift -= 2) {
            var index = (bitmap >> shift) & 0x03;
            var pixel = switch (index) {
                case 0 -> color0;
                case 1 -> color1;
                case 2 -> color2;
                default -> color3;
            };
            var isForeground = index >= 2;
            screen[ptr] = pixel;
            foreground[ptr++] = isForeground;
            screen[ptr] = pixel;
            foreground[ptr++] = isForeground;
        }
        return ptr;
    }
}
