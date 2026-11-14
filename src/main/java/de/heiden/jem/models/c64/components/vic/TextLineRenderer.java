package de.heiden.jem.models.c64.components.vic;

/**
 * Standard text mode.
 */
class TextLineRenderer extends AbstractLineRenderer {
    TextLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int vc, int rc, int character, int color) {
        renderHires(pixels, foreground, x, readCharacter(character, rc), vic.regBackgroundColor0, (byte) color);
    }
}
