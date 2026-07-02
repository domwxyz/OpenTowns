package xaos.compat.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import xaos.compat.LWJGLException;
import xaos.compat.input.Keyboard;
import xaos.compat.input.Mouse;

/**
 * Drop-in replacement for org.lwjgl.opengl.Display backed by a GLFW window.
 *
 * LWJGL 2 semantics kept by the game are preserved:
 * - setDisplayMode/setResizable/setFullscreen/setVSyncEnabled/setTitle/setIcon
 *   may all be called BEFORE create() (UtilsGL.initGL does exactly that);
 *   settings are buffered and applied at window creation.
 * - setDisplayMode(dm) targets WINDOWED mode: called while fullscreen it
 *   leaves fullscreen (UtilsGL.toggleFullScreen relies on this).
 *   setDisplayModeAndFullscreen(dm) targets fullscreen at dm.
 * - update() swaps buffers and pumps input events (glfwPollEvents), which is
 *   when the Keyboard/Mouse shim callbacks fire.
 * - wasResized() reports resizes since the last update() call.
 * - sync(fps) is the frame limiter the game calls from its loops.
 *
 * The GL context is created with no profile hints: on Windows/Linux drivers
 * hand back a compatibility context, so the game's immediate-mode GL 1.x
 * calls work unchanged. (macOS would cap this at GL 2.1 — still sufficient.)
 *
 * Main-thread only, like the rest of the game.
 */
public final class Display {

    private static boolean glfwInitialized;
    private static boolean created;
    private static long window = MemoryUtil.NULL;

    // Buffered configuration, applied at create() (and live afterwards).
    private static DisplayMode mode;
    private static String title = "Game";
    private static boolean resizable;
    private static boolean fullscreen;
    private static boolean vsync;
    private static ByteBuffer[] icons;

    private static int width;
    private static int height;
    private static boolean resizedSinceUpdate;

    // Windowed position/size, remembered across fullscreen switches.
    private static int windowedX;
    private static int windowedY;

    private static GLFWErrorCallback errorCallback;
    private static GLFWWindowSizeCallback sizeCallback; // strong ref, see Keyboard

    private static long syncNextFrame;

    private Display() {
    }

    private static void ensureGlfwInitialized() {
        if (!glfwInitialized) {
            errorCallback = GLFWErrorCallback.createPrint(System.err);
            GLFW.glfwSetErrorCallback(errorCallback);
            if (!GLFW.glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }
            glfwInitialized = true;
        }
    }

    public static DisplayMode getDesktopDisplayMode() {
        ensureGlfwInitialized();
        GLFWVidMode vm = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        return toDisplayMode(vm);
    }

    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        ensureGlfwInitialized();
        GLFWVidMode.Buffer modes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());
        if (modes == null) {
            throw new LWJGLException("glfwGetVideoModes failed");
        }
        DisplayMode[] result = new DisplayMode[modes.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = toDisplayMode(modes.get(i));
        }
        return result;
    }

    private static DisplayMode toDisplayMode(GLFWVidMode vm) {
        return new DisplayMode(vm.width(), vm.height(), vm.redBits() + vm.greenBits() + vm.blueBits(), vm.refreshRate());
    }

    /** Targets windowed mode at the given size; exits fullscreen if active. */
    public static void setDisplayMode(DisplayMode dm) throws LWJGLException {
        mode = dm;
        if (created) {
            if (fullscreen) {
                GLFW.glfwSetWindowMonitor(window, MemoryUtil.NULL, windowedX, windowedY, dm.getWidth(), dm.getHeight(), GLFW.GLFW_DONT_CARE);
                fullscreen = false;
                applyVSync();
            } else {
                GLFW.glfwSetWindowSize(window, dm.getWidth(), dm.getHeight());
            }
        }
    }

    public static void setDisplayModeAndFullscreen(DisplayMode dm) throws LWJGLException {
        mode = dm;
        if (created) {
            enterFullscreen(dm);
        } else {
            fullscreen = true;
        }
    }

    public static void setFullscreen(boolean fs) throws LWJGLException {
        if (!created) {
            fullscreen = fs;
        } else if (fs != fullscreen) {
            if (fs) {
                enterFullscreen(mode != null ? mode : getDesktopDisplayMode());
            } else {
                setDisplayMode(mode);
            }
        }
    }

    private static void enterFullscreen(DisplayMode dm) {
        rememberWindowedPos();
        long monitor = GLFW.glfwGetPrimaryMonitor();
        int freq = dm.getFrequency() > 0 ? dm.getFrequency() : GLFW.GLFW_DONT_CARE;
        GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, dm.getWidth(), dm.getHeight(), freq);
        fullscreen = true;
        applyVSync();
    }

    private static void rememberWindowedPos() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(window, x, y);
            windowedX = x.get(0);
            windowedY = y.get(0);
        }
    }

    private static void applyVSync() {
        // Swap interval is context state but some drivers reset it on
        // monitor changes; reapplying is free.
        GLFW.glfwSwapInterval(vsync ? 1 : 0);
    }

    public static void setResizable(boolean value) {
        resizable = value;
        if (created) {
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_RESIZABLE, value ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static void setVSyncEnabled(boolean value) {
        vsync = value;
        if (created) {
            applyVSync();
        }
    }

    public static void setTitle(String value) {
        title = value;
        if (created) {
            GLFW.glfwSetWindowTitle(window, value);
        }
    }

    /**
     * LWJGL 2 contract: square RGBA byte buffers of arbitrary sizes. The
     * return value (number of icons used) is ignored by the game.
     */
    public static int setIcon(ByteBuffer[] value) {
        icons = value;
        if (created) {
            applyIcon();
        }
        return value != null ? value.length : 0;
    }

    private static void applyIcon() {
        if (icons == null || icons.length == 0) {
            return;
        }
        try (GLFWImage.Buffer images = GLFWImage.malloc(icons.length)) {
            for (int i = 0; i < icons.length; i++) {
                ByteBuffer rgba = icons[i];
                int dim = (int) Math.sqrt(rgba.remaining() / 4.0);
                images.get(i).set(dim, dim, rgba);
            }
            GLFW.glfwSetWindowIcon(window, images);
        }
    }

    public static void create() throws LWJGLException {
        ensureGlfwInitialized();

        if (mode == null) {
            mode = getDesktopDisplayMode();
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        // No context version/profile hints: request a legacy-compatible
        // context for the game's immediate-mode rendering.

        long monitor = fullscreen ? GLFW.glfwGetPrimaryMonitor() : MemoryUtil.NULL;
        window = GLFW.glfwCreateWindow(mode.getWidth(), mode.getHeight(), title, monitor, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new LWJGLException("Failed to create the GLFW window");
        }

        if (!fullscreen) {
            // Center the window, but never push the title bar off-screen:
            // glfwSetWindowPos positions the client area, so clamp to the
            // decoration (frame) size.
            DisplayMode desktop = getDesktopDisplayMode();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer frameLeft = stack.mallocInt(1);
                IntBuffer frameTop = stack.mallocInt(1);
                GLFW.glfwGetWindowFrameSize(window, frameLeft, frameTop, null, null);
                windowedX = Math.max(frameLeft.get(0), (desktop.getWidth() - mode.getWidth()) / 2);
                windowedY = Math.max(frameTop.get(0), (desktop.getHeight() - mode.getHeight()) / 2);
            }
            GLFW.glfwSetWindowPos(window, windowedX, windowedY);
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        applyVSync();
        applyIcon();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(window, w, h);
            width = w.get(0);
            height = h.get(0);
        }

        sizeCallback = GLFWWindowSizeCallback.create((win, w, h) -> {
            if (w > 0 && h > 0) {
                width = w;
                height = h;
                resizedSinceUpdate = true;
                Mouse.updateWindowHeight(h);
            }
        });
        GLFW.glfwSetWindowSizeCallback(window, sizeCallback);

        // Input shims: register their callbacks and hand them the initial
        // window height for the y-axis flip.
        Keyboard.install(window);
        Mouse.install(window);

        GLFW.glfwShowWindow(window);
        created = true;
    }

    public static boolean isCreated() {
        return created;
    }

    /** Swaps buffers and pumps events (input callbacks fire here). */
    public static void update() {
        resizedSinceUpdate = false;
        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    /** True if the window was resized since the last update() call. */
    public static boolean wasResized() {
        return resizedSinceUpdate;
    }

    public static boolean isCloseRequested() {
        return created && GLFW.glfwWindowShouldClose(window);
    }

    public static boolean isFullscreen() {
        return fullscreen;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    /**
     * Frame limiter, replacing LWJGL 2's Display.sync. Sleeps in 1ms slices
     * until close to the deadline, then spin-waits for accuracy. If a frame
     * ran long, the deadline resets instead of accumulating catch-up debt.
     */
    public static void sync(int fps) {
        if (fps <= 0) {
            return;
        }
        long period = 1_000_000_000L / fps;
        long now = System.nanoTime();

        if (syncNextFrame == 0) {
            syncNextFrame = now + period;
            return;
        }

        syncNextFrame += period;
        if (syncNextFrame <= now) {
            syncNextFrame = now;
            return;
        }

        long remaining;
        while ((remaining = syncNextFrame - System.nanoTime()) > 0) {
            if (remaining > 2_000_000L) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    public static void destroy() {
        if (created) {
            Callbacks.glfwFreeCallbacks(window);
            GLFW.glfwDestroyWindow(window);
            window = MemoryUtil.NULL;
            created = false;
        }
        if (glfwInitialized) {
            GLFW.glfwTerminate();
            if (errorCallback != null) {
                errorCallback.free();
                errorCallback = null;
            }
            glfwInitialized = false;
        }
    }
}
