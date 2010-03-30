package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import org.apache.log4j.Logger;
import org.serialthreads.Interruptible;

/**
 * Display unit of vic.
 *
 * TODO refactor dependencies
 */
public class DisplayUnit extends AbstractDisplayUnit
{
  /**
   * Hidden constructor.
   *
   * @param vic vic this display unit belongs to
   * @param clock clock
   */
  DisplayUnit(VIC vic, Clock clock)
  {
    super(vic, clock,
      (vic._lastVBlank + 1) * vic._cyclesPerLine * 8,
      vic._cyclesPerLine * 8, vic._linesPerScreen,
      vic._lastX - vic._firstVisibleX + vic._lastVisibleX, vic._firstVBlank - (vic._lastVBlank + 1));
  }

  @Override
  @Interruptible
  public final void run()
  {
    _vic.reset();

    //noinspection InfiniteLoopStatement
    while (true)
    {
      byte[] screen = _screenRender;

      int linesPerScreen = _vic._linesPerScreen;
      int cyclesPerLine = _vic._cyclesPerLine;

      boolean borderY = true;
      boolean borderX = true;

      // first left part of first line is not available, because there is no previous line
      int ptr = _vic._lastX - _vic._firstVisibleX;
      for (int line = 0; line < linesPerScreen; line++)
      {
        // turn y border on / off
        if (line == _vic._firstLine_25)
        {
          borderY = false;
        }
        else if (line == _vic._lastLine_25)
        {
          borderY = true;
        }

        for (int column = 0; column < cyclesPerLine * 8; column++)
        {
          if (column == _vic._firstX_25)
          {
            borderX = false;
          }
          else if (column == _vic._lastX_25)
          {
            borderX = true;
          }

          if (borderY || borderX)
          {
            screen[ptr++] = (byte) _vic._regExteriorColor;
          }
          else
          {
            screen[ptr++] = 1;
          }

          _tick.waitForTick();
        }

/*
        readSprite(3);
        readSprite(4);
        readSprite(5);
        readSprite(6);
        readSprite(7);
        for (int refresh = 0; refresh < 5; refresh++)
        {
          _tick.waitForTick();
        }
        readLine();
        // TODO 2010-03-15 mh: correct number of idle cycles for ntsc
        for (int idle = 0; idle < 2; idle++)
        {
          _tick.waitForTick();
        }
        readSprite(0);
        readSprite(1);
        readSprite(2);
*/
      }

      rendered(screen);
    }
  }

  @Interruptible
  private void readLine()
  {

  }

  @Interruptible
  private void readSprite(int i)
  {
    // TODO 2010-03-15 mh: sprite pointer access
    _tick.waitForTick();
    // TODO 2010-03-15 mh: 3 sprite data access / idle access
    _tick.waitForTick();
  }

  //
  // private attributes
  //

  /**
   * Logger.
   */
  private static final Logger _logger = Logger.getLogger(DisplayUnit.class);
}
