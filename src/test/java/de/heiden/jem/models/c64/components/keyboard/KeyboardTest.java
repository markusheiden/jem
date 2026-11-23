package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.components.ports.InputOutputPortImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test.
 */
class KeyboardTest {
    private final InputOutputPortImpl port0 = new InputOutputPortImpl();
    private final InputOutputPortImpl port1 = new InputOutputPortImpl();
    private final Keyboard keyboard = new Keyboard(new SerialClock(), port0, port1);

    /**
     * Test single key presses.
     */
    @Test
    void singleKey() {
        keyboard.press(Key.A);
        keyboard.updateKeys();
        port1.setOutputData(0x00); // Port 1 is input.
        port1.setOutputMask(0x00);
        port0.setOutputData(0x00); // Port 0 drives all bits 0.
        port0.setOutputMask(0xFF);
        assertThat(port1.inputData()).isEqualTo(0xFB); // Row 2 is driven to 0.
        assertThat(port1.inputMask()).isEqualTo(0x04);
        port0.setOutputData(0xFF); // Port 0 drives all bits 1.
        assertThat(port1.inputData()).isEqualTo(0xFF); // Row 2 is driven to 1.
        assertThat(port1.inputMask()).isEqualTo(0x04);
        port0.setOutputData(0x00); // Port 0 is input.
        port0.setOutputMask(0x00);
        port1.setOutputData(0x00); // Port 1 drives all bits 0.
        port1.setOutputMask(0xFF);
        assertThat(port0.inputData()).isEqualTo(0xFD); // Row 1 is driven to 0.
        assertThat(port0.inputMask()).isEqualTo(0x02);
        port1.setOutputData(0xFF); // Port 1 drives all bits 1.
        assertThat(port0.inputData()).isEqualTo(0xFF); // Row 1 is driven to 1.
        assertThat(port0.inputMask()).isEqualTo(0x02);
        keyboard.release(Key.A);

        keyboard.press(Key.B);
        keyboard.updateKeys();
        port1.setOutputData(0x00);
        port1.setOutputMask(0x00);
        port0.setOutputData(0x00);
        port0.setOutputMask(0xFF);
        assertThat(port1.inputData()).isEqualTo(0xEF);
        assertThat(port1.inputMask()).isEqualTo(0x10);
        port0.setOutputData(0xFF);
        assertThat(port1.inputData()).isEqualTo(0xFF);
        assertThat(port1.inputMask()).isEqualTo(0x10);
        port0.setOutputData(0x00);
        port0.setOutputMask(0x00);
        port1.setOutputData(0x00);
        port1.setOutputMask(0xFF);
        assertThat(port0.inputData()).isEqualTo(0xF7);
        assertThat(port0.inputMask()).isEqualTo(0x08);
        port1.setOutputData(0xFF);
        assertThat(port0.inputData()).isEqualTo(0xFF);
        assertThat(port0.inputMask()).isEqualTo(0x08);
        keyboard.release(Key.B);
    }

    /**
     * Keys are not visible before the C64 thread updates the ports.
     */
    @Test
    void deferred() {
        port1.setOutputData(0x00); // Port 1 is input.
        port1.setOutputMask(0x00);
        port0.setOutputData(0x00); // Port 0 drives all bits 0.
        port0.setOutputMask(0xFF);

        keyboard.press(Key.A);
        assertThat(port1.inputData()).isEqualTo(0xFF);
        keyboard.updateKeys();
        assertThat(port1.inputData()).isEqualTo(0xFB); // Row 2 is driven to 0.

        keyboard.release(Key.A);
        assertThat(port1.inputData()).isEqualTo(0xFB);
        keyboard.updateKeys();
        assertThat(port1.inputData()).isEqualTo(0xFF);
    }

    /**
     * Restore is not visible before the C64 thread updates the NMI output.
     */
    @Test
    void restore() {
        assertThat(keyboard.getNMI().outputData() & 0x1).isEqualTo(0x1);
        keyboard.press(Key.RESTORE);
        assertThat(keyboard.getNMI().outputData() & 0x1).isEqualTo(0x1);
        keyboard.updateKeys();
        assertThat(keyboard.getNMI().outputData() & 0x1).isEqualTo(0x0);
        keyboard.release(Key.RESTORE);
        keyboard.updateKeys();
        assertThat(keyboard.getNMI().outputData() & 0x1).isEqualTo(0x1);
    }

    /**
     * Test double key presses one on the column or row.
     */
    @Test
    void doubleKey() {
        // TODO Not implemented yet...
/*
    keyboard.pressed(KeyEvent.VK_2, KeyEvent.KEY_LOCATION_STANDARD);
    keyboard.pressed(KeyEvent.VK_0, KeyEvent.KEY_LOCATION_STANDARD);
    port0.setOutputData(0x00);
    port0.setOutputMask(0xEF);
    port1.setOutputData(0x00);
    port1.setOutputMask(0x00);
    assertThat(port0.inputData()).isEqualTo(0xEF);
    assertThat(port0.inputMask()).isEqualTo(0x10);
    assertThat(port1.inputData()).isEqualTo(0xF7);
    assertThat(port1.inputMask()).isEqualTo(0x08);
    port0.setOutputData(0x00);
    port0.setOutputMask(0x00);
    port1.setOutputData(0x00);
    port1.setOutputMask(0xFF);
    assertThat(port0.inputData()).isEqualTo(0x6F);
    assertThat(port0.inputMask()).isEqualTo(0x90);
    keyboard.released(KeyEvent.VK_0, KeyEvent.KEY_LOCATION_STANDARD);
    keyboard.released(KeyEvent.VK_2, KeyEvent.KEY_LOCATION_STANDARD);
*/
    }
}
