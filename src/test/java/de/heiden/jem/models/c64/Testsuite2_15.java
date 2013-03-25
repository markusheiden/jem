package de.heiden.jem.models.c64;

import org.junit.Test;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;

/**
 * Testsuite 2.15.
 */
public class Testsuite2_15 {
  @Test
  public void test() throws Exception {
    ClassLoader classLoader = new TransformingClassLoader(Startup.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");

    Class<?> clazz = classLoader.loadClass("de.heiden.jem.models.c64.TestC64");
    Object c64 = clazz.getConstructor().newInstance();
    c64.getClass().getDeclaredMethod("start").invoke(c64);
  }
}
