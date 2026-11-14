package de.heiden.jem.models.c64.components.vic;

/**
 * Standard text mode.
 */
class TextLineRenderer extends AbstractLineRenderer {
    TextLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] screen, boolean[] foreground, int ptr, int line) {
        int screenAddress = screenAddress(line);
        int charsetAddress = charsetAddress(line);
        for (int column = 0; column < COLUMNS; column++, screenAddress++) {
            int bitmap = vic._bus.read(charsetAddress + (vic._bus.read(screenAddress) << 3));
            ptr = renderHires(screen, foreground, ptr, bitmap, vic._regBackgroundColor0, color(screenAddress));
        }
    }
}
