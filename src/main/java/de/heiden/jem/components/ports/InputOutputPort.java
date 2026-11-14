package de.heiden.jem.components.ports;

/**
 * Input / output port.
 */
public interface InputOutputPort extends InputPort, OutputPort {
    /**
     * Pin levels: The output for output bits, the input for input bits.
     * External devices can pull output bits low too (wired AND).
     */
    default int pinData() {
        return outputData() & inputData();
    }
}
