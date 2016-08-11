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

import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import static de.heiden.c64dt.util.ByteUtil.hi;
import static de.heiden.c64dt.util.ByteUtil.lo;
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

  private static final Random random = new Random();

  private Clock _clock;
  private RAM _ram;
  private LoggingBus _loggingBus;
  private WordBus _bus;
  private CPU6510 _cpu;

  private long startTick;
  CPU6510State state;
  CPU6510State expectedState;

  /**
   * Test port.
   */
  @Test
  public void testPort() {
    _cpu.writePort(0x2F, 0x00);
    _cpu.writePort(0x34, 0x01);
    assertEquals(0xF4, _cpu.getPort().outputData());
    _cpu.writePort(0x2E, 0x00);
    _cpu.writePort(0x34, 0x01);
    assertEquals(0xF5, _cpu.getPort().outputData());
    _cpu.writePort(0x2F, 0x00);
    _cpu.writePort(0x35, 0x01);
    assertEquals(0xF5, _cpu.getPort().outputData());
  }

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
   * Test opcode 0x05: ORA $xx.
   */
  @Test
  public void test0x05() {
    test_xxA_ZP(0x05, (a, value) -> a | value);
  }

  /**
   * Test opcode 0x08: PHP.
   */
  @Test
  public void test0x08() {
    // TODO 2015-12-13 markus: Is this behaviour correct or are other flags changed too?
    test_PHx(0x08, CPU6510State::setP, value -> value | 0x30);
  }

  /**
   * Test opcode 0x09: ORA #$xx.
   */
  @Test
  public void test0x09() {
    test_xxA_IMM(0x09, (a, value) -> a | value);
  }

  /**
   * Test opcode 0x0D: ORA $xxxx.
   */
  @Test
  public void test0x0D() {
    test_xxA_ABS(0x0D, (a, value) -> a | value);
  }

  /**
   * Test opcode 0x25: AND $xx.
   */
  @Test
  public void test0x25() {
    test_xxA_ZP(0x25, (a, value) -> a & value);
  }

  /**
   * Test opcode 0x29: AND #$xx.
   */
  @Test
  public void test0x29() {
    test_xxA_IMM(0x29, (a, value) -> a & value);
  }

  /**
   * Test opcode 0x2D: AND $xxxx.
   */
  @Test
  public void test0x2D() {
    test_xxA_ABS(0x2D, (a, value) -> a & value);
  }

  /**
   * Test opcode 0x45: EOR $xx.
   */
  @Test
  public void test0x45() {
    test_xxA_ZP(0x45, (a, value) -> a ^ value);
  }

  /**
   * Test opcode 0x48: PHA.
   */
  @Test
  public void test0x48() {
    test_PHx(0x48, CPU6510State::setA, null);
  }

  /**
   * Test opcode 0x49: EOR #$xx.
   */
  @Test
  public void test0x49() {
    test_xxA_IMM(0x49, (a, value) -> a ^ value);
  }

  /**
   * Test opcode 0x4D: EOR $xxxx.
   */
  @Test
  public void test0x4D() {
    test_xxA_ABS(0x4D, (a, value) -> a ^ value);
  }

  /**
   * Test opcode 0x84: STY $xx.
   */
  @Test
  public void test0x84() {
    test_STx_ZP(0x84, CPU6510State::setY);
  }

  /**
   * Test opcode 0x85: STA $xx.
   */
  @Test
  public void test0x85() {
    test_STx_ZP(0x85, CPU6510State::setA);
  }

  /**
   * Test opcode 0x86: STX $xx.
   */
  @Test
  public void test0x86() {
    test_STx_ZP(0x86, CPU6510State::setX);
  }

  /**
   * Test opcode 0x88: DEX.
   */
  @Test
  public void test0x88() {
    test_increment(0x88, CPU6510State::setY, -1);
  }

  /**
   * Test opcode 0x8A: TXA.
   */
  @Test
  public void test0x8A() {
    test_Txx(0x8A, CPU6510State::setX, CPU6510State::setA, true);
  }

  /**
   * Test opcode 0x8C: STY $xxxx.
   */
  @Test
  public void test0x8C() {
    test_STx_ABS(0x8C, CPU6510State::setY);
  }

  /**
   * Test opcode 0x8D: STA $xxxx.
   */
  @Test
  public void test0x8D() {
    test_STx_ABS(0x8D, CPU6510State::setA);
  }

  /**
   * Test opcode 0x8E: STX $xxxx.
   */
  @Test
  public void test0x8E() {
    test_STx_ABS(0x8E, CPU6510State::setX);
  }

  /**
   * Test opcode 0x98: TYA.
   */
  @Test
  public void test0x98() {
    test_Txx(0x98, CPU6510State::setY, CPU6510State::setA, true);
  }

  /**
   * Test opcode 0x9A: TXS.
   */
  @Test
  public void test0x9A() {
    test_Txx(0x9A, CPU6510State::setX, CPU6510State::setS, false);
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
    test_Txx(0xA8, CPU6510State::setA, CPU6510State::setY, true);
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
    test_Txx(0xAA, CPU6510State::setA, CPU6510State::setX, true);
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
   * Test opcode 0xB9: LDA $xxxx,Y.
   */
  @Test
  public void test0xB9() {
    test_LDx_ABx(0xB9, CPU6510State::setA, CPU6510State::setY);
  }

  /**
   * Test opcode 0xBA: TSX.
   */
  @Test
  public void test0xBA() {
    test_Txx(0xBA, CPU6510State::setS, CPU6510State::setX, true);
  }

  /**
   * Test opcode 0xBC: LDY $xxxx,X.
   */
  @Test
  public void test0xBC() {
    test_LDx_ABx(0xBC, CPU6510State::setY, CPU6510State::setX);
  }

  /**
   * Test opcode 0xBD: LDA $xxxx,X.
   */
  @Test
  public void test0xBD() {
    test_LDx_ABx(0xBD, CPU6510State::setA, CPU6510State::setX);
  }

  /**
   * Test opcode 0xBE: LDX $xxxx,Y.
   */
  @Test
  public void test0xBE() {
    test_LDx_ABx(0xBE, CPU6510State::setX, CPU6510State::setY);
  }

  /**
   * Test opcode 0xC8: INY.
   */
  @Test
  public void test0xC8() {
    test_increment(0xC8, CPU6510State::setY, 1);
  }

  /**
   * Test opcode 0xCA: DEX.
   */
  @Test
  public void test0xCA() {
    test_increment(0xCA, CPU6510State::setX, -1);
  }

  /**
   * Test opcode 0xE8: INX.
   */
  @Test
  public void test0xE8() {
    test_increment(0xE8, CPU6510State::setX, 1);
  }

  //
  //
  //

  /**
   * Test of ??A #$xx.
   */
  private void test_xxA_IMM(int opcode, BiFunction<Integer, Integer, Integer> operator) {
    for (int a = 0x00; a <= 0xFF; a = inc(a)) {
      for (int value = 0x00; value <= 0xFF; value = inc(value)) {
        resetState(value);
        state.A = a;
        captureExpectedState();

        execute(opcode, value); // xxA #$xx, JMP

        // register and flags change
        int result = operator.apply(a, value);
        expectedState.A = result;
        expectedZN(result);
        // Check state and jump back
        checkAndJmpBack();
      }
    }
  }

  /**
   * Test of ??A $xx.
   */
  private void test_xxA_ZP(int opcode, BiFunction<Integer, Integer, Integer> operator) {
    for (int a = 0x00; a <= 0xFF; a = inc(a)) {
      for (int value = 0x00; value <= 0xFF; value = inc(value)) {
        resetState(value);
        state.A = a;
        captureExpectedState();

        _ram.write(value, 0x00FF);
        execute(opcode, 0xFF); // xxA $xx, JMP

        // load value from address
        executeOneTick_read(expectedState, 0x00FF, value);
        // register and flags change
        int result = operator.apply(a, value);
        expectedState.A = result;
        expectedZN(result);
        // Check state and jump back
        checkAndJmpBack();
      }
    }
  }

  /**
   * Test of ??A $xxxx.
   */
  private void test_xxA_ABS(int opcode, BiFunction<Integer, Integer, Integer> operator) {
    for (int a = 0x00; a <= 0xFF; a = inc(a)) {
      for (int value = 0x00; value <= 0xFF; value = inc(value)) {
        resetState(value);
        state.A = a;
        captureExpectedState();

        _ram.write(value, 0x00FF);
        execute(opcode, 0xFF, 0x00); // xxA $xxxx, JMP

        // load value from address
        executeOneTick_read(expectedState, 0x00FF, value);
        // register and flags change
        int result = operator.apply(a, value);
        expectedState.A = result;
        expectedZN(result);
        // Check state and jump back
        checkAndJmpBack();
      }
    }
  }

  /**
   * Test of IN?/DE?.
   */
  private void test_increment(int opcode, SetRegister destination, int increment) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      destination.set(state, value);
      captureExpectedState();

      execute(opcode); // IN?/DE?, JMP

      // register and flags change
      int result = (value + increment) & 0xFF;
      destination.set(expectedState, result);
      expectedZN(result);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of LD? #$xx.
   */
  private void test_LDx_IMM(int opcode, SetRegister destination) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      captureExpectedState();

      execute(opcode, value); // LD? #$xx, JMP

      // register and flags change
      destination.set(expectedState, value);
      expectedZN(value);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of LD? $xx.
   */
  private void test_LDx_ZP(int opcode, SetRegister destination) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      captureExpectedState();

      _ram.write(value, 0x00FF);
      execute(opcode, 0xFF); // LD? $FF, JMP

      // load value from zp address
      executeOneTick_read(expectedState, 0x00FF, value);
      // register and flags change
      destination.set(expectedState, value);
      expectedZN(value);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of LD? $xxxx.
   */
  private void test_LDx_ABS(int opcode, SetRegister destination) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      captureExpectedState();

      _ram.write(value, 0x00FF);
      execute(opcode, 0xFF, 0x00); // LD? $00FF, JMP

      // load value from address
      executeOneTick_read(expectedState, 0x00FF, value);
      // register and flags change
      destination.set(expectedState, value);
      expectedZN(value);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of LD? $xxxx,X.
   */
  private void test_LDx_ABx(int opcode, SetRegister destination, SetRegister index) {
    for (int i = 0x00; i <= 0xFF; i = inc(i)) {
      for (int value = 0x00; value <= 0xFF; value = inc(value)) {
        resetState(value);
        index.set(state, i);
        captureExpectedState();

        int address = 0x1080;
        int indexedAddress = address + i;
        _ram.write(value, indexedAddress);
        execute(opcode, lo(address), hi(address)); // LD? $xxxx,? JMP

        // idle read
        // TODO test idle read
        // TODO test extra cycle, if crossing page boundary
        _clock.run(1);
        // load byte from address
        executeOneTick_read(expectedState, indexedAddress, value);
        // register and flags change
        destination.set(expectedState, value);
        expectedZN(value);
        // Check state and jump back
        checkAndJmpBack();
      }
    }
  }

  /**
   * Test of PH?.
   */
  private void test_PHx(int opcode, SetRegister source, Function<Integer, Integer> modifier) {
    // 256 values -> stack overflow will be tested too
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      source.set(state, value);
      captureExpectedState();

      execute(opcode); // PHx, JMP

      // write to stack, decrement S
      int expectedValue = modifier != null? modifier.apply(value) : value;
      executeOneTick_push(expectedState, expectedValue);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of ST? $xx.
   */
  private void test_STx_ZP(int opcode, SetRegister source) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      source.set(state, value);
      captureExpectedState();

      execute(opcode, 0xFF); // ST? $FF, JMP

      // write byte to zp address
      executeOneTick_write(expectedState, 0x00FF, value);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of ST? $xxxx.
   */
  private void test_STx_ABS(int opcode, SetRegister source) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      source.set(state, value);
      captureExpectedState();

      execute(opcode, 0xFF, 0x00); // ST? $00FF, JMP

      // write byte to zp address
      executeOneTick_write(expectedState, 0x00FF, value);
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  /**
   * Test of T??.
   */
  private void test_Txx(int opcode, SetRegister source, SetRegister destination, boolean updateP) {
    for (int value = 0x00; value <= 0xFF; value = inc(value)) {
      resetState(value);
      source.set(state, value);
      captureExpectedState();

      // Setup code.
      execute(opcode); // T??, JMP

      // register and flags change
      destination.set(expectedState, value);
      if (updateP) {
        expectedZN(value);
      }
      // Check state and jump back
      checkAndJmpBack();
    }
  }

  //
  //
  //

  public static class TXA_Test extends ValueTest {
    public TXA_Test() {
      super(0x8A);
    }

    @Override
    public void before(int value) {
      state.X = value;
    }
    @Override
    public void after(int value) {
      expectedState.A = value;
      expectedZN(value);
    }
  }

  public static abstract class ValueTest extends CPU6510Test {
    private int opcode;
    private int[] arguments;

    public ValueTest(int opcode, int... arguments) {
      this.opcode = opcode;
      this.arguments = arguments;
    }

    public abstract void before(int value);

    @Test
    public void test() {
      for (int value = 0x00; value <= 0xFF; value = inc(value)) {
        resetState(value);
        before(value);
        captureExpectedState();
        // Setup code.
        execute(opcode, arguments); // opcode, JMP
        // register and flags change
        after(value);
        // Check state and jump back
        checkAndJmpBack();
      }
    }

    public abstract void after(int value);
  }


  //
  // Setup and helper
  //

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
   * Increment value by a random value between 1 and 4.
   * Reduces the number different values to reduce test time.
   */
  int inc(int value) {
    return value + 1 + random.nextInt(4);
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
  private boolean z(int value) {
    return value == 0;
  }

  /**
   * Expected negative flag for value.
   */
  private boolean n(int value) {
    return (value & 0x80) != 0;
  }

  /**
   * Reset state.
   */
  void resetState(int value){
    // Set register which gets loaded to a different value.
    state.A = value ^ 0xFF;
    state.X = value ^ 0xFF;
    state.Y = value ^ 0xFF;
    // Set status which get changed to a different value.
    state.Z = !z(value);
    state.N = !n(value);
  }

  /**
   * Capture current state as expected state and state after.
   */
  void captureExpectedState() {
    expectedState = state.copy();
  }

  /**
   * Write opcode with arguments to RAM at $0300.
   * Append a JMP $0300.
   * Execute reading of opcode and its argument.
   */
  void execute(int opcode, int... arguments) {
    int address = 0x0300;
    expectedState.PC = address;

    _ram.write(opcode, address++);
    for (int argument : arguments) {
      _ram.write(argument, address++);
    }
    _ram.write(0x4C, address++);
    _ram.write(0x00, address++);
    _ram.write(0x03, address++);

    // execute opcode
    executeOneTick_readPC(expectedState, opcode);
    // execute read argument
    if (arguments.length == 0) {
      // No arguments -> Idle read instead of argument read.
      executeOneTick_idleRead(expectedState);
    } else {
      for (int argument : arguments) {
        executeOneTick_readPC(expectedState, argument);
      }
    }
  }

  /**
   * Expect Z and N flag to be changed according to the given value.
   */
  void expectedZN(int value) {
    expectedState.Z = z(value);
    expectedState.N = n(value);
  }

  /**
   * Check state and execute JMP.
   */
  void checkAndJmpBack() {
    // JMP after opcode.
    executeOneTick_readPC(expectedState, 0x4C);
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
   */
  private void executeOneTick_idleRead(CPU6510State expectedState) {
    executeOneTick(expectedState, new LogEntry(true, expectedState.PC, _ram.read(expectedState.PC)));
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
    expectedState.S = (expectedState.S + 1) & 0xFF;
    LogEntry log = new LogEntry(true, 0x0100 + expectedState.S, value);
    executeOneTick(expectedState, log);
  }

  /**
   * Execute one cpu cycle and check for expected cpu state and push of the value onto the stack. Decrements S.
   *
   * @param expectedState expected state after execution.
   * @param value value which has been written.
   */
  private void executeOneTick_push(CPU6510State expectedState, int value) {
    LogEntry log = new LogEntry(false, 0x0100 + expectedState.S, value);
    expectedState.S = (expectedState.S - 1) & 0xFF;
    executeOneTick(expectedState, log);
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
