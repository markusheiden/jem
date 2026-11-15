package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the old CIA 6526 (PAL).
 * Tests for the new CIA 6526A and interactive tests without result are ignored.
 */
@Execution(CONCURRENT)
class CiaTest extends AbstractTest {
    /**
     * Filter: Ignore tests for the new CIA.
     */
    private static final String OLD_CIA = "^(?!.*new).*$";

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/cia-timer/cia-timer-oldcias.prg", filter = OLD_CIA)
    @ParameterizedTest(name = "{1}")
    void ciaTimer(Path program, String programName) throws Exception {
        testBorderResult(program, 220, false);
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/ciavarious/cia1.prg", filter = OLD_CIA)
    @ParameterizedTest(name = "{1}")
    void ciaVarious(Path program, String programName) throws Exception {
        // cia15 loops over all timer values.
        testBorderResult(program, "cia15".equals(programName) ? 180 : 30, false);
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/dd0dtest/dd0dtest.prg")
    @ParameterizedTest(name = "{1}")
    void dd0d(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }

    // irqdelay, irqdelay2 and irqdelay-oneshot are interactive tests without result.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/irqdelay/irqdelay-cia1.prg", filter = OLD_CIA,
            ignore = {"irqdelay", "irqdelay2", "irqdelay-oneshot"})
    @ParameterizedTest(name = "{1}")
    void irqDelay(Path program, String programName) throws Exception {
        testBorderResult(program, 30, false);
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/reload0/reload0a.prg")
    @ParameterizedTest(name = "{1}")
    void reload0(Path program, String programName) throws Exception {
        testBorderResult(program, 40, false);
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/shiftregister/cia-sp-test-oneshot-old.prg", filter = OLD_CIA)
    @ParameterizedTest(name = "{1}")
    void shiftRegister(Path program, String programName) throws Exception {
        testBorderResult(program, 70, false);
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/timerbasics/test.prg", filter = OLD_CIA)
    @ParameterizedTest(name = "{1}")
    void timerBasics(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }

    // stability and stability-ntsc are interactive tests without result.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/tod/alarm.prg", ignore = {"stability", "stability-ntsc"})
    @ParameterizedTest(name = "{1}")
    void tod(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }
}
