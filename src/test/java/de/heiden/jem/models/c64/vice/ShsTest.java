package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * VICE test suite.
 */
class ShsTest extends AbstractTest {
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/shs/shsabsy1.prg")
  @ParameterizedTest(name = "{1}")
  void test(Path program, String programName) throws Exception {
    // These test seem to run endless, let them fail fast for now.
    assertFalse(programName.equals("shsabsy3"), "Not correctly implemented yet");

    testBorderResult(program, 60, true);
  }
}
