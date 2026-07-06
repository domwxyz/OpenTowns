package xaos.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The launcher's view of towns.ini: the pre-launch, player-facing subset
 * (window size, fullscreen, music/FX and volumes, enabled mods).
 *
 * Reads the same two config layers the game reads: the bundled towns.ini
 * (rooted at towns.home when set, like Towns.resolveHome) with the user-folder
 * towns.ini loaded on top. Saving writes only the user-folder overlay, the
 * same file the in-game options rewrite (UtilsFiles.saveOptions), and keeps
 * every key already in it (keybinds and options the launcher does not manage).
 * The bundled towns.ini is never modified.
 *
 * Like FirstRunSetup, this class imports nothing from the game packages: it
 * runs before first-run setup and must not class-initialize Game. The user
 * folder resolution (USER_FOLDER from the bundled ini, else user.home, plus
 * ".towns") mirrors Game's constructor and UtilsFiles.createUserFolder.
 */
final class LauncherConfig {

    int windowWidth = 1024;
    int windowHeight = 600;
    boolean fullscreen;
    boolean music;
    int volumeMusic = 10;
    boolean fx;
    int volumeFX = 10;

    /** Mods checked on (the MODS= list), in their configured order. */
    final LinkedHashSet<String> enabledMods = new LinkedHashSet<String>();
    /** Every mod to show: folders found in the user mods folder plus any
     *  configured mod whose folder is missing (kept so a launcher visit
     *  never silently drops it from MODS=). */
    final ArrayList<String> availableMods = new ArrayList<String>();

    private final Path userTownsFolder;

    private LauncherConfig(Path userTownsFolder) {
        this.userTownsFolder = userTownsFolder;
    }

    /** True when the mod folder exists on disk (false for a stale MODS= entry). */
    boolean modExistsOnDisk(String mod) {
        return Files.isDirectory(userTownsFolder.resolve("mods").resolve(mod));
    }

    static LauncherConfig load() {
        String home = System.getProperty("towns.home", "").trim();
        Path baseIni = home.isEmpty() ? Path.of("towns.ini") : Path.of(home, "towns.ini");

        Properties merged = new Properties();
        loadInto(merged, baseIni);

        // USER_FOLDER only ever comes from the bundled ini (the overlay lives
        // inside the folder it would name), same as the game's startup.
        String userBase = merged.getProperty("USER_FOLDER", "").trim();
        if (userBase.isEmpty()) {
            userBase = System.getProperty("user.home", "");
        }
        Path userTownsFolder = Path.of(userBase, ".towns");
        loadInto(merged, userTownsFolder.resolve("towns.ini"));

        LauncherConfig config = new LauncherConfig(userTownsFolder);
        config.windowWidth = intValue(merged, "WINDOW_WIDTH", config.windowWidth);
        config.windowHeight = intValue(merged, "WINDOW_HEIGHT", config.windowHeight);
        config.fullscreen = Boolean.parseBoolean(merged.getProperty("FULLSCREEN", "false").trim());
        config.music = Boolean.parseBoolean(merged.getProperty("MUSIC", "false").trim());
        config.fx = Boolean.parseBoolean(merged.getProperty("FX", "false").trim());
        // Same clamp as Game.setVolumeMusic/setVolumeFX: out of 1..10 means 10.
        config.volumeMusic = clampVolume(intValue(merged, "VOLUME_MUSIC", 10));
        config.volumeFX = clampVolume(intValue(merged, "VOLUME_FX", 10));

        String mods = merged.getProperty("MODS", "");
        for (String mod : mods.split(",")) {
            if (!mod.trim().isEmpty()) {
                config.enabledMods.add(mod.trim());
            }
        }

        // Discover installed mods (folders under <userfolder>/mods), then
        // append configured-but-missing ones so they stay visible.
        Path modsFolder = userTownsFolder.resolve("mods");
        if (Files.isDirectory(modsFolder)) {
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(modsFolder, Files::isDirectory)) {
                for (Path dir : dirs) {
                    config.availableMods.add(dir.getFileName().toString());
                }
            } catch (IOException e) {
                // Unreadable mods folder: show only the configured list.
            }
        }
        config.availableMods.sort(String.CASE_INSENSITIVE_ORDER);
        for (String mod : config.enabledMods) {
            if (!config.availableMods.contains(mod)) {
                config.availableMods.add(mod);
            }
        }

        return config;
    }

    /**
     * Writes the launcher-managed keys into the user-folder towns.ini,
     * preserving every other key already there. Plain "KEY = value" lines in
     * ISO-8859-1, the same format PropertiesWriter produces; the in-game
     * options save later regenerates this file wholesale from Game state,
     * which by then includes these values.
     */
    void save() throws IOException {
        Files.createDirectories(userTownsFolder);
        Path file = userTownsFolder.resolve("towns.ini");

        Properties existing = new Properties();
        loadInto(existing, file);

        TreeMap<String, String> keys = new TreeMap<String, String>();
        for (String name : existing.stringPropertyNames()) {
            keys.put(name, existing.getProperty(name));
        }
        keys.put("WINDOW_WIDTH", Integer.toString(windowWidth));
        keys.put("WINDOW_HEIGHT", Integer.toString(windowHeight));
        keys.put("FULLSCREEN", Boolean.toString(fullscreen));
        keys.put("MUSIC", Boolean.toString(music));
        keys.put("VOLUME_MUSIC", Integer.toString(volumeMusic));
        keys.put("FX", Boolean.toString(fx));
        keys.put("VOLUME_FX", Integer.toString(volumeFX));
        keys.put("MODS", String.join(",", enabledMods));

        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.ISO_8859_1)) {
            out.write("# OpenTowns user settings. Loaded on top of the towns.ini next to the game.\n");
            out.write("# Rewritten by the launcher and by the in-game options.\n");
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                out.write(entry.getKey() + " = " + entry.getValue() + "\n");
            }
        }
    }

    Path getUserTownsFolder() {
        return userTownsFolder;
    }

    private static void loadInto(Properties properties, Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            // Missing or unreadable layer: same as the game, carry on with
            // what loaded so far (Towns.loadPropertiesMain ignores a missing
            // user overlay too).
        }
    }

    private static int intValue(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int clampVolume(int volume) {
        return (volume < 1 || volume > 10) ? 10 : volume;
    }
}
