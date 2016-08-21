package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractProgramSuiteTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.serialthreads.agent.TransformingParameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * VICE test suite.
 */
@RunWith(TransformingParameterized.class)
public class CiaCountsTest extends AbstractProgramSuiteTest {
  /**
   * Ignored tests.
   */
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
    "cia-b-counts-a"
  ));

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    return createParameters("/vice-emu-testprogs/CIA/CIA-AcountsB/cia-b-counts-a.prg", IGNORE);
  }

  @Test
  public void test() throws Exception {
    // New CIA and NTSC not implemented yet.
    assertFalse("Not implemented yet", programName.equals("cmp-b-counts-a-new"));
    assertFalse("Not implemented yet", programName.equals("cmp-b-counts-a-new_ntsc"));
    assertFalse("Not implemented yet", programName.equals("cmp-b-counts-a-old_ntsc"));

    testBorderResult(60, false);
  }
}
