package de.heiden.jem.models.c64.components.cpu;

import de.heiden.jem.components.bus.LogEntry;
import de.heiden.jem.components.bus.LoggingBus;
import de.heiden.jem.components.bus.WordBus;
import de.heiden.jem.components.clock.synchronization.SequentialClock;
import de.heiden.jem.models.c64.components.memory.RAM;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test.
 */
public class CPU6510Test extends TestCase {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private SequentialClock _clock;
  private RAM _ram;
  private LoggingBus _loggingBus;
  private WordBus _bus;
  private CPU6510 _cpu;

  /**
   * Test opcode 0x00: BRK.
   */
  public void test0x00() {
    // TODO check cpu state!

    logger.debug("test 0x00");

    _ram.write(0xEA, 0x0300); // NOP
    _ram.write(0x00, 0x0301); // BRK
    _ram.write(0xA5, 0x0302); // dummy
    _ram.write(0x48, 0xFFFE); // BRK vector low
    _ram.write(0xFF, 0xFFFF); // BRK vector high

    // Execute NOP
    _clock.run(1);
//    assertEquals(0x0301, _cpu.getState().PC);

    CPU6510State expectedState = _cpu.getState().copy();

    // load opcode -> PC = 0x0302
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC++, 0x00));

    // load byte after opcode -> PC = 0x0303
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC++, 0xA5));

    // store high(PC) at stack
    executeOneTick(expectedState, new LogEntry(false, expectedState.S--, 0x03));

    // store low(PC) at stack
    executeOneTick(expectedState, new LogEntry(false, expectedState.S--, 0x03));

    // store status flag at stack
    executeOneTick(expectedState, new LogEntry(false, expectedState.S--, expectedState.getP()));

    // load low(vector)
    executeOneTick(expectedState, new LogEntry(true, 0xFFFE, 0x48));

    // load high(vector)
    expectedState.PC = 0xFF48;
    executeOneTick(expectedState, new LogEntry(true, 0xFFFF, 0xFF));

    assertEquals(7, _clock.getTick());
  }

  /**
   * Setup.
   * <p/>
   * Creates CPU test environment with 0x1000 bytes of RAM starting ab 0x0000.
   * PC is set to 0x300.
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    logger.debug("set up");

    _clock = new SequentialClock();
    _ram = new RAM(0x10000);
    _loggingBus = new LoggingBus(_ram);
    _bus = new WordBus(_loggingBus);
    _cpu = new CPU6510(_clock);
    _cpu.connect(_bus);

    // Test code starts at $300
    _bus.writeWord(0xFFFC, 0x0300);

    // Execute reset sequence
    _clock.run(2);
  }

  /**
   * Tear down.
   *
   * @throws Exception
   */
  @Override
  protected void tearDown() throws Exception {
    logger.debug("tear down");

    _clock.close();

    super.tearDown();
  }

  /**
   * Execute one cpu cycle and check for expected cpu state.
   *
   * @param expectedState expected state after execution.
   * @param expectedLog expected bus activity
   */
  private void executeOneTick(CPU6510State expectedState, LogEntry expectedLog) {
    _clock.run(1);
    assertEquals(expectedState, _cpu.getState());
    assertEquals(expectedLog, _loggingBus.getLastLogEntry());
  }
}
