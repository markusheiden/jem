package de.heiden.jem.models.c64;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.serialthreads.agent.TransformingParameterized;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.Dumper;

/**
 * Base class for test defined via a program.
 */
@RunWith(TransformingParameterized.class)
public abstract class AbstractProgramTest extends AbstractTest {
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

  /**
   * Create parameters.
   *
   * @param resource Classpath to directory with test programs.
   * @param filter File name filter to use.
   */
  protected static Collection<Object[]> createParameters(String resource, FilenameFilter filter) throws Exception {
    URL start = AbstractProgramTest.class.getResource(resource);
    File testDir = new File(start.toURI()).getParentFile();
    File[] programs = testDir.listFiles(filter);

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
      assumeTrue(assumptions());

      thread.start();
      waitFor(2000000, "READY.");
      screen.clear();

      screen.setLower(true);
      type("load\"" + programName + "\",8\n");
      // Skip further loads
      c64.rts(0xE16F);
      type("run\n");

      checkResult();

    } catch (AssertionError | Exception e) {
      // For debugging purposes disassemble test program
      System.out.println();
      System.out.println();
      new Disassembler().disassemble(CodeBuffer.fromProgram(new FileInputStream(program)), new PrintWriter(System.out));
      System.out.println();
      new Dumper().dump(CodeBuffer.fromProgram(new FileInputStream(program)), new PrintWriter(System.out));
      System.out.flush();
      throw e;
    }
  }

  /**
   * Assumptions.
   */
  protected boolean assumptions() {
    return true;
  }

  /**
   * Check output of test program.
   */
  protected abstract void checkResult() throws Exception;
}
