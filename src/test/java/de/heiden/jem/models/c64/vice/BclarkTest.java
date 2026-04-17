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
class BclarkTest extends AbstractTest {
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/bclark/decimalmode.prg")
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        testBorderResult(program, 60, true);
    }
}
