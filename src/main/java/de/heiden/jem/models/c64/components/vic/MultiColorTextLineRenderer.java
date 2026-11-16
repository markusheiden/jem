package de.heiden.jem.models.c64.components.vic;

/**
 * Multi color text mode: Bit 3 of the color selects multi color for each character.
 */
class MultiColorTextLineRenderer extends AbstractLineRenderer {
    MultiColorTextLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public int fetch(int vc, int rc, int character) {
        return readCharacter(character, rc);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color) {
        if ((color & 0x08) == 0) {
            renderHires(pixels, foreground, x, data, vic.regBackgroundColor0, (byte) color);
        } else {
            renderMultiColor(pixels, foreground, x, data,
                    vic.regBackgroundColor0, vic.regBackgroundColor1, vic.regBackgroundColor2, (byte) (color & 0x07));
        }
    }
}
