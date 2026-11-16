package de.heiden.jem.models.c64.components.vic;

/**
 * Standard text mode.
 */
class TextLineRenderer extends AbstractLineRenderer {
    TextLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public int fetch(int vc, int rc, int character) {
        return readCharacter(character, rc);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color) {
        renderHires(pixels, foreground, x, data, vic.regBackgroundColor0, (byte) color);
    }
}
