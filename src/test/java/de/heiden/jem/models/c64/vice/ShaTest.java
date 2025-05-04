package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * VICE test suite.
 */
class ShaTest extends AbstractTest {
  @ParameterizedTest(name = "{1}")
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/sha/shaabsy1.prg")
  void test(Path program, String programName) throws Exception {
    // These tests seem to run endlessly, let them fail fast for now.
    assertNotEquals("shaabsy3", programName, "Not correctly implemented yet");
    assertNotEquals("shazpy3", programName, "Not correctly implemented yet");

    testBorderResult(program, 60, true);
  }
}
