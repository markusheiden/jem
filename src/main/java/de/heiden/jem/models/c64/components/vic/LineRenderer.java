package de.heiden.jem.models.c64.components.vic;

/**
 * Reads (g-access) and renders the graphics of a line in one graphics mode, one column at a time.
 */
interface LineRenderer {
    /**
     * g-access: Read the graphics data of one column.
     *
     * @param vc
     *         Video counter: index of the column in the video matrix.
     * @param rc
     *         Row counter: line within the text row.
     * @param character
     *         Byte of the video matrix.
     */
    int fetch(int vc, int rc, int character);

    /**
     * Render the 8 pixels of one column.
     *
     * @param pixels
     *         Pixels of the line, indexed by X coordinate.
     * @param foreground
     *         Foreground flags parallel to the pixels, for the priority of sprites.
     * @param x
     *         X coordinate of the first pixel.
     * @param data
     *         Graphics data read by the g-access.
     * @param character
     *         Byte of the video matrix.
     * @param color
     *         Color from the color RAM.
     */
    void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color);
}
