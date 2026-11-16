package de.heiden.jem.models.c64.components.vic;

/**
 * Sprites of VIC.
 */
public class Sprite {
    public final int number;
    public final int bitmask;

    public boolean enabled;

    public int x;
    public int y;

    public boolean expandX;
    public boolean expandY;

    public boolean multicolor;
    public byte color;
    public byte multicolor1;
    public byte multicolor2;

    /**
     * Sprite sequencer: Is the sprite DMA on?
     */
    public boolean dma;

    /**
     * Sprite sequencer: Is the sprite displayed?
     */
    public boolean display;

    /**
     * Sprite sequencer: Data counter base, index of the first byte of the current sprite line.
     */
    public int mcBase;

    /**
     * Sprite sequencer: Data counter, index of the next byte to read.
     */
    public int mc;

    /**
     * Sprite sequencer: Expansion flip-flop. Cleared for every second line of y expanded sprites.
     */
    public boolean expansion;

    /**
     * Sprite sequencer: Data of the current sprite line, 24 bits.
     */
    public int data;

    /**
     * Sprite sequencer: Has the data been read, but not been displayed yet?
     */
    public boolean armed;

    /**
     * Constructor.
     *
     * @param number
     *         number of sprite
     */
    public Sprite(int number) {
        this.number = number;
        bitmask = 1 << number;

        enabled = false;

        x = 0;
        y = 0;

        expandX = false;
        expandY = false;

        multicolor = false;
        color = 0;
        multicolor1 = 0;
        multicolor2 = 0;

        dma = false;
        display = false;
        mcBase = 0;
        mc = 0;
        expansion = true;
        data = 0;
    }

    /**
     * Enable / disable sprite.
     */
    public void enable(int bitmap) {
        enabled = (bitmap & bitmask) != 0;
    }

    /**
     * Set LSB of x.
     */
    public void setXLSB(int x) {
        this.x = this.x & 0x100 | x & 0xFF;
    }

    /**
     * Get LSB of x.
     */
    public int getXLSB() {
        return x & 0xFF;
    }

    /**
     * Set MSB of x.
     */
    public void setXMSB(int bitmap) {
        if ((bitmap & bitmask) != 0) {
            x |= 0x100;
        } else {
            x &= 0xFF;
        }
    }

    /**
     * Enable / disable expand x.
     */
    public void setExpandX(int bitmap) {
        expandX = (bitmap & bitmask) != 0;
    }

    /**
     * Enable / disable expand y.
     */
    public void setExpandY(int bitmap) {
        expandY = (bitmap & bitmask) != 0;
    }

    /**
     * Enable / disable multicolor.
     */
    public void setMulticolor(int bitmap) {
        multicolor = (bitmap & bitmask) != 0;
    }
}
