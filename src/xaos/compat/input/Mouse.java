package xaos.compat.input;

import java.nio.IntBuffer;
import java.util.ArrayDeque;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.system.MemoryStack;

import xaos.compat.LWJGLException;

/**
 * Drop-in replacement for org.lwjgl.input.Mouse backed by GLFW (LWJGL 3).
 *
 * Coordinate system is LWJGL 2's: origin at the BOTTOM-LEFT of the window
 * client area, y growing upward. GLFW reports top-left/downward positions;
 * they are flipped here at the callback boundary because the game flips them
 * back itself (e.g. Game.checkMouseEvents does "getHeight() - getEventY() - 1").
 *
 * Button indices are unchanged (GLFW and LWJGL 2 agree: 0=left, 1=right,
 * 2=middle). Wheel deltas are scaled to LWJGL 2's Windows convention of
 * ±120 per notch; the game only tests the sign.
 *
 * Lifecycle: the Display shim calls install(window) once after creating the
 * window and updateWindowHeight(h) whenever the window is resized (needed
 * for the y flip). Not thread-safe; see Keyboard.
 */
public final class Mouse {

    private static final class MouseEvent {
        final int button;      // -1 for move/wheel events, as in LWJGL 2
        final boolean state;
        final int x;
        final int y;
        final int dwheel;

        MouseEvent(int button, boolean state, int x, int y, int dwheel) {
            this.button = button;
            this.state = state;
            this.x = x;
            this.y = y;
            this.dwheel = dwheel;
        }
    }

    private static final int WHEEL_SCALE = 120; // Win32 WHEEL_DELTA, LWJGL 2's unit

    private static final ArrayDeque<MouseEvent> eventQueue = new ArrayDeque<>();
    private static MouseEvent currentEvent;

    private static final boolean[] buttonDown = new boolean[8];
    private static int x;
    private static int y;
    private static int windowHeight = 1;
    private static boolean insideWindow;

    private static long window;
    private static Cursor nativeCursor;

    // Strong references, see Keyboard.
    private static GLFWCursorPosCallback posCallback;
    private static GLFWMouseButtonCallback buttonCallback;
    private static GLFWScrollCallback scrollCallback;
    private static GLFWCursorEnterCallback enterCallback;

    private static boolean created;

    private Mouse() {
    }

    /**
     * Registers the GLFW callbacks. Called once by the Display shim after
     * window creation.
     */
    public static void install(long windowHandle) {
        window = windowHandle;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(window, w, h);
            windowHeight = Math.max(1, h.get(0));
        }

        posCallback = GLFWCursorPosCallback.create((win, xpos, ypos) -> {
            x = (int) Math.round(xpos);
            y = flipY((int) Math.round(ypos));
        });
        GLFW.glfwSetCursorPosCallback(window, posCallback);

        buttonCallback = GLFWMouseButtonCallback.create((win, button, action, mods) -> {
            boolean state = action == GLFW.GLFW_PRESS;
            if (button >= 0 && button < buttonDown.length) {
                buttonDown[button] = state;
            }
            eventQueue.add(new MouseEvent(button, state, x, y, 0));
        });
        GLFW.glfwSetMouseButtonCallback(window, buttonCallback);

        scrollCallback = GLFWScrollCallback.create((win, xoffset, yoffset) -> {
            int dwheel = (int) Math.round(yoffset * WHEEL_SCALE);
            if (dwheel != 0) {
                eventQueue.add(new MouseEvent(-1, false, x, y, dwheel));
            }
        });
        GLFW.glfwSetScrollCallback(window, scrollCallback);

        enterCallback = GLFWCursorEnterCallback.create((win, entered) -> insideWindow = entered);
        GLFW.glfwSetCursorEnterCallback(window, enterCallback);

        if (nativeCursor != null) {
            GLFW.glfwSetCursor(window, nativeCursor.handle());
        }

        created = true;
    }

    /** Called by the Display shim whenever the window client area resizes. */
    public static void updateWindowHeight(int height) {
        windowHeight = Math.max(1, height);
    }

    private static int flipY(int glfwY) {
        return windowHeight - 1 - glfwY;
    }

    public static boolean isCreated() {
        return created;
    }

    public static int getX() {
        return x;
    }

    public static int getY() {
        return y;
    }

    public static boolean isButtonDown(int button) {
        return button >= 0 && button < buttonDown.length && buttonDown[button];
    }

    public static boolean isInsideWindow() {
        return insideWindow;
    }

    public static boolean next() {
        currentEvent = eventQueue.poll();
        return currentEvent != null;
    }

    public static int getEventButton() {
        return currentEvent != null ? currentEvent.button : -1;
    }

    public static boolean getEventButtonState() {
        return currentEvent != null && currentEvent.state;
    }

    public static int getEventX() {
        return currentEvent != null ? currentEvent.x : x;
    }

    public static int getEventY() {
        return currentEvent != null ? currentEvent.y : y;
    }

    public static int getEventDWheel() {
        return currentEvent != null ? currentEvent.dwheel : 0;
    }

    /**
     * Sets a custom hardware cursor. The Cursor may be created before the
     * window exists (UtilsGL builds it during init); it is applied on
     * install() in that case.
     */
    public static void setNativeCursor(Cursor cursor) throws LWJGLException {
        nativeCursor = cursor;
        if (created && cursor != null) {
            GLFW.glfwSetCursor(window, cursor.handle());
        }
    }
}
