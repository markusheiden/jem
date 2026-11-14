package de.heiden.jem.models.c64.components.vic;

import java.util.Arrays;

/**
 * Invalid modes (extended color combined with bitmap or multi color) display black.
 */
class InvalidModeLineRenderer implements LineRenderer {
    @Override
    public void render(byte[] screen, boolean[] foreground, int ptr, int line) {
        Arrays.fill(screen, ptr, ptr + 320, (byte) 0x00);
        Arrays.fill(foreground, ptr, ptr + 320, false);
    }
}
