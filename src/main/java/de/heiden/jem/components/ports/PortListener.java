package de.heiden.jem.components.ports;

/**
 * Port listener.
 */
public interface PortListener {
    /**
     * Port changed.
     *
     * @param value
     *         New port value.
     * @param mask
     *         Driven bits.
     */
    void portChanged(int value, int mask);
}
