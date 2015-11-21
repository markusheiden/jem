package de.heiden.jem.components.clock.synchronization;

/**
 * Encapsulates {@link InterruptedException}.
 */
public class ManualAbort extends RuntimeException {
  /**
   * Constructor.
   */
  public ManualAbort() {
    super("Thread has been interrupted.");
  }
}
