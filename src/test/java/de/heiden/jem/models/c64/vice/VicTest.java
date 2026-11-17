package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the VICs 6569 (PAL), 6567R8 (NTSC) and 6567R56A (old NTSC): Tests with a result for the accesses of the VIC,
 * for the cycles it steals from the CPU, for sprites and for their collisions.
 */
@Execution(CONCURRENT)
class VicTest extends AbstractTest {
    // spritegap/spritegap2.prg and spritegap/spritegap3.prg fail randomly since their VIC init enables all sprites
    // for a frame (VICE r36660): 3 of 7 runs failed, the old versions passed 5 of 5 runs.
    @ParameterizedTest
    @Timeout(180)
    @ValueSource(strings = {
            "colorram/test.prg",
            "phi1timing/phi1timing.prg",
            "phi1timing/phi1timing_ntsc.prg",
            "phi1timing/phi1timing_ntscold.prg",
            "split-tests/bascan/bascan.prg",
            "split-tests/spritescan/spritescan.prg",
            "spritecollisions/sprite-sprite.prg",
            "spritesteal/spritesteal.prg",
            "spritesteal/spritesteal_ntsc.prg",
            "spritesteal/spritesteal_ntscold.prg",
            "spritex/testsuite.prg",
            "spritex/testsuite_ntsc.prg",
            "spritex/testsuite_ntscold.prg",
            "spritey/spritey.prg",
    })
    void vic(String program) throws Exception {
        videoSystem = videoSystemOf(program);
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

    /**
     * Clearing of sprite collisions, restarting sprites and VSP (variable screen position).
     * These tests fail:
     * lp-trigger/test2.prg and lplatency/lplatency.prg: The light pen is not emulated.
     * spritevssprite/spritevssprite.prg: Collisions of sprites at the same position.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "spritecollclear/bug2135.prg",
            "spriterestart/spriterestart.prg",
            "vspbug/vsp_bug.prg",
            "vsp-tester/vsp-tester.prg",
            "vsp-tester/vsp-tester-ntsc.prg",
    })
    void various(String program) throws Exception {
        videoSystem = videoSystemOf(program);
        testDebugCartridgeResult(path("/vice-emu-testprogs/VICII/" + program), 40);
    }
}
