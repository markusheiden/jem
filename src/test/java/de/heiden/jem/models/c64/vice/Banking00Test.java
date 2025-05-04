package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VICE test suite.
 */
class Banking00Test extends AbstractTest {
    @Test
    void banking00() throws Exception {
        loadAndRun("/vice-emu-testprogs/general/banking00/banking00.prg");

        // Just run for 1 second, because the test program does never stop.
        var result = waitSecondsFor(1);
        var screen = captureScreen();
        System.out.println(screen);

        assertNull(result);
        var passed = Pattern.compile("passed        (\\p{XDigit}{8})").matcher(screen);
        var ram = Pattern.compile("ram->io fails (\\p{XDigit}{8})").matcher(screen);
        var io = Pattern.compile("io->ram fails (\\p{XDigit}{8})").matcher(screen);

        assertTrue(passed.find() && ram.find() && io.find(), "Result page is visible.");
        assertEquals("ram -> io error counter is 0.", "00000000", ram.group(1));
        assertEquals("io -> ram error counter is 0.", "00000000", io.group(1));
        int passes = Integer.parseInt(passed.group(1), 16);
        assertTrue(passes > 1, "Check that more than one pass has been executed, but just executed " + passes + " passes.");
    }
}
