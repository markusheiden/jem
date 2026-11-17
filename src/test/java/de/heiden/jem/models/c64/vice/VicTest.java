package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the VIC 6569 (PAL): Tests with a result for the accesses of the VIC,
 * for the cycles it steals from the CPU, for sprites and for their collisions.
 */
@Execution(CONCURRENT)
class VicTest extends AbstractTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "phi1timing/phi1timing.prg",
            "split-tests/bascan/bascan.prg",
            "split-tests/spritescan/spritescan.prg",
            "spritecollisions/sprite-sprite.prg",
            "spritegap/spritegap2.prg",
            "spritegap/spritegap3.prg",
            "spritesteal/spritesteal.prg",
            "spritex/testsuite.prg",
            "spritey/spritey.prg",
    })
    void vic(String program) throws Exception {
        testBorderResult(path("/vice-emu-testprogs/VICII/" + program), 1000, false);
    }

    /**
     * Tests, which run in the interrupt after the program ended.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "spritecollisions/sprite-gfx-collision-cycle.prg",
            "spritecollisions/sprite-sprite-collision-cycle.prg",
    })
    void vicInterrupt(String program) throws Exception {
        loadAndRun(path("/vice-emu-testprogs/VICII/" + program));
        waitSeconds(5);
        assertTrue(debugCartridgePassed.test(), "Test failed");
    }
}
