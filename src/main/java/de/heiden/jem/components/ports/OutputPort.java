package de.heiden.jem.components.ports;

import jakarta.annotation.Nonnull;

/**
 * Output port.
 */
public interface OutputPort {
    /**
     * Add the port listener.
     *
     * @param listener
     *         port listener
     * @require listener != null
     */
    void addOutputPortListener(@Nonnull PortListener listener);

    /**
     * Remove the port listener.
     *
     * @param listener
     *         port listener
     * @require listener != null
     */
    void removeOutputPortListener(@Nonnull PortListener listener);

    /**
     * The port output data.
     */
    int outputData();

    /**
     * The port output mask.
     * Set bit means port bit is output. Cleared bit means port bit is not driven.
     */
    int outputMask();

    /**
     * Set the port output data.
     *
     * @param data
     *         new output value.
     */
    void setOutputData(int data);

    /**
     * Set the port output mask.
     * Set bit means port bit is output. Cleared bit means port bit is not driven.
     *
     * @param mask
     *         driven bits.
     */
    void setOutputMask(int mask);
}
