package xaos.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * UtilsGL.viewportRect is the pure decision behind the game's glViewport
 * call: window size in screen units vs framebuffer size in pixels. The two
 * differ on HiDPI displays (macOS Retina), where a window-sized viewport
 * confines rendering to the lower-left quarter of the window. No GL context
 * or window is needed here; the method takes plain ints.
 */
class ViewportRectTest {

    @Test
    void framebufferEqualToWindowGivesFullWindowViewport() {
        // Windows, Linux, non-Retina macOS: framebuffer == window size, and
        // the result must be exactly the old behavior (window-sized viewport).
        assertArrayEquals(new int[] { 0, 0, 1024, 768 },
                UtilsGL.viewportRect(1024, 768, 1024, 768));
        assertArrayEquals(new int[] { 0, 0, 800, 600 },
                UtilsGL.viewportRect(800, 600, 800, 600));
    }

    @Test
    void retinaFramebufferTwiceTheWindowCoversTheWholeFramebuffer() {
        // 2x Retina: the viewport must span all framebuffer pixels, not the
        // window-unit quarter of them.
        assertArrayEquals(new int[] { 0, 0, 2048, 1536 },
                UtilsGL.viewportRect(1024, 768, 2048, 1536));
    }

    @Test
    void fractionalScaleFramebufferIsUsedAsIs() {
        // Non-integer scale factors exist too (e.g. 150%).
        assertArrayEquals(new int[] { 0, 0, 1536, 1152 },
                UtilsGL.viewportRect(1024, 768, 1536, 1152));
    }

    @Test
    void missingFramebufferSizeFallsBackToWindowSize() {
        assertArrayEquals(new int[] { 0, 0, 1024, 768 },
                UtilsGL.viewportRect(1024, 768, 0, 0));
        assertArrayEquals(new int[] { 0, 0, 1024, 768 },
                UtilsGL.viewportRect(1024, 768, -1, -1));
    }
}
