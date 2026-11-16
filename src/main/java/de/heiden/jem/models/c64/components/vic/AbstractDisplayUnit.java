package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

/**
 * Display unit of vic.
 */
public abstract class AbstractDisplayUnit implements ClockedComponent {
    protected final VIC vic;
    protected Tick tick;

    private final int offset;
    private final int lineLength;
    private final int lines;
    private final int width;
    private final int height;

    private IScreenListener listener;

    /**
     * Screen buffer for rendering.
     */
    protected byte[] screenRender;
    /**
     * Spare screen buffer.
     */
    private byte[] screenSpare;
    /**
     * Screen to display next.
     */
    private byte[] screenToDisplay;
    /**
     * Currently displayed screen.
     */
    private volatile byte[] screenDisplaying;

    /**
     * Hidden constructor.
     *
     * @param vic
     *         vic this display unit belongs to
     */
    AbstractDisplayUnit(VIC vic, int offset, int lineLength, int lines, int width, int height) {
        this.vic = vic;

        this.offset = offset;
        this.lineLength = lineLength;
        this.lines = lines;
        this.width = width;
        this.height = height;

        var size = offset + lineLength * lines;

        screenRender = new byte[size];
        screenSpare = new byte[size];
        screenToDisplay = new byte[size];
        screenDisplaying = screenToDisplay;
    }

    /**
     * The CPU changed the graphics mode or a background color in the current cycle.
     */
    void graphicsChanged() {
        // nothing to do by default
    }

    /**
     * The CPU changed X coordinates of sprites in the current cycle.
     */
    void spriteXChanged() {
        // nothing to do by default
    }

    @Override
    public void setTick(Tick tick) {
        this.tick = tick;
    }

    @Override
    public String getName() {
        return vic.getClass().getSimpleName() + " display";
    }

    /**
     * Screen rendering has been finished. Handles triple buffering.
     *
     * @param screen
     *         new rendered screen
     */
    protected final synchronized void rendered(byte[] screen) {
        if (screenToDisplay == screenDisplaying) {
            screenRender = screenSpare;
            screenSpare = screenToDisplay;
        } else {
            screenRender = screenToDisplay;
            // spare may not be changed, because it is currently displayed
        }
        screenToDisplay = screen;

        if (listener != null) {
            listener.newScreenRendered();
        }
    }

    /**
     * Notify display unit that a new screen will be displayed.
     *
     * @return screen to display next
     */
    public final synchronized byte[] display() {
        screenDisplaying = screenToDisplay;
        return screenDisplaying;
    }

    /**
     * Sets the screen listener of this display unit.
     *
     * @param listener
     *         Listener
     */
    public void setScreenListener(IScreenListener listener) {
        this.listener = listener;
    }

    /**
     * Offset of first pixel in screen.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Total number of pixel/bytes per line.
     */
    public int getLineLength() {
        return lineLength;
    }


    /**
     * Total number of lines.
     */
    public int getLines() {
        return lines;
    }


    /**
     * Number of visible pixel/bytes per line.
     */
    public int getWidth() {
        return width;
    }


    /**
     * Number of visible lines per screen.
     */
    public int getHeight() {
        return height;
    }
}
