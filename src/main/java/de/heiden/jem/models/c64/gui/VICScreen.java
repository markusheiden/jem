package de.heiden.jem.models.c64.gui;

import de.heiden.c64dt.gui.JC64ScreenComponent;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;
import de.heiden.jem.models.c64.components.vic.IScreenListener;
import org.apache.log4j.Logger;

import java.awt.Graphics;

/**
 * VIC screen display component.
 */
public class VICScreen extends JC64ScreenComponent implements IScreenListener
{
  private final AbstractDisplayUnit _displayUnit;

  /**
   * Constructor.
   *
   * @param displayUnit display unit to display
   * @require displayUnit != null
   */
  public VICScreen(AbstractDisplayUnit displayUnit)
  {
    super(displayUnit.getOffset(), displayUnit.getWidth(), displayUnit.getLineLength(), displayUnit.getHeight(), 2);

    _displayUnit = displayUnit;
    _displayUnit.setScreenListener(this);
  }

  @Override
  public void newScreenRendered()
  {
    repaint();
    Thread.yield();
  }

  /**
   * A repaint has been requested.
   * So the backing image will be updated.
   *
   * @param g graphics
   */
  @Override
  public void doPaintComponent(Graphics g)
  {
    // write screen to image source
    updateImageData(_displayUnit.display());
  }

  @Override
  protected void drawImage(Graphics g)
  {
    drawImageResized(g);
  }
}
