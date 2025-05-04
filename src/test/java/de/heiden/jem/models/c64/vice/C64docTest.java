package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * VICE test suite.
 */
class C64docTest extends AbstractTest {
    private static final Set<String> LONG_RUNNING = new HashSet<>(asList("sbx", "vsbx"));

    // Skip long-running test (> 10 min.) for now.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/64doc/dadc.prg", ignore = { "sbx", "vsbx" })
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        loadAndRun(program);

        // Most tests need at max 21 seconds.
        // sbx needs 5778 seconds.
        // vsbx needs 6856 seconds.
        int maxSeconds = LONG_RUNNING.contains(programName) ? 7000 : 30;
        var passed = programEnd;
        var failed = brk;
        var result = waitSecondsFor(maxSeconds, passed, failed);

        assertSame(passed, result);
    }
}
