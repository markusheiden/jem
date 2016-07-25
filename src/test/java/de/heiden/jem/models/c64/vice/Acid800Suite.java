package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractProgramTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

import java.util.Collection;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class Acid800Suite extends AbstractProgramTest {
  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice/acid800/cpu-decimal.prg", (dir, name) -> true);
  }

  @Override
  protected void checkResult() throws Exception {
    // Just run for 2 seconds, because test program does never stop.
    int seconds = 10;
    waitFor(seconds * 1000000);

    String console = captureScreen();
    System.out.println(console);
  }
}
