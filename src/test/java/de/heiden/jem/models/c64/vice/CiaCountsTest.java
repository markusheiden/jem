package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.Condition;
import de.heiden.jem.models.c64.components.patch.LoadFromDirectory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.serialthreads.agent.TransformingRunner;

import java.nio.file.Path;

import static org.junit.Assert.assertSame;

/**
 * VICE test suite.
 */
@RunWith(TransformingRunner.class)
public class CiaCountsTest extends AbstractTest {
  @Test
  public void ciaCounts() throws Exception {
    createC64("ciaCounts");
    Path directory = path("/vice-emu-testprogs/CIA/CIA-AcountsB/cia-b-counts-a.prg").getParent();
    c64.add(new LoadFromDirectory(directory));

    // Load program.
    load("dump-oldcia.bin", 8, 1);
    load("cia-b-counts-a", 8);
    run();

    Condition passed = greenBorder;
    Condition failed = lightRedBorder;
    assertSame(passed, waitSecondsFor(60, passed, failed));
  }
}
