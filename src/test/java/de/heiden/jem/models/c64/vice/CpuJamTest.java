package de.heiden.jem.models.c64.vice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

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
public class CpuJamTest extends AbstractProgramSuiteTest {
  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/cpujam/nojam.prg");
  }

  @Test
  public void test() throws Exception {
    assertFalse(programName.equals("nojam"));

    testBorderResult(60, true);
    // Check again, because the border is set to green some cycles before the end of the test.
    waitCycles(100);
    assertSame(greenBorder, waitCyclesFor(100, greenBorder));
  }
}
