package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the new CIA 6526A (PAL).
 * Tests for the old CIA 6526 are ignored.
 * CIA-AcountsB is part of {@link CiaCountsTest}.
 */
@Execution(CONCURRENT)
class CiaNewTest extends AbstractTest {
    /**
     * Filter: Ignore tests for the old CIA.
     */
    private static final String NEW_CIA = "^(?!.*old).*$";

    @BeforeEach
    void setUp() {
        newCias = true;
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/cia-timer/cia-timer-newcias.prg", filter = "newcias")
    @ParameterizedTest(name = "{1}")
    void ciaTimer(Path program, String programName) throws Exception {
        testBorderResult(program, 220, false);
    }

    // cia3, cia3a, cia4 and cia8 have variants for the new CIA.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/ciavarious/cia1.prg", ignore = {"cia3", "cia3a", "cia4", "cia8"})
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

    // irqdelay, irqdelay2 and irqdelay-oneshot are for the old CIA.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/irqdelay/irqdelay-cia1.prg", filter = NEW_CIA,
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

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/shiftregister/cia-sp-test-oneshot-new.prg", filter = NEW_CIA)
    @ParameterizedTest(name = "{1}")
    void shiftRegister(Path program, String programName) throws Exception {
        testBorderResult(program, 70, false);
    }

    /**
     * Serial interrupt flag for each cycle of a transfer and after an aborted transfer (VICE bug #1219).
     * The tests for the CIAs with the timestamp 4485 are ignored, they behave differently.
     * The tests for all timer values (_7f) pass too, but are ignored, because each takes 130 seconds.
     */
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/shiftregister/cia-sdr-icr/cia1-sdr-icr-0.prg",
            filter = "^(?!.*(4485|_7f)).*$")
    @ParameterizedTest(name = "{1}")
    void shiftRegisterIcr(Path program, String programName) throws Exception {
        testBorderResult(program, 30, false);
    }

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/timerbasics/test_new.prg", filter = "_new$")
    @ParameterizedTest(name = "{1}")
    void timerBasics(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }

    // stability is tested by todStability(). stability-ntsc is for NTSC only.
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/tod/alarm.prg", ignore = {"stability", "stability-ntsc"})
    @ParameterizedTest(name = "{1}")
    void tod(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }

    /**
     * stability measures the time between the ticks of the TOD for about 30 seconds.
     * It shows its progress in the border, so just the debug cartridge tells the result.
     */
    @Test
    void todStability() throws Exception {
        loadAndRun("/vice-emu-testprogs/CIA/tod/stability.prg");
        assertSame(debugCartridgePassed, waitSecondsFor(50, debugCartridgePassed, debugCartridgeFailed));
    }
}
