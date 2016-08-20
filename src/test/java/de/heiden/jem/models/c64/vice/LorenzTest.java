package de.heiden.jem.models.c64.vice;

import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.jem.models.c64.AbstractProgramSuiteTest;
import de.heiden.jem.models.c64.Condition;

/**
 * Testsuite 2.15.
 */
@RunWith(TransformingParameterized.class)
public class LorenzTest extends AbstractProgramSuiteTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
    "start.prg", "finish.prg"
  ));

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/general/Lorenz-2.15/src/start.prg", IGNORE, programName ->
//        name.startsWith("flipos") &&
        true);
  }

  @Test
  public void test() throws Exception {
    // ignore some failing tests, because functionality has not been implemented yet
    assumeTrue(!programName.startsWith("cia") && !programName.startsWith("cnt"));

    loadAndRun(program);

    Condition passed = onConsole("- ok");
    Condition failed = onConsole("right", "error");
    Condition event = waitSecondsFor(999, passed, failed);
    waitCycles(1000);

    // Assert that test program exits with "OK" message.
    // Consider everything else (timeout, error messages) as a test failure.
    assertSame(passed, event);
  }
}
