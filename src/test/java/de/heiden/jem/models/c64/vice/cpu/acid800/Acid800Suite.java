package de.heiden.jem.models.c64.vice.cpu.acid800;

import de.heiden.jem.models.c64.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.serialthreads.agent.TransformingRunner;

import static org.junit.Assert.assertEquals;

/**
 * VICE test suite.
 */
@RunWith(TransformingRunner.class)
public class Acid800Suite extends AbstractTest {
  @Test
  public void cpuDecimal() throws Exception {
    loadAndRun("/vice/cpu/acid800/cpu-decimal.prg");

    // Just run for 2 seconds, because test program does never stop.
    int seconds = 100;
    int result = waitFor(seconds * 1000000);

    String console = captureScreen();
    System.out.println(console);
    assertEquals(WAIT_PROGRAM_END, result);
  }
}
