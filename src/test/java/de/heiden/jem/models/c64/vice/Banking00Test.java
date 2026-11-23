package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(result).isNull();
        var passed = Pattern.compile("passed        (\\p{XDigit}{8})").matcher(screen);
        var ram = Pattern.compile("ram->io fails (\\p{XDigit}{8})").matcher(screen);
        var io = Pattern.compile("io->ram fails (\\p{XDigit}{8})").matcher(screen);

        assertThat(passed.find() && ram.find() && io.find()).as("Result page is visible.").isTrue();
        assertThat(ram.group(1)).as("ram -> io error counter is 0.").isEqualTo("00000000");
        assertThat(io.group(1)).as("io -> ram error counter is 0.").isEqualTo("00000000");
        var passes = Integer.parseInt(passed.group(1), 16);
        assertThat(passes).as("Check that more than one pass has been executed, but just executed " + passes + " passes.").isGreaterThan(1);
    }
}
