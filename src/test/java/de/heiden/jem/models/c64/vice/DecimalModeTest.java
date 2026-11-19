package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the decimal mode of ADC, SBC, ARR, ISC and RRA.
 * Each test checks all operands for one combination of the flags and takes 550 seconds on a C64.
 * They need just the CPU, so they are executed without the other components, which is much faster.
 */
@Tag("cpu")
@Execution(CONCURRENT)
class DecimalModeTest extends AbstractTest {
    /**
     * Start address of the test programs (SYS 2061).
     */
    private static final int START = 2061;

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/decimalmode/adc00.prg")
    @ParameterizedTest(name = "{1}")
    @Timeout(120)
    void test(Path program, String programName) throws Exception {
        // scanner takes 750 seconds on a C64. Without the other components each test takes about 10 seconds.
        testCpuResult(program, START, 800);
    }
}
