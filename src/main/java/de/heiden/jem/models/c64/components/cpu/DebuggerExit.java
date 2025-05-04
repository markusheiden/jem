package de.heiden.jem.models.c64.components.cpu;

/**
 * Easy exit of the execution of the C64 debug cpu.
 */
public class DebuggerExit extends RuntimeException {
    /**
     * Constructor.
     *
     * @param message
     *         message
     */
    public DebuggerExit(String message) {
        super(message);
    }
}
