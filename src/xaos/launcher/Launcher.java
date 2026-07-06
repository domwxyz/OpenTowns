package xaos.launcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBEasyFont;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import xaos.setup.FirstRunSetup;
import xaos.setup.SteamLocator;

/**
 * Branded pre-launch window: shown from Towns.main before the game window
 * exists. The player adjusts the pre-launch settings (window size, fullscreen,
 * audio, enabled mods) and clicks Play; the changes are saved through
 * LauncherConfig and startup proceeds exactly as before. Closing the window
 * (or Escape) exits instead of starting the game.
 *
 * The proprietary-asset setup lives here too: when data/graphics, data/audio
 * or data/fonts are missing, Play is disabled and the assets row offers to
 * copy them from a detected Steam Towns install or from a folder the player
 * picks (native tinyfd folder dialog, like FirstRunSetup's own flow). The
 * copy itself runs on a background thread so the window stays responsive;
 * detection, validation and copying are FirstRunSetup/SteamLocator logic,
 * reused, and FirstRunSetup.run() in Towns.main remains as a no-op safety
 * net once the assets exist.
 *
 * Built on the same stack as the game itself: a GLFW window with
 * immediate-mode GL 1.x and a hand-rolled UI, no toolkit (AWT/Swing would
 * fight GLFW for the main thread on macOS, the reason FirstRunSetup already
 * avoids it). Text is a system font baked through stb_truetype (lwjgl-stb,
 * already a dependency): the proprietary game fonts may not exist yet at this
 * point, and stb_easy_font alone looks too blocky. On a system with none of
 * the candidate fonts, stb_easy_font is the fallback. The window, its GL
 * context and GLFW itself are fully torn down before returning, so
 * Display.create() later starts from the same clean state as today.
 *
 * Like xaos.setup (the only package this one imports from, itself free of
 * game imports), this package imports nothing from the game packages: it
 * must not class-initialize Game or draw from the game RNG.
 *
 * Escape hatch: -Dtowns.skipLauncher=true boots straight into the game.
 */
public final class Launcher {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 600;

    // Neutral graphite palette; green is reserved for the Play button.
    private static final float[] COLOR_BG = {0.114f, 0.118f, 0.129f};
    private static final float[] COLOR_PANEL = {0.157f, 0.165f, 0.180f};
    private static final float[] COLOR_PANEL_HOVER = {0.210f, 0.220f, 0.238f};
    private static final float[] COLOR_BORDER = {0.286f, 0.298f, 0.318f};
    private static final float[] COLOR_TEXT = {0.906f, 0.910f, 0.914f};
    private static final float[] COLOR_DIM = {0.565f, 0.588f, 0.612f};
    private static final float[] COLOR_FILL = {0.780f, 0.800f, 0.820f};
    private static final float[] COLOR_PLAY = {0.267f, 0.533f, 0.310f};
    private static final float[] COLOR_PLAY_HOVER = {0.325f, 0.616f, 0.373f};
    private static final float[] COLOR_WHITE = {1f, 1f, 1f};
    private static final float[] COLOR_ERROR = {0.85f, 0.45f, 0.42f};

    // Text sizes in pixels; one baked font per size (see fontFor).
    private static final float SIZE_SMALL = 13;
    private static final float SIZE_LABEL = 16;
    private static final float SIZE_BUTTON = 18;
    private static final float SIZE_TITLE = 26;

    private static final int MOD_ROW_HEIGHT = 24;
    private static final int MOD_ROWS_VISIBLE = 5;

    private static long window = MemoryUtil.NULL;
    private static GLFWErrorCallback errorCallback;
    private static LauncherConfig config;

    // Mouse state, fed by GLFW callbacks and consumed once per frame.
    private static double mouseX;
    private static double mouseY;
    private static boolean mouseDown;
    private static boolean mouseClicked;
    private static double scrollY;
    private static int activeSlider; // 0 none, 1 music, 2 fx

    private static boolean playPressed;
    private static boolean quitPressed;

    private static int modScroll;

    // Window-size presets the stepper walks through (filtered to the desktop).
    private static final ArrayList<int[]> resolutions = new ArrayList<int[]>();
    private static int resolutionIndex;

    private static int logoTexture;

    // Proprietary-asset state. PRESENT gates the Play button; the copy runs
    // on a background thread, so state and message are volatile.
    private enum AssetState { PRESENT, MISSING, COPYING }
    private static volatile AssetState assetState = AssetState.MISSING;
    private static volatile String assetMessage = "";
    private static volatile boolean assetMessageIsError;
    private static Path steamInstallData; // validated data root of a detected Steam install

    private static Font fontSmall;
    private static Font fontLabel;
    private static Font fontButton;
    private static Font fontTitle;

    private static final ByteBuffer easyFontBuffer = BufferUtils.createByteBuffer(64 * 1024);

    private Launcher() {
    }

    /** Returns when the player clicks Play; exits the JVM on close/Escape. */
    public static void run() {
        if (Boolean.getBoolean("towns.skipLauncher")) {
            return;
        }

        config = LauncherConfig.load();
        initAssetState();

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);
        if (!GLFW.glfwInit()) {
            // No windowing available at all; the game would fail the same way
            // moments later, so skip the launcher rather than block startup.
            System.err.println("Launcher: GLFW init failed, starting the game directly");
            freeErrorCallback();
            return;
        }

        buildResolutionPresets();
        createWindow();

        while (!playPressed && !quitPressed && !GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            drawFrame();
            GLFW.glfwSwapBuffers(window);
            mouseClicked = false;
            scrollY = 0;
        }

        boolean play = playPressed;
        destroyWindow();

        if (!play) {
            System.exit(0);
        }
        try {
            config.save();
        } catch (IOException e) {
            // Not fatal: the game still starts, just with the previous
            // settings on the next launch.
            System.err.println("Launcher: could not save settings to "
                    + config.getUserTownsFolder() + ": " + e);
        }
    }

    // ------------------------------------------------------------------
    // Window lifecycle

    private static void createWindow() {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        // No context hints: a legacy-compatible context, like the game.

        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "OpenTowns", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create the launcher window");
        }

        GLFWVidMode desktop = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (desktop != null) {
            GLFW.glfwSetWindowPos(window, (desktop.width() - WIDTH) / 2, (desktop.height() - HEIGHT) / 2);
        }

        GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseDown = true;
                    mouseClicked = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseDown = false;
                }
            }
        });
        GLFW.glfwSetScrollCallback(window, (win, dx, dy) -> scrollY += dy);
        GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    quitPressed = true;
                } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    playPressed = assetState == AssetState.PRESENT;
                }
            }
        });

        setWindowIcon();

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GLFW.glfwSwapInterval(1);

        loadFonts();
        loadLogo();
        GLFW.glfwShowWindow(window);
    }

    private static void destroyWindow() {
        if (logoTexture != 0) {
            GL11.glDeleteTextures(logoTexture);
            logoTexture = 0;
        }
        disposeFonts();
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);
        window = MemoryUtil.NULL;
        GLFW.glfwTerminate();
        freeErrorCallback();
    }

    private static void freeErrorCallback() {
        GLFW.glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
            errorCallback = null;
        }
    }

    // ------------------------------------------------------------------
    // Branding: repo logo as in-window art, pre-scaled logos as window icon

    /** A launcher asset: from the app dir in a packaged build (staged there by
     *  the jpackage input task), or from packaging/icons/ in a dev run (the
     *  working directory is src/). Null when absent; never fatal. */
    private static Path brandingFile(String packagedRelative, String devRelative) {
        String home = System.getProperty("towns.home", "").trim();
        Path packaged = home.isEmpty() ? Path.of(packagedRelative) : Path.of(home, packagedRelative);
        if (Files.isRegularFile(packaged)) {
            return packaged;
        }
        Path dev = Path.of("..", "packaging", "icons", devRelative);
        return Files.isRegularFile(dev) ? dev : null;
    }

    /**
     * Window/taskbar icon from the pre-scaled logo-16/32/48.png files, so the
     * OS picks an exact size instead of downscaling the big logo. Skipped on
     * macOS (the app bundle icon applies there and glfwSetWindowIcon would
     * only log an error).
     */
    private static void setWindowIcon() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return;
        }

        ArrayList<ByteBuffer> pixelBuffers = new ArrayList<ByteBuffer>();
        ArrayList<int[]> sizes = new ArrayList<int[]>();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int size : new int[] {16, 32, 48}) {
                Path path = brandingFile("icons/logo-" + size + ".png", "source/logo-" + size + ".png");
                ByteBuffer encoded = readFile(path);
                if (encoded == null) {
                    continue;
                }
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, w, h, comp, 4);
                if (pixels != null) {
                    pixelBuffers.add(pixels);
                    sizes.add(new int[] {w.get(0), h.get(0)});
                }
            }
        }
        if (pixelBuffers.isEmpty()) {
            return;
        }
        try (GLFWImage.Buffer icons = GLFWImage.malloc(pixelBuffers.size())) {
            for (int i = 0; i < pixelBuffers.size(); i++) {
                icons.get(i).set(sizes.get(i)[0], sizes.get(i)[1], pixelBuffers.get(i));
            }
            GLFW.glfwSetWindowIcon(window, icons);
        }
        for (ByteBuffer pixels : pixelBuffers) {
            STBImage.stbi_image_free(pixels);
        }
    }

    private static void loadLogo() {
        ByteBuffer encoded = readFile(brandingFile("OpenTowns.png", "OpenTowns.png"));
        if (encoded == null) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, w, h, comp, 4);
            if (pixels == null) {
                return;
            }
            logoTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, logoTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w.get(0), h.get(0), 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            STBImage.stbi_image_free(pixels);
        }
    }

    private static ByteBuffer readFile(Path path) {
        if (path == null) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Frame: layout + interaction in one immediate-mode pass

    private static void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, x, y);
            mouseX = x.get(0);
            mouseY = y.get(0);
        }
        if (!mouseDown) {
            activeSlider = 0;
        }

        // The viewport uses framebuffer pixels (HiDPI) while the ortho space
        // and mouse coordinates stay in window coordinates.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fbw = stack.mallocInt(1);
            IntBuffer fbh = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, fbw, fbh);
            GL11.glViewport(0, 0, fbw.get(0), fbh.get(0));
        }
        GL11.glClearColor(COLOR_BG[0], COLOR_BG[1], COLOR_BG[2], 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int margin = 32;

        // Header: logo + title
        int headerBottom;
        if (logoTexture != 0) {
            drawLogo((WIDTH - 88) / 2f, 16, 88);
            textCentered("OpenTowns", WIDTH / 2f, 110, SIZE_TITLE, COLOR_TEXT);
            headerBottom = 142;
        } else {
            textCentered("OpenTowns", WIDTH / 2f, 26, SIZE_TITLE, COLOR_TEXT);
            headerBottom = 58;
        }
        line(margin, headerBottom, WIDTH - margin, headerBottom);

        // Assets first: nothing runs without them, so their state (and the
        // buttons to fix it) leads the list, with a status line below.
        int rowY = headerBottom + 16;
        drawAssetsRow(margin, rowY);
        rowY += 26;
        if (!assetMessage.isEmpty()) {
            text(fit(assetMessage, SIZE_SMALL, WIDTH - 2 * margin), margin, rowY, SIZE_SMALL,
                    assetMessageIsError ? COLOR_ERROR : COLOR_DIM);
        }
        rowY += 22;

        // Settings rows
        drawResolutionRow(margin, rowY);
        rowY += 32;
        config.fullscreen = labeledCheckbox("Fullscreen", margin, rowY, config.fullscreen, true);
        rowY += 32;
        config.music = labeledCheckbox("Music", margin, rowY, config.music, true);
        config.volumeMusic = volumeSlider(1, 210, rowY, config.volumeMusic, config.music);
        rowY += 32;
        config.fx = labeledCheckbox("Sound effects", margin, rowY, config.fx, true);
        config.volumeFX = volumeSlider(2, 210, rowY, config.volumeFX, config.fx);
        rowY += 30;

        line(margin, rowY, WIDTH - margin, rowY);
        rowY += 12;

        // Mods
        text("Mods", margin, rowY, SIZE_LABEL, COLOR_TEXT);
        text("loaded at startup", WIDTH - margin - textWidth("loaded at startup", SIZE_SMALL), rowY + 3,
                SIZE_SMALL, COLOR_DIM);
        rowY += 24;
        drawModList(margin, rowY, WIDTH - 2 * margin, MOD_ROWS_VISIBLE * MOD_ROW_HEIGHT + 8);
        rowY += MOD_ROWS_VISIBLE * MOD_ROW_HEIGHT + 8;

        textCentered("Settings are saved to your Towns user folder when you press Play",
                WIDTH / 2f, rowY + 8, SIZE_SMALL, COLOR_DIM);

        // Play, gated on the assets being in place
        if (button((WIDTH - 180) / 2f, HEIGHT - 62, 180, 44, "Play", assetState == AssetState.PRESENT)) {
            playPressed = true;
        }
    }

    // ------------------------------------------------------------------
    // Proprietary-asset setup (detection and copy via xaos.setup)

    private static void initAssetState() {
        if (FirstRunSetup.assetsPresent(FirstRunSetup.dataDir())) {
            assetState = AssetState.PRESENT;
            return;
        }
        assetState = AssetState.MISSING;
        Path detected = SteamLocator.findTownsInstall();
        steamInstallData = FirstRunSetup.validateSelection(detected);
        if (steamInstallData != null) {
            assetMessage = "Towns found at " + detected + ".";
        } else {
            assetMessage = "OpenTowns needs the assets from the original game (Towns on Steam).";
        }
    }

    private static void drawAssetsRow(float labelX, float y) {
        text("Game assets", labelX, y, SIZE_LABEL, COLOR_TEXT);

        float right = WIDTH - 32;
        if (assetState == AssetState.PRESENT) {
            text("Installed", right - textWidth("Installed", SIZE_LABEL), y, SIZE_LABEL, COLOR_DIM);
        } else if (assetState == AssetState.COPYING) {
            text("Copying...", right - textWidth("Copying...", SIZE_LABEL), y, SIZE_LABEL, COLOR_DIM);
        } else {
            float buttonHeight = 24;
            float browseWidth = 90;
            float browseX = right - browseWidth;
            if (smallButton(browseX, y - 4, browseWidth, buttonHeight, "Browse...")) {
                browseForAssets();
            }
            if (steamInstallData != null) {
                float copyWidth = 130;
                if (smallButton(browseX - 8 - copyWidth, y - 4, copyWidth, buttonHeight, "Copy from Steam")) {
                    startCopy(steamInstallData);
                }
            }
        }
    }

    /**
     * Native folder picker (tinyfd, same as FirstRunSetup's fallback flow).
     * Modal: the launcher window does not repaint while it is open.
     */
    private static void browseForAssets() {
        String picked = TinyFileDialogs.tinyfd_selectFolderDialog(
                "Select your Towns installation folder", System.getProperty("user.home", ""));
        if (picked == null) {
            return;
        }
        Path dataRoot = FirstRunSetup.validateSelection(Path.of(picked));
        if (dataRoot == null) {
            assetMessageIsError = true;
            assetMessage = "That folder does not look like a Towns install (no data/graphics inside).";
        } else {
            startCopy(dataRoot);
        }
    }

    /**
     * Copies on a background thread so the window stays responsive; the
     * tmp-then-rename scheme in FirstRunSetup.copyAssets keeps an interrupted
     * copy (say, the player quits mid-way) from ever looking complete.
     */
    private static void startCopy(Path sourceData) {
        assetState = AssetState.COPYING;
        assetMessageIsError = false;
        assetMessage = "Copying assets from " + sourceData + " ...";
        Thread copier = new Thread(() -> {
            try {
                FirstRunSetup.copyAssets(sourceData, FirstRunSetup.dataDir());
                if (FirstRunSetup.assetsPresent(FirstRunSetup.dataDir())) {
                    assetState = AssetState.PRESENT;
                    assetMessage = "";
                } else {
                    assetState = AssetState.MISSING;
                    assetMessageIsError = true;
                    assetMessage = "Copy finished but the assets are still missing.";
                }
            } catch (IOException e) {
                assetState = AssetState.MISSING;
                assetMessageIsError = true;
                assetMessage = "Copy failed: " + e.getMessage();
            }
        }, "launcher-asset-copy");
        copier.setDaemon(true);
        copier.start();
    }

    private static void drawLogo(float x, float y, float size) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, logoTexture);
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + size, y);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + size, y + size);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + size);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    // ------------------------------------------------------------------
    // Window-size stepper

    private static void buildResolutionPresets() {
        int desktopWidth = Integer.MAX_VALUE;
        int desktopHeight = Integer.MAX_VALUE;
        GLFWVidMode desktop = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (desktop != null) {
            desktopWidth = desktop.width();
            desktopHeight = desktop.height();
        }

        // Common sizes at or above the game's minimum (Game.MIN_DISPLAY_*),
        // capped at the desktop; the desktop size itself closes the list.
        int[][] candidates = {
            {1024, 600}, {1280, 720}, {1280, 800}, {1366, 768}, {1440, 900},
            {1600, 900}, {1680, 1050}, {1920, 1080}, {2560, 1440}, {3840, 2160},
        };
        for (int[] candidate : candidates) {
            if (candidate[0] <= desktopWidth && candidate[1] <= desktopHeight) {
                resolutions.add(candidate);
            }
        }
        if (desktop != null && !containsResolution(desktopWidth, desktopHeight)) {
            resolutions.add(new int[] {desktopWidth, desktopHeight});
        }
        // Keep whatever is configured selectable even if it is not a preset.
        if (!containsResolution(config.windowWidth, config.windowHeight)) {
            int insert = resolutions.size();
            for (int i = 0; i < resolutions.size(); i++) {
                if (resolutions.get(i)[0] > config.windowWidth) {
                    insert = i;
                    break;
                }
            }
            resolutions.add(insert, new int[] {config.windowWidth, config.windowHeight});
        }
        for (int i = 0; i < resolutions.size(); i++) {
            if (resolutions.get(i)[0] == config.windowWidth && resolutions.get(i)[1] == config.windowHeight) {
                resolutionIndex = i;
                break;
            }
        }
    }

    private static boolean containsResolution(int width, int height) {
        for (int[] resolution : resolutions) {
            if (resolution[0] == width && resolution[1] == height) {
                return true;
            }
        }
        return false;
    }

    private static void drawResolutionRow(float labelX, float y) {
        boolean enabled = !config.fullscreen;
        text("Window size", labelX, y, SIZE_LABEL, enabled ? COLOR_TEXT : COLOR_DIM);

        float controlRight = WIDTH - 32;
        float arrowSize = 22;
        float valueWidth = 110;
        float rightArrowX = controlRight - arrowSize;
        float leftArrowX = rightArrowX - valueWidth - arrowSize;

        if (arrowButton(leftArrowX, y - 3, arrowSize, "<", enabled) && resolutionIndex > 0) {
            resolutionIndex--;
        }
        if (arrowButton(rightArrowX, y - 3, arrowSize, ">", enabled) && resolutionIndex < resolutions.size() - 1) {
            resolutionIndex++;
        }
        int[] resolution = resolutions.get(resolutionIndex);
        config.windowWidth = resolution[0];
        config.windowHeight = resolution[1];
        String label = resolution[0] + " x " + resolution[1];
        textCentered(label, leftArrowX + arrowSize + valueWidth / 2f, y, SIZE_LABEL,
                enabled ? COLOR_TEXT : COLOR_DIM);
    }

    // ------------------------------------------------------------------
    // Mods list

    private static void drawModList(float x, float y, float width, float height) {
        panel(x, y, width, height);

        if (config.availableMods.isEmpty()) {
            text("No mods found in " + config.getUserTownsFolder().resolve("mods"),
                    x + 10, y + 10, SIZE_SMALL, COLOR_DIM);
            return;
        }

        int maxScroll = Math.max(0, config.availableMods.size() - MOD_ROWS_VISIBLE);
        if (scrollY != 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            modScroll = Math.max(0, Math.min(maxScroll, modScroll - (int) Math.signum(scrollY)));
        }

        for (int row = 0; row < MOD_ROWS_VISIBLE; row++) {
            int index = modScroll + row;
            if (index >= config.availableMods.size()) {
                break;
            }
            String mod = config.availableMods.get(index);
            boolean missing = !config.modExistsOnDisk(mod);
            float rowY = y + 6 + row * MOD_ROW_HEIGHT;
            boolean checked = config.enabledMods.contains(mod);
            String label = missing ? mod + " (missing)" : mod;
            boolean newChecked = labeledCheckbox(label, x + 10, rowY, checked, !missing || checked);
            if (newChecked != checked) {
                if (newChecked) {
                    config.enabledMods.add(mod);
                } else {
                    config.enabledMods.remove(mod);
                }
            }
        }

        if (maxScroll > 0) {
            // Thin scroll indicator on the right edge of the list box.
            float trackX = x + width - 6;
            float thumbHeight = height * MOD_ROWS_VISIBLE / config.availableMods.size();
            float thumbY = y + (height - thumbHeight) * modScroll / maxScroll;
            setColor(COLOR_BORDER);
            rect(trackX, thumbY, 3, thumbHeight);
        }
    }

    // ------------------------------------------------------------------
    // Widgets

    private static boolean button(float x, float y, float w, float h, String label, boolean enabled) {
        boolean hover = enabled && mouseOver(x, y, w, h);
        if (enabled) {
            setColor(hover ? COLOR_PLAY_HOVER : COLOR_PLAY);
        } else {
            setColor(COLOR_PANEL);
        }
        rect(x, y, w, h);
        if (!enabled) {
            outline(x, y, w, h);
        }
        float textW = textWidth(label, SIZE_BUTTON);
        text(label, x + (w - textW) / 2f, y + (h - SIZE_BUTTON) / 2f - 1, SIZE_BUTTON,
                enabled ? COLOR_WHITE : COLOR_DIM);
        return hover && mouseClicked;
    }

    private static boolean smallButton(float x, float y, float w, float h, String label) {
        boolean hover = mouseOver(x, y, w, h);
        setColor(hover ? COLOR_PANEL_HOVER : COLOR_PANEL);
        rect(x, y, w, h);
        outline(x, y, w, h);
        float textW = textWidth(label, SIZE_SMALL);
        text(label, x + (w - textW) / 2f, y + (h - SIZE_SMALL) / 2f - 1, SIZE_SMALL, COLOR_TEXT);
        return hover && mouseClicked;
    }

    private static boolean arrowButton(float x, float y, float size, String label, boolean enabled) {
        boolean hover = enabled && mouseOver(x, y, size, size);
        setColor(hover ? COLOR_PANEL_HOVER : COLOR_PANEL);
        rect(x, y, size, size);
        outline(x, y, size, size);
        float[] textColor = enabled ? COLOR_TEXT : COLOR_DIM;
        text(label, x + (size - textWidth(label, SIZE_LABEL)) / 2f, y + (size - SIZE_LABEL) / 2f - 1,
                SIZE_LABEL, textColor);
        return hover && mouseClicked;
    }

    /** Label at x with the checkbox right after it; both are clickable. */
    private static boolean labeledCheckbox(String label, float x, float y, boolean checked, boolean enabled) {
        float box = 16;
        float labelWidth = textWidth(label, SIZE_LABEL);
        float boxX = x + labelWidth + 12;
        text(label, x, y, SIZE_LABEL, enabled ? COLOR_TEXT : COLOR_DIM);

        setColor(COLOR_PANEL);
        rect(boxX, y, box, box);
        outline(boxX, y, box, box);
        if (checked) {
            setColor(enabled ? COLOR_FILL : COLOR_DIM);
            rect(boxX + 4, y + 4, box - 8, box - 8);
        }
        // The label is clickable too.
        boolean hit = mouseOver(x, y - 2, boxX + box - x, box + 4);
        if (enabled && hit && mouseClicked) {
            return !checked;
        }
        return checked;
    }

    private static int volumeSlider(int id, float x, float y, int value, boolean enabled) {
        float width = 170;
        float trackY = y + 7;

        setColor(COLOR_PANEL);
        rect(x, trackY, width, 4);
        outline(x, trackY, width, 4);

        float handleX = x + (value - 1) / 9f * width;
        setColor(enabled ? COLOR_FILL : COLOR_DIM);
        rect(handleX - 4, y + 1, 8, 16);

        text(Integer.toString(value), x + width + 12, y, SIZE_LABEL, enabled ? COLOR_TEXT : COLOR_DIM);

        if (enabled) {
            if (mouseClicked && mouseOver(x - 6, y - 2, width + 12, 22)) {
                activeSlider = id;
            }
            if (activeSlider == id && mouseDown) {
                int newValue = 1 + Math.round((float) (mouseX - x) / width * 9f);
                return Math.max(1, Math.min(10, newValue));
            }
        }
        return value;
    }

    // ------------------------------------------------------------------
    // Drawing primitives

    private static boolean mouseOver(float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static void setColor(float[] color) {
        GL11.glColor3f(color[0], color[1], color[2]);
    }

    private static void rect(float x, float y, float w, float h) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    private static void outline(float x, float y, float w, float h) {
        setColor(COLOR_BORDER);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x + 0.5f, y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + 0.5f);
        GL11.glVertex2f(x + w - 0.5f, y + h - 0.5f);
        GL11.glVertex2f(x + 0.5f, y + h - 0.5f);
        GL11.glEnd();
    }

    private static void panel(float x, float y, float w, float h) {
        setColor(COLOR_PANEL);
        rect(x, y, w, h);
        outline(x, y, w, h);
    }

    private static void line(float x1, float y1, float x2, float y2) {
        setColor(COLOR_BORDER);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1 + 0.5f);
        GL11.glVertex2f(x2, y2 + 0.5f);
        GL11.glEnd();
    }

    // ------------------------------------------------------------------
    // Text: system font via stb_truetype, stb_easy_font as the fallback

    private static void loadFonts() {
        ByteBuffer ttf = Font.findSystemFont();
        if (ttf == null) {
            return;
        }
        fontSmall = Font.bake(ttf, SIZE_SMALL);
        fontLabel = Font.bake(ttf, SIZE_LABEL);
        fontButton = Font.bake(ttf, SIZE_BUTTON);
        fontTitle = Font.bake(ttf, SIZE_TITLE);
        if (fontSmall == null || fontLabel == null || fontButton == null || fontTitle == null) {
            disposeFonts(); // partial bake: fall back to easy_font entirely
        }
    }

    private static void disposeFonts() {
        for (Font font : new Font[] {fontSmall, fontLabel, fontButton, fontTitle}) {
            if (font != null) {
                font.dispose();
            }
        }
        fontSmall = fontLabel = fontButton = fontTitle = null;
    }

    private static Font fontFor(float size) {
        if (size == SIZE_SMALL) {
            return fontSmall;
        }
        if (size == SIZE_LABEL) {
            return fontLabel;
        }
        if (size == SIZE_BUTTON) {
            return fontButton;
        }
        if (size == SIZE_TITLE) {
            return fontTitle;
        }
        return fontLabel;
    }

    /** Draws s with its top-left corner at (x, y) at the given pixel size. */
    private static void text(String s, float x, float y, float size, float[] color) {
        Font font = fontFor(size);
        if (font != null) {
            font.draw(s, x, y, color);
        } else {
            textEasy(s, x, y, size / 12f, color);
        }
    }

    private static void textCentered(String s, float centerX, float y, float size, float[] color) {
        text(s, centerX - textWidth(s, size) / 2f, y, size, color);
    }

    private static float textWidth(String s, float size) {
        Font font = fontFor(size);
        if (font != null) {
            return font.width(s);
        }
        return STBEasyFont.stb_easy_font_width(s) * size / 12f;
    }

    /** Truncates s with a "..." tail when wider than maxWidth (long paths). */
    private static String fit(String s, float size, float maxWidth) {
        if (textWidth(s, size) <= maxWidth) {
            return s;
        }
        while (s.length() > 1 && textWidth(s + "...", size) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    /** stb_easy_font fallback: quads only, so it scales without artifacts. */
    private static void textEasy(String s, float x, float y, float scale, float[] color) {
        setColor(color);
        easyFontBuffer.clear();
        int quads = STBEasyFont.stb_easy_font_print(0, 0, s, null, easyFontBuffer);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 16, easyFontBuffer);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, quads * 4);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glPopMatrix();
    }

    /**
     * One system font baked at one pixel size into a luminance-alpha texture
     * (luminance stays white so glColor tints the text; a pure GL_ALPHA
     * texture would multiply the color to black under GL_MODULATE).
     * Coordinates are top-left anchored like the rest of the drawing here;
     * draw() converts to the font baseline internally.
     */
    private static final class Font {

        private static final int BITMAP_SIZE = 512;
        private static final int FIRST_CHAR = 32;
        private static final int CHAR_COUNT = 96; // printable ASCII

        private final int texture;
        private final STBTTBakedChar.Buffer chars;
        private final float ascent;

        private Font(int texture, STBTTBakedChar.Buffer chars, float ascent) {
            this.texture = texture;
            this.chars = chars;
            this.ascent = ascent;
        }

        /** The first readable font from the per-OS candidate list, or null. */
        static ByteBuffer findSystemFont() {
            String os = System.getProperty("os.name", "").toLowerCase();
            String[] candidates;
            if (os.contains("win")) {
                String windir = System.getenv("WINDIR");
                String fonts = (windir == null ? "C:\\Windows" : windir) + "\\Fonts\\";
                candidates = new String[] {fonts + "segoeui.ttf", fonts + "tahoma.ttf", fonts + "arial.ttf"};
            } else if (os.contains("mac") || os.contains("darwin")) {
                candidates = new String[] {
                    "/System/Library/Fonts/Supplemental/Arial.ttf",
                    "/System/Library/Fonts/Supplemental/Tahoma.ttf",
                    "/System/Library/Fonts/Supplemental/Verdana.ttf",
                    "/Library/Fonts/Arial.ttf",
                };
            } else {
                candidates = new String[] {
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/usr/share/fonts/dejavu/DejaVuSans.ttf",
                    "/usr/share/fonts/TTF/DejaVuSans.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                    "/usr/share/fonts/liberation-sans/LiberationSans-Regular.ttf",
                    "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
                };
            }
            for (String candidate : candidates) {
                Path path = Path.of(candidate);
                if (Files.isRegularFile(path)) {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
                        buffer.put(bytes).flip();
                        return buffer;
                    } catch (IOException e) {
                        // try the next candidate
                    }
                }
            }
            return null;
        }

        /** Bakes one pixel size; null when baking fails. Needs a GL context. */
        static Font bake(ByteBuffer ttf, float pixelHeight) {
            ByteBuffer coverage = BufferUtils.createByteBuffer(BITMAP_SIZE * BITMAP_SIZE);
            STBTTBakedChar.Buffer chars = STBTTBakedChar.malloc(CHAR_COUNT);
            int rows = STBTruetype.stbtt_BakeFontBitmap(ttf, pixelHeight, coverage,
                    BITMAP_SIZE, BITMAP_SIZE, FIRST_CHAR, chars);
            if (rows <= 0) {
                chars.free();
                return null;
            }

            float ascent = pixelHeight * 0.8f; // fallback if metrics fail
            try (MemoryStack stack = MemoryStack.stackPush()) {
                STBTTFontinfo info = STBTTFontinfo.malloc(stack);
                if (STBTruetype.stbtt_InitFont(info, ttf)) {
                    IntBuffer fontAscent = stack.mallocInt(1);
                    IntBuffer fontDescent = stack.mallocInt(1);
                    IntBuffer lineGap = stack.mallocInt(1);
                    STBTruetype.stbtt_GetFontVMetrics(info, fontAscent, fontDescent, lineGap);
                    ascent = fontAscent.get(0) * STBTruetype.stbtt_ScaleForPixelHeight(info, pixelHeight);
                }
            }

            // Expand coverage to luminance(white) + alpha(coverage).
            ByteBuffer pixels = BufferUtils.createByteBuffer(BITMAP_SIZE * BITMAP_SIZE * 2);
            for (int i = 0; i < BITMAP_SIZE * BITMAP_SIZE; i++) {
                pixels.put((byte) 0xFF);
                pixels.put(coverage.get(i));
            }
            pixels.flip();

            int texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_LUMINANCE_ALPHA, BITMAP_SIZE, BITMAP_SIZE, 0,
                    GL11.GL_LUMINANCE_ALPHA, GL11.GL_UNSIGNED_BYTE, pixels);

            return new Font(texture, chars, ascent);
        }

        /** Draws s with its top-left corner at (x, y). */
        void draw(String s, float x, float y, float[] color) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glColor3f(color[0], color[1], color[2]);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer penX = stack.floats(x);
                FloatBuffer penY = stack.floats(y + ascent);
                STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);
                GL11.glBegin(GL11.GL_QUADS);
                for (int i = 0; i < s.length(); i++) {
                    STBTruetype.stbtt_GetBakedQuad(chars, BITMAP_SIZE, BITMAP_SIZE,
                            charIndex(s.charAt(i)), penX, penY, quad, true);
                    GL11.glTexCoord2f(quad.s0(), quad.t0());
                    GL11.glVertex2f(quad.x0(), quad.y0());
                    GL11.glTexCoord2f(quad.s1(), quad.t0());
                    GL11.glVertex2f(quad.x1(), quad.y0());
                    GL11.glTexCoord2f(quad.s1(), quad.t1());
                    GL11.glVertex2f(quad.x1(), quad.y1());
                    GL11.glTexCoord2f(quad.s0(), quad.t1());
                    GL11.glVertex2f(quad.x0(), quad.y1());
                }
                GL11.glEnd();
            }
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        float width(String s) {
            float width = 0;
            for (int i = 0; i < s.length(); i++) {
                width += chars.get(charIndex(s.charAt(i))).xadvance();
            }
            return width;
        }

        private static int charIndex(char c) {
            return (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) ? '?' - FIRST_CHAR : c - FIRST_CHAR;
        }

        void dispose() {
            GL11.glDeleteTextures(texture);
            chars.free();
        }
    }
}
