package de.heiden.jem.models.c64;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.*;
import java.util.regex.Pattern;

/**
 * D64 suite parameters.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(D64SuiteArgumentsProvider.class)
public @interface D64SuiteSource {
  /**
   * Classpath to D64 image.
   */
  String resource();

  /**
   * Program names to ignore.
   */
  String[] ignore() default "NULL";

  /**
   * Additional program name filter {@link Pattern regex} to use.
   */
  String filter() default "NULL";
}
