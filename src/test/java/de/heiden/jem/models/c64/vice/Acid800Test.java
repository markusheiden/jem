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
class Acid800Test extends AbstractTest {
    @ParameterizedTest(name = "{1}")
    // cpu_timing fails since the screen is disabled during the test (VICE r45319):
    // The first DEX/BNE loop after the start of the frame ends in raster line 17 instead of 16.
    // cpu_timing_ntsc is for NTSC only.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/Acid800/cpu_decimal.prg", ignore = {"cpu_timing", "cpu_timing_ntsc"})
    void test(Path program, String programName) throws Exception {
        // cpu_insn takes about 1 second.
        testBorderResult(program, 2, true);
    }
}
