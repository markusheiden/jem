package de.heiden.jem.models.c64;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Base class for test defined via a program suite.
 */
public class ProgramSuiteArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<ProgramSuiteSource> {
  /**
   * Program file suffix.
   */
  private static final String PRG_SUFFIX = ".prg";

  /**
   * Classpath to directory with test programs.
   */
  private String resource;

  /**
   * Program names to ignore.
   */
  private Set<String> ignore;

  /**
   * Additional program name filter {@link Pattern regex} to use.
   */
  private Predicate<String> filter;

  @Override
  public void accept(ProgramSuiteSource source) {
    resource = source.resource();
    if (resource == null) {
      throw new NullPointerException("Resource has to be specified.");
    }

    ignore = !source.ignore().equals("NULL")?
      new HashSet<>(asList(source.ignore())) : emptySet();
    filter = !source.filter().equals("NULL")?
      Pattern.compile(source.filter()).asPredicate() : programName -> true;
  }

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
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
  private static Stream<Arguments> createParametersFromDirectory(String resource, Predicate<Path> filter) throws Exception {
    URL start = ProgramSuiteArgumentsProvider.class.getResource(resource);
    assertNotNull(start, "Resource exists.");
    return Files.list(Paths.get(start.toURI()).getParent())
      .filter(filter)
      .map(program -> {
        String programName = program.getFileName().toString();
        programName = programName.substring(0, programName.length() - ".prg".length());
        return Arguments.of(program, programName);
      });
  }
}
