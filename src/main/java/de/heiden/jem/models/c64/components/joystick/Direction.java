package de.heiden.jem.models.c64.components.joystick;

/**
 * Joystick directions and fire button with their (low active) port bits.
 */
public enum Direction {
    UP(0x01),
    DOWN(0x02),
    LEFT(0x04),
    RIGHT(0x08),
    FIRE(0x10);

    /**
     * Port bit.
     */
    final int bit;

    Direction(int bit) {
        this.bit = bit;
    }
}
