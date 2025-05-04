package de.heiden.jem.components.ports;

import jakarta.annotation.Nonnull;

/**
 * Input Port.
 */
public interface InputPort {
    /**
     * Add the port listener.
     *
     * @param listener
     *         port listener
     * @require listener != null
     */
    void addInputPortListener(@Nonnull PortListener listener);

    /**
     * Remove the port listener.
     *
     * @param listener
     *         port listener
     * @require listener != null
     */
    void removeInputPortListener(@Nonnull PortListener listener);

    /**
     * Connect the output port to this input port.
     *
     * @param port
     *         output port to connect to.
     * @require port != null
     */
    void connect(@Nonnull OutputPort port);

    /**
     * Disconnect the output port from this input port.
     *
     * @param port
     *         output port to disconnect.
     * @require port != null
     */
    void disconnect(@Nonnull OutputPort port);

    /**
     * Data of this input port.
     */
    int inputData();

    /**
     * Mask of this input port.
     * Set bit means port bit is driven. Cleared bit means port bit is not driven.
     */
    int inputMask();
}
