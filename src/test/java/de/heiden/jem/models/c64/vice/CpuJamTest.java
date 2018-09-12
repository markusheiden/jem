package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * VICE test suite.
 */
class CpuJamTest extends AbstractTest {
  @ParameterizedTest(name = "{1}")
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/cpujam/nojam.prg")
  void test(Path program, String programName) throws Exception {
    assertFalse(programName.equals("nojam"));

    testBorderResult(program, 60, true);
    // Check again, because the border is set to green some cycles before the end of the test.
    waitSeconds(1);
    assertSame(greenBorder, waitCyclesFor(1, greenBorder));
  }
}
