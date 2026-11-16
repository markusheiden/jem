package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the VIC 6569 (PAL): Tests with a result for the cycles the VIC steals from the CPU.
 */
@Execution(CONCURRENT)
class VicTest extends AbstractTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "split-tests/bascan/bascan.prg",
            "spritesteal/spritesteal.prg",
    })
    void vic(String program) throws Exception {
        testBorderResult(path("/vice-emu-testprogs/VICII/" + program), 120, false);
    }
}
