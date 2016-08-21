package de.heiden.jem.models.c64.vice;

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
public class BasicTest extends AbstractProgramSuiteTest {
  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/C64/autostart/basic/basictest.prg");
  }

  @Test
  public void test() throws Exception {
    testBorderResult(1, false);
    // Check again, because the border is set to green some cycles before the end of the test.
    waitSeconds(1);
    assertSame(greenBorder, waitCyclesFor(1, greenBorder, lightRedBorder));
  }
}
