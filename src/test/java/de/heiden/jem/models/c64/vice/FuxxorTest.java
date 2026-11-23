package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite: Emulator detection tests of "emu-fuxxor v2".
 */
@Execution(CONCURRENT)
class FuxxorTest extends AbstractTest {
    // test is the source of test-fuxxored, a BASIC program.
    // test-fuxxored and ef2-inst1 fail: They execute code in the disk drive, which is not emulated.
    @ProgramSuiteSource(
            resource = "/vice-emu-testprogs/general/fuxxortest/test-fuxxored.prg",
            ignore = {
                    "test",
                    "test-fuxxored",
                    "ef2-inst1"
            })
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        testDebugCartridgeResult(program, 20);
    }
}
