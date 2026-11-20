package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
@Tag("vic")
@Execution(CONCURRENT)
class VicTest extends AbstractTest {
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
     * Light pen, clearing of sprite collisions, restarting sprites and VSP (variable screen position).
     * lp-trigger/test2new.prg is skipped, because it needs the new VIC 8565.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "lp-trigger/test2.prg",
            "lplatency/lplatency.prg",
            "spritecollclear/bug2135.prg",
            "spritecollisions/sprite-sprite-hi-hi.prg",
            "spritecollisions/sprite-sprite-hi-mc.prg",
            "spritecollisions/sprite-sprite-mc-hi.prg",
            "spritecollisions/sprite-sprite-mc-mc.prg",
            "spritecollisions/sprite-gfx-hi-hi.prg",
            "spritecollisions/sprite-gfx-hi-mc.prg",
            "spritecollisions/sprite-gfx-mc-hi.prg",
            "spritecollisions/sprite-gfx-mc-mc.prg",
            "spriterestart/spriterestart.prg",
            "spritevssprite/spritevssprite.prg",
            "spritex/demusinterruptus.prg",
            "vspbug/vsp_bug.prg",
            "vsp-tester/vsp-tester.prg",
            "vsp-tester/vsp-tester-ntsc.prg",
    })
    void various(String program) throws Exception {
        videoSystem = videoSystemOf(program);
        testDebugCartridgeResult(path("/vice-emu-testprogs/VICII/" + program), 40);
    }

    /**
     * Light pen in each cycle of a raster line. It needs more than 40 emulated seconds.
     */
    @Test
    @Timeout(180)
    void lightPen() throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/VICII/split-tests/lightpen/lightpen.prg"), 300);
    }

    /**
     * Sprite DMA in the gap between the sprites. Each test takes about 45 seconds.
     */
    @ParameterizedTest
    @Timeout(360)
    @ValueSource(strings = {
            "spritegap/spritegap2.prg",
            "spritegap/spritegap3.prg",
    })
    void spriteGap(String program) throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/VICII/" + program), 1000);
    }
}
