package de.heiden.jem.models.c64;

import de.heiden.c64dt.assembler.Disassembler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

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
  public String programName;

  @Before
  public void setUp() throws Exception {
    setUp(program);
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    URL start = Testsuite2_15.class.getResource("/testsuite2.15/ start.prg");
    File testDir = new File(start.toURI()).getParentFile();
    File[] programs = testDir.listFiles((dir, name) ->
      !IGNORE.contains(name) &&
//        name.startsWith("beqr") &&
        name.endsWith(".prg"));

    Collection<Object[]> result = new ArrayList<>(programs.length);
    for (File program : programs) {
      String programName = program.getName();
      programName = programName.substring(0, programName.length() - ".prg".length());
      result.add(new Object[]{program, programName});
    }

    return result;
  }

  @Test
  public void test() throws Exception {
    try {
      // ignore some failing tests, because functionality has not been implemented yet
      assumeTrue(
        !programName.startsWith("cia") &&
          !programName.startsWith("cnt"));

      thread.start();
      waitFor(2000000, "READY.");
      screen.clear();

      screen.setLower(true);
      type("load\"" + programName + "\",8\n");
      // Skip further loads
      rts(0xE16F);
      type("run\n");

      int event = waitFor(999999999, "- ok", "right", "error");
      waitCycles(1000);

      // Assert that test program exits with "OK" message.
      // Consider everything else (timeout, error messages) as a test failure.
      assertEquals(0, event);

    } catch (AssertionError | Exception e) {
      // For debugging purposes disassemble test program
      System.out.println();
      new Disassembler().disassemble(new FileInputStream(program), new PrintWriter(System.out));
      throw e;
    }
  }
}
