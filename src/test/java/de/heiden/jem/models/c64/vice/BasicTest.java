package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * VICE test suite.
 */
class BasicTest extends AbstractTest {
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/C64/autostart/basic/basictest.prg")
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        testBorderResult(program, 1, false);
        // Check again, because the border is set to green some cycles before the end of the test.
        waitSeconds(1);
        assertSame(greenBorder, waitCyclesFor(1, greenBorder, lightRedBorder));
    }
}
