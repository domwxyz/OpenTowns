package xaos.compat.input;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import xaos.compat.LWJGLException;

/**
 * Drop-in replacement for org.lwjgl.input.Cursor backed by glfwCreateCursor.
 *
 * Keeps the LWJGL 2 constructor contract: pixels are packed ARGB ints in
 * BOTTOM-UP row order (the game already flips its cursor image when building
 * the buffer) and the y hotspot is measured from the bottom edge. Both are
 * converted to GLFW's top-down/RGBA/top-edge conventions here.
 *
 * Only single-image (non-animated) cursors are supported; the game only ever
 * creates numImages=1. The GLFW cursor object is created lazily on first use
 * because GLFW may not be initialized when UtilsGL builds the Cursor.
 */
public final class Cursor {

    private final int width;
    private final int height;
    private final int xHotspot;
    private final int yHotspotFromTop;
    private final int[] argbTopDown;

    private long handle;

    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) throws LWJGLException {
        if (numImages != 1) {
            throw new LWJGLException("Animated cursors are not supported by the GLFW shim (numImages=" + numImages + ")");
        }
        if (images.remaining() < width * height) {
            throw new LWJGLException("Cursor image buffer too small: " + images.remaining() + " < " + width * height);
        }

        this.width = width;
        this.height = height;
        this.xHotspot = xHotspot;
        this.yHotspotFromTop = height - 1 - yHotspot;

        // Re-order bottom-up -> top-down now; conversion to RGBA happens at
        // handle creation time.
        this.argbTopDown = new int[width * height];
        int base = images.position();
        for (int row = 0; row < height; row++) {
            int srcRow = height - 1 - row;
            for (int col = 0; col < width; col++) {
                argbTopDown[row * width + col] = images.get(base + srcRow * width + col);
            }
        }
    }

    /** The GLFW cursor object, created on first use. GLFW must be initialized. */
    long handle() {
        if (handle == 0) {
            ByteBuffer rgba = MemoryUtil.memAlloc(width * height * 4);
            for (int argb : argbTopDown) {
                rgba.put((byte) ((argb >> 16) & 0xFF)); // R
                rgba.put((byte) ((argb >> 8) & 0xFF));  // G
                rgba.put((byte) (argb & 0xFF));         // B
                rgba.put((byte) ((argb >> 24) & 0xFF)); // A
            }
            rgba.flip();
            try (GLFWImage image = GLFWImage.malloc()) {
                image.set(width, height, rgba);
                // GLFW copies the pixels before returning, so the buffer can
                // be freed immediately.
                handle = GLFW.glfwCreateCursor(image, xHotspot, yHotspotFromTop);
            } finally {
                MemoryUtil.memFree(rgba);
            }
        }
        return handle;
    }
}
