package de.heiden.jem.models.c64.components.vic;

/**
 * Reads (g-access) and renders the graphics of a line in one graphics mode, one column at a time.
 */
interface LineRenderer {
    /**
     * g-access: Read the graphics data of one column.
     *
     * @param vc
     *         video counter: index of the column in the video matrix
     * @param rc
     *         row counter: line within the text row
     * @param character
     *         byte of the video matrix
     */
    int fetch(int vc, int rc, int character);

    /**
     * Render the 8 pixels of one column.
     *
     * @param pixels
     *         pixels of the line, indexed by X coordinate
     * @param foreground
     *         foreground flags parallel to the pixels, for the priority of sprites
     * @param x
     *         X coordinate of the first pixel
     * @param data
     *         graphics data read by the g-access
     * @param character
     *         byte of the video matrix
     * @param color
     *         color from the color ram
     */
    void render(byte[] pixels, boolean[] foreground, int x, int data, int character, int color);
}
