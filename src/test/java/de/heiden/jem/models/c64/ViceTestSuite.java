package de.heiden.jem.models.c64;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class ViceTestSuite extends AbstractProgramTest {
  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    Collection<Object[]> parameters = new ArrayList<>();
    parameters.addAll(createParameters("/vice/banking00/banking00.prg", (dir, name) -> true));
    return parameters;
  }

  @Override
  protected void checkResult() throws Exception {
    int event = waitFor(999999999, "- ok", "right", "error");
    waitCycles(1000);

    // Assert that test program exits with "OK" message.
    // Consider everything else (timeout, error messages) as a test failure.
    assertEquals(0, event);
  }
}
