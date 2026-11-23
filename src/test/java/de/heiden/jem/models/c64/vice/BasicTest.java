package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite.
 */
@Execution(CONCURRENT)
class BasicTest extends AbstractTest {
    // batest just prints the device number in $BA, it has no result.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/C64/autostart/basic/basictest.prg", ignore = "batest")
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        testBorderResult(program, 1, false);
        // Check again, because the border is set to green some cycles before the end of the test.
        waitSeconds(1);
        assertThat(waitCyclesFor(1, greenBorder, lightRedBorder)).isSameAs(greenBorder);
    }
}
