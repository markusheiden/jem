package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

/**
 * VICE test suite.
 */
public class KDormannTest extends AbstractTest {
  @ParameterizedTest(name = "{1}")
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/kdormann/6502_functional_test.prg")
  public void test(Path program, String programName) throws Exception {
    testBorderResult(program, 120, true);
  }
}
