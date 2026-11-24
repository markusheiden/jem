package de.heiden.jem.components.clock;

import org.jspecify.annotations.Nullable;

/**
 * Clock event.
 */
public abstract class ClockEvent {
    /**
     * Name of event.
     */
    private final String name;

    /**
     * Tick of clock event.
     * Used for efficient list implementation in clocks.
     */
    long tick;

    /**
     * Previous clock event.
     * Used for efficient list implementation in clocks.
     */
    @Nullable ClockEvent previous;

    /**
     * Next clock event.
     * Used for efficient list implementation in clocks.
     */
    @Nullable ClockEvent next;

    /**
     * Constructor.
     */
    protected ClockEvent(String name) {
        this.name = name;
    }

    /**
     * Execute event.
     */
    public abstract void execute(long tick);

    @Override
    public String toString() {
        return name;
    }
}
