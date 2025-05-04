package de.heiden.jem.models.c64;

/**
 * Test condition.
 */
@FunctionalInterface
public interface Condition {
    /**
     * Is the condition met?.
     */
    boolean test() throws Exception;
}
