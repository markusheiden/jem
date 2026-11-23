package de.heiden.jem.models.c64;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for pasting text into the C64 via the keyboard buffer of the KERNAL.
 */
class PasteTest extends AbstractTest {
    /**
     * A pasted BASIC command is executed, even if it is longer than the keyboard buffer.
     */
    @Test
    void paste() throws Exception {
        createC64("paste");

        var result = onConsole(" 3628800\n");
        c64.getKeyboardBuffer().type("print 1*2*3*4*5*6*7*8*9*10\n");

        assertThat(waitSecondsFor(5, result)).isSameAs(result);
    }
}
