package de.heiden.jem.models.c64;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Testsuite 2.15.
 */
@RunWith(TransformingParameterized.class)
public class Testsuite2_15 extends AbstractProgramSuiteTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
    " start.prg"
  ));

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/testsuite2.15/ start.prg", path -> {
      String name = path.getFileName().toString();
      return !IGNORE.contains(name) &&
//        name.startsWith("flipos") &&
        name.endsWith(".prg");
    });
  }

  @Override
  protected boolean assumptions() {
    // ignore some failing tests, because functionality has not been implemented yet
    return !programName.startsWith("cia") && !programName.startsWith("cnt");
  }

  @Override
  protected void checkResult() throws Exception {
    int event = waitSecondsFor(999, "- ok", "right", "error");
    waitCycles(1000);

    // Assert that test program exits with "OK" message.
    // Consider everything else (timeout, error messages) as a test failure.
    assertEquals(0, event);
  }
}
