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

import java.util.function.BiConsumer;

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
    test_Txx(0x8A, CPU6510State::setX, CPU6510State::setA);
  }

  /**
   * Test opcode 0x98: TYA.
   */
  @Test
  public void test0x98() {
    test_Txx(0x98, CPU6510State::setY, CPU6510State::setA);
  }

  /**
   * Test opcode 0xA0: LDY #$xx.
   */
  @Test
  public void test0xA0() {
    test_LDx_IMM(0xA0, CPU6510State::setY);
  }

  /**
   * Test opcode 0xA2: LDX #$xx.
   */
  @Test
  public void test0xA2() {
    test_LDx_IMM(0xA2, CPU6510State::setX);
  }

  /**
   * Test opcode 0xA4: LDY $xx.
   */
  @Test
  public void test0xA4() {
    test_LDx_ZP(0xA4, CPU6510State::setY);
  }

  /**
   * Test opcode 0xA5: LDA $xx.
   */
  @Test
  public void test0xA5() {
    test_LDx_ZP(0xA5, CPU6510State::setA);
  }

  /**
   * Test opcode 0xA6: LDX $xx.
   */
  @Test
  public void test0xA6() {
    test_LDx_ZP(0xA6, CPU6510State::setX);
  }

  /**
   * Test opcode 0xA8: TAY.
   */
  @Test
  public void test0xA8() {
    test_Txx(0xA8, CPU6510State::setA, CPU6510State::setY);
  }

  /**
   * Test opcode 0xA9: LDA #$xx.
   */
  @Test
  public void test0xA9() {
    test_LDx_IMM(0xA9, CPU6510State::setA);
  }

  /**
   * Test opcode 0xAA: TAX.
   */
  @Test
  public void test0xAA() {
    test_Txx(0xAA, CPU6510State::setA, CPU6510State::setX);
  }

  /**
   * Test opcode 0xAD: LDA $xxxx.
   */
  @Test
  public void test0xAD() {
    test_LDx_ABS(0xAD, CPU6510State::setA);
  }

  /**
   * Test opcode 0xAE: LDX $xxxx.
   */
  @Test
  public void test0xAE() {
    test_LDx_ABS(0xAE, CPU6510State::setX);
  }

  /**
   * Test opcode 0xAC: LDY $xxxx.
   */
  @Test
  public void test0xAC() {
    test_LDx_ABS(0xAC, CPU6510State::setY);
  }

  /**
   * Test of LD? #$xx.
   */
  private void test_LDx_IMM(int opcode, BiConsumer<CPU6510State, Integer> destination) {
    for (int value = 0x00; value <= 0xFF; value++) {
      // Set register which gets loaded to a different value.
      state.A = value ^ 0xFF;
      state.X = value ^ 0xFF;
      state.Y = value ^ 0xFF;
      // Set status which get changed to a different value.
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      destination.accept(stateAfter, value);
      stateAfter.Z = z(value);
      stateAfter.N = n(value);

      writeRam(opcode, value); // LD? #$xx, JMP

      // load opcode
      executeOneTick_readPC(expectedState, opcode);
      // load byte after opcode
      executeOneTick_readPC(expectedState, value);
      // NOP after L??
      checkStateAfter();
    }
  }

  /**
   * Test of LD? $xx.
   */
  private void test_LDx_ZP(int opcode, BiConsumer<CPU6510State, Integer> destination) {
    for (int value = 0x00; value <= 0xFF; value++) {
      // Set register which gets loaded to a different value.
      state.A = value ^ 0xFF;
      state.X = value ^ 0xFF;
      state.Y = value ^ 0xFF;
      // Set status which get changed to a different value.
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      destination.accept(stateAfter, value);
      stateAfter.Z = z(value);
      stateAfter.N = n(value);

      writeRam(opcode, 0xFF); // LD? $FF, JMP
      _ram.write(value, 0xFF);

      // load opcode
      executeOneTick_readPC(expectedState, opcode);
      // load zp address
      executeOneTick_readPC(expectedState, 0xFF);
      // load byte from zp address
      executeOneTick_read(expectedState, 0xFF, value);
      // NOP after L??
      checkStateAfter();
    }
  }

  /**
   * Test of LD? $xxxx.
   */
  private void test_LDx_ABS(int opcode, BiConsumer<CPU6510State, Integer> destination) {
    for (int value = 0x00; value <= 0xFF; value++) {
      // Set register which gets loaded to a different value.
      state.A = value ^ 0xFF;
      state.X = value ^ 0xFF;
      state.Y = value ^ 0xFF;
      // Set status which get changed to a different value.
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      destination.accept(stateAfter, value);
      stateAfter.Z = z(value);
      stateAfter.N = n(value);

      writeRam(opcode, 0xFF, 0x00); // LD? $00FF, JMP
      _ram.write(value, 0xFF);

      // load opcode
      executeOneTick_readPC(expectedState, opcode);
      // load low nibble of address
      executeOneTick_readPC(expectedState, 0xFF);
      // load high nibble of address
      executeOneTick_readPC(expectedState, 0x00);
      // load byte from address
      executeOneTick_read(expectedState, 0x00FF, value);
      // NOP after L??
      checkStateAfter();
    }
  }

  /**
   * Test of T??.
   */
  private void test_Txx(int opcode, BiConsumer<CPU6510State, Integer> source, BiConsumer<CPU6510State, Integer> destination) {
    for (int value = 0x00; value <= 0xFF; value++) {
      // Set register which gets loaded to a different value.
      state.A = value ^ 0xFF;
      state.X = value ^ 0xFF;
      state.Y = value ^ 0xFF;
      source.accept(state, value);
      // Set status which get changed to a different value.
      state.Z = !z(value);
      state.N = !n(value);
      captureExpectedState();
      destination.accept(stateAfter, value);
      stateAfter.Z = z(value);
      stateAfter.N = n(value);

      // Setup code.
      writeRam(opcode); // T??, JMP

      // load opcode -> PC = 0x0301
      executeOneTick_readPC(expectedState, opcode);
      // idle read -> PC = 0x0301
      executeOneTick_idleRead(expectedState, 0x4C);
      // JMP after T??
      checkStateAfter();
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
    _clock.run(3 - 1);
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
