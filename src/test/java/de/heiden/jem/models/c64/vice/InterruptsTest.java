package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * VICE test suite for interrupts (PAL).
 * Interactive tests without result are ignored.
 */
@Execution(CONCURRENT)
class InterruptsTest extends AbstractTest {
    /**
     * Interrupts of the CIAs, while their interrupt control register is read as part of an opcode.
     * The border is green from the start, so just the debug cartridge tells the result.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "cia-int/cia-int-irq.prg",
            "cia-int/cia-int-irq-new.prg",
            "cia-int/cia-int-nmi.prg",
            "cia-int/cia-int-nmi-new.prg",
    })
    void ciaInt(String program) throws Exception {
        newCias = program.contains("new");
        testDebugCartridgeResult(path("/vice-emu-testprogs/interrupts/" + program), 30);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "irq-ackn-bug/cia1.prg",
            "irq-ackn-bug/cia1new.prg",
            "irq-ackn-bug/cia2.prg",
            "irq-ackn-bug/cia2new.prg",
            "irq-ackn-bug/irq-ack-vicii.prg",
            "irqdummy/irqdummy.prg",
            "irqnmi/irqnmi-new.prg",
            "irqnmi/irqnmi-old.prg",
    })
    void interrupts(String program) throws Exception {
        newCias = program.contains("new");
        testBorderResult(path("/vice-emu-testprogs/interrupts/" + program), 30, false);
    }

    /**
     * Interrupts while the VIC steals cycles from the CPU.
     * Each test compares the timing of 16384 interrupts with a real C64. That takes 330 seconds.
     */
    @ProgramSuiteSource(resource = "/vice-emu-testprogs/interrupts/irqdma/test1.prg", filter = "test")
    @ParameterizedTest(name = "{1}")
    @Timeout(300)
    void irqDma(Path program, String programName) throws Exception {
        testBorderResult(program, 400, false);
    }
}
