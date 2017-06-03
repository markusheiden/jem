package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.c64dt.javafx.C64ScreenComponent;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;

/**
 * VIC screen display component.
 */
public class VICScreen extends C64ScreenComponent {
  /**
   * Constructor.
   *
   * @param displayUnit display unit to display
   * @require displayUnit != null
   */
  public VICScreen(final AbstractDisplayUnit displayUnit) {
    super(displayUnit.getOffset(), displayUnit.getWidth(), displayUnit.getLineLength(), displayUnit.getHeight(), 2);

    displayUnit.setScreenListener(() -> updateImageData(displayUnit.display()));
  }
}
