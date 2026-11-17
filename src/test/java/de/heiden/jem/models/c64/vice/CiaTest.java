package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the old CIA 6526 (PAL).
 * Tests for the new CIA 6526A are ignored.
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

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/irqdelay/irqdelay-cia1.prg", filter = OLD_CIA)
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

    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/timerbasics/test.prg", filter = OLD_CIA)
    @ParameterizedTest(name = "{1}")
    void timerBasics(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }

    // stability and stability-ntsc are tested by todStability().
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/tod/alarm.prg", ignore = {"stability", "stability-ntsc"})
    @ParameterizedTest(name = "{1}")
    void tod(Path program, String programName) throws Exception {
        testBorderResult(program, 120, false);
    }

    /**
     * stability measures the time between the ticks of the TOD for about 30 seconds.
     * It shows its progress in the border, so just the debug cartridge tells the result.
     */
    @ParameterizedTest
    @ValueSource(strings = {"stability", "stability-ntsc"})
    void todStability(String programName) throws Exception {
        videoSystem = videoSystemOf(programName);
        testDebugCartridgeResult(path("/vice-emu-testprogs/CIA/tod/" + programName + ".prg"), 50);
    }

    /**
     * Mirrors of the registers, timer output on PB6/PB7
     * and acknowledging the interrupts by the dummy read of STA abs,X to $DD0D.
     */
    @ParameterizedTest
    @ValueSource(strings = {"mirrors/ciamirrors.prg", "pb6pb7/main.prg", "dd0drw/dd0drw.prg"})
    void various(String program) throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/CIA/" + program), 15);
    }
}
