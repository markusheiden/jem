package de.heiden.jem.components.ports;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Output port abstract implementation
 */
public final class OutputPortImpl implements OutputPort {
    /**
     * Output port listeners.
     */
    private final List<PortListener> outputListenerList = new ArrayList<>();

    /**
     * Output port listener array.
     */
    private PortListener[] outputListeners = new PortListener[0];

    /**
     * Port data.
     */
    private volatile int outputData = 0xFF;

    /**
     * Output mask: bit == 1 -> line is output.
     */
    private volatile int outputMask = 0x00;

    /**
     * Add the port listener.
     *
     * @param listener
     *         port listener.
     * @require listener != null
     */
    @Override
    public void addOutputPortListener(@Nonnull PortListener listener) {
        outputListenerList.add(listener);
        outputListeners = outputListenerList.toArray(PortListener[]::new);
    }

    /**
     * Remove the port listener.
     *
     * @param listener
     *         port listener.
     * @require listener != null
     */
    @Override
    public void removeOutputPortListener(@Nonnull PortListener listener) {
        outputListenerList.remove(listener);
        outputListeners = outputListenerList.toArray(PortListener[]::new);
    }

    /**
     * Port output data.
     */
    @Override
    public int outputData() {
        return outputData;
    }

    /**
     * Port output mask.
     * Set bit means port bit is output. Cleared bit means port bit is not driven.
     */
    @Override
    public int outputMask() {
        return outputMask;
    }

    /**
     * Set the port output data.
     *
     * @param data
     *         new output value.
     */
    @Override
    public void setOutputData(int data) {
        outputData = data;
        notifyOutputPortListeners();
    }

    /**
     * Set the port output mask.
     * Set bit means port bit is output. Cleared bit means port bit is not driven.
     *
     * @param mask
     *         driven bits.
     */
    @Override
    public void setOutputMask(int mask) {
        outputMask = mask;
        notifyOutputPortListeners();
    }

    //
    // protected
    //

    /**
     * Notify all listeners.
     */
    protected void notifyOutputPortListeners() {
        final int outputData = this.outputData;
        final int outputMask = this.outputMask;
        for (PortListener listener : outputListeners) {
            listener.portChanged(outputData, outputMask);
        }
    }
}
