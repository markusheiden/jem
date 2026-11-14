package de.heiden.jem.models.c64.components.vic;

/**
 * Renders one line of the 320 pixel wide display window in one graphics mode.
 */
interface LineRenderer {
    /**
     * Render one line.
     *
     * @param screen
     *         screen data
     * @param foreground
     *         foreground flags parallel to screen data, for the priority of sprites
     * @param ptr
     *         index of the first pixel of the line in screen data
     * @param line
     *         line in the 200 lines of graphics data, already corrected by vertical fine scrolling
     */
    void render(byte[] screen, boolean[] foreground, int ptr, int line);
}
