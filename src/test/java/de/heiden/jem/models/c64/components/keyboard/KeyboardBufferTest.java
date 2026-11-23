package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(KeyboardBuffer.toPetscii('a')).isEqualTo(0x41);
        assertThat(KeyboardBuffer.toPetscii('A')).isEqualTo(0x41);
        assertThat(KeyboardBuffer.toPetscii('z')).isEqualTo(0x5A);
        assertThat(KeyboardBuffer.toPetscii('"')).isEqualTo(0x22);
        assertThat(KeyboardBuffer.toPetscii('\n')).isEqualTo(0x0D);
        assertThat(KeyboardBuffer.toPetscii('£')).isEqualTo(0x5C);
        assertThat(KeyboardBuffer.toPetscii('\t')).isEqualTo(-1);
        assertThat(KeyboardBuffer.toPetscii('ä')).isEqualTo(-1);
    }

    /**
     * Text longer than the keyboard buffer is typed in chunks, whenever the KERNAL emptied the buffer.
     */
    @Test
    void type() {
        keyboardBuffer.type("load\"bluemax\",8,1\r\n");

        keyboardBuffer.execute(1);
        assertThat(memory[0xC6]).isEqualTo(10);
        assertThat(text(10)).isEqualTo("LOAD\"BLUEM");

        // Buffer not yet consumed by the KERNAL: Nothing changes.
        keyboardBuffer.execute(2);
        assertThat(memory[0xC6]).isEqualTo(10);
        assertThat(text(10)).isEqualTo("LOAD\"BLUEM");

        // KERNAL consumed the buffer: Rest is typed, "\r\n" as a single return.
        memory[0xC6] = 0;
        keyboardBuffer.execute(3);
        assertThat(memory[0xC6]).isEqualTo(8);
        assertThat(text(8)).isEqualTo("AX\",8,1\r");
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
