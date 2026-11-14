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
        int screenAddress = screenAddress(line);
        int bitmapAddress = bitmapAddress(line);
        for (int column = 0; column < COLUMNS; column++, screenAddress++, bitmapAddress += 8) {
            int colors = vic._bus.read(screenAddress);
            ptr = renderHires(screen, foreground, ptr, vic._bus.read(bitmapAddress), (byte) (colors & 0x0F), (byte) (colors >> 4));
        }
    }
}
