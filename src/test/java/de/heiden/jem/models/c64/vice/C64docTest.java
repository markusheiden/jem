package de.heiden.jem.models.c64.vice;

import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.jem.models.c64.AbstractProgramSuiteTest;
import de.heiden.jem.models.c64.Condition;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class C64docTest extends AbstractProgramSuiteTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
    // Skip long running test (> 10 min.).
    "sbx.prg", "vsbx.prg"
  ));

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/64doc/dadc.prg", path -> {
      String name = path.getFileName().toString();
      return !IGNORE.contains(name) &&
        name.endsWith(".prg");
    });
  }

  /**
   * Condition for "test passed".
   */
  private final Condition passed = programEnd;

  /**
   * Condition for "test failed".
   */
  private final Condition failed = brk;

  @Test
  public void test() throws Exception {
    loadAndRun(program);

    // Most need at max 21 seconds.
    // sbx needs 5778 seconds.
    // vsbx needs 6856 seconds.
    int seconds = programName.equals("sbx") || programName.equals("vsbx")? 7000 : 30;
    Condition result = waitSecondsFor(seconds, passed, failed);

    assertSame(passed, result);
  }
}
