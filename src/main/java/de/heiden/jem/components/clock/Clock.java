package de.heiden.jem.components.clock;

/**
 * Clock.
 */
public interface Clock
{
  /**
   * Position constants.
   *
   * TODO Should be in subclass...
   */
  public static final int CPU = 1000;
  public static final int VIC_MEM = 2000;
  public static final int VIC_DISPLAY = 2001;

  /**
   * Dispose clock and all its clocked components.
   */
  public void dispose();

  /**
   * Has the clock been started?
   */
  public boolean isStarted();

  /**
   * Add clocked component.
   *
   * @param position position to insert component in execute queue
   * @param component clocked component to add
   * @require component != null
   * @require position >= 0
   * @require !isStarted()
   */
  public Tick addClockedComponent(int position, ClockedComponent component);

  /**
   * Run this clock as master clock.
   */
  public void run();

  /**
   * Run this clock as master clock.
   *
   * @param ticks number of ticks to run this clock for
   */
  public void run(int ticks);

  /**
   * Add event.
   *
   * @param tick tick to execute event at.
   * @param event event to add.
   * @require tick > getTick()
   * @require listener != null
   */
  public void addClockEvent(long tick, ClockEvent event);

  /**
   * Remove event.
   *
   * @param event event to remove
   * @require listener != null
   */
  public void removeClockEvent(ClockEvent event);

  /**
   * Get current tick.
   * Avoid usage of this method for performance reasons, if current tick is available as parameter.
   *
   * @ensure result >= 0
   */
  public long getTick();
}
