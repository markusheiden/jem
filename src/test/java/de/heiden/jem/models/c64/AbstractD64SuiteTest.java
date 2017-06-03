package de.heiden.jem.models.c64;

import de.heiden.c64dt.disk.FileType;
import de.heiden.c64dt.disk.d64.D64;
import de.heiden.jem.models.c64.components.util.StringUtil;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.serialthreads.agent.TransformingParameterized;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

/**
 * Base class for test defined via a program suite.
 */
@RunWith(TransformingParameterized.class)
public abstract class AbstractD64SuiteTest extends AbstractTest {
  /**
   * Test program.
   */
  @Parameter(0)
  public byte[] program;

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
   * @param resource Classpath to D64 image.
   * @param ignore Program names to ignore.
   * @param filter Additional program name filter to use.
   */
  protected static Collection<Object[]> createParameters(String resource, Set<String> ignore, Predicate<String> filter) throws Exception {
    return createParametersFromD64(resource, program -> !ignore.contains(program) && filter.test(program));
  }

  /**
   * Create parameters.
   *
   * @param resource Classpath to D64 image.
   * @param filter Program name filter to use.
   */
  public static Collection<Object[]> createParametersFromD64(String resource, Predicate<String> filter) throws Exception {
    URL start = AbstractD64SuiteTest.class.getResource(resource);
    assertNotNull("Resource exists.", start);
    D64 d64 = new D64(35, false);
    d64.load(Files.newInputStream(Paths.get(start.toURI())));
    return d64.getDirectory().getFiles().stream()
      .filter(file -> file.getMode().getType().equals(FileType.PRG))
      .filter(file -> filter.test(StringUtil.read(file.getName())))
      .map(file -> new Object[]{ d64.read(file), StringUtil.read(file.getName()) })
      .collect(Collectors.toList());
  }

  /**
   * Load and run program, evaluate border color to determine test result.
   *
   * @param maxSeconds Max seconds to wait. Assumes 1 MHz clock.
   * @param screenCapture Capture screen and print it to {@link System#out}?.
   */
  protected void testBorderResult(int maxSeconds, boolean screenCapture) throws Exception {
    loadAndRun(programName, program);
    doTestBorderResult(maxSeconds, screenCapture);
  }
}
