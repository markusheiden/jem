package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

/**
 * VICE test suite.
 */
public class Hmc6502Test extends AbstractTest {
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/hmc6502/AllSuiteA.prg")
  @ParameterizedTest(name = "{1}")
  public void test(Path program, String programName) throws Exception {
    testBorderResult(program, 1, true);
  }
}
