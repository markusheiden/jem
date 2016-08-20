package de.heiden.jem.models.c64.vice;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.jem.models.c64.AbstractProgramSuiteTest;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class KDormannTest extends AbstractProgramSuiteTest {
  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/kdormann/6502_functional_test.prg");
  }

  @Test
  public void test() throws Exception {
    testBorderResult(120, true);
  }
}
