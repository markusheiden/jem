package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * VICE test suite.
 */
class ShxyTest extends AbstractTest {
  @ParameterizedTest(name = "{1}")
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/shxy/shx-t2.prg")
  void test(Path program, String programName) throws Exception {
    // These test seem to run endless, let them fail fast for now.
    assertNotEquals("shx-t2", programName, "Not correctly implemented yet");
    assertNotEquals("shxy3", programName, "Not correctly implemented yet");
    assertNotEquals("shxy4", programName, "Not correctly implemented yet");
    assertNotEquals("shyx3", programName, "Not correctly implemented yet");
    assertNotEquals("shyx4", programName, "Not correctly implemented yet");

    testBorderResult(program, 60, true);
  }
}
