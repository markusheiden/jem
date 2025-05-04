package de.heiden.jem.components.ports;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input port implementation.
 */
public final class InputPortImpl implements InputPort {
    /**
     * Output ports and the listeners listening to them.
     */
    private final Map<OutputPort, PortListener> outputPortListeners = new HashMap<>();

    /**
     * Output ports driving this port.
     */
    private OutputPort[] outputPorts = new OutputPort[0];

    /**
     * Input port listeners.
     */
    private List<PortListener> inputListenerLists = new ArrayList<>();

    /**
     * Input port listener array.
     */
    private PortListener[] inputListeners = new PortListener[0];

    /**
     * Port data.
     */
    private int inputData = 0xFF;

    /**
     * Input mask: bit == 1 -> line is input.
     */
    private int inputMask = 0xFF;

    /**
     * Connect the output port to this input port.
     *
     * @param port
     *         output port to connect to.
     * @require port != null
     */
    @Override
    public void connect(@Nonnull OutputPort port) {
        PortListener listener = (value, mask) -> updateInputPort();
        outputPortListeners.put(port, listener);
        outputPorts = outputPortListeners.keySet().toArray(OutputPort[]::new);
        port.addOutputPortListener(listener);
        updateInputPort();
    }

    /**
     * Disconnect the output port from this input port.
     *
     * @param port
     *         output port to disconnect.
     * @require port != null
     */
    @Override
    public void disconnect(@Nonnull OutputPort port) {
        var listener = outputPortListeners.remove(port);
        outputPorts = outputPortListeners.keySet().toArray(OutputPort[]::new);
        port.removeOutputPortListener(listener);
        updateInputPort();
    }

    /**
     * Add the input port listener.
     *
     * @param listener
     *         port listener.
     * @require listener != null
     */
    @Override
    public void addInputPortListener(@Nonnull PortListener listener) {
        inputListenerLists.add(listener);
        inputListeners = inputListenerLists.toArray(PortListener[]::new);
    }

    /**
     * Remove the input port listener.
     *
     * @param listener
     *         port listener.
     * @require listener != null
     */
    @Override
    public void removeInputPortListener(@Nonnull PortListener listener) {
        inputListenerLists.remove(listener);
        inputListeners = inputListenerLists.toArray(PortListener[]::new);
    }

    /**
     * Data of this input port.
     */
    @Override
    public int inputData() {
        return inputData;
    }

    /**
     * Mask of this input port.
     * Set bit means port bit is driven. Cleared bit means port bit is not driven.
     */
    @Override
    public int inputMask() {
        return inputMask;
    }

    //
    // protected interface
    //

    /**
     * Update the current value of thee input port.
     */
    protected void updateInputPort() {
        int inputData = 0xFF;
        int inputMask = 0x00;
        for (var port : outputPorts) {
            inputData &= port.outputData();
            inputMask |= port.outputMask();
        }
        inputData |= 0xFF - inputMask;

        if (inputData != this.inputData || inputMask != this.inputMask) {
            this.inputData = inputData;
            this.inputMask = inputMask;
            notifyInputPortListeners();
        }
    }

    /**
     * Notify all listeners.
     */
    protected void notifyInputPortListeners() {
        final int inputData = this.inputData;
        final int inputMask = this.inputMask;
        for (var listener : inputListeners) {
            listener.portChanged(inputData, inputMask);
        }
    }
}
