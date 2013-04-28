package de.heiden.jem.models.c64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Testsuite 2.15.
 */
@RunWith(Parameterized.class)
public class Testsuite2_15 extends AbstractTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
    " start.prg"
  ));

  /**
   * Test program.
   */
  @Parameter(0)
  public File program;

  /**
   * Test program filename, just for test naming purposes.
   */
  @Parameter(1)
  public String filename;

  @Before
  public void setUp() throws Exception {
    setUp(program);
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    URL start = Testsuite2_15.class.getResource("/testsuite2.15/ start.prg");
    File testDir = new File(start.toURI()).getParentFile();
    File[] programs = testDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return
          !IGNORE.contains(name) &&
            !name.startsWith("cia") &&
            !name.startsWith("cnt") &&
//            name.startsWith("dcm") &&
            name.endsWith(".prg");
      }
    });

    Collection<Object[]> result = new ArrayList<>(programs.length);
    for (File program : programs) {
      result.add(new Object[]{program, program.getName()});
    }

    return result;
  }

  @Test
  public void test() throws Exception {
    thread.start();
    waitFor(2000000, "READY.");
    screen.clear();

    screen.setLower(true);
    type("poke2,1:sys2070\n");
    int event = waitFor(999999999, "- ok", "right", "error");
    waitCycles(1000);

    // Assert that test program exits with "OK" message.
    // Consider everything else (timeout, error messages) as a test failure.
    assertEquals(0, event);
  }
}
