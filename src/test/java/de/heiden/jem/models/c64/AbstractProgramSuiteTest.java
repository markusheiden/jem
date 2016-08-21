package de.heiden.jem.models.c64;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.serialthreads.agent.TransformingParameterized;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Base class for test defined via a program suite.
 */
@RunWith(TransformingParameterized.class)
public abstract class AbstractProgramSuiteTest extends AbstractTest {
  /**
   * Program file suffix.
   */
  private static final String PRG_SUFFIX = ".prg";

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
   */
  protected static Collection<Object[]> createParameters(String resource) throws Exception {
    return createParameters(resource, Collections.emptySet());
  }

  /**
   * Create parameters.
   *
   * @param resource Classpath to directory with test programs.
   * @param ignore Program names to ignore.
   */
  protected static Collection<Object[]> createParameters(String resource, Set<String> ignore) throws Exception {
    return createParameters(resource, ignore, programName -> true);
  }

  /**
   * Create parameters.
   *
   * @param resource Classpath to directory with test programs.
   * @param ignore Program names to ignore.
   * @param filter Additional program name filter to use.
   */
  protected static Collection<Object[]> createParameters(String resource, Set<String> ignore, Predicate<String> filter) throws Exception {
    return createParametersFromDirectory(resource, (Path path) -> {
      String filename = path.getFileName().toString();
      if (!filename.endsWith(PRG_SUFFIX)) {
        return false;
      }
      String program = filename.substring(0, filename.length() - PRG_SUFFIX.length());
      return !ignore.contains(program) && filter.test(program);
    });
  }

  /**
   * Create parameters.
   *
   * @param resource Classpath to directory with test programs.
   * @param filter File name filter to use.
   */
  private static Collection<Object[]> createParametersFromDirectory(String resource, Predicate<Path> filter) throws Exception {
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

  /**
   * Load and run program, evaluate border color to determine test result.
   *
   * @param maxSeconds Max seconds to wait. Assumes 1 MHz clock.
   */
  protected void testBorderResult(int maxSeconds, boolean screenCapture) throws Exception {
    loadAndRun(program);

    Condition passed = greenBorder;
    Condition failed1 = lightRedBorder;
    Condition failed2 = redBorder;
    Condition result = waitSecondsFor(maxSeconds, passed, failed1, failed2);
    if (screenCapture) {
      printScreen();
    }

    assertSame("Test failed", passed, result);
  }
}
