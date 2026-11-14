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
    public void render(byte[] screen, boolean[] foreground, int ptr, int line) {
        var screenAddress = screenAddress(line);
        var charsetAddress = charsetAddress(line);
        for (var column = 0; column < COLUMNS; column++, screenAddress++) {
            var character = vic.bus.read(screenAddress);
            var bitmap = vic.bus.read(charsetAddress + ((character & 0x3F) << 3));
            ptr = renderHires(screen, foreground, ptr, bitmap, backgroundColor(character >> 6), color(screenAddress));
        }
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
