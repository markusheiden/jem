package de.heiden.jem.models.c64.components.vic;

import java.util.Arrays;

/**
 * Invalid modes (extended color combined with bitmap or multi color) display black.
 */
class InvalidModeLineRenderer implements LineRenderer {
    @Override
    public void render(byte[] pixels, boolean[] foreground, int x, int vc, int rc, int character, int color) {
        Arrays.fill(pixels, x, x + 8, (byte) 0x00);
        Arrays.fill(foreground, x, x + 8, false);
    }
}
