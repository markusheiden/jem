package de.heiden.jem.models.c64.components.vic;

/**
 * Listener for screen updates.
 */
public interface IScreenListener {
  /**
   * Notify listener that a new screen has been rendered.
   */
  public void newScreenRendered();
}
