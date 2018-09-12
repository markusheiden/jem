package de.heiden.jem.models.c64;

import de.heiden.c64dt.disk.FileType;
import de.heiden.c64dt.disk.d64.D64;
import de.heiden.jem.models.c64.components.patch.StringUtil;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.net.URL;
import java.nio.file.Files;
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
 * Base class for test defined via a D64 suite.
 */
public abstract class D64SuiteArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<D64SuiteSource> {
  /**
   * Program file suffix.
   */
  private static final String PRG_SUFFIX = ".prg";

  /**
   * Classpath to directory with test D64 image.
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
  public void accept(D64SuiteSource source) {
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
    return createParametersFromD64(resource, (String filename) -> {
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
   * @param resource Classpath to D64 image.
   * @param filter Program name filter to use.
   */
  private static Stream<Arguments> createParametersFromD64(String resource, Predicate<String> filter) throws Exception {
    URL start = D64SuiteArgumentsProvider.class.getResource(resource);
    assertNotNull(start, "Resource exists.");
    D64 d64 = new D64(35, false);
    d64.load(Files.newInputStream(Paths.get(start.toURI())));
    return d64.getDirectory().getFiles().stream()
      .filter(file -> file.getMode().getType().equals(FileType.PRG))
      .filter(file -> filter.test(StringUtil.read(file.getName())))
      .map(file -> new Object[]{ d64.read(file), StringUtil.read(file.getName()) })
      .map(Arguments::of);
  }
}
