package de.heiden.jem.models.c64.vice.cpu.acid800;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.serialthreads.agent.TransformingRunner;

import static org.junit.Assert.assertSame;

/**
 * VICE test suite.
 */
@RunWith(TransformingRunner.class)
public class Acid800Test extends AbstractTest {
  private final Condition passed = inMemory(0xd7ff, 0x00);
  private final Condition failed = inMemory(0xd7ff, 0xff);

  @Test
  public void cpuDecimal() throws Exception {
    loadAndRun("/vice/cpu/acid800/cpu-decimal.prg");

    Condition result = waitSecondsFor(1, passed, failed);
    System.out.println(captureScreen());

    assertSame(passed, result);
  }

  @Test
  public void cpuInsn() throws Exception {
    loadAndRun("/vice/cpu/acid800/cpu-insn.prg");

    Condition result = waitSecondsFor(10, passed, failed);
    System.out.println(captureScreen());

    assertSame(passed, result);
  }
}