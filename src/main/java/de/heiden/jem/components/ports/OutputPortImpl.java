package de.heiden.jem.components.ports;

import java.util.ArrayList;
import java.util.List;

/**
 * Output port abstract implementation.
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
     *         Port listener.
     */
    @Override
    public void addOutputPortListener(PortListener listener) {
        outputListenerList.add(listener);
        outputListeners = outputListenerList.toArray(PortListener[]::new);
    }

    /**
     * Remove the port listener.
     *
     * @param listener
     *         Port listener.
     */
    @Override
    public void removeOutputPortListener(PortListener listener) {
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
     *         New output value.
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
     *         Driven bits.
     */
    @Override
    public void setOutputMask(int mask) {
        outputMask = mask;
        notifyOutputPortListeners();
    }

    //
    // Protected.
    //

    /**
     * Notify all listeners.
     */
    protected void notifyOutputPortListeners() {
        var outputData = this.outputData;
        var outputMask = this.outputMask;
        for (var listener : outputListeners) {
            listener.portChanged(outputData, outputMask);
        }
    }
}
