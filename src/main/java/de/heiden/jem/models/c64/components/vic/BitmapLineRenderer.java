package de.heiden.jem.models.c64.components.vic;

/**
 * Standard bitmap mode: Video ram holds the colors of set (upper nibble) and unset (lower nibble) pixels.
 */
class BitmapLineRenderer extends AbstractLineRenderer {
    BitmapLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public int fetch(int vc, int rc, int character) {
        return readBitmap(vc, rc);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color) {
        renderHires(pixels, foreground, x, data, (byte) (character & 0x0F), (byte) (character >> 4));
    }
}
