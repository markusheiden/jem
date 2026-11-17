package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for the CPU: Unstable opcodes, CPU port and JSR.
 */
@Execution(CONCURRENT)
class CpuTest extends AbstractTest {
    /**
     * ANE #imm and LAX #imm with the "magic constant": Without DMA, with bad lines and with sprite DMA.
     * Each test takes 70 seconds on a C64.
     * The ane tests fail: ANE uses the magic constant $EE instead of $EF ($EE just in the cycle, in which RDY is low).
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "lax/lax.prg",
            "lax/lax-none.prg",
            "lax/lax-border.prg",
    })
    @Timeout(300)
    void aneLax(String program) throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/CPU/" + program), 80);
    }

    /**
     * Fading of the unused bits of the CPU port.
     */
    @Test
    void cpuPort() throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/CPU/cpuport/test1.prg"), 10);
    }

    /**
     * JSR reads its second operand byte after pushing the return address.
     */
    @Test
    void jsrSelfModifying() throws Exception {
        testDebugCartridgeResult(path("/vice-emu-testprogs/CPU/jsrselfmod/jsr.prg"), 10);
    }
}
