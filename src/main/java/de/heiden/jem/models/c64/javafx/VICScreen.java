package de.heiden.jem.models.c64.javafx;

import de.heiden.c64dt.javafx.JC64ScreenComponent;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;
import de.heiden.jem.models.c64.components.vic.IScreenListener;

/**
 * VIC screen display component.
 */
public class VICScreen extends JC64ScreenComponent implements IScreenListener {
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
    _displayUnit.setScreenListener(this);
  }

  @Override
  public void newScreenRendered() {
    updateImageData(_displayUnit.display());
    Thread.yield();
  }
}
