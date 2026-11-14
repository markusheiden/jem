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
        var screenAddress = screenAddress(line);
        var charsetAddress = charsetAddress(line);
        for (var column = 0; column < COLUMNS; column++, screenAddress++) {
            var bitmap = vic.bus.read(charsetAddress + (vic.bus.read(screenAddress) << 3));
            ptr = renderHires(screen, foreground, ptr, bitmap, vic.regBackgroundColor0, color(screenAddress));
        }
    }
}
