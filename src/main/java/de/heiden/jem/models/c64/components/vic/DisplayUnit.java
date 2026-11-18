package de.heiden.jem.models.c64.components.vic;

import org.serialthreads.Interruptible;

import java.util.Arrays;

/**
 * Display unit of the VIC: Cycle exact sequencer, which outputs 8 pixels per cycle.
 * <p>
 * Implements the counters VC, VCBASE, RC and VMLI, display and idle state, bad lines,
 * the video matrix accesses (c-accesses) in cycles 15-54 of bad lines,
 * the graphics accesses (g-accesses) in cycles 16-55, in idle state to $3FFF, and the border flip-flops,
 * as described in "The MOS 6567/6569 video controller (VIC-II) and its application in the Commodore 64" by Christian Bauer.
 * <p>
 * Simplifications: The graphics of a column are rendered with the registers of its g-access cycle,
 * changes of the graphics mode and the background colors by the CPU are applied to the pixels following the change.
 * A sprite is rendered with the registers of the cycle, in which the raster beam reaches its X coordinate.
 */
public class DisplayUnit extends AbstractDisplayUnit {
    /**
     * Number of pixels into a cycle, after which a write of the CPU to the X coordinate of a sprite takes effect.
     */
    private static final int SPRITE_X_DELAY = 4;

    /**
     * Delay in pixels, after which a collision is visible in the collision registers.
     */
    private static final int COLLISION_DELAY = 12;

    /**
     * Number of pixels, whose collisions with other sprites are lost,
     * because the CPU read the sprite-sprite collision register.
     */
    private int spriteCollisionsRead = 0;

    /**
     * No border pixel.
     */
    private static final byte NO_BORDER = -1;

    /**
     * Number of X coordinates in a line buffer.
     */
    private static final int X_COORDINATES = 512;

    /**
     * Number of columns of the display window.
     */
    private static final int COLUMNS = 40;

    /**
     * Renderers for the graphics modes, indexed by the mode bits ECM, BMM, MCM.
     */
    private final LineRenderer[] lineRenderers;

    /**
     * Renderer for sprites.
     */
    private final SpriteRenderer spriteRenderer;

    /**
     * Graphics pixels of the current line, indexed by X coordinate.
     */
    private final byte[] graphics = new byte[X_COORDINATES];

    /**
     * Foreground flags of the graphics pixels, for the priority of sprites.
     */
    private final boolean[] foreground = new boolean[X_COORDINATES];

    /**
     * Video matrix line: Characters read by the c-accesses of the last bad line.
     */
    private final int[] characters = new int[COLUMNS];

    /**
     * Video matrix line: Colors read by the c-accesses of the last bad line.
     */
    private final int[] colors = new int[COLUMNS];

    /**
     * Columns of the last three g-accesses, the pixels of which may still be affected by register changes:
     * X coordinate of the first pixel (-1 for none), graphics data, video matrix data and color.
     */
    private final int[] columnX = {-1, -1, -1};
    private final int[] columnData = new int[3];
    private final int[] columnCharacter = new int[3];
    private final int[] columnColor = new int[3];

    /**
     * Buffers to render a column again after a register change.
     */
    private final byte[] changedGraphics = new byte[X_COORDINATES];
    private final boolean[] changedForeground = new boolean[X_COORDINATES];

    /**
     * Video counter.
     */
    private int vc = 0;

    /**
     * Video counter base.
     */
    private int vcBase = 0;

    /**
     * Row counter.
     */
    private int rc = 0;

    /**
     * Video matrix line index.
     */
    private int vmli = 0;

    /**
     * Display state (true) or idle state (false).
     */
    private boolean displayState = false;

    /**
     * Main border flip-flop.
     */
    private boolean mainBorder = true;

    /**
     * Vertical border flip-flop.
     */
    private boolean verticalBorder = true;

    /**
     * Border pixels of the line, indexed by X coordinate, {@link #NO_BORDER} for no border.
     */
    private final byte[] border = new byte[X_COORDINATES];

    /**
     * Raster line of the last cycle. Its border is evaluated in the next cycle.
     */
    private int lastRaster = 0;

    /**
     * Last cycle, 0 if none. Its border is evaluated in the next cycle.
     */
    private int lastCycle = 0;

    /**
     * Raster line of the cycle whose pixels are output next.
     */
    private int outputRaster = 0;

    /**
     * Cycle whose pixels are output next, 0 if none.
     */
    private int outputCycle = 0;

    /**
     * Hidden constructor.
     *
     * @param vic
     *         vic this display unit belongs to
     */
    DisplayUnit(VIC vic) {
        super(vic,
                0,
                vic.lastX - vic.firstVisibleX + vic.lastVisibleX, visibleLines(vic),
                vic.lastX - vic.firstVisibleX + vic.lastVisibleX, visibleLines(vic));

        var invalidMode = new InvalidModeLineRenderer();
        lineRenderers = new LineRenderer[]{
                new TextLineRenderer(vic), new MultiColorTextLineRenderer(vic),
                new BitmapLineRenderer(vic), new MultiColorBitmapLineRenderer(vic),
                new ExtendedColorTextLineRenderer(vic), invalidMode,
                invalidMode, invalidMode};
        spriteRenderer = new SpriteRenderer(vic, X_COORDINATES);
    }

    /**
     * Number of raster lines outside of the vertical blanking. It starts at the end of the frame of the NTSC VICs.
     */
    private static int visibleLines(VIC vic) {
        return (vic.firstVBlank - vic.lastVBlank + vic.linesPerScreen) % vic.linesPerScreen;
    }

    @Override
    @Interruptible
    public final void run() {
        vic.reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            for (var raster = 0; raster < vic.linesPerScreen; raster++) {
                for (var cycle = 1; cycle <= vic.cyclesPerLine; cycle++) {
                    // The raster line changes in cycle 1, line 0 is reached one cycle later.
                    if (cycle == (raster == 0 ? 2 : 1)) {
                        vic.setRasterLine(raster);
                    }
                    vic.lightPenCycle(raster, cycle);
                    cycle(raster, cycle);
                    tick.waitForTick();
                }
            }

            rendered(screenRender);
        }
    }

    /**
     * Start of a raster line.
     */
    private void startLine(int raster) {
        if (raster == 0) {
            vcBase = 0;
        }

        // Pixels outside of the columns, e.g. visible with opened borders, show the background.
        Arrays.fill(graphics, background());
        Arrays.fill(foreground, false);
        Arrays.fill(columnX, -1);
    }

    /**
     * Graphics mode: Index of the mode bits ECM, BMM, MCM.
     */
    private int mode() {
        return (vic.regControl1 & (VIC.CONTROL1_EXT_COLOR | VIC.CONTROL1_BITMAP)) >> 4 | (vic.regControl2 & VIC.CONTROL2_MULTI_COLOR) >> 4;
    }

    /**
     * Color of pixels without graphics data: The background, black in invalid modes.
     */
    private byte background() {
        return mode() > 4 ? 0 : vic.regBackgroundColor0;
    }

    /**
     * Execute one cycle of a raster line.
     *
     * @param raster
     *         raster line
     * @param cycle
     *         cycle, starting with 1
     */
    private void cycle(int raster, int cycle) {
        // The pixels of a cycle are output two cycles later, because register writes of the CPU affect pixels
        // of the cycle before. The border of the last cycle is evaluated after the CPU accessed the registers in it.
        if (outputCycle != 0) {
            output(outputRaster, outputCycle);
        }
        if (lastCycle != 0) {
            updateBorder(lastRaster, lastCycle);
        }
        outputRaster = lastRaster;
        outputCycle = lastCycle;
        if (cycle == 1) {
            // The line buffers are about to change.
            if (outputCycle != 0) {
                output(outputRaster, outputCycle);
                outputCycle = 0;
            }
            startLine(raster);
        }

        // Sprites reached by the raster beam since the last write of the CPU, before and after the sprite sequencer.
        vic.completeSpriteData();
        var beamX = vic.x(cycle);
        spriteRenderer.trigger((vic.x(cycle - 1) + SPRITE_X_DELAY) % vic.lastX, 8 - SPRITE_X_DELAY);

        vic.spriteCycle(raster, cycle);
        vic.updateBA(cycle);
        spriteRenderer.trigger(beamX, SPRITE_X_DELAY);
        var badLine = vic.isBadLine();
        // A bad line condition, which arises in idle state, switches to display state after the g-access of the cycle.
        var badLineInIdleState = badLine && !displayState;
        if (badLine && !(badLineInIdleState && cycle >= 16 && cycle <= 55)) {
            displayState = true;
        }

        if (cycle == 14) {
            vc = vcBase;
            vmli = 0;
            if (badLine) {
                rc = 0;
            }
        }

        if (badLine && cycle >= 15 && cycle <= 54) {
            readVideoMatrix(cycle - 15);
        }

        if (cycle >= 16 && cycle <= 55) {
            readGraphics(cycle - 16, badLineInIdleState);
            if (badLine) {
                displayState = true;
            }
        } else if (cycle == 56) {
            // Right of the last column.
            var x = vic.lastX_25 + (vic.regControl2 & VIC.CONTROL2_SCROLL_X);
            Arrays.fill(graphics, x, vic.firstX, background());
            Arrays.fill(foreground, x, vic.firstX, false);
        }

        if (cycle == 58) {
            if (rc == 7) {
                vcBase = vc;
                displayState = badLine;
            }
            if (displayState) {
                rc = (rc + 1) & 0x07;
            }
        }

        detectCollisions(cycle);

        lastRaster = raster;
        lastCycle = cycle;
    }

    /**
     * Detect the collisions of sprites with other sprites and with the foreground of the graphics
     * for the 8 pixels, which become visible in the collision registers in a cycle.
     * The border does not prevent collisions.
     */
    private void detectCollisions(int cycle) {
        var sprites = 0;
        var background = 0;
        var x = (vic.x(cycle) - COLLISION_DELAY + vic.lastX) % vic.lastX;
        for (var i = 0; i < 8; i++) {
            var mask = spriteRenderer.sprites[x] & 0xFF;
            if (spriteCollisionsRead > 0) {
                spriteCollisionsRead--;
            } else if ((mask & (mask - 1)) != 0) {
                sprites |= mask;
            }
            if (mask != 0) {
                if (foreground[x]) {
                    background |= mask;
                }
            }
            if (++x == vic.lastX) {
                x = 0;
            }
        }

        if ((sprites | background) != 0) {
            vic.spriteCollisions(sprites, background);
        }

        // All pixels up to the cycle before the last cycle have been output.
        spriteRenderer.clear(vic.x(cycle - 2), 8);
    }

    /**
     * The CPU read the sprite-sprite collision register in the current cycle:
     * The collisions of the pixels, which are not visible in it yet, are lost.
     */
    @Override
    void spriteCollisionsRead() {
        spriteCollisionsRead = COLLISION_DELAY;
    }

    /**
     * The CPU changed the graphics mode or a background color in the current cycle:
     * Render the pixels following the change again.
     */
    @Override
    void graphicsChanged() {
        var cycle = lastCycle;
        if (cycle == 0) {
            // Not running yet.
            return;
        }

        // Changes are aligned with the pixels of a column with a horizontal scroll of 1, starting in the cycle before.
        var changeX = (vic.x(cycle) - 3 + vic.lastX) % vic.lastX;

        // Left of the first column.
        if (changeX >= vic.firstX && cycle > 1) {
            Arrays.fill(graphics, changeX, vic.lastX, background());
            changeX = 0;
        }
        var first = vic.firstX_25 + (vic.regControl2 & VIC.CONTROL2_SCROLL_X);
        if (changeX < first) {
            Arrays.fill(graphics, changeX, first, background());
        }

        var renderer = lineRenderers[mode()];
        for (var i = 0; i < columnX.length; i++) {
            var x = columnX[i];
            if (x >= 0 && x + 8 > changeX) {
                renderer.render(changedGraphics, changedForeground, x, columnData[i], columnCharacter[i], columnColor[i]);
                var from = Math.max(x, changeX);
                System.arraycopy(changedGraphics, from, graphics, from, x + 8 - from);
                System.arraycopy(changedForeground, from, foreground, from, x + 8 - from);
            }
        }

        if (cycle >= 56 || cycle == 1) {
            // Right of the last column.
            var x = Math.max(changeX, vic.lastX_25 + (vic.regControl2 & VIC.CONTROL2_SCROLL_X));
            if (x < vic.firstX) {
                Arrays.fill(graphics, x, vic.firstX, background());
            }
        }
    }

    /**
     * c-access: Read a character and its color of the video matrix.
     *
     * @param column
     *         column
     */
    private void readVideoMatrix(int column) {
        var address = (vcBase + column) & 0x03FF;
        characters[column] = vic.bus.read(vic.baseCharacterMode + address);
        colors[column] = vic.colorRam.read(address) & 0x0F;
    }

    /**
     * g-access: Render the graphics of a column with the current registers.
     * In idle state the graphics data is read from $3FFF ($39FF in extended color mode), with video matrix data 0.
     * In the cycle, in which a bad line condition arises in idle state, it is read from $38FF instead
     * (VICE bug #1855, measured with the 6569 and the 6567R8).
     *
     * @param column
     *         column
     * @param badLineInIdleState
     *         has a bad line condition arisen in idle state in this cycle?
     */
    private void readGraphics(int column, boolean badLineInIdleState) {
        var x = vic.firstX_25 + (vic.regControl2 & VIC.CONTROL2_SCROLL_X) + 8 * column;

        if (column == 0) {
            // Horizontal fine scrolling: The gap left of the first column shows the background.
            Arrays.fill(graphics, vic.firstX_25, x, background());
            Arrays.fill(foreground, vic.firstX_25, x, false);
        }

        var renderer = lineRenderers[mode()];
        int data;
        var character = 0;
        var color = 0;
        if (displayState) {
            character = characters[vmli];
            color = colors[vmli];
            data = renderer.fetch(vc, rc, character);
            vc = (vc + 1) & 0x03FF;
            vmli++;
        } else {
            data = vic.bus.read(badLineInIdleState ? 0x38FF : (vic.regControl1 & VIC.CONTROL1_EXT_COLOR) != 0 ? 0x39FF : 0x3FFF);
        }
        renderer.render(graphics, foreground, x, data, character, color);

        var i = column % columnX.length;
        columnX[i] = x;
        columnData[i] = data;
        columnCharacter[i] = character;
        columnColor[i] = color;
    }

    /**
     * Output the 8 pixels of a cycle: Border, sprites or graphics.
     */
    private void output(int raster, int cycle) {
        var row = (raster - vic.lastVBlank + vic.linesPerScreen) % vic.linesPerScreen;
        var visible = row < getLines();
        var lineLength = getLineLength();
        var lineStart = row * lineLength;

        var x = vic.x(cycle);
        for (var i = 0; i < 8; i++) {
            var index = (x - vic.firstVisibleX + vic.lastX) % vic.lastX;
            if (visible && index < lineLength) {
                var borderPixel = border[x];
                screenRender[lineStart + index] = borderPixel != NO_BORDER ? borderPixel : pixel(x);
            }
            if (++x == vic.lastX) {
                x = 0;
            }
        }
    }

    /**
     * Evaluate the border of the 8 pixels of a cycle.
     * Executed in the next cycle, so that register writes of the CPU in the cycle are considered,
     * e.g. switching to 38 columns in cycle 56 opens the side border.
     */
    private void updateBorder(int raster, int cycle) {
        var x = vic.x(cycle);
        for (var i = 0; i < 8; i++) {
            updateMainBorder(raster, x);
            border[x] = mainBorder ? vic.regExteriorColor : NO_BORDER;
            if (++x == vic.lastX) {
                x = 0;
            }
        }

        if (cycle == vic.cyclesPerLine) {
            updateVerticalBorder(raster);
        }
    }

    /**
     * Color of a pixel: Sprite or graphics.
     */
    private byte pixel(int x) {
        var sprite = spriteRenderer.pixels[x];
        if (sprite != SpriteRenderer.NONE && !(spriteRenderer.behind[x] && foreground[x])) {
            return sprite;
        }

        return graphics[x];
    }

    /**
     * Update the main border flip-flop at the left and right border of the display window.
     */
    private void updateMainBorder(int raster, int x) {
        var columns40 = (vic.regControl2 & VIC.CONTROL2_40_COLUMNS) != 0;
        if (x == (columns40 ? vic.lastX_25 : vic.lastX_24)) {
            mainBorder = true;
        } else if (x == (columns40 ? vic.firstX_25 : vic.firstX_24)) {
            updateVerticalBorder(raster);
            if (!verticalBorder) {
                mainBorder = false;
            }
        }
    }

    /**
     * Update the vertical border flip-flop at the top and bottom border of the display window.
     */
    private void updateVerticalBorder(int raster) {
        var control1 = vic.regControl1;
        var rows25 = (control1 & VIC.CONTROL1_25_ROWS) != 0;
        if (raster == (rows25 ? vic.lastLine_25 : vic.lastLine_24)) {
            verticalBorder = true;
        } else if (raster == (rows25 ? vic.firstLine_25 : vic.firstLine_24) && (control1 & VIC.CONTROL1_DEN) != 0) {
            verticalBorder = false;
        }
    }
}
