package de.heiden.jem.models.c64.components.cpu;

/**
 * Easy exit of the execution of the C64 debug CPU.
 */
public class DebuggerExit extends RuntimeException {
    /**
     * Constructor.
     *
     * @param message
     *         Message.
     */
    public DebuggerExit(String message) {
        super(message);
    }
}
