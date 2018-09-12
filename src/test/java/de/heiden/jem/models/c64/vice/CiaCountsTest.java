package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * VICE test suite.
 */
class CiaCountsTest extends AbstractTest {
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/CIA/CIA-AcountsB/cia-b-counts-a.prg", ignore = "cia-b-counts-a")
  @ParameterizedTest(name = "{1}")
  void test(Path program, String programName) throws Exception {
    // New CIA and NTSC not implemented yet.
    assertFalse(programName.equals("cmp-b-counts-a-new"), "Not implemented yet");
    assertFalse(programName.equals("cmp-b-counts-a-new_ntsc"), "Not implemented yet");
    assertFalse(programName.equals("cmp-b-counts-a-old_ntsc"), "Not implemented yet");

    testBorderResult(program, 60, false);
  }
}
