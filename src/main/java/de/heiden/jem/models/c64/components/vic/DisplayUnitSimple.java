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
    private final LineRenderer[] _lineRenderers;

    /**
     * Renderer for sprites.
     */
    private final SpriteRenderer _spriteRenderer;

    /**
     * Foreground flags parallel to the screen data, for the priority of sprites.
     */
    private final boolean[] _foreground;

    /**
     * Hidden constructor.
     *
     * @param vic
     *         vic this display unit belongs to
     */
    DisplayUnitSimple(VIC vic) {
        super(vic,
                0,
                vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - vic._lastVBlank,
                vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - vic._lastVBlank);

        var invalidMode = new InvalidModeLineRenderer();
        _lineRenderers = new LineRenderer[]{
                new TextLineRenderer(vic), new MultiColorTextLineRenderer(vic),
                new BitmapLineRenderer(vic), new MultiColorBitmapLineRenderer(vic),
                new ExtendedColorTextLineRenderer(vic), invalidMode,
                invalidMode, invalidMode};
        _spriteRenderer = new SpriteRenderer(vic);
        _foreground = new boolean[_screenRender.length];
    }

    @Override
    @Interruptible
    public final void run() {
        _vic.reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            int ptr = 0;

            int raster = 0;

            // top vblank
            for (; raster < _vic._lastVBlank; raster++) {
                _vic.setRasterLine(raster);
                for (int cycle = 0; cycle < _vic._cyclesPerLine; cycle++) {
                    _tick.waitForTick();
                }
            }

            // top border
            for (; raster < _vic._firstLine_25; raster++) {
                _vic.setRasterLine(raster);
                for (int cycle = 0; cycle < _vic._cyclesPerLine; cycle++) {
                    _tick.waitForTick();
                }

                final int pixelPerLine = _vic._lastX - _vic._firstVisibleX + _vic._lastVisibleX;
                Arrays.fill(_screenRender, ptr, ptr += pixelPerLine, _vic._regExteriorColor);
            }

            // visible area
            for (int y = 0; raster < _vic._lastLine_25; raster++, y++) {
                _vic.setRasterLine(raster);

                // Render with the registers at the start of the display window,
                // so changes later in the line, e.g. raster splits, take effect in the next line.
                int cycle = 0;
                for (; cycle < RENDER_CYCLE; cycle++) {
                    _tick.waitForTick();
                }

                final int leftBorder = _vic._lastX - _vic._firstVisibleX + _vic._firstX_25;
                Arrays.fill(_screenRender, ptr, ptr += leftBorder, _vic._regExteriorColor);
                int newPtr = renderLine(_screenRender, ptr, y, raster);
                ptr = newPtr;
                final int rightBorder = _vic._lastVisibleX - _vic._lastX_25;
                Arrays.fill(_screenRender, ptr, ptr += rightBorder, _vic._regExteriorColor);

                for (; cycle < _vic._cyclesPerLine; cycle++) {
                    _tick.waitForTick();
                }
            }

            // bottom border
            for (; raster < _vic._firstVBlank; raster++) {
                _vic.setRasterLine(raster);
                for (int cycle = 0; cycle < _vic._cyclesPerLine; cycle++) {
                    _tick.waitForTick();
                }

                final int pixelPerLine = _vic._lastX - _vic._firstVisibleX + _vic._lastVisibleX;
                Arrays.fill(_screenRender, ptr, ptr += pixelPerLine, _vic._regExteriorColor);
            }

            // bottom vblank
            for (; raster < _vic._linesPerScreen; raster++) {
                _vic.setRasterLine(raster);
                for (int cycle = 0; cycle < _vic._cyclesPerLine; cycle++) {
                    _tick.waitForTick();
                }
            }

            rendered(_screenRender);
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
        int control1 = _vic._regControl1;
        int control2 = _vic._regControl2;

        // Vertical fine scrolling: The default scroll value 3 aligns the first text row with the top of the display window.
        // Lines outside the 200 lines of graphics data show the background.
        int line = y + 3 - (control1 & VIC.CONTROL1_SCROLL_Y);
        if (line >= 0 && line < 200) {
            int mode = (control1 & (VIC.CONTROL1_EXT_COLOR | VIC.CONTROL1_BITMAP)) >> 4 | (control2 & VIC.CONTROL2_MULTI_COLOR) >> 4;
            _lineRenderers[mode].render(screen, _foreground, ptr, line);
        } else {
            Arrays.fill(screen, ptr, ptr + 320, _vic._regBackgroundColor0);
            Arrays.fill(_foreground, ptr, ptr + 320, false);
        }

        // Horizontal fine scrolling: Shift the line to the right, fill the gap with the background.
        int scrollX = control2 & VIC.CONTROL2_SCROLL_X;
        if (scrollX != 0) {
            System.arraycopy(screen, ptr, screen, ptr + scrollX, 320 - scrollX);
            Arrays.fill(screen, ptr, ptr + scrollX, _vic._regBackgroundColor0);
            System.arraycopy(_foreground, ptr, _foreground, ptr + scrollX, 320 - scrollX);
            Arrays.fill(_foreground, ptr, ptr + scrollX, false);
        }

        _spriteRenderer.render(screen, _foreground, ptr, raster);

        // 24 rows mode: The border covers the top 4 and the bottom 4 lines.
        // 38 columns mode: The border covers the leftmost 7 and the rightmost 9 pixels.
        if ((control1 & VIC.CONTROL1_25_ROWS) == 0 && (raster < _vic._firstLine_24 || raster >= _vic._lastLine_24)) {
            Arrays.fill(screen, ptr, ptr + 320, _vic._regExteriorColor);
        } else if ((control2 & VIC.CONTROL2_40_COLUMNS) == 0) {
            Arrays.fill(screen, ptr, ptr + _vic._firstX_24 - _vic._firstX_25, _vic._regExteriorColor);
            Arrays.fill(screen, ptr + 320 - (_vic._lastX_25 - _vic._lastX_24), ptr + 320, _vic._regExteriorColor);
        }

        return ptr + 320;
    }
}
