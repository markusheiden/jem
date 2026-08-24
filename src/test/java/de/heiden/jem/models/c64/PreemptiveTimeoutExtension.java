package de.heiden.jem.models.c64;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class PreemptiveTimeoutExtension implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        runWithTimeout(invocation, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        runWithTimeout(invocation, extensionContext);
    }

    private void runWithTimeout(Invocation<Void> invocation, ExtensionContext context) throws Throwable {
        var annotation = resolveAnnotation(context);
        if (annotation == null) {
            invocation.proceed();
            return;
        }
        var duration = Duration.of(annotation.value(), annotation.unit().toChronoUnit());
        assertTimeoutPreemptively(duration, invocation::proceed);
    }

    private PreemptiveTimeout resolveAnnotation(ExtensionContext context) {
        // Method-level annotation takes precedence over class-level.
        return findAnnotation(context.getRequiredTestMethod(), PreemptiveTimeout.class)
                .or(() -> findAnnotation(context.getRequiredTestClass(), PreemptiveTimeout.class))
                .orElse(null);
    }
}
