package de.heiden.jem.models.c64.components.vic;

/**
 * Extended background color text mode:
 * The upper 2 bits of the character select the background color, the lower 6 bits select the character.
 */
class ExtendedColorTextLineRenderer extends AbstractLineRenderer {
    ExtendedColorTextLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public int fetch(int vc, int rc, int character) {
        return readCharacter(character & 0x3F, rc);
    }

    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color) {
        renderHires(pixels, foreground, x, data, backgroundColor(character >> 6), (byte) color);
    }

    /**
     * Background color register 0..3.
     */
    private byte backgroundColor(int index) {
        return switch (index) {
            case 0 -> vic.regBackgroundColor0;
            case 1 -> vic.regBackgroundColor1;
            case 2 -> vic.regBackgroundColor2;
            default -> vic.regBackgroundColor3;
        };
    }
}
