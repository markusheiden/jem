package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractProgramTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

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
    int seconds = 10;
    int result = waitFor(seconds * 1000000);

    String console = captureScreen();
    System.out.println(console);
    assertEquals(WAIT_PROGRAM_END, result);
  }
}
