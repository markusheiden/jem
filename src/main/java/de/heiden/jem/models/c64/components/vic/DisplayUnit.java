package de.heiden.jem.models.c64.components.vic;

import org.serialthreads.Interruptible;

import java.util.Arrays;

/**
 * Display unit of the VIC: Cycle exact sequencer, which outputs 8 pixels per cycle.
 * <p>
 * Implements the counters VC, VCBASE, RC and VMLI, display and idle state, bad lines,
 * the video matrix accesses (c-accesses) in cycles 15-54 of bad lines,
 * the graphics accesses (g-accesses) in cycles 16-55 and the border flip-flops,
 * as described in "The MOS 6567/6569 video controller (VIC-II) and its application in the Commodore 64" by Christian Bauer.
 * <p>
 * Simplifications: The graphics of a column are rendered with the registers of its g-access cycle.
 * The sprites of a line are rendered in cycle 11 with the registers of that cycle.
 */
public class DisplayUnit extends AbstractDisplayUnit {
    /**
     * X coordinate of the first pixel of cycle 1 (PAL 6569).
     */
    private static final int FIRST_X = 0x194;

    /**
     * Cycle after the last sprite data access of a line, in which the sprites of the line are rendered.
     */
    private static final int SPRITE_RENDER_CYCLE = 11;

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
     * Hidden constructor.
     *
     * @param vic
     *         vic this display unit belongs to
     */
    DisplayUnit(VIC vic) {
        super(vic,
                0,
                vic.lastX - vic.firstVisibleX + vic.lastVisibleX, vic.firstVBlank - vic.lastVBlank,
                vic.lastX - vic.firstVisibleX + vic.lastVisibleX, vic.firstVBlank - vic.lastVBlank);

        var invalidMode = new InvalidModeLineRenderer();
        lineRenderers = new LineRenderer[]{
                new TextLineRenderer(vic), new MultiColorTextLineRenderer(vic),
                new BitmapLineRenderer(vic), new MultiColorBitmapLineRenderer(vic),
                new ExtendedColorTextLineRenderer(vic), invalidMode,
                invalidMode, invalidMode};
        spriteRenderer = new SpriteRenderer(vic, X_COORDINATES);
    }

    @Override
    @Interruptible
    public final void run() {
        vic.reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            for (var raster = 0; raster < vic.linesPerScreen; raster++) {
                startLine(raster);
                for (var cycle = 1; cycle <= vic.cyclesPerLine; cycle++) {
                    // The raster line changes in cycle 1, line 0 is reached one cycle later.
                    if (cycle == (raster == 0 ? 2 : 1)) {
                        vic.setRasterLine(raster);
                    }
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
        Arrays.fill(graphics, vic.regBackgroundColor0);
        Arrays.fill(foreground, false);
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
        vic.spriteCycle(raster, cycle);
        vic.updateBA(cycle);
        if (cycle == SPRITE_RENDER_CYCLE) {
            spriteRenderer.render();
        }
        var badLine = vic.isBadLine();
        if (badLine) {
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
            readGraphics(cycle - 16);
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

        output(raster, cycle);

        if (cycle == vic.cyclesPerLine) {
            updateVerticalBorder(raster);
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
     * In idle state the column shows the background.
     *
     * @param column
     *         column
     */
    private void readGraphics(int column) {
        var control1 = vic.regControl1;
        var control2 = vic.regControl2;
        var x = vic.firstX_25 + (control2 & VIC.CONTROL2_SCROLL_X) + 8 * column;

        if (column == 0) {
            // Horizontal fine scrolling: The gap left of the first column shows the background.
            Arrays.fill(graphics, vic.firstX_25, x, vic.regBackgroundColor0);
            Arrays.fill(foreground, vic.firstX_25, x, false);
        }

        if (displayState) {
            var mode = (control1 & (VIC.CONTROL1_EXT_COLOR | VIC.CONTROL1_BITMAP)) >> 4 | (control2 & VIC.CONTROL2_MULTI_COLOR) >> 4;
            lineRenderers[mode].render(graphics, foreground, x, vc, rc, characters[vmli], colors[vmli]);
            vc = (vc + 1) & 0x03FF;
            vmli++;
        } else {
            Arrays.fill(graphics, x, x + 8, vic.regBackgroundColor0);
            Arrays.fill(foreground, x, x + 8, false);
        }
    }

    /**
     * Output the 8 pixels of a cycle: Border, sprites or graphics.
     */
    private void output(int raster, int cycle) {
        var row = raster - vic.lastVBlank;
        var visible = row >= 0 && row < getLines();
        var lineLength = getLineLength();
        var lineStart = row * lineLength;

        var x = (FIRST_X + (cycle - 1) * 8) % vic.lastX;
        for (var i = 0; i < 8; i++) {
            updateMainBorder(raster, x);
            var index = (x - vic.firstVisibleX + vic.lastX) % vic.lastX;
            if (visible && index < lineLength) {
                screenRender[lineStart + index] = pixel(x);
            }
            if (++x == vic.lastX) {
                x = 0;
            }
        }
    }

    /**
     * Color of a pixel: Border, sprite or graphics.
     */
    private byte pixel(int x) {
        if (mainBorder) {
            return vic.regExteriorColor;
        }

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
