package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.apache.log4j.Logger;

/**
 * Display unit of vic.
 */
public abstract class AbstractDisplayUnit implements ClockedComponent
{
  protected final VIC _vic;
  protected final Tick _tick;

  private final int _offset;
  private final int _lineLength;
  private final int _lines;
  private final int _width;
  private final int _height;

  private IScreenListener _listener;

  /**
   * Screen buffer for rendering.
   */
  protected byte[] _screenRender;
  /**
   * Spare screen buffer.
   */
  private byte[] _screenSpare;
  /**
   * Screen to display next.
   */
  private byte[] _screenToDisplay;
  /**
   * Currently displayed screen.
   */
  private volatile byte[] _screenDisplaying;

  /**
   * Hidden constructor.
   *
   * @param vic vic this display unit belongs to
   * @param clock clock
   */
  AbstractDisplayUnit(VIC vic, Clock clock,
    int offset, int lineLength, int lines, int width, int height)
  {
    _vic = vic;
    _tick = clock.addClockedComponent(Clock.VIC_DISPLAY, this);

    _offset = offset;
    _lineLength = lineLength;
    _lines = lines;
    _width = width;
    _height = height;

    int size = offset + lineLength * lines;

    _screenRender = new byte[size];
    _screenSpare = new byte[size];
    _screenToDisplay = new byte[size];
    _screenDisplaying = _screenToDisplay;
  }

  @Override
  public String getName()
  {
    return _vic.getClass().getSimpleName() + " display";
  }

  /**
   * Screen rendering has been finished. Handles tripple buffering.
   *
   * @param screen new rendered screen
   */
  protected final synchronized void rendered(byte[] screen)
  {
    if (_screenToDisplay == _screenDisplaying)
    {
      _screenRender = _screenSpare;
      _screenSpare = _screenToDisplay;
    }
    else
    {
      _screenRender = _screenToDisplay;
      // spare may not be changed, because it is currently displayed
    }
    _screenToDisplay = screen;

    if (_listener != null)
    {
      _listener.newScreenRendered();
    }
  }

  /**
   * Notify display unit that a new screen will be displayed.
   *
   * @return screen to display next
   */
  public final synchronized byte[] display()
  {
    _screenDisplaying = _screenToDisplay;
    return _screenDisplaying;
  }

  /**
   * Sets the screen listener of this display unit.
   *
   * @param listener Listener
   */
  public void setScreenListener(IScreenListener listener)
  {
    _listener = listener;
  }

  /**
   * Offset of first pixel in screen.
   */
  public int getOffset()
  {
    return _offset;
  }

  /**
   * Total number of pixel/bytes per line.
   */
  public int getLineLength()
  {
    return _lineLength;
  }


  /**
   * Total number of lines.
   */
  public int getLines()
  {
    return _lines;
  }


  /**
   * Number of visible pixel/bytes per line.
   */
  public int getWidth()
  {
    return _width;
  }


  /**
   * Number of visible lines per screen.
   */
  public int getHeight()
  {
    return _height;
  }
}
