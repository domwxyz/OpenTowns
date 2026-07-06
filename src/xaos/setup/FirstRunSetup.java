package xaos.setup;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * First-run asset setup: if the proprietary Towns assets (data/graphics,
 * data/audio, data/fonts) are missing from the working directory, locate a
 * Steam Towns install (or ask the user for one) and copy them in.
 *
 * Runs at the top of Towns.main, before any Game/GLFW/AL initialization;
 * TownsHeadless never calls it. The launcher (xaos.launcher) normally takes
 * care of the assets first through the public helpers here, making run() a
 * no-op; the run() dialog flow remains as the fallback for
 * -Dtowns.skipLauncher=true and launcher-less startup paths. The data/ folder names are hardcoded on
 * purpose: reading GRAPHICS_FOLDER etc. through Towns.getPropertiesString
 * would class-initialize Game, and a player who customized the folder layout
 * already has assets and never enters setup. Dialog text avoids quote
 * characters; the macOS tinyfd backend goes through osascript.
 */
public final class FirstRunSetup {

    static final String[] REQUIRED_FOLDERS = { "graphics", "audio", "fonts" };

    private FirstRunSetup() {
    }

    /**
     * The data/ directory the proprietary assets are copied into. Rooted at the
     * app home (towns.home, set by the packaged launcher to $APPDIR) when
     * present, otherwise resolved against the working directory as before. Read
     * straight from the system property, not via Towns, to keep this class free
     * of game-package imports (it must not class-initialize Game or draw RNG).
     */
    public static Path dataDir() {
        String home = System.getProperty("towns.home", "").trim();
        return home.isEmpty() ? Path.of("data") : Path.of(home, "data");
    }

    /** Returns normally when the assets are present; exits the JVM otherwise. */
    public static void run() {
        Path data = dataDir();
        if (assetsPresent(data)) {
            return;
        }

        Path sourceData = null;
        Path detected = SteamLocator.findTownsInstall();
        if (detected != null && confirmDetected(detected)) {
            sourceData = validateSelection(detected);
        }
        if (sourceData == null) {
            sourceData = pickFolderLoop();
        }

        try {
            copyAssets(sourceData, data);
        } catch (IOException e) {
            e.printStackTrace();
            error("Copying the game assets failed:\n" + e.getMessage()
                    + "\nNothing was left half-copied; run the game again to retry.");
            System.exit(1);
        }

        if (!assetsPresent(data)) {
            error("Setup finished but the assets are still missing.\n"
                    + "Please copy data/graphics, data/audio and data/fonts\n"
                    + "from your Towns install into " + data.toAbsolutePath() + " manually.");
            System.exit(1);
        }

        TinyFileDialogs.tinyfd_messageBox("OpenTowns setup",
                "Assets installed. Starting the game.", "ok", "info", true);
    }

    /** True when data/graphics, data/audio and data/fonts all exist and are non-empty. */
    public static boolean assetsPresent(Path data) {
        for (String name : REQUIRED_FOLDERS) {
            if (!isNonEmptyDir(data.resolve(name))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accepts either the Towns install root (contains data/graphics and
     * data/fonts) or a data folder directly (contains graphics/ and fonts/).
     * Returns the normalized data root, or null when the folder does not look
     * like a Towns install.
     */
    public static Path validateSelection(Path picked) {
        if (picked == null) {
            return null;
        }
        Path asRoot = picked.resolve("data");
        if (Files.isDirectory(asRoot.resolve("graphics")) && Files.isDirectory(asRoot.resolve("fonts"))) {
            return asRoot;
        }
        if (Files.isDirectory(picked.resolve("graphics")) && Files.isDirectory(picked.resolve("fonts"))) {
            return picked;
        }
        return null;
    }

    /**
     * Copies each missing or empty required folder from sourceData into
     * targetData. Each folder is copied to a sibling <name>.tmp first and then
     * renamed into place, so an interrupted copy can never look like a complete
     * install on the next run. Folders already present and non-empty are kept.
     */
    public static void copyAssets(Path sourceData, Path targetData) throws IOException {
        Files.createDirectories(targetData);
        for (String name : REQUIRED_FOLDERS) {
            Path target = targetData.resolve(name);
            Path tmp = targetData.resolve(name + ".tmp");
            deleteRecursively(tmp); // stale tmp from an interrupted earlier run
            if (isNonEmptyDir(target)) {
                continue;
            }
            Path source = sourceData.resolve(name);
            if (!Files.isDirectory(source)) {
                throw new IOException("Missing folder in the Towns install: " + source);
            }
            System.out.print("Copying " + name + "...");
            try {
                copyRecursively(source, tmp);
            } catch (IOException e) {
                try {
                    deleteRecursively(tmp);
                } catch (IOException ignored) {
                }
                throw e;
            }
            deleteRecursively(target); // may exist but empty
            Files.move(tmp, target);
            System.out.println(" done");
        }
    }

    private static boolean isNonEmptyDir(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findFirst().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> tree = Files.walk(root)) {
            for (Path path : tree.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    // --- dialogs: the only tinyfd touchpoints, never reached by tests ---

    private static boolean confirmDetected(Path install) {
        return TinyFileDialogs.tinyfd_messageBox("OpenTowns setup",
                "OpenTowns needs the graphics, audio and font assets from the original game.\n"
                + "A Towns installation was found at:\n" + install.toAbsolutePath() + "\n"
                + "Copy the assets from there?", "yesno", "question", true);
    }

    private static Path pickFolderLoop() {
        while (true) {
            String picked = TinyFileDialogs.tinyfd_selectFolderDialog(
                    "Select your Towns installation folder", System.getProperty("user.home", ""));
            if (picked == null) {
                boolean exit = TinyFileDialogs.tinyfd_messageBox("OpenTowns setup",
                        "OpenTowns cannot run without the assets from the original game\n"
                        + "(Towns on Steam, app 221020). Exit setup?", "yesno", "question", true);
                if (exit) {
                    System.out.println("First-run setup cancelled. To set up manually, copy the");
                    System.out.println("data/graphics, data/audio and data/fonts folders from your");
                    System.out.println("Towns install into " + dataDir().toAbsolutePath() + ".");
                    System.exit(0);
                }
                continue;
            }
            Path dataRoot = validateSelection(Path.of(picked));
            if (dataRoot != null) {
                return dataRoot;
            }
            TinyFileDialogs.tinyfd_messageBox("OpenTowns setup",
                    "That folder does not look like a Towns installation\n"
                    + "(no data/graphics inside). Please select the folder that\n"
                    + "contains the Towns executable, or its data folder.", "ok", "error", true);
        }
    }

    private static void error(String message) {
        TinyFileDialogs.tinyfd_messageBox("OpenTowns setup", message, "ok", "error", true);
    }
}
