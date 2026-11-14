package de.heiden.jem.components.ports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link InputOutputPort}.
 */
class InputOutputPortTest {
    private final InputOutputPortImpl port = new InputOutputPortImpl();
    private final OutputPortImpl device = new OutputPortImpl();

    /**
     * Pin levels: Output bits from the output, input bits from the input, external devices pull bits low.
     */
    @Test
    void pinData() {
        port.connect(device);
        port.setOutputMask(0xF0); // upper nibble output
        port.setOutputData(0xA5);
        assertEquals(0xAF, port.pinData()); // unconnected inputs are high

        device.setOutputMask(0x3C);
        device.setOutputData(0xC3); // pull bits 2..5 low
        assertEquals(0x83, port.pinData()); // output bit 5 and input bits 2..3 pulled low
        assertEquals(0xA3, port.outputData()); // output bits from the register, without the external device
    }
}
