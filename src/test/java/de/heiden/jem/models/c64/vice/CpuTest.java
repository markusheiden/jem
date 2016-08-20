package de.heiden.jem.models.c64.vice;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.jem.models.c64.AbstractD64SuiteTest;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class CpuTest extends AbstractD64SuiteTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
  ));

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/cputest.d64", IGNORE);
  }

  @Test
  public void test() throws Exception {
    testBorderResult(60, true);
  }
}
