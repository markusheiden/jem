package de.heiden.jem.models.c64.components.vic;

/**
 * Standard bitmap mode: Video ram holds the colors of set (upper nibble) and unset (lower nibble) pixels.
 */
class BitmapLineRenderer extends AbstractLineRenderer {
    BitmapLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] screen, boolean[] foreground, int ptr, int line) {
        var screenAddress = screenAddress(line);
        var bitmapAddress = bitmapAddress(line);
        for (var column = 0; column < COLUMNS; column++, screenAddress++, bitmapAddress += 8) {
            var colors = vic.bus.read(screenAddress);
            ptr = renderHires(screen, foreground, ptr, vic.bus.read(bitmapAddress), (byte) (colors & 0x0F), (byte) (colors >> 4));
        }
    }
}
