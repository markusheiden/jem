package de.heiden.jem.models.c64.vice;

import static org.junit.Assert.assertFalse;

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
public class ShxyTest extends AbstractProgramSuiteTest {
  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/shxy/shx-t2.prg");
  }

  @Test
  public void test() throws Exception {
    // These test seem to run endless, let them fail fast for now.
    assertFalse(programName.equals("shx-t2"));
    assertFalse(programName.equals("shxy3"));
    assertFalse(programName.equals("shxy4"));
    assertFalse(programName.equals("shyx3"));
    assertFalse(programName.equals("shyx4"));

    testBorderResult(60, true);
  }
}
