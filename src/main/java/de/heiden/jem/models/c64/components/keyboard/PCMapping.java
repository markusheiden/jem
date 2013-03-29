package de.heiden.jem.models.c64.components.keyboard;

import java.awt.event.KeyEvent;

import static de.heiden.jem.models.c64.components.keyboard.Key.*;

/**
 * Key mapping using the default pc keys.
 */
public class PCMapping extends KeyMapping {
  @Override
  protected final void generateKeyMapping() {
    _chars.put(' ', new Key[]{SPACE});

    _chars.put('1', new Key[]{NUMBER_1});
    _chars.put('2', new Key[]{NUMBER_2});
    _chars.put('3', new Key[]{NUMBER_3});
    _chars.put('4', new Key[]{NUMBER_4});
    _chars.put('5', new Key[]{NUMBER_5});
    _chars.put('6', new Key[]{NUMBER_6});
    _chars.put('7', new Key[]{NUMBER_7});
    _chars.put('8', new Key[]{NUMBER_8});
    _chars.put('9', new Key[]{NUMBER_9});
    _chars.put('0', new Key[]{NUMBER_0});

    _chars.put('!', new Key[]{LEFT_SHIFT, NUMBER_1});
    _chars.put('"', new Key[]{LEFT_SHIFT, NUMBER_2});
    _chars.put('#', new Key[]{LEFT_SHIFT, NUMBER_3});
    _chars.put('$', new Key[]{LEFT_SHIFT, NUMBER_4});
    _chars.put('%', new Key[]{LEFT_SHIFT, NUMBER_5});
    _chars.put('&', new Key[]{LEFT_SHIFT, NUMBER_6});
    _chars.put('\'', new Key[]{LEFT_SHIFT, NUMBER_7});
    _chars.put('(', new Key[]{LEFT_SHIFT, NUMBER_8});
    _chars.put(')', new Key[]{LEFT_SHIFT, NUMBER_9});
    _chars.put('+', new Key[]{PLUS});
    _chars.put('-', new Key[]{MINUS});
    // TODO how to map POUND?

    _chars.put('a', new Key[]{A});
    _chars.put('A', new Key[]{LEFT_SHIFT, A});
    _chars.put('b', new Key[]{B});
    _chars.put('B', new Key[]{LEFT_SHIFT, B});
    _chars.put('c', new Key[]{C});
    _chars.put('C', new Key[]{LEFT_SHIFT, C});
    _chars.put('d', new Key[]{D});
    _chars.put('D', new Key[]{LEFT_SHIFT, D});
    _chars.put('e', new Key[]{E});
    _chars.put('E', new Key[]{LEFT_SHIFT, E});
    _chars.put('f', new Key[]{F});
    _chars.put('F', new Key[]{LEFT_SHIFT, F});
    _chars.put('g', new Key[]{G});
    _chars.put('G', new Key[]{LEFT_SHIFT, G});
    _chars.put('h', new Key[]{H});
    _chars.put('H', new Key[]{LEFT_SHIFT, H});
    _chars.put('i', new Key[]{I});
    _chars.put('I', new Key[]{LEFT_SHIFT, I});
    _chars.put('j', new Key[]{J});
    _chars.put('J', new Key[]{LEFT_SHIFT, J});
    _chars.put('k', new Key[]{K});
    _chars.put('K', new Key[]{LEFT_SHIFT, K});
    _chars.put('l', new Key[]{L});
    _chars.put('L', new Key[]{LEFT_SHIFT, L});
    _chars.put('m', new Key[]{M});
    _chars.put('M', new Key[]{LEFT_SHIFT, M});
    _chars.put('n', new Key[]{N});
    _chars.put('N', new Key[]{LEFT_SHIFT, N});
    _chars.put('o', new Key[]{O});
    _chars.put('O', new Key[]{LEFT_SHIFT, O});
    _chars.put('p', new Key[]{P});
    _chars.put('P', new Key[]{LEFT_SHIFT, P});
    _chars.put('q', new Key[]{Q});
    _chars.put('Q', new Key[]{LEFT_SHIFT, Q});
    _chars.put('r', new Key[]{R});
    _chars.put('R', new Key[]{LEFT_SHIFT, R});
    _chars.put('s', new Key[]{S});
    _chars.put('S', new Key[]{LEFT_SHIFT, S});
    _chars.put('t', new Key[]{T});
    _chars.put('T', new Key[]{LEFT_SHIFT, T});
    _chars.put('u', new Key[]{U});
    _chars.put('U', new Key[]{LEFT_SHIFT, U});
    _chars.put('v', new Key[]{V});
    _chars.put('V', new Key[]{LEFT_SHIFT, V});
    _chars.put('w', new Key[]{W});
    _chars.put('W', new Key[]{LEFT_SHIFT, W});
    _chars.put('x', new Key[]{X});
    _chars.put('X', new Key[]{LEFT_SHIFT, X});
    _chars.put('y', new Key[]{Y});
    _chars.put('Y', new Key[]{LEFT_SHIFT, Y});
    _chars.put('z', new Key[]{Z});
    _chars.put('Z', new Key[]{LEFT_SHIFT, Z});

    _chars.put('.', new Key[]{PERIOD});
    _chars.put(':', new Key[]{COLON});
    _chars.put('<', new Key[]{LEFT_SHIFT, COMMA});
    _chars.put('>', new Key[]{LEFT_SHIFT, PERIOD});
    _chars.put(',', new Key[]{COMMA});
    _chars.put(';', new Key[]{SEMICOLON});
    _chars.put('[', new Key[]{LEFT_SHIFT, COLON});
    _chars.put(']', new Key[]{LEFT_SHIFT, SEMICOLON});
    _chars.put('=', new Key[]{EQUALS});
    _chars.put('/', new Key[]{SLASH});
    _chars.put('?', new Key[]{LEFT_SHIFT, SLASH});

    _chars.put('@', new Key[]{AT});
    _chars.put('*', new Key[]{ASTERISK});
    // TODO how to map UP_ARROW?
    _chars.put('^', new Key[]{UP_ARROW});
    _chars.put('\n', new Key[]{ENTER});

    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_ENTER), new Key[]{ENTER});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_BACK_SPACE), new Key[]{DEL});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_INSERT), new Key[]{LEFT_SHIFT, DEL});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_HOME), new Key[]{HOME});

    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_UP), new Key[]{RIGHT_SHIFT, DOWN});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_DOWN), new Key[]{DOWN});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_LEFT), new Key[]{RIGHT_SHIFT, RIGHT});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_RIGHT), new Key[]{RIGHT});

    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F1), new Key[]{F1});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F2), new Key[]{LEFT_SHIFT, F1});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F3), new Key[]{F3});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F4), new Key[]{LEFT_SHIFT, F3});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F5), new Key[]{F5});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F6), new Key[]{LEFT_SHIFT, F5});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F7), new Key[]{F7});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F8), new Key[]{LEFT_SHIFT, F7});

    // Shift is disabled, because the above chars/keys already contain shift
    // _keys.put(keyID(KeyEvent.KEY_LOCATION_LEFT, KeyEvent.VK_SHIFT), new Key[]{LEFT_SHIFT});
    // _keys.put(keyID(KeyEvent.KEY_LOCATION_RIGHT, KeyEvent.VK_SHIFT), new Key[]{RIGHT_SHIFT});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_CONTROL), new Key[]{CTRL});
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_ALT), new Key[]{COMMODORE});

    // run stop -> esc
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_ESCAPE), new Key[]{RUN_STOP});
    // restore -> delete
    _keys.put(keyID(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_DELETE), new Key[]{RESTORE});

    // TODO how to map LEFT_ARROW
  }
}
