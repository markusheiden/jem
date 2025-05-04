package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.ports.InputOutputPortImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test.
 */
class KeyboardTest {
    private final InputOutputPortImpl port0 = new InputOutputPortImpl();
    private final InputOutputPortImpl port1 = new InputOutputPortImpl();
    private final Keyboard keyboard = new Keyboard(port0, port1);

    /**
     * Test single key presses.
     */
    @Test
    void singleKey() {
        keyboard.press(Key.A);
        port1.setOutputData(0x00); // port 1 is input
        port1.setOutputMask(0x00);
        port0.setOutputData(0x00); // port 0 drives all bits 0
        port0.setOutputMask(0xFF);
        assertEquals(0xFB, port1.inputData()); // row 2 is driven to 0
        assertEquals(0x04, port1.inputMask());
        port0.setOutputData(0xFF); // port 0 drives all bits 1
        assertEquals(0xFF, port1.inputData()); // row 2 is driven to 1
        assertEquals(0x04, port1.inputMask());
        port0.setOutputData(0x00); // port 0 is input
        port0.setOutputMask(0x00);
        port1.setOutputData(0x00); // port 1 drives all bits 0
        port1.setOutputMask(0xFF);
        assertEquals(0xFD, port0.inputData()); // row 1 is driven to 0
        assertEquals(0x02, port0.inputMask());
        port1.setOutputData(0xFF); // port 1 drives all bits 1
        assertEquals(0xFF, port0.inputData()); // row 1 is driven to 1
        assertEquals(0x02, port0.inputMask());
        keyboard.release(Key.A);

        keyboard.press(Key.B);
        port1.setOutputData(0x00);
        port1.setOutputMask(0x00);
        port0.setOutputData(0x00);
        port0.setOutputMask(0xFF);
        assertEquals(0xEF, port1.inputData());
        assertEquals(0x10, port1.inputMask());
        port0.setOutputData(0xFF);
        assertEquals(0xFF, port1.inputData());
        assertEquals(0x10, port1.inputMask());
        port0.setOutputData(0x00);
        port0.setOutputMask(0x00);
        port1.setOutputData(0x00);
        port1.setOutputMask(0xFF);
        assertEquals(0xF7, port0.inputData());
        assertEquals(0x08, port0.inputMask());
        port1.setOutputData(0xFF);
        assertEquals(0xFF, port0.inputData());
        assertEquals(0x08, port0.inputMask());
        keyboard.release(Key.B);
    }

    /**
     * Test double key presses one on the column or row.
     */
    @Test
    void doubleKey() {
        // TODO not implemented yet...
/*
    keyboard.pressed(KeyEvent.VK_2, KeyEvent.KEY_LOCATION_STANDARD);
    keyboard.pressed(KeyEvent.VK_0, KeyEvent.KEY_LOCATION_STANDARD);
    port0.setOutputData(0x00);
    port0.setOutputMask(0xEF);
    port1.setOutputData(0x00);
    port1.setOutputMask(0x00);
    assertEquals(0xEF, port0.inputData());
    assertEquals(0x10, port0.inputMask());
    assertEquals(0xF7, port1.inputData());
    assertEquals(0x08, port1.inputMask());
    port0.setOutputData(0x00);
    port0.setOutputMask(0x00);
    port1.setOutputData(0x00);
    port1.setOutputMask(0xFF);
    assertEquals(0x6F, port0.inputData());
    assertEquals(0x90, port0.inputMask());
    keyboard.released(KeyEvent.VK_0, KeyEvent.KEY_LOCATION_STANDARD);
    keyboard.released(KeyEvent.VK_2, KeyEvent.KEY_LOCATION_STANDARD);
*/
    }
}
