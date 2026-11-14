package de.heiden.jem.models.c64.components.vic;

/**
 * Renders the graphics of a line in one graphics mode, one column (g-access) at a time.
 */
interface LineRenderer {
    /**
     * Render the 8 pixels of one column.
     *
     * @param pixels
     *         pixels of the line, indexed by X coordinate
     * @param foreground
     *         foreground flags parallel to the pixels, for the priority of sprites
     * @param x
     *         X coordinate of the first pixel
     * @param vc
     *         video counter: index of the column in the video matrix
     * @param rc
     *         row counter: line within the text row
     * @param character
     *         byte of the video matrix
     * @param color
     *         color from the color ram
     */
    void render(byte[] pixels, boolean[] foreground, int x, int vc, int rc, int character, int color);
}
