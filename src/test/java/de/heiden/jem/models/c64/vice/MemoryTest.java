package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the memory: Banking, open I/O area, RAM power-up pattern and the RAM under the CPU port
 * and the data bus of the SID.
 */
@Execution(CONCURRENT)
class MemoryTest extends AbstractTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "C64/bankio/bankio.prg",
            "C64/openio/gauntlet.prg",
            "C64/raminitpattern/cyberloadtest.prg",
            "C64/raminitpattern/darkstarbbstest.prg",
            "C64/raminitpattern/platoontest.prg",
            "C64/raminitpattern/typicaltest.prg",
            "general/ram0001/test1.prg",
            "SID/busvalue/busvalue.prg",
    })
    void test(String program) throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/" + program), 15);
    }
}
