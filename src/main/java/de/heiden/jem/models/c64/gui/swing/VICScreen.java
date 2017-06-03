package de.heiden.jem.models.c64.gui.swing;

import de.heiden.c64dt.gui.JC64ScreenComponent;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;

import java.awt.*;

/**
 * VIC screen display component.
 */
public class VICScreen extends JC64ScreenComponent {
  /**
   * The VIC display unit.
   */
  private final AbstractDisplayUnit _displayUnit;

  /**
   * Constructor.
   *
   * @param displayUnit display unit to display
   * @require displayUnit != null
   */
  public VICScreen(AbstractDisplayUnit displayUnit) {
    super(displayUnit.getOffset(), displayUnit.getWidth(), displayUnit.getLineLength(), displayUnit.getHeight(), 2);

    _displayUnit = displayUnit;
    _displayUnit.setScreenListener(() -> {
      repaint();
      Thread.yield();
    });
  }

  /**
   * A repaint has been requested.
   * So the backing image will be updated.
   *
   * @param g graphics
   */
  @Override
  protected void doPaintComponent(Graphics g) {
    // write screen to image source
    updateImageData(_displayUnit.display());
  }

  @Override
  protected void drawImage(Graphics g) {
    drawImageResized(g);
  }
}
