package de.heiden.jem.models.c64.vice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.jem.models.c64.AbstractProgramTest;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class Banking00Suite extends AbstractProgramTest {
  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice/banking00/banking00.prg", (dir, name) -> true);
  }

  @Override
  protected void checkResult() throws Exception {
    // Just run for 2 seconds, because test program does never stop.
    int seconds = 2;
    waitFor(seconds * 1000000);

    String console = screen.toString();
    Matcher passed = Pattern.compile("passed        (\\d{8})").matcher(console);
    Matcher ram = Pattern.compile("ram->io fails (\\d{8})").matcher(console);
    Matcher io = Pattern.compile("io->ram fails (\\d{8})").matcher(console);

    assertTrue("Result page is visible.", passed.find() && ram.find() && io.find());
    int passes = Integer.parseInt(passed.group(1));
    assertTrue("Check that more than one pass has been executed, but just executed " + passes + " passes.", passes > 1);
    assertEquals("ram -> io error counter is 0.", "00000000", ram.group(1));
    assertEquals("io -> ram error counter is 0.", "00000000", io.group(1));
  }
}
