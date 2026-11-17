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
     * Register of the debug cartridge, the test programs write their result to.
     */
    private static final int DEBUG_CARTRIDGE = 0xD7FF;

    // sbx and vsbx take too long on a complete C64, see cpu().
    @ProgramSuiteSource(resource = DIRECTORY + "dadc.prg", ignore = {"sbx", "vsbx"})
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
            // Reset: Start the program like its BASIC line does (SYS start of BASIC + 26).
            write(ram, 0xFFFC, (BASIC + 26) & 0xFF, (BASIC + 26) >> 8);
            // No result yet. The program writes $00 for passed and $FF for failed.
            var noResult = 0x80;
            write(ram, DEBUG_CARTRIDGE, noResult);

            for (var end = 8000 * 1000000L; ram.read(DEBUG_CARTRIDGE) == noResult && clock.getTick() < end; ) {
                clock.run(10000000);
            }

            assertEquals(0x00, ram.read(DEBUG_CARTRIDGE), "Result of " + programName);
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
