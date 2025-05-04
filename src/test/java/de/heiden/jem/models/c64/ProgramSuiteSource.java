package de.heiden.jem.models.c64;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Program suite parameters.
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(ProgramSuiteArgumentsProvider.class)
public @interface ProgramSuiteSource {
    /**
     * Classpath to file in directory with test programs.
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
