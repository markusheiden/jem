package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractProgramSuiteTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.serialthreads.agent.TransformingParameterized;

import java.util.Collection;

import static org.junit.Assert.assertFalse;

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
    assertFalse("Not correctly implemented yet", programName.equals("shx-t2"));
    assertFalse("Not correctly implemented yet", programName.equals("shxy3"));
    assertFalse("Not correctly implemented yet", programName.equals("shxy4"));
    assertFalse("Not correctly implemented yet", programName.equals("shyx3"));
    assertFalse("Not correctly implemented yet", programName.equals("shyx4"));

    testBorderResult(60, true);
  }
}
