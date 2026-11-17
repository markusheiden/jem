package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static de.heiden.c64dt.bytes.HexUtil.hexByte;
import static de.heiden.c64dt.bytes.HexUtil.hexWord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite.
 */
@Execution(CONCURRENT)
class CiaCountsTest extends AbstractTest {
    /**
     * Directory of the test programs.
     */
    private static final String DIRECTORY = "/vice-emu-testprogs/CIA/CIA-AcountsB/";

    /**
     * Start address of the data created by cia-b-counts-a.
     */
    private static final int DATA = 0x4000;

    /**
     * Length of the data created by cia-b-counts-a.
     */
    private static final int DATA_LENGTH = 0x1200;

    // cia-b-counts-a and cia-b-counts-a_ntsc have no result, see data().
    @ProgramSuiteSource(resource = DIRECTORY + "cia-b-counts-a.prg", ignore = {"cia-b-counts-a", "cia-b-counts-a_ntsc"})
    @ParameterizedTest(name = "{1}")
    void test(Path program, String programName) throws Exception {
        newCias = programName.contains("-new");
        videoSystem = videoSystemOf(programName);
        testBorderResult(program, 60, false);
    }

    /**
     * cia-b-counts-a has no result, it creates the data the other tests compare against.
     * So its data is compared with the dumps of the real CIAs.
     */
    @ParameterizedTest(name = "{0}, new CIAs: {1}")
    @CsvSource({
            "cia-b-counts-a, false, dump-oldcia.bin",
            "cia-b-counts-a, true, dump-newcia.bin",
            "cia-b-counts-a_ntsc, false, dump-oldcia-ntsc.bin",
            "cia-b-counts-a_ntsc, true, dump-newcia-ntsc.bin",
    })
    void data(String programName, boolean newCias, String dump) throws Exception {
        this.newCias = newCias;
        videoSystem = videoSystemOf(programName);
        loadAndRun(path(DIRECTORY + programName + ".prg"));

        // The program runs in the interrupt after it ended.
        assertSame(programEnd, waitSecondsFor(5, programEnd));
        assertSame(debugCartridgePassed, waitSecondsFor(60, debugCartridgePassed));

        // The NTSC dumps contain some bytes after the data.
        var expected = Arrays.copyOf(Files.readAllBytes(path(DIRECTORY + dump)), DATA_LENGTH);
        var actual = new byte[expected.length];
        for (var i = 0; i < actual.length; i++) {
            actual[i] = (byte) c64.getBus().read(DATA + i);
        }
        assertEquals(hexDump(expected), hexDump(actual));
    }

    /**
     * Hex dump with 8 bytes per line: One line for each test of the program.
     */
    private static String hexDump(byte[] bytes) {
        var result = new StringBuilder();
        for (var i = 0; i < bytes.length; i += 8) {
            result.append(hexWord(DATA + i)).append(':');
            for (var j = i; j < i + 8 && j < bytes.length; j++) {
                result.append(' ').append(hexByte(bytes[j] & 0xFF));
            }
            result.append('\n');
        }
        return result.toString();
    }
}
