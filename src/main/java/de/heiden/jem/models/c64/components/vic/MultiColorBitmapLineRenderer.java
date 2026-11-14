package de.heiden.jem.models.c64.components.vic;

/**
 * Multi color bitmap mode: Colors from background color 0, video ram (upper and lower nibble) and color ram.
 */
class MultiColorBitmapLineRenderer extends AbstractLineRenderer {
    MultiColorBitmapLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int vc, int rc, int character, int color) {
        renderMultiColor(pixels, foreground, x, readBitmap(vc, rc),
                vic.regBackgroundColor0, (byte) (character >> 4), (byte) (character & 0x0F), (byte) color);
    }
}
