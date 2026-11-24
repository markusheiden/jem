package de.heiden.jem.components.ports;

/**
 * Input / output port implementation.
 */
public final class InputOutputPortImpl implements InputOutputPort {
    /**
     * Input port.
     */
    private final InputPort inputPort = new InputPortImpl();

    /**
     * Output port.
     */
    private final OutputPort outputPort = new OutputPortImpl();

    /**
     * Connect the output port to this input port.
     *
     * @param port
     *         Output port to connect to.
     */
    @Override
    public void connect(OutputPort port) {
        inputPort.connect(port);
    }

    /**
     * Disconnect the output port from this input port.
     *
     * @param port
     *         Output port to disconnect.
     */
    @Override
    public void disconnect(OutputPort port) {
        inputPort.disconnect(port);
    }

    /**
     * Add the input port listener.
     *
     * @param listener
     *         Port listener.
     */
    @Override
    public void addInputPortListener(PortListener listener) {
        inputPort.addInputPortListener(listener);
    }

    /**
     * Remove the input port listener.
     *
     * @param listener
     *         Port listener.
     */
    @Override
    public void removeInputPortListener(PortListener listener) {
        inputPort.removeInputPortListener(listener);
    }

    /**
     * Data of this input port.
     */
    @Override
    public int inputData() {
        return inputPort.inputData();
    }

    /**
     * Mask of this input port.
     * Set bit means port bit is driven. Cleared bit means port bit is not driven.
     */
    @Override
    public int inputMask() {
        return inputPort.inputMask();
    }

    /**
     * Add the output port listener.
     *
     * @param listener
     *         Port listener.
     */
    @Override
    public void addOutputPortListener(PortListener listener) {
        outputPort.addOutputPortListener(listener);
    }

    /**
     * Remove the output port listener.
     *
     * @param listener
     *         Port listener.
     */
    @Override
    public void removeOutputPortListener(PortListener listener) {
        outputPort.removeOutputPortListener(listener);
    }

    /**
     * The port output data.
     */
    @Override
    public int outputData() {
        var mask = outputPort.outputMask();
        return outputPort.outputData() & mask | inputPort.inputData() & ~mask;
    }

    /**
     * The port output mask.
     * Set bit means port bit is output. Cleared bit means port bit is not driven.
     */
    @Override
    public int outputMask() {
        return outputPort.outputMask();
    }

    /**
     * Set the port output data.
     */
    @Override
    public void setOutputData(int data) {
        outputPort.setOutputData(data);
    }

    /**
     * Set the port output mask.
     * Set bit means port bit is output. Cleared bit means port bit is not driven.
     */
    @Override
    public void setOutputMask(int mask) {
        outputPort.setOutputMask(mask);
    }
}
