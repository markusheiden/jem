package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link KeyboardBuffer}.
 */
class KeyboardBufferTest {
    /**
     * Memory for the keyboard buffer.
     */
    private final int[] memory = new int[0x10000];

    private final BusDevice bus = new BusDevice() {
        @Override
        public void write(int value, int address) {
            memory[address] = value;
        }

        @Override
        public int read(int address) {
            return memory[address];
        }
    };

    private final KeyboardBuffer keyboardBuffer = new KeyboardBuffer(new SerialClock(), bus);

    /**
     * Mapping to PETSCII: Letters unshifted, line separators to return, unknown characters skipped.
     */
    @Test
    void toPetscii() {
        assertEquals(0x41, KeyboardBuffer.toPetscii('a'));
        assertEquals(0x41, KeyboardBuffer.toPetscii('A'));
        assertEquals(0x5A, KeyboardBuffer.toPetscii('z'));
        assertEquals(0x22, KeyboardBuffer.toPetscii('"'));
        assertEquals(0x0D, KeyboardBuffer.toPetscii('\n'));
        assertEquals(0x5C, KeyboardBuffer.toPetscii('£'));
        assertEquals(-1, KeyboardBuffer.toPetscii('\t'));
        assertEquals(-1, KeyboardBuffer.toPetscii('ä'));
    }

    /**
     * Text longer than the keyboard buffer is typed in chunks, whenever the KERNAL emptied the buffer.
     */
    @Test
    void type() {
        keyboardBuffer.type("load\"bluemax\",8,1\r\n");

        keyboardBuffer.execute(1);
        assertEquals(10, memory[0xC6]);
        assertEquals("LOAD\"BLUEM", text(10));

        // Buffer not yet consumed by the KERNAL: Nothing changes.
        keyboardBuffer.execute(2);
        assertEquals(10, memory[0xC6]);
        assertEquals("LOAD\"BLUEM", text(10));

        // KERNAL consumed the buffer: Rest is typed, "\r\n" as a single return.
        memory[0xC6] = 0;
        keyboardBuffer.execute(3);
        assertEquals(8, memory[0xC6]);
        assertEquals("AX\",8,1\r", text(8));
    }

    /**
     * Text in the keyboard buffer.
     */
    private String text(int length) {
        var result = new StringBuilder();
        for (var i = 0; i < length; i++) {
            result.append((char) memory[0x0277 + i]);
        }
        return result.toString();
    }
}
