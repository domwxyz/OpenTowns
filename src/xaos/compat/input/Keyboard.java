package xaos.compat.input;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * Drop-in replacement for org.lwjgl.input.Keyboard backed by GLFW (LWJGL 3).
 *
 * The public API, the KEY_* constant values and the getKeyName() strings are
 * kept identical to LWJGL 2: towns.ini keybinds are stored through
 * getKeyName()/getKeyIndex() and saved configs must keep working. GLFW
 * keycodes never escape this class; they are translated to LWJGL 2 codes at
 * the callback boundary.
 *
 * Text input is the one deliberate improvement over LWJGL 2: event characters
 * come from the GLFW char callback, which respects the OS keyboard layout,
 * instead of LWJGL 2's raw-scancode-to-QWERTY mapping.
 *
 * Lifecycle: the Display shim owns the GLFW window and must call
 * install(window) once after creating it. Repeat events are not delivered,
 * matching the LWJGL 2 default (the game never enables them).
 *
 * Not thread-safe: callbacks fire inside glfwPollEvents() on the main thread,
 * the same thread that runs the game loop, as in LWJGL 2.
 */
public final class Keyboard {

    public static final char CHAR_NONE = '\0';

    // LWJGL 2 keycodes (DirectInput scancodes). Values are persisted in
    // towns.ini through getKeyName()/getKeyIndex() — do not renumber.
    public static final int KEY_NONE = 0x00;
    public static final int KEY_ESCAPE = 0x01;
    public static final int KEY_1 = 0x02;
    public static final int KEY_2 = 0x03;
    public static final int KEY_3 = 0x04;
    public static final int KEY_4 = 0x05;
    public static final int KEY_5 = 0x06;
    public static final int KEY_6 = 0x07;
    public static final int KEY_7 = 0x08;
    public static final int KEY_8 = 0x09;
    public static final int KEY_9 = 0x0A;
    public static final int KEY_0 = 0x0B;
    public static final int KEY_MINUS = 0x0C;
    public static final int KEY_EQUALS = 0x0D;
    public static final int KEY_BACK = 0x0E;
    public static final int KEY_TAB = 0x0F;
    public static final int KEY_Q = 0x10;
    public static final int KEY_W = 0x11;
    public static final int KEY_E = 0x12;
    public static final int KEY_R = 0x13;
    public static final int KEY_T = 0x14;
    public static final int KEY_Y = 0x15;
    public static final int KEY_U = 0x16;
    public static final int KEY_I = 0x17;
    public static final int KEY_O = 0x18;
    public static final int KEY_P = 0x19;
    public static final int KEY_LBRACKET = 0x1A;
    public static final int KEY_RBRACKET = 0x1B;
    public static final int KEY_RETURN = 0x1C;
    public static final int KEY_LCONTROL = 0x1D;
    public static final int KEY_A = 0x1E;
    public static final int KEY_S = 0x1F;
    public static final int KEY_D = 0x20;
    public static final int KEY_F = 0x21;
    public static final int KEY_G = 0x22;
    public static final int KEY_H = 0x23;
    public static final int KEY_J = 0x24;
    public static final int KEY_K = 0x25;
    public static final int KEY_L = 0x26;
    public static final int KEY_SEMICOLON = 0x27;
    public static final int KEY_APOSTROPHE = 0x28;
    public static final int KEY_GRAVE = 0x29;
    public static final int KEY_LSHIFT = 0x2A;
    public static final int KEY_BACKSLASH = 0x2B;
    public static final int KEY_Z = 0x2C;
    public static final int KEY_X = 0x2D;
    public static final int KEY_C = 0x2E;
    public static final int KEY_V = 0x2F;
    public static final int KEY_B = 0x30;
    public static final int KEY_N = 0x31;
    public static final int KEY_M = 0x32;
    public static final int KEY_COMMA = 0x33;
    public static final int KEY_PERIOD = 0x34;
    public static final int KEY_SLASH = 0x35;
    public static final int KEY_RSHIFT = 0x36;
    public static final int KEY_MULTIPLY = 0x37;
    public static final int KEY_LMENU = 0x38;
    public static final int KEY_SPACE = 0x39;
    public static final int KEY_CAPITAL = 0x3A;
    public static final int KEY_F1 = 0x3B;
    public static final int KEY_F2 = 0x3C;
    public static final int KEY_F3 = 0x3D;
    public static final int KEY_F4 = 0x3E;
    public static final int KEY_F5 = 0x3F;
    public static final int KEY_F6 = 0x40;
    public static final int KEY_F7 = 0x41;
    public static final int KEY_F8 = 0x42;
    public static final int KEY_F9 = 0x43;
    public static final int KEY_F10 = 0x44;
    public static final int KEY_NUMLOCK = 0x45;
    public static final int KEY_SCROLL = 0x46;
    public static final int KEY_NUMPAD7 = 0x47;
    public static final int KEY_NUMPAD8 = 0x48;
    public static final int KEY_NUMPAD9 = 0x49;
    public static final int KEY_SUBTRACT = 0x4A;
    public static final int KEY_NUMPAD4 = 0x4B;
    public static final int KEY_NUMPAD5 = 0x4C;
    public static final int KEY_NUMPAD6 = 0x4D;
    public static final int KEY_ADD = 0x4E;
    public static final int KEY_NUMPAD1 = 0x4F;
    public static final int KEY_NUMPAD2 = 0x50;
    public static final int KEY_NUMPAD3 = 0x51;
    public static final int KEY_NUMPAD0 = 0x52;
    public static final int KEY_DECIMAL = 0x53;
    public static final int KEY_F11 = 0x57;
    public static final int KEY_F12 = 0x58;
    public static final int KEY_F13 = 0x64;
    public static final int KEY_F14 = 0x65;
    public static final int KEY_F15 = 0x66;
    public static final int KEY_F16 = 0x67;
    public static final int KEY_F17 = 0x68;
    public static final int KEY_F18 = 0x69;
    public static final int KEY_F19 = 0x71;
    public static final int KEY_NUMPADEQUALS = 0x8D;
    public static final int KEY_NUMPADENTER = 0x9C;
    public static final int KEY_RCONTROL = 0x9D;
    public static final int KEY_NUMPADCOMMA = 0xB3;
    public static final int KEY_DIVIDE = 0xB5;
    public static final int KEY_SYSRQ = 0xB7;
    public static final int KEY_RMENU = 0xB8;
    public static final int KEY_PAUSE = 0xC5;
    public static final int KEY_HOME = 0xC7;
    public static final int KEY_UP = 0xC8;
    public static final int KEY_PRIOR = 0xC9;
    public static final int KEY_LEFT = 0xCB;
    public static final int KEY_RIGHT = 0xCD;
    public static final int KEY_END = 0xCF;
    public static final int KEY_DOWN = 0xD0;
    public static final int KEY_NEXT = 0xD1;
    public static final int KEY_INSERT = 0xD2;
    public static final int KEY_DELETE = 0xD3;
    public static final int KEY_LMETA = 0xDB;
    public static final int KEY_RMETA = 0xDC;
    public static final int KEY_APPS = 0xDD;

    public static final int KEYBOARD_SIZE = 256;

    // Key names, identical to LWJGL 2's: built by reflection over the KEY_*
    // fields exactly as LWJGL 2 did, so names like "LBRACKET" or "PRIOR"
    // round-trip through existing towns.ini files unchanged.
    private static final String[] keyNames = new String[KEYBOARD_SIZE];
    private static final Map<String, Integer> keyIndices = new HashMap<>();

    // GLFW keycode -> LWJGL 2 keycode. Index by GLFW code; 0 (KEY_NONE) for
    // anything unmapped.
    private static final int[] GLFW_TO_LWJGL = new int[GLFW.GLFW_KEY_LAST + 1];

    private static final boolean[] keyDown = new boolean[KEYBOARD_SIZE];

    private static final class KeyEvent {
        final int key;
        final boolean state;
        char character;

        KeyEvent(int key, boolean state, char character) {
            this.key = key;
            this.state = state;
            this.character = character;
        }
    }

    private static final ArrayDeque<KeyEvent> eventQueue = new ArrayDeque<>();
    private static KeyEvent currentEvent;

    // Press event still waiting for its character from the GLFW char
    // callback (which fires right after the key callback for printable keys).
    private static KeyEvent pendingCharEvent;

    // Strong references: GLFW callbacks are native resources and must not be
    // garbage-collected while registered.
    private static GLFWKeyCallback keyCallback;
    private static GLFWCharCallback charCallback;

    private static boolean created;

    static {
        for (Field field : Keyboard.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class && field.getName().startsWith("KEY_")) {
                try {
                    int code = field.getInt(null);
                    String name = field.getName().substring(4);
                    keyNames[code] = name;
                    keyIndices.put(name, code);
                } catch (IllegalAccessException e) {
                    // public field, cannot happen
                }
            }
        }

        map(GLFW.GLFW_KEY_SPACE, KEY_SPACE);
        map(GLFW.GLFW_KEY_APOSTROPHE, KEY_APOSTROPHE);
        map(GLFW.GLFW_KEY_COMMA, KEY_COMMA);
        map(GLFW.GLFW_KEY_MINUS, KEY_MINUS);
        map(GLFW.GLFW_KEY_PERIOD, KEY_PERIOD);
        map(GLFW.GLFW_KEY_SLASH, KEY_SLASH);
        map(GLFW.GLFW_KEY_0, KEY_0);
        map(GLFW.GLFW_KEY_1, KEY_1);
        map(GLFW.GLFW_KEY_2, KEY_2);
        map(GLFW.GLFW_KEY_3, KEY_3);
        map(GLFW.GLFW_KEY_4, KEY_4);
        map(GLFW.GLFW_KEY_5, KEY_5);
        map(GLFW.GLFW_KEY_6, KEY_6);
        map(GLFW.GLFW_KEY_7, KEY_7);
        map(GLFW.GLFW_KEY_8, KEY_8);
        map(GLFW.GLFW_KEY_9, KEY_9);
        map(GLFW.GLFW_KEY_SEMICOLON, KEY_SEMICOLON);
        map(GLFW.GLFW_KEY_EQUAL, KEY_EQUALS);
        map(GLFW.GLFW_KEY_A, KEY_A);
        map(GLFW.GLFW_KEY_B, KEY_B);
        map(GLFW.GLFW_KEY_C, KEY_C);
        map(GLFW.GLFW_KEY_D, KEY_D);
        map(GLFW.GLFW_KEY_E, KEY_E);
        map(GLFW.GLFW_KEY_F, KEY_F);
        map(GLFW.GLFW_KEY_G, KEY_G);
        map(GLFW.GLFW_KEY_H, KEY_H);
        map(GLFW.GLFW_KEY_I, KEY_I);
        map(GLFW.GLFW_KEY_J, KEY_J);
        map(GLFW.GLFW_KEY_K, KEY_K);
        map(GLFW.GLFW_KEY_L, KEY_L);
        map(GLFW.GLFW_KEY_M, KEY_M);
        map(GLFW.GLFW_KEY_N, KEY_N);
        map(GLFW.GLFW_KEY_O, KEY_O);
        map(GLFW.GLFW_KEY_P, KEY_P);
        map(GLFW.GLFW_KEY_Q, KEY_Q);
        map(GLFW.GLFW_KEY_R, KEY_R);
        map(GLFW.GLFW_KEY_S, KEY_S);
        map(GLFW.GLFW_KEY_T, KEY_T);
        map(GLFW.GLFW_KEY_U, KEY_U);
        map(GLFW.GLFW_KEY_V, KEY_V);
        map(GLFW.GLFW_KEY_W, KEY_W);
        map(GLFW.GLFW_KEY_X, KEY_X);
        map(GLFW.GLFW_KEY_Y, KEY_Y);
        map(GLFW.GLFW_KEY_Z, KEY_Z);
        map(GLFW.GLFW_KEY_LEFT_BRACKET, KEY_LBRACKET);
        map(GLFW.GLFW_KEY_BACKSLASH, KEY_BACKSLASH);
        map(GLFW.GLFW_KEY_RIGHT_BRACKET, KEY_RBRACKET);
        map(GLFW.GLFW_KEY_GRAVE_ACCENT, KEY_GRAVE);
        map(GLFW.GLFW_KEY_ESCAPE, KEY_ESCAPE);
        map(GLFW.GLFW_KEY_ENTER, KEY_RETURN);
        map(GLFW.GLFW_KEY_TAB, KEY_TAB);
        map(GLFW.GLFW_KEY_BACKSPACE, KEY_BACK);
        map(GLFW.GLFW_KEY_INSERT, KEY_INSERT);
        map(GLFW.GLFW_KEY_DELETE, KEY_DELETE);
        map(GLFW.GLFW_KEY_RIGHT, KEY_RIGHT);
        map(GLFW.GLFW_KEY_LEFT, KEY_LEFT);
        map(GLFW.GLFW_KEY_DOWN, KEY_DOWN);
        map(GLFW.GLFW_KEY_UP, KEY_UP);
        map(GLFW.GLFW_KEY_PAGE_UP, KEY_PRIOR);
        map(GLFW.GLFW_KEY_PAGE_DOWN, KEY_NEXT);
        map(GLFW.GLFW_KEY_HOME, KEY_HOME);
        map(GLFW.GLFW_KEY_END, KEY_END);
        map(GLFW.GLFW_KEY_CAPS_LOCK, KEY_CAPITAL);
        map(GLFW.GLFW_KEY_SCROLL_LOCK, KEY_SCROLL);
        map(GLFW.GLFW_KEY_NUM_LOCK, KEY_NUMLOCK);
        map(GLFW.GLFW_KEY_PRINT_SCREEN, KEY_SYSRQ);
        map(GLFW.GLFW_KEY_PAUSE, KEY_PAUSE);
        map(GLFW.GLFW_KEY_F1, KEY_F1);
        map(GLFW.GLFW_KEY_F2, KEY_F2);
        map(GLFW.GLFW_KEY_F3, KEY_F3);
        map(GLFW.GLFW_KEY_F4, KEY_F4);
        map(GLFW.GLFW_KEY_F5, KEY_F5);
        map(GLFW.GLFW_KEY_F6, KEY_F6);
        map(GLFW.GLFW_KEY_F7, KEY_F7);
        map(GLFW.GLFW_KEY_F8, KEY_F8);
        map(GLFW.GLFW_KEY_F9, KEY_F9);
        map(GLFW.GLFW_KEY_F10, KEY_F10);
        map(GLFW.GLFW_KEY_F11, KEY_F11);
        map(GLFW.GLFW_KEY_F12, KEY_F12);
        map(GLFW.GLFW_KEY_F13, KEY_F13);
        map(GLFW.GLFW_KEY_F14, KEY_F14);
        map(GLFW.GLFW_KEY_F15, KEY_F15);
        map(GLFW.GLFW_KEY_F16, KEY_F16);
        map(GLFW.GLFW_KEY_F17, KEY_F17);
        map(GLFW.GLFW_KEY_F18, KEY_F18);
        map(GLFW.GLFW_KEY_F19, KEY_F19);
        map(GLFW.GLFW_KEY_KP_0, KEY_NUMPAD0);
        map(GLFW.GLFW_KEY_KP_1, KEY_NUMPAD1);
        map(GLFW.GLFW_KEY_KP_2, KEY_NUMPAD2);
        map(GLFW.GLFW_KEY_KP_3, KEY_NUMPAD3);
        map(GLFW.GLFW_KEY_KP_4, KEY_NUMPAD4);
        map(GLFW.GLFW_KEY_KP_5, KEY_NUMPAD5);
        map(GLFW.GLFW_KEY_KP_6, KEY_NUMPAD6);
        map(GLFW.GLFW_KEY_KP_7, KEY_NUMPAD7);
        map(GLFW.GLFW_KEY_KP_8, KEY_NUMPAD8);
        map(GLFW.GLFW_KEY_KP_9, KEY_NUMPAD9);
        map(GLFW.GLFW_KEY_KP_DECIMAL, KEY_DECIMAL);
        map(GLFW.GLFW_KEY_KP_DIVIDE, KEY_DIVIDE);
        map(GLFW.GLFW_KEY_KP_MULTIPLY, KEY_MULTIPLY);
        map(GLFW.GLFW_KEY_KP_SUBTRACT, KEY_SUBTRACT);
        map(GLFW.GLFW_KEY_KP_ADD, KEY_ADD);
        map(GLFW.GLFW_KEY_KP_ENTER, KEY_NUMPADENTER);
        map(GLFW.GLFW_KEY_KP_EQUAL, KEY_NUMPADEQUALS);
        map(GLFW.GLFW_KEY_LEFT_SHIFT, KEY_LSHIFT);
        map(GLFW.GLFW_KEY_LEFT_CONTROL, KEY_LCONTROL);
        map(GLFW.GLFW_KEY_LEFT_ALT, KEY_LMENU);
        map(GLFW.GLFW_KEY_LEFT_SUPER, KEY_LMETA);
        map(GLFW.GLFW_KEY_RIGHT_SHIFT, KEY_RSHIFT);
        map(GLFW.GLFW_KEY_RIGHT_CONTROL, KEY_RCONTROL);
        map(GLFW.GLFW_KEY_RIGHT_ALT, KEY_RMENU);
        map(GLFW.GLFW_KEY_RIGHT_SUPER, KEY_RMETA);
        map(GLFW.GLFW_KEY_MENU, KEY_APPS);
    }

    private Keyboard() {
    }

    private static void map(int glfwKey, int lwjglKey) {
        GLFW_TO_LWJGL[glfwKey] = lwjglKey;
    }

    /**
     * Registers the GLFW callbacks. Called once by the Display shim after
     * window creation.
     */
    public static void install(long window) {
        keyCallback = GLFWKeyCallback.create((win, glfwKey, scancode, action, mods) -> {
            if (glfwKey < 0 || glfwKey >= GLFW_TO_LWJGL.length) {
                return;
            }
            int key = GLFW_TO_LWJGL[glfwKey];

            if (action == GLFW.GLFW_REPEAT) {
                // LWJGL 2 default: repeat events are not delivered. Held keys
                // stay visible through isKeyDown().
                pendingCharEvent = null;
                return;
            }

            boolean state = action == GLFW.GLFW_PRESS;
            keyDown[key] = state;

            KeyEvent event = new KeyEvent(key, state, CHAR_NONE);
            eventQueue.add(event);
            pendingCharEvent = state ? event : null;
        });
        GLFW.glfwSetKeyCallback(window, keyCallback);

        charCallback = GLFWCharCallback.create((win, codepoint) -> {
            char character = codepoint <= Character.MAX_VALUE ? (char) codepoint : CHAR_NONE;
            if (pendingCharEvent != null && pendingCharEvent.character == CHAR_NONE) {
                // Printable key press: GLFW fires the char callback right
                // after the key callback; merge them into one LWJGL 2-style
                // event carrying both key and character.
                pendingCharEvent.character = character;
                pendingCharEvent = null;
            } else {
                // Character with no owning key press (dead-key composition,
                // IME…): LWJGL 2 delivered these as KEY_NONE press events.
                eventQueue.add(new KeyEvent(KEY_NONE, true, character));
            }
        });
        GLFW.glfwSetCharCallback(window, charCallback);

        created = true;
    }

    public static boolean isCreated() {
        return created;
    }

    public static boolean isKeyDown(int key) {
        return key >= 0 && key < KEYBOARD_SIZE && keyDown[key];
    }

    public static boolean next() {
        currentEvent = eventQueue.poll();
        return currentEvent != null;
    }

    public static int getEventKey() {
        return currentEvent != null ? currentEvent.key : KEY_NONE;
    }

    public static boolean getEventKeyState() {
        return currentEvent != null && currentEvent.state;
    }

    public static char getEventCharacter() {
        return currentEvent != null ? currentEvent.character : CHAR_NONE;
    }

    /** LWJGL 2-compatible key name ("A", "SPACE", "LBRACKET"…). */
    public static String getKeyName(int key) {
        return key >= 0 && key < KEYBOARD_SIZE ? keyNames[key] : null;
    }

    /** Inverse of getKeyName(); KEY_NONE for unknown names, as in LWJGL 2. */
    public static int getKeyIndex(String keyName) {
        Integer key = keyIndices.get(keyName);
        return key != null ? key : KEY_NONE;
    }
}
