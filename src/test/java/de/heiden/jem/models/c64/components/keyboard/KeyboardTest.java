package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.ports.InputOutputPortImpl;
import junit.framework.TestCase;

import java.awt.event.KeyEvent;

/**
 * Test.
 */
public class KeyboardTest extends TestCase
{
  private InputOutputPortImpl _port0;
  private InputOutputPortImpl _port1;
  private Keyboard _keyboard;

  /**
   * Test single key presses.
   */
  public void testSingleKey()
  {
    KeyEvent a = new KeyEvent(null, 0, 0, 0, KeyEvent.VK_A, 'a', KeyEvent.KEY_LOCATION_STANDARD);
    _keyboard.keyPressed(a);
    _port1.setOutputData(0x00); // port 1 is input
    _port1.setOutputMask(0x00);
    _port0.setOutputData(0x00); // port 0 drives all bits 0
    _port0.setOutputMask(0xFF);
    assertEquals(0xFB, _port1.inputData()); // row 2 is driven to 0
    assertEquals(0x04, _port1.inputMask());
    _port0.setOutputData(0xFF); // port 0 drives all bits 1
    assertEquals(0xFF, _port1.inputData()); // row 2 is driven to 1
    assertEquals(0x04, _port1.inputMask());
    _port0.setOutputData(0x00); // port 0 is input
    _port0.setOutputMask(0x00);
    _port1.setOutputData(0x00); // port 1 drives all bits 0
    _port1.setOutputMask(0xFF);
    assertEquals(0xFD, _port0.inputData()); // row 1 is driven to 0
    assertEquals(0x02, _port0.inputMask());
    _port1.setOutputData(0xFF); // port 1 drives all bits 1
    assertEquals(0xFF, _port0.inputData()); // row 1 is driven to 1
    assertEquals(0x02, _port0.inputMask());
    _keyboard.keyReleased(a);

    KeyEvent b = new KeyEvent(null, 0, 0, 0, KeyEvent.VK_B, 'b', KeyEvent.KEY_LOCATION_STANDARD);
    _keyboard.keyPressed(b);
    _port1.setOutputData(0x00);
    _port1.setOutputMask(0x00);
    _port0.setOutputData(0x00);
    _port0.setOutputMask(0xFF);
    assertEquals(0xEF, _port1.inputData());
    assertEquals(0x10, _port1.inputMask());
    _port0.setOutputData(0xFF);
    assertEquals(0xFF, _port1.inputData());
    assertEquals(0x10, _port1.inputMask());
    _port0.setOutputData(0x00);
    _port0.setOutputMask(0x00);
    _port1.setOutputData(0x00);
    _port1.setOutputMask(0xFF);
    assertEquals(0xF7, _port0.inputData());
    assertEquals(0x08, _port0.inputMask());
    _port1.setOutputData(0xFF);
    assertEquals(0xFF, _port0.inputData());
    assertEquals(0x08, _port0.inputMask());
    _keyboard.keyReleased(b);
  }

  /**
   * Test double key presses one on column or row.
   */
  public void testDoubleKey()
  {
    // TODO not implemented yet...
/*
    _keyboard.pressed(KeyEvent.VK_2, KeyEvent.KEY_LOCATION_STANDARD);
    _keyboard.pressed(KeyEvent.VK_0, KeyEvent.KEY_LOCATION_STANDARD);
    _port0.setOutputData(0x00);
    _port0.setOutputMask(0xEF);
    _port1.setOutputData(0x00);
    _port1.setOutputMask(0x00);
    assertEquals(0xEF, _port0.inputData());
    assertEquals(0x10, _port0.inputMask());
    assertEquals(0xF7, _port1.inputData());
    assertEquals(0x08, _port1.inputMask());
    _port0.setOutputData(0x00);
    _port0.setOutputMask(0x00);
    _port1.setOutputData(0x00);
    _port1.setOutputMask(0xFF);
    assertEquals(0x6F, _port0.inputData());
    assertEquals(0x90, _port0.inputMask());
    _keyboard.released(KeyEvent.VK_0, KeyEvent.KEY_LOCATION_STANDARD);
    _keyboard.released(KeyEvent.VK_2, KeyEvent.KEY_LOCATION_STANDARD);
*/
  }

  //
  // protected interface
  //

  /**
   * Setup.
   */
  protected void setUp() throws Exception
  {
    super.setUp();
    // TODO create port instances
    _port0 = new InputOutputPortImpl();
    _port1 = new InputOutputPortImpl();
    _keyboard = new Keyboard(_port0, _port1);
  }
}
