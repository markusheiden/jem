package de.heiden.jem.models.c64.components.vic;

import org.serialthreads.Interruptible;

import java.util.Arrays;

/**
 * Display unit of vic.
 * <p>
 * TODO refactor dependencies
 */
public class DisplayUnitSimple extends AbstractDisplayUnit {
    /**
     * Cycle of a line in which the VIC starts to fetch the graphics of the display window.
     */
    private static final int RENDER_CYCLE = 16;

    /**
     * Renderers for the graphics modes, indexed by the mode bits ECM, BMM, MCM.
     */
    private final LineRenderer[] lineRenderers;

    /**
     * Renderer for sprites.
     */
    private final SpriteRenderer spriteRenderer;

    /**
     * Foreground flags parallel to the screen data, for the priority of sprites.
     */
    private final boolean[] foreground;

    /**
     * Hidden constructor.
     *
     * @param vic
     *         vic this display unit belongs to
     */
    DisplayUnitSimple(VIC vic) {
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
        spriteRenderer = new SpriteRenderer(vic);
        foreground = new boolean[screenRender.length];
    }

    @Override
    @Interruptible
    public final void run() {
        vic.reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            var ptr = 0;

            var raster = 0;

            // top vblank
            for (; raster < vic.lastVBlank; raster++) {
                vic.setRasterLine(raster);
                for (var cycle = 0; cycle < vic.cyclesPerLine; cycle++) {
                    tick.waitForTick();
                }
            }

            // top border
            for (; raster < vic.firstLine_25; raster++) {
                vic.setRasterLine(raster);
                for (var cycle = 0; cycle < vic.cyclesPerLine; cycle++) {
                    tick.waitForTick();
                }

                final var pixelPerLine = vic.lastX - vic.firstVisibleX + vic.lastVisibleX;
                Arrays.fill(screenRender, ptr, ptr += pixelPerLine, vic.regExteriorColor);
            }

            // visible area
            for (var y = 0; raster < vic.lastLine_25; raster++, y++) {
                vic.setRasterLine(raster);

                // Render with the registers at the start of the display window,
                // so changes later in the line, e.g. raster splits, take effect in the next line.
                var cycle = 0;
                for (; cycle < RENDER_CYCLE; cycle++) {
                    tick.waitForTick();
                }

                final var leftBorder = vic.lastX - vic.firstVisibleX + vic.firstX_25;
                Arrays.fill(screenRender, ptr, ptr += leftBorder, vic.regExteriorColor);
                var newPtr = renderLine(screenRender, ptr, y, raster);
                ptr = newPtr;
                final var rightBorder = vic.lastVisibleX - vic.lastX_25;
                Arrays.fill(screenRender, ptr, ptr += rightBorder, vic.regExteriorColor);

                for (; cycle < vic.cyclesPerLine; cycle++) {
                    tick.waitForTick();
                }
            }

            // bottom border
            for (; raster < vic.firstVBlank; raster++) {
                vic.setRasterLine(raster);
                for (var cycle = 0; cycle < vic.cyclesPerLine; cycle++) {
                    tick.waitForTick();
                }

                final var pixelPerLine = vic.lastX - vic.firstVisibleX + vic.lastVisibleX;
                Arrays.fill(screenRender, ptr, ptr += pixelPerLine, vic.regExteriorColor);
            }

            // bottom vblank
            for (; raster < vic.linesPerScreen; raster++) {
                vic.setRasterLine(raster);
                for (var cycle = 0; cycle < vic.cyclesPerLine; cycle++) {
                    tick.waitForTick();
                }
            }

            rendered(screenRender);
        }
    }

    /**
     * Render one line of the display window: Graphics in the current mode, sprites and the borders of the 24 rows and 38 columns modes.
     *
     * @param screen
     *         screen data
     * @param ptr
     *         index of the first pixel of the display window in screen data
     * @param y
     *         line in the display window
     * @param raster
     *         raster line
     * @return next index in screen data
     */
    private int renderLine(byte[] screen, int ptr, int y, int raster) {
        var control1 = vic.regControl1;
        var control2 = vic.regControl2;

        // Vertical fine scrolling: The default scroll value 3 aligns the first text row with the top of the display window.
        // Lines outside the 200 lines of graphics data show the background.
        var line = y + 3 - (control1 & VIC.CONTROL1_SCROLL_Y);
        if (line >= 0 && line < 200) {
            var mode = (control1 & (VIC.CONTROL1_EXT_COLOR | VIC.CONTROL1_BITMAP)) >> 4 | (control2 & VIC.CONTROL2_MULTI_COLOR) >> 4;
            lineRenderers[mode].render(screen, foreground, ptr, line);
        } else {
            Arrays.fill(screen, ptr, ptr + 320, vic.regBackgroundColor0);
            Arrays.fill(foreground, ptr, ptr + 320, false);
        }

        // Horizontal fine scrolling: Shift the line to the right, fill the gap with the background.
        var scrollX = control2 & VIC.CONTROL2_SCROLL_X;
        if (scrollX != 0) {
            System.arraycopy(screen, ptr, screen, ptr + scrollX, 320 - scrollX);
            Arrays.fill(screen, ptr, ptr + scrollX, vic.regBackgroundColor0);
            System.arraycopy(foreground, ptr, foreground, ptr + scrollX, 320 - scrollX);
            Arrays.fill(foreground, ptr, ptr + scrollX, false);
        }

        spriteRenderer.render(screen, foreground, ptr, raster);

        // 24 rows mode: The border covers the top 4 and the bottom 4 lines.
        // 38 columns mode: The border covers the leftmost 7 and the rightmost 9 pixels.
        if ((control1 & VIC.CONTROL1_25_ROWS) == 0 && (raster < vic.firstLine_24 || raster >= vic.lastLine_24)) {
            Arrays.fill(screen, ptr, ptr + 320, vic.regExteriorColor);
        } else if ((control2 & VIC.CONTROL2_40_COLUMNS) == 0) {
            Arrays.fill(screen, ptr, ptr + vic.firstX_24 - vic.firstX_25, vic.regExteriorColor);
            Arrays.fill(screen, ptr + 320 - (vic.lastX_25 - vic.lastX_24), ptr + 320, vic.regExteriorColor);
        }

        return ptr + 320;
    }
}
