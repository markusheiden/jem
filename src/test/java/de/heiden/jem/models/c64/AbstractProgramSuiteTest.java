package de.heiden.jem.models.c64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.serialthreads.agent.TransformingParameterized;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Base class for test defined via a program suite.
 */
@RunWith(TransformingParameterized.class)
public abstract class AbstractProgramSuiteTest extends AbstractTest {
  /**
   * Test program.
   */
  @Parameter(0)
  public Path program;

  /**
   * Test program filename, just for test naming purposes.
   */
  @Parameter(1)
  public String programName;

  /**
   * Create parameters.
   *
   * @param resource Classpath to directory with test programs.
   * @param filter File name filter to use.
   */
  protected static Collection<Object[]> createParameters(String resource, Predicate<Path> filter) throws Exception {
    URL start = AbstractProgramSuiteTest.class.getResource(resource);
    assertNotNull("Resource exists.", start);
    return Files.list(Paths.get(start.toURI()).getParent())
      .filter(filter)
      .map(program -> {
        String programName = program.getFileName().toString();
        programName = programName.substring(0, programName.length() - ".prg".length());
        return new Object[]{ program, programName };
      })
      .collect(Collectors.toList());
  }

  @Test
  public void test() throws Exception {
    try {
      assumeTrue(assumptions());
      loadAndRun(program);
      checkResult();

    } catch (AssertionError | Exception e) {
      dumpProgram(Files.readAllBytes(program));
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
