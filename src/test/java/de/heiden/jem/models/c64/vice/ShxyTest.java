package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite.
 */
@Execution(CONCURRENT)
class ShxyTest extends AbstractTest {
    @ParameterizedTest(name = "{1}")
    // shx-t2 is an interactive test without result.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/shxy/shxy1.prg", ignore = "shx-t2")
    void test(Path program, String programName) throws Exception {
        // shx-test loops over all values of X and all pages, which takes about 150 seconds.
        testBorderResult(program, "shx-test".equals(programName) ? 200 : 60, true);
    }
}
