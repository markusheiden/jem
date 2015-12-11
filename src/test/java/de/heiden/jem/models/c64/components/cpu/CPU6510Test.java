package de.heiden.jem.models.c64.components.cpu;

import de.heiden.jem.components.bus.LogEntry;
import de.heiden.jem.components.bus.LoggingBus;
import de.heiden.jem.components.bus.WordBus;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.memory.RAM;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.serialthreads.agent.Transform;
import org.serialthreads.agent.TransformingRunner;
import org.serialthreads.transformer.strategies.frequent3.FrequentInterruptsTransformer3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link CPU6510}.
 */
@RunWith(TransformingRunner.class)
@Transform(transformer = FrequentInterruptsTransformer3.class, classPrefixes = "de.heiden.jem")
public class CPU6510Test {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Clock _clock;
  private RAM _ram;
  private LoggingBus _loggingBus;
  private WordBus _bus;
  private CPU6510 _cpu;

  private long startTick;
  private CPU6510State state;
  private CPU6510State expectedState;
  private CPU6510State stateAfter;

  /**
   * Test opcode 0x00: BRK.
   */
  @Test
  public void test0x00() {
    _ram.write(0x00, 0x0300); // BRK
    _ram.write(0xA5, 0x0301); // dummy
    _ram.write(0x48, 0xFFFE); // BRK vector low
    _ram.write(0xFF, 0xFFFF); // BRK vector high
    _ram.write(0xEA, 0xFF48); // NOP at BRK vector

    // load opcode -> PC = 0x0301
    executeOneTick_readPC(expectedState, 0x00);

    // load byte after opcode -> PC = 0x0302
    executeOneTick_readPC(expectedState, 0xA5);

    // store high(PC) at stack
    executeOneTick_push(expectedState, 0x03);

    // store low(PC) at stack
    executeOneTick_push(expectedState, 0x02);

    // store status flag at stack
    executeOneTick_push(expectedState, expectedState.getP());

    // set I and load low(vector)
    expectedState.I = true;
    executeOneTick_read(expectedState, 0xFFFE, 0x48);

    // load high(vector)
    executeOneTick_read(expectedState, 0xFFFF, 0xFF);
    expectedState.PC = 0xFF48;

    // Check overall clock cycles.
    assertEquals(startTick + 7, _clock.getTick());

    // Check that NOP at BRK vector gets executed.
    executeOneTick_readPC(expectedState, 0xEA);
  }

  /**
   * Test opcode 0x8A: TXA.
   */
  @Test
  public void test0x8A() {
    test_Txx(0x8A, value -> {
      state.A = 0xFF;
      state.X = value;
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      stateAfter.A = value;
      stateAfter.X = value;
      stateAfter.Z = z(value);
      stateAfter.N = n(value);
    });
  }

  /**
   * Test opcode 0x98: TYA.
   */
  @Test
  public void test0x98() {
    test_Txx(0x98, value -> {
      state.A = 0xFF;
      state.Y = value;
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      stateAfter.A = value;
      stateAfter.Y = value;
      stateAfter.Z = z(value);
      stateAfter.N = n(value);
    });
  }

  /**
   * Test opcode 0xA4: LDY #$00.
   */
  @Test
  public void test0xA0_00() {
    state.Y = 0xFF;
    state.Z = false;
    state.N = true;
    captureExpectedState();
    stateAfter.Y = 0x00;
    stateAfter.Z = true;
    stateAfter.N = false;
    test_LDx_IMM(0xA0, 0x00);
  }

  /**
   * Test opcode 0xA4: LDY #$80.
   */
  @Test
  public void test0xA0_80() {
    state.Y = 0x00;
    state.Z = true;
    state.N = false;
    captureExpectedState();
    stateAfter.Y = 0x80;
    stateAfter.Z = false;
    stateAfter.N = true;
    test_LDx_IMM(0xA0, 0x80);
  }

  /**
   * Test opcode 0xA2: LDX #$00.
   */
  @Test
  public void test0xA2_00() {
    state.X = 0xFF;
    state.Z = false;
    state.N = true;
    captureExpectedState();
    stateAfter.X = 0x00;
    stateAfter.Z = true;
    stateAfter.N = false;
    test_LDx_IMM(0xA2, 0x00);
  }

  /**
   * Test opcode 0xA2: LDX #$80.
   */
  @Test
  public void test0xA2_80() {
    state.X = 0x00;
    state.Z = true;
    state.N = false;
    captureExpectedState();
    stateAfter.X = 0x80;
    stateAfter.Z = false;
    stateAfter.N = true;
    test_LDx_IMM(0xA2, 0x80);
  }

  /**
   * Test opcode 0xA8: TAY.
   */
  @Test
  public void test0xA8() {
    test_Txx(0xA8, value -> {
      state.A = value;
      state.Y = 0xFF;
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      stateAfter.A = value;
      stateAfter.Y = value;
      stateAfter.Z = z(value);
      stateAfter.N = n(value);
    });
  }

  /**
   * Test opcode 0xA9: LDA #$00.
   */
  @Test
  public void test0xA9_00() {
    state.A = 0xFF;
    state.Z = false;
    state.N = true;
    captureExpectedState();
    stateAfter.A = 0x00;
    stateAfter.Z = true;
    stateAfter.N = false;
    test_LDx_IMM(0xA9, 0x00);
  }

  /**
   * Test opcode 0xA9: LDA #$80.
   */
  @Test
  public void test0xA9_80() {
    state.A = 0x00;
    state.Z = true;
    state.N = false;
    captureExpectedState();
    stateAfter.A = 0x80;
    stateAfter.Z = false;
    stateAfter.N = true;
    test_LDx_IMM(0xA9, 0x80);
  }

  /**
   * Test opcode 0xAA: TAX.
   */
  @Test
  public void test0xAA() {
    test_Txx(0xAA, value -> {
      state.A = value;
      state.X = 0xFF;
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      stateAfter.A = value;
      stateAfter.X = value;
      stateAfter.Z = z(value);
      stateAfter.N = n(value);
    });
  }

  /**
   * Test of LD? #$xx.
   */
  private void test_LDx_IMM(int opcode, int value) {
    writeRam(opcode, value); // LD? #$xx, NOP

    // load opcode -> PC = 0x0301
    executeOneTick_readPC(expectedState, opcode);
    // load byte after opcode -> PC = 0x0302
    executeOneTick_readPC(expectedState, value);
    // NOP after L??
    checkStateAfter();
  }

  /**
   * Test of T??.
   */
  private void test_Txx(int opcode, Consumer<Integer> stateProvider) {
    for (int value = 0x00; value <= 0xFF; value++) {
      // Compute states.
      stateProvider.accept(value);

      // Setup code.
      writeRam(opcode); // T??, NOP

      // load opcode -> PC = 0x0301
      executeOneTick_readPC(expectedState, opcode);
      // idle read -> PC = 0x0301
      executeOneTick_idleRead(expectedState, 0x4C);
      // JMP after T??
      checkStateAfter();
      _clock.run(3 - 1);
    }
  }

  /**
   * Setup.
   * <p/>
   * Creates CPU test environment with 0x1000 bytes of RAM starting ab 0x0000.
   * PC is set to 0x300.
   */
  @Before
  public void setUp() throws Exception {
    logger.debug("set up");

    _clock = new SerialClock();
    _ram = new RAM(0x10000);
    _loggingBus = new LoggingBus(_ram);
    _bus = new WordBus(_loggingBus);
    _cpu = _clock.addClockedComponent(Clock.CPU, new CPU6510());
    _cpu.connect(_bus);

    // Test code starts at $300
    _bus.writeWord(0xFFFC, 0x0300);

    // Execute reset sequence
    _clock.run(3);

    // Store state at start of test.
    startTick = _clock.getTick();
    state = _cpu.getState();
    expectedState = new CPU6510State(0x0300, 0xFF, 0, 0, 0, 0, false, false);
  }

  /**
   * Tear down.
   *
   * @throws Exception
   */
  @After
  public void tearDown() throws Exception {
    logger.debug("tear down");

    _clock.close();
  }

  /**
   * Expected zero flag for value.
   */
  public boolean z(int value) {
    return value == 0;
  }

  /**
   * Expected negative flag for value.
   */
  public boolean n(int value) {
    return (value & 0x80) != 0;
  }

  /**
   * Capture current state as expected state and state after.
   */
  private void captureExpectedState() {
    expectedState = state.copy();
    stateAfter = state.copy();
  }

  /**
   * Writer opcode with arguments to RAM at $0300. Append a JMP $0300. Set PC after to address of JMP.
   */
  private void writeRam(int... values) {
    int address = 0x0300;
    expectedState.PC = address;
    for (int value : values) {
      _ram.write(value, address++);
    }
    stateAfter.PC = address;
    _ram.write(0x4C, address++);
    _ram.write(0x00, address++);
    _ram.write(0x03, address++);
  }

  /**
   * Check state after execution of opcode. Checks that NOP after opcode gets executed.
   */
  private void checkStateAfter() {
    // JMP after opcode.
    executeOneTick_readPC(stateAfter, 0x4C);
  }


  /**
   * Execute one cpu cycle and check for expected cpu state and read of the value from the PC. Increments PC.
   *
   * @param expectedState expected state after execution.
   * @param value value which has been read.
   */
  private void executeOneTick_readPC(CPU6510State expectedState, int value) {
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC++, value));
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and read of the value from the PC. Increments PC.
   *
   * @param expectedState expected state after execution.
   * @param value value which has been read.
   */
  private void executeOneTick_idleRead(CPU6510State expectedState, int value) {
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC, value));
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and read of the value from the given address.
   *
   * @param expectedState expected state after execution.
   * @param address accessed address.
   * @param value value which has been read.
   */
  private void executeOneTick_read(CPU6510State expectedState, int address, int value) {
    executeOneTick(expectedState, new LogEntry(true, address, value));
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and write of the value to the given address.
   *
   * @param expectedState expected state after execution.
   * @param address accessed address.
   * @param value value which has been written.
   */
  private void executeOneTick_write(CPU6510State expectedState, int address, int value) {
    executeOneTick(expectedState, new LogEntry(false, address, value));
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and pop of the value from the stacks. Increments S.
   *
   * @param expectedState expected state after execution.
   * @param value value which has been read.
   */
  private void executeOneTick_pop(CPU6510State expectedState, int value) {
    executeOneTick(expectedState, new LogEntry(true, 0x0100 + ++expectedState.S, value));
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and push of the value onto the stack. Decrements S.
   *
   * @param expectedState expected state after execution.
   * @param value value which has been written.
   */
  private void executeOneTick_push(CPU6510State expectedState, int value) {
    executeOneTick(expectedState, new LogEntry(false, 0x0100 + expectedState.S--, value));
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and the given bus action.
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
