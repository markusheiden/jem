package de.heiden.jem.models.c64;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Applies {@code assertTimeoutPreemptively} to every test method.
 * Can be placed on a class (inherited by subclasses) or on individual methods.
 * A method-level annotation takes precedence over a class-level one.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(PreemptiveTimeoutExtension.class)
public @interface PreemptiveTimeout {
    /** Timeout value. */
    long value();

    /** Time unit (default: seconds). */
    TimeUnit unit() default TimeUnit.SECONDS;
}
