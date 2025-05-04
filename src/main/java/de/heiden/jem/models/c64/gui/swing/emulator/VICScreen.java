package de.heiden.jem.models.c64.gui.swing.emulator;

import de.heiden.c64dt.gui.swing.JC64ScreenComponent;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * VIC screen display component.
 */
public class VICScreen extends JC64ScreenComponent {
    /**
     * The VIC display unit.
     */
    private final AbstractDisplayUnit displayUnit;

    /**
     * Constructor.
     *
     * @param displayUnit
     *         display unit to display.
     * @require displayUnit != null
     */
    public VICScreen(@Nonnull AbstractDisplayUnit displayUnit) {
        super(displayUnit.getOffset(), displayUnit.getWidth(), displayUnit.getLineLength(), displayUnit.getHeight(), 2);

        this.displayUnit = displayUnit;
        this.displayUnit.setScreenListener(() -> {
            repaint();
            Thread.yield();
        });
    }

    /**
     * A repaint has been requested.
     * So the backing image will be updated.
     *
     * @param g
     *         graphics.
     */
    @Override
    protected void doPaintComponent(Graphics g) {
        // Write the screen to the image source.
        updateImageData(displayUnit.display());
    }

    @Override
    protected void drawImage(Graphics g) {
        drawImageResized(g);
    }
}
