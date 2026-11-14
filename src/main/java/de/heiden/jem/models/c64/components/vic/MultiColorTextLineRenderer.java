package de.heiden.jem.models.c64.components.vic;

/**
 * Multi color text mode: Bit 3 of the color selects multi color for each character.
 */
class MultiColorTextLineRenderer extends AbstractLineRenderer {
    MultiColorTextLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] screen, boolean[] foreground, int ptr, int line) {
        int screenAddress = screenAddress(line);
        int charsetAddress = charsetAddress(line);
        for (int column = 0; column < COLUMNS; column++, screenAddress++) {
            int bitmap = vic._bus.read(charsetAddress + (vic._bus.read(screenAddress) << 3));
            byte color = color(screenAddress);
            ptr = (color & 0x08) == 0 ?
                    renderHires(screen, foreground, ptr, bitmap, vic._regBackgroundColor0, color) :
                    renderMultiColor(screen, foreground, ptr, bitmap,
                            vic._regBackgroundColor0, vic._regBackgroundColor1, vic._regBackgroundColor2, (byte) (color & 0x07));
        }
    }
}
