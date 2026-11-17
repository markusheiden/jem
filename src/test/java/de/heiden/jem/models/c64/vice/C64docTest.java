package de.heiden.jem.models.c64.vice;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.memory.RAM;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite.
 */
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

    /**
     * Address of the code, which starts the test program. The CPU stays at its end, if the test program passed.
     */
    private static final int PASSED = 0xC000;

    /**
     * Address of the BRK handler. The CPU stays at it, if the test program failed.
     */
    private static final int FAILED = 0xC010;

    // sbx and vsbx take too long on a complete C64, see cpu().
    @ProgramSuiteSource(resource = DIRECTORY + "dadc.prg", ignore = {"sbx", "vsbx"})
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        loadAndRun(program);

        // The tests need at max 21 seconds.
        var passed = programEnd;
        var failed = brk;
        var result = waitSecondsFor(30, passed, failed);

        assertSame(passed, result);
    }

    /**
     * sbx and vsbx test all combinations of the operands and flags of SBX.
     * On a C64 sbx needs 5778 seconds and vsbx needs 6856 seconds.
     * They need just the CPU, so they are executed without the other components, which is much faster.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"sbx", "vsbx"})
    @Timeout(600)
    void cpu(String programName) throws Exception {
        var program = Files.readAllBytes(path(DIRECTORY + programName + ".prg"));

        try (var clock = new SerialClock()) {
            var ram = new RAM(0x10000);
            var cpu = clock.addClockedComponent(Clock.CPU, new CPU6510());
            cpu.connect(ram);

            // Load the program. It locates itself by the pointer to the start of BASIC.
            for (var i = 2; i < program.length; i++) {
                ram.write(program[i] & 0xFF, BASIC + i - 2);
            }
            write(ram, 0x002B, BASIC & 0xFF, BASIC >> 8);
            // CHROUT, the program prints its progress: RTS.
            write(ram, 0xFFD2, 0x60);
            // Reset: Start the program like its BASIC line does (SYS start of BASIC + 26). Stay here, when it returns.
            write(ram, PASSED, 0x20, (BASIC + 26) & 0xFF, (BASIC + 26) >> 8, 0x4C, (PASSED + 3) & 0xFF, PASSED >> 8);
            write(ram, 0xFFFC, PASSED & 0xFF, PASSED >> 8);
            // BRK: Stay here.
            write(ram, FAILED, 0x4C, FAILED & 0xFF, FAILED >> 8);
            write(ram, 0xFFFE, FAILED & 0xFF, FAILED >> 8);

            var result = "Timeout";
            for (var end = 8000 * 1000000L; result.equals("Timeout") && clock.getTick() < end; ) {
                clock.run(10000000);
                var pc = cpu.getState().PC;
                if (pc >= PASSED + 3 && pc < PASSED + 6) {
                    result = "Passed";
                } else if (pc >= FAILED && pc < FAILED + 3) {
                    result = "Failed";
                }
            }

            assertEquals("Passed", result);
        }
    }

    /**
     * Write bytes to the RAM.
     */
    private static void write(RAM ram, int addr, int... bytes) {
        for (var i = 0; i < bytes.length; i++) {
            ram.write(bytes[i], addr + i);
        }
    }
}
