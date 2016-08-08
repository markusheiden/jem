package de.heiden.jem.models.c64.vice.general.banking00;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.serialthreads.agent.TransformingParameterized;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class Banking00Suite extends AbstractTest {
  @Test
  protected void banking00() throws Exception {
    loadAndRun("/vice/general/banking00/banking00.prg");

    // Just run for 10 seconds, because the test program does never stop.
    int seconds = 10;
    int result = waitFor(seconds * 1000000);
    String screen = captureScreen();
    System.out.println(screen);

    assertEquals(WAIT_NO_MATCH, result);
    Matcher passed = Pattern.compile("passed        (\\p{XDigit}{8})").matcher(screen);
    Matcher ram = Pattern.compile("ram->io fails (\\p{XDigit}{8})").matcher(screen);
    Matcher io = Pattern.compile("io->ram fails (\\p{XDigit}{8})").matcher(screen);

    assertTrue("Result page is visible.", passed.find() && ram.find() && io.find());
    assertEquals("ram -> io error counter is 0.", "00000000", ram.group(1));
    assertEquals("io -> ram error counter is 0.", "00000000", io.group(1));
    int passes = Integer.parseInt(passed.group(1), 16);
    assertTrue("Check that more than one pass has been executed, but just executed " + passes + " passes.", passes > 1);
  }
}
