package de.heiden.jem.models.c64.components.vic;

/**
 * Standard bitmap mode: Video ram holds the colors of set (upper nibble) and unset (lower nibble) pixels.
 */
class BitmapLineRenderer extends AbstractLineRenderer {
    BitmapLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int vc, int rc, int character, int color) {
        renderHires(pixels, foreground, x, readBitmap(vc, rc), (byte) (character & 0x0F), (byte) (character >> 4));
    }
}
