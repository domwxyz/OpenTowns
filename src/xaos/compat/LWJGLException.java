package xaos.compat;

/**
 * Drop-in replacement for org.lwjgl.LWJGLException, which no longer exists in
 * LWJGL 3. Kept so existing catch blocks (e.g. UtilsGL.setNativeCursor)
 * survive the import swap unchanged.
 */
public class LWJGLException extends Exception {

    private static final long serialVersionUID = 1L;

    public LWJGLException() {
    }

    public LWJGLException(String message) {
        super(message);
    }

    public LWJGLException(String message, Throwable cause) {
        super(message, cause);
    }

    public LWJGLException(Throwable cause) {
        super(cause);
    }
}
