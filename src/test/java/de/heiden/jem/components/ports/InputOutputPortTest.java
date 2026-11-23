package de.heiden.jem.components.ports;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        port.setOutputMask(0xF0); // Upper nibble output.
        port.setOutputData(0xA5);
        assertThat(port.pinData()).isEqualTo(0xAF); // Unconnected inputs are high.

        device.setOutputMask(0x3C);
        device.setOutputData(0xC3); // Pull bits 2..5 low.
        assertThat(port.pinData()).isEqualTo(0x83); // Output bit 5 and input bits 2..3 pulled low.
        assertThat(port.outputData()).isEqualTo(0xA3); // Output bits from the register, without the external device.
    }
}
