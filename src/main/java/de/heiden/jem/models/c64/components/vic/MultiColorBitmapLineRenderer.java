package de.heiden.jem.models.c64.components.vic;

/**
 * Multi color bitmap mode: Colors from background color 0, video RAM (upper and lower nibble) and color RAM.
 */
class MultiColorBitmapLineRenderer extends AbstractLineRenderer {
    MultiColorBitmapLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public int fetch(int vc, int rc, int character) {
        return readBitmap(vc, rc);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color) {
        renderMultiColor(pixels, foreground, x, data,
                vic.regBackgroundColor0, (byte) (character >> 4), (byte) (character & 0x0F), (byte) color);
    }
}
