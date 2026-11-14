package de.heiden.jem.models.c64.components.vic;

/**
 * Multi color bitmap mode: Colors from background color 0, video ram (upper and lower nibble) and color ram.
 */
class MultiColorBitmapLineRenderer extends AbstractLineRenderer {
    MultiColorBitmapLineRenderer(VIC vic) {
        super(vic);
    }

    @Override
    public void render(byte[] screen, boolean[] foreground, int ptr, int line) {
        var screenAddress = screenAddress(line);
        var bitmapAddress = bitmapAddress(line);
        for (var column = 0; column < COLUMNS; column++, screenAddress++, bitmapAddress += 8) {
            var colors = vic.bus.read(screenAddress);
            ptr = renderMultiColor(screen, foreground, ptr, vic.bus.read(bitmapAddress),
                    vic.regBackgroundColor0, (byte) (colors >> 4), (byte) (colors & 0x0F), color(screenAddress));
        }
    }
}
