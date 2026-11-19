package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite.
 */
@Tag("cpu")
@Execution(CONCURRENT)
class Acid800Test extends AbstractTest {
    @ParameterizedTest(name = "{1}")
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/Acid800/cpu_decimal.prg")
    void test(Path program, String programName) throws Exception {
        videoSystem = videoSystemOf(programName);
        if ("cpu_timing".equals(programName)) {
            // cpu_timing waits for the start of the frame by polling every 8 cycles, so its result depends on its start:
            // Started at the regular position of the raster beam, its first loop ends just in the cycle,
            // in which raster line 17 starts, instead of in raster line 16.
            runDelay = 16;
        }
        // cpu_insn takes about 1 second.
        testBorderResult(program, 2, true);
    }
}
