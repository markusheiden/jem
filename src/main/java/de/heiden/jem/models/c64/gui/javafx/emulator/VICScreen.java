package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.c64dt.gui.javafx.C64ScreenComponent;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;
import jakarta.annotation.Nonnull;

/**
 * VIC screen display component.
 */
public class VICScreen extends C64ScreenComponent {
    /**
     * Constructor.
     *
     * @param displayUnit
     *         Display unit to display.
     * @require displayUnit != null
     */
    public VICScreen(@Nonnull AbstractDisplayUnit displayUnit) {
        super(displayUnit.getOffset(), displayUnit.getWidth(), displayUnit.getLineLength(), displayUnit.getHeight(), 2);

        displayUnit.setScreenListener(() -> updateImageData(displayUnit.display()));
    }
}
