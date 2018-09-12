package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * VICE test suite.
 */
public class ShxyTest extends AbstractTest {
  @ParameterizedTest(name = "{1}")
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CPU/shxy/shx-t2.prg")
  public void test(Path program, String programName) throws Exception {
    // These test seem to run endless, let them fail fast for now.
    assertFalse(programName.equals("shx-t2"), "Not correctly implemented yet");
    assertFalse(programName.equals("shxy3"), "Not correctly implemented yet");
    assertFalse(programName.equals("shxy4"), "Not correctly implemented yet");
    assertFalse(programName.equals("shyx3"), "Not correctly implemented yet");
    assertFalse(programName.equals("shyx4"), "Not correctly implemented yet");

    testBorderResult(program, 60, true);
  }
}
