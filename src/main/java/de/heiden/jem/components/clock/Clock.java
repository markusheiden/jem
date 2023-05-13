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
  int CPU = 1000;
  int VIC_MEM = 2000;
  int VIC_DISPLAY = 2001;

  /**
   * Dispose clock and all its clocked components.
   */
  @Override
  void close();

  /**
   * Has the clock been started?
   */
  boolean isStarted();

  /**
   * Add clocked component.
   *
   * @param position position to insert component in execute queue
   * @param component clocked component to add
   * @require component != null
   * @require position >= 0
   * @require !isStarted()
   */
  <C extends ClockedComponent> C addClockedComponent(int position, C component);

  /**
   * Run this clock for ever as master clock.
   */
  void run();

  /**
   * Run this clock a given number of ticks as master clock.
   *
   * @param ticks number of ticks to run this clock for
   */
  void run(int ticks);

  /**
   * Add a new event.
   *
   * @param tick tick to execute event at.
   * @param event event to add.
   * @require tick > getTick()ate
   * @require event != null
   */
  void addClockEvent(long tick, ClockEvent event);

  /**
   * Set tick to execute an existing event at.
   * Shortcut for removing and re-adding an event.
   *
   * @param tick tick to execute event at.
   * @param event event.
   * @require tick > getTick()
   * @require event != null
   */
  void updateClockEvent(long tick, ClockEvent event);

  /**
   * Remove event.
   *
   * @param event event to remove
   * @require event != null
   */
  void removeClockEvent(ClockEvent event);

  /**
   * Get current tick.
   * Avoid usage of this method for performance reasons, if current tick is available as parameter.
   *
   * @ensure result >= 0
   */
  long getTick();
}
