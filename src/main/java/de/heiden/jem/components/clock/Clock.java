package de.heiden.jem.components.clock;

/**
 * Clock.
 */
public interface Clock extends AutoCloseable {
    /**
     * Position constants.
     * <p>
     * TODO Should be in subclass...
     */
    int VIC_DISPLAY = 100;
    int CPU = 200;
    int CIA1 = 250;
    int CIA2 = 260;

    /**
     * Dispose the clock and all its clocked components.
     */
    @Override
    void close();

    /**
     * Has the clock been started?
     */
    boolean isStarted();

    /**
     * Add a clocked component.
     *
     * @param position
     *         Position to insert component in execute queue.
     * @param component
     *         Clocked component to add.
     * @require position >= 0
     * @require !isStarted()
     */
    <C extends ClockedComponent> C addClockedComponent(int position, C component);

    /**
     * Run this clock forever as the master clock.
     */
    void run();

    /**
     * Run this clock a given number of ticks as the master clock.
     *
     * @param ticks
     *         Number of ticks to run this clock for.
     */
    void run(int ticks);

    /**
     * Add a new event.
     *
     * @param tick
     *         Tick to execute event at.
     * @param event
     *         Event to add.
     * @require tick > getTick()
     */
    void addClockEvent(long tick, ClockEvent event);

    /**
     * Set tick to execute an existing event at.
     * Shortcut for removing and re-adding an event.
     *
     * @param tick
     *         Tick to execute event at.
     * @param event
     *         Event.
     * @require tick > getTick()
     */
    void updateClockEvent(long tick, ClockEvent event);

    /**
     * Remove event.
     *
     * @param event
     *         Event to remove.
     */
    void removeClockEvent(ClockEvent event);

    /**
     * Get current tick.
     * Avoid usage of this method for performance reasons, if current tick is available as a parameter.
     *
     * @ensure result >= 0
     */
    long getTick();
}
