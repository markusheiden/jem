package de.heiden.jem.components.ports;

/**
 * Input Port.
 */
public interface InputPort {
    /**
     * Add the port listener.
     *
     * @param listener
     *         Port listener.
     * @require listener != null
     */
    void addInputPortListener(PortListener listener);

    /**
     * Remove the port listener.
     *
     * @param listener
     *         Port listener.
     * @require listener != null
     */
    void removeInputPortListener(PortListener listener);

    /**
     * Connect the output port to this input port.
     *
     * @param port
     *         Output port to connect to.
     * @require port != null
     */
    void connect(OutputPort port);

    /**
     * Disconnect the output port from this input port.
     *
     * @param port
     *         Output port to disconnect.
     * @require port != null
     */
    void disconnect(OutputPort port);

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
