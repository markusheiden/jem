package de.heiden.jem.models.c64.components.cpu;

import de.heiden.jem.components.clock.synchronization.SerializedClock;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.cpu.CPUTestBus.LogEntry;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 * Test.
 */
public class CPU6510_Test extends TestCase
{
  /**
   * Test opcode 0x00: BRK.
   */
  public void test0x00()
  {
    // TODO check cpu state!

    _logger.debug("test 0x00");

    _ram.write(0x00, 0x0300); // BRK
    _ram.write(0xA5, 0x0301); // dummy
    _ram.write(0x48, 0xFFFE); // BRK vector low
    _ram.write(0xFF, 0xFFFF); // BRK vector high

    _clock.run(1);

    CPU6510State expectedState = _cpu.getState().copy();

    // load opcode -> PC = 0x0301
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC++, 0x00));

    // load byte after opcode -> PC = 0x0302
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC++, 0xA5));

    // store high(PC) at stack
    executeOneTick(expectedState, new LogEntry(false, expectedState.S--, 0x03));

    // store low(PC) at stack
    executeOneTick(expectedState, new LogEntry(false, expectedState.S--, 0x02));

    // store status flag at stack
    executeOneTick(expectedState, new LogEntry(false, expectedState.S--, expectedState.getP()));

    // load low(vector)
    executeOneTick(expectedState, new LogEntry(true, 0xFFFE, 0x48));

    // load high(vector)
    expectedState.PC = 0xFF48;
    executeOneTick(expectedState, new LogEntry(true, 0xFFFE, 0x48));

    assertEquals(7, _clock.getTick());
  }

  /**
   * Setup.
   * <p/>
   * Creates CPU test environment with 0x1000 bytes of RAM starting ab 0x0000.
   * PC is set to 0x300.
   */
  protected void setUp() throws Exception
  {
    super.setUp();

    _logger.debug("set up");

    _clock = new SerializedClock();
    _ram = new RAM(0x10000);
    _bus = new CPUTestBus(_ram);
    _cpu = new CPU6510(_clock);
    _cpu.connect(_bus);
    _clock.run(2);
    _cpu.getState().PC = 0x0300;
  }

  /**
   * Tear down.
   *
   * @throws Exception
   */
  protected void tearDown() throws Exception
  {
    _logger.debug("tear down");

    _clock.dispose();

    super.tearDown();
  }

  /**
   * Execute one cpu cycle and check for expected cpu state.
   *
   * @param expectedState expected state after execution.
   * @param expectedLog expected bus activity
   */
  private void executeOneTick(CPU6510State expectedState, LogEntry expectedLog)
  {
    _clock.run(1);
    assertEquals(expectedState, _cpu.getState());
    assertEquals(expectedLog, _bus.getLastLogEntry());
  }

  //
  // private attributes
  //

  private SerializedClock _clock;
  private RAM _ram;
  private CPUTestBus _bus;
  private CPU6510 _cpu;

  private static final Logger _logger = Logger.getLogger(CPU6510_Test.class);
}
