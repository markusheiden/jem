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
     * Columns of the last two g-accesses, the pixels of which may still be affected by register changes:
     * X coordinate of the first pixel (-1 for none), graphics data, video matrix data and color.
     */
    private final int[] columnX = {-1, -1};
    private final int[] columnData = new int[2];
    private final int[] columnCharacter = new int[2];
    private final int[] columnColor = new int[2];

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
     * Raster line of the cycle whose pixels are output in the next cycle.
     */
    private int outputRaster = 0;

    /**
     * Cycle whose pixels are output in the next cycle, 0 if none.
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
        Arrays.fill(graphics, background());
        Arrays.fill(foreground, false);
        columnX[0] = -1;
        columnX[1] = -1;
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
        // The pixels of the last cycle are output after the CPU accessed the registers in that cycle.
        if (outputCycle != 0) {
            output(outputRaster, outputCycle);
            outputBorder(outputRaster, outputCycle);
        }
        if (cycle == 1) {
            startLine(raster);
        }

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
        } else if (cycle == 56) {
            // Right of the last column.
            var x = vic.lastX_25 + (vic.regControl2 & VIC.CONTROL2_SCROLL_X);
            Arrays.fill(graphics, x, FIRST_X, background());
            Arrays.fill(foreground, x, FIRST_X, false);
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

        outputRaster = raster;
        outputCycle = cycle;
    }

    /**
     * The CPU changed the graphics mode or a background color in the current cycle:
     * Render the pixels following the change again.
     */
    @Override
    void graphicsChanged() {
        var cycle = outputCycle;
        if (cycle == 0) {
            // Not running yet.
            return;
        }

        // Changes are delayed by one pixel relative to the pixels of a column without horizontal scrolling.
        var changeX = (FIRST_X + (cycle - 1) * 8 + 5) % vic.lastX;

        if (cycle < 16) {
            // Left of the first column.
            if (changeX >= FIRST_X) {
                Arrays.fill(graphics, changeX, vic.lastX, background());
                changeX = 0;
            }
            Arrays.fill(graphics, changeX, vic.firstX_25, background());
            return;
        }

        var renderer = lineRenderers[mode()];
        for (var i = 0; i < 2; i++) {
            var x = columnX[i];
            if (x >= 0 && x + 8 > changeX) {
                renderer.render(changedGraphics, changedForeground, x, columnData[i], columnCharacter[i], columnColor[i]);
                var from = Math.max(x, changeX);
                System.arraycopy(changedGraphics, from, graphics, from, x + 8 - from);
                System.arraycopy(changedForeground, from, foreground, from, x + 8 - from);
            }
        }

        if (cycle >= 56) {
            // Right of the last column.
            var x = Math.max(changeX, vic.lastX_25 + (vic.regControl2 & VIC.CONTROL2_SCROLL_X));
            if (x < FIRST_X) {
                Arrays.fill(graphics, x, FIRST_X, background());
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
     *
     * @param column
     *         column
     */
    private void readGraphics(int column) {
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
            data = vic.bus.read((vic.regControl1 & VIC.CONTROL1_EXT_COLOR) != 0 ? 0x39FF : 0x3FFF);
        }
        renderer.render(graphics, foreground, x, data, character, color);

        var i = column & 1;
        columnX[i] = x;
        columnData[i] = data;
        columnCharacter[i] = character;
        columnColor[i] = color;
    }

    /**
     * Output the 8 pixels of a cycle: Sprites or graphics.
     * Executed in the next cycle, so that register writes of the CPU in the cycle are considered.
     */
    private void output(int raster, int cycle) {
        var row = raster - vic.lastVBlank;
        var visible = row >= 0 && row < getLines();
        var lineLength = getLineLength();
        var lineStart = row * lineLength;

        var x = (FIRST_X + (cycle - 1) * 8) % vic.lastX;
        for (var i = 0; i < 8; i++) {
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
     * Output the border over the 8 pixels of a cycle.
     * Executed in the next cycle too, e.g. switching to 38 columns in cycle 56 opens the side border.
     */
    private void outputBorder(int raster, int cycle) {
        var row = raster - vic.lastVBlank;
        var visible = row >= 0 && row < getLines();
        var lineLength = getLineLength();
        var lineStart = row * lineLength;

        var x = (FIRST_X + (cycle - 1) * 8) % vic.lastX;
        for (var i = 0; i < 8; i++) {
            updateMainBorder(raster, x);
            var index = (x - vic.firstVisibleX + vic.lastX) % vic.lastX;
            if (mainBorder && visible && index < lineLength) {
                screenRender[lineStart + index] = vic.regExteriorColor;
            }
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
