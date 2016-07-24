package de.heiden.jem.models.c64.vice;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.jem.models.c64.AbstractProgramTest;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class Ram0001Suite extends AbstractProgramTest {
  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice/ram0001/quicktest.prg", (dir, name) -> true);
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
