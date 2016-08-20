package de.heiden.jem.models.c64.vice;

import static org.junit.Assert.assertSame;

import java.util.Collection;

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
  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CPU/Acid800/cpu_decimal.prg");
  }

  @Test
  public void test() throws Exception {
    loadAndRun(program);

    Condition passed = greenBorder;
    Condition failed = redBorder;
    Condition result = waitSecondsFor(1, passed, failed);
    printScreen();

    assertSame(passed, result);
  }
}
