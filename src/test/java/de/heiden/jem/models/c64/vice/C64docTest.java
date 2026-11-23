package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite.
 */
@Tag("c64doc")
@Execution(CONCURRENT)
class C64docTest extends AbstractTest {
    /**
     * Directory of the test programs.
     */
    private static final String DIRECTORY = "/vice-emu-testprogs/CPU/64doc/";

    /**
     * Start of BASIC, the test programs are loaded to.
     */
    private static final int BASIC = 0x0801;

    // sbx and vsbx take too long on a complete C64, see cpu().
    @ProgramSuiteSource(
            resource = DIRECTORY + "dadc.prg",
            ignore = {
                    "sbx",
                    "vsbx"
            })
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        // The tests need at max 21 seconds.
        testBorderResult(program, 30, false);
    }

    /**
     * sbx and vsbx test all combinations of the operands and flags of SBX.
     * On a C64 sbx needs 5778 seconds and vsbx needs 6856 seconds.
     * They need just the CPU, so they are executed without the other components, which is much faster.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "sbx", "vsbx" })
    @Timeout(600)
    void cpu(String programName) throws Exception {
        // Start the program like its BASIC line does (SYS start of BASIC + 26).
        testCpuResult(path(DIRECTORY + programName + ".prg"), BASIC + 26, 8000);
    }
}
