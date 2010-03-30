package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.apache.log4j.Logger;
import org.serialthreads.Interruptible;

/**
 * Memory access unit of vic.
 *
 * TODO refactor dependencies
 */
class MemAccessUnit implements ClockedComponent
{
  /**
   * Constructor.
   *
   * @param vic vic this mem access unit belongs to
   * @param clock clock
   */
  MemAccessUnit(VIC vic, Clock clock)
  {
    _vic = vic;

    _tick = clock.addClockedComponent(Clock.VIC_MEM, this);

    x = 0;
    y = 0;
    characters = new int[40];
    colors = new int[40];
  }

  @Override
  @Interruptible
  public final void run()
  {
    _vic.reset();

    while (true)
    {
      loadScreen();
    }
  }

  @Override
  public String getName()
  {
    return _vic.getClass().getSimpleName() + " mem access";
  }

  /**
   * Start unit.
   * Implementation: CPU cycles belong to next VIC cycle.
   */
  @Interruptible
  public final void loadScreen()
  {
    final int linesPerScreen = _vic._linesPerScreen;
    for (int line = 0; line < linesPerScreen; line++)
    {
      boolean badline = false;
      int row = 0; // 0-7

      readSprite(3); // cycle 1-2
      readSprite(4); // cycle 3-4
      readSprite(5); // cycle 5-6
      readSprite(6); // cycle 7-8
      readSprite(7); // cycle 9-10
      refresh(); // cycle 11-15

      // cycle 16-55
      int characterAddress = 0; // TODO
      int colorAddress = 0; // TODO
      for (int column = 0; column < 40; column++)
      {
        if (badline)
        {
          int character = _vic._bus.read(characterAddress++);
          int color = _vic._colorRam.read(colorAddress++);
        }
        int bitmapAddress = 0; // TODO
        int bitmap = _vic._bus.read(bitmapAddress + row);
        waitForTick();
      }

      // 2-4 idle cycles
      idle();

      // PAL +0, NTSC + 1, NTSC + 2
      readSprite(0); // cycle 58-59
      readSprite(1); // cycle 60-61
      readSprite(2); // cycle 62-63
    }
  }

  /**
   * Read sprite data.
   *
   * @param number number of sprite.
   * @require number >= 0 && number < 8
   */
  @Interruptible
  protected final void readSprite(int number)
  {
    // TODO BA for next sprites
    int pointer = _vic._bus.read(0); // VIC cycle part
    int address = 0 + pointer << 7 + 0; // TODO base and row
    waitForTick();
    if (_vic._sprites[number].enabled)
    {
      // fetch sprite data
      int data = _vic._bus.read(address++) & 0xFF; // CPU cycle part
      data <<= 8;
      data |= _vic._bus.read(address++) & 0xFF; // VIC cycle part
      data <<= 8;
      waitForTick();
      data |= _vic._bus.read(address++) & 0xFF; // CPU cycle part
    }
    else
    {
      // idle access
      waitForTick();
    }
  }

  /**
   * DRAM refresh.
   */
  @Interruptible
  protected final void refresh()
  {
    // TODO BA for badline
    // TODO speed up? (5)
    waitForTick();
    waitForTick();
    waitForTick();
    waitForTick();
    waitForTick();
  }

  /**
   * Idle cycles.
   */
  @Interruptible
  protected final void idle()
  {
    // TODO BA for next sprites
    // TODO speed up? (65 - cycles per line + 2)
    waitForTick();
    waitForTick();
    if (_vic._cyclesPerLine > 63)
    {
      waitForTick();
    }
    if (_vic._cyclesPerLine > 64)
    {
      waitForTick();
    }
  }

  /**
   * Wait for 1 clock tick.
   */
  @Interruptible
  protected final void waitForTick()
  {
    _tick.waitForTick();
    x += 8;
  }

  //
  // attributes
  //

  private final VIC _vic;
  private final Tick _tick;

  protected int x;
  protected int y;
  protected final int[] characters;
  protected final int[] colors;

  /**
   * Logger.
   */
  private static final Logger _logger = Logger.getLogger(MemAccessUnit.class);
}
