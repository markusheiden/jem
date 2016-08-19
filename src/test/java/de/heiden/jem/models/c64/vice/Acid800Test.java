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
public class Acid800Test extends AbstractProgramSuiteTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
  ));

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/Acid800/cpu_decimal.prg", path -> {
      String name = path.getFileName().toString();
      return !IGNORE.contains(name) &&
        name.endsWith(".prg");
    });
  }

  /**
   * Condition for "test passed": Green border.
   */
  private final Condition passed = inMemory(0xd020, 0x05);

  /**
   * Condition for "test failed": Red border.
   */
  private final Condition failed = inMemory(0xd020, 0x0a);

  @Test
  public void test() throws Exception {
    loadAndRun(program);

    Condition result = waitSecondsFor(1, passed, failed);
    System.out.println(captureScreen());

    assertSame(passed, result);
  }
}