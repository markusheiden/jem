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
class CiaCountsTest extends AbstractTest {
    // NTSC is not implemented yet.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/CIA-AcountsB/cia-b-counts-a.prg", filter = "^(?!.*ntsc).*$",
            ignore = "cia-b-counts-a")
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        newCias = programName.contains("-new");
        testBorderResult(program, 60, false);
    }
}
