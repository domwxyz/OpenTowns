package xaos.setup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locates a Steam install of Towns (app 221020) without any UI.
 *
 * Like the rest of xaos.setup, this imports nothing from the game packages:
 * it runs before Game exists and must not class-initialize anything that
 * touches config, GL or the game RNG.
 */
public final class SteamLocator {

    private static final Pattern VDF_PATH = Pattern.compile("\"path\"\\s+\"(.*)\"");

    private SteamLocator() {
    }

    /** First Steam library containing a valid Towns install, or null. */
    public static Path findTownsInstall() {
        for (Path root : steamRoots()) {
            for (Path library : libraryFolders(root)) {
                Path towns = library.resolve("steamapps").resolve("common").resolve("Towns");
                if (FirstRunSetup.validateSelection(towns) != null) {
                    return towns;
                }
            }
        }
        return null;
    }

    /** Steam root candidates that exist on this machine, most likely first. */
    static List<Path> steamRoots() {
        String osName = System.getProperty("os.name", "");
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        if (osName.toLowerCase().contains("win")) {
            windowsRegistrySteamPath().ifPresent(roots::add);
        }
        roots.addAll(steamRootsFor(osName, Path.of(System.getProperty("user.home", "."))));

        List<Path> existing = new ArrayList<>();
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                existing.add(root);
            }
        }
        return existing;
    }

    /** Default Steam install locations per OS. Pure: no filesystem access. */
    static List<Path> steamRootsFor(String osName, Path home) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("win")) {
            return List.of(
                    Path.of("C:", "Program Files (x86)", "Steam"),
                    Path.of("C:", "Program Files", "Steam"));
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return List.of(home.resolve("Library").resolve("Application Support").resolve("Steam"));
        }
        return List.of(
                home.resolve(".steam").resolve("steam"),
                home.resolve(".local").resolve("share").resolve("Steam"),
                home.resolve(".var").resolve("app").resolve("com.valvesoftware.Steam")
                        .resolve(".local").resolve("share").resolve("Steam"));
    }

    /** Steam install path from the Windows registry; empty on any failure. */
    static Optional<Path> windowsRegistrySteamPath() {
        try {
            Process process = new ProcessBuilder(
                    "reg", "query", "HKCU\\Software\\Valve\\Steam", "/v", "SteamPath")
                    .redirectErrorStream(true)
                    .start();
            List<String> lines;
            try (var reader = process.inputReader()) {
                lines = reader.lines().toList();
            }
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            for (String line : lines) {
                int marker = line.indexOf("REG_SZ");
                if (marker >= 0 && line.contains("SteamPath")) {
                    String value = line.substring(marker + "REG_SZ".length()).trim();
                    if (!value.isEmpty()) {
                        return Optional.of(Path.of(value));
                    }
                }
            }
        } catch (Throwable t) {
            // No reg.exe, no permission, interrupted: fall back to default paths.
        }
        return Optional.empty();
    }

    /** The Steam root itself plus extra libraries from libraryfolders.vdf. */
    static List<Path> libraryFolders(Path steamRoot) {
        List<Path> libraries = new ArrayList<>();
        libraries.add(steamRoot);
        Path vdf = steamRoot.resolve("steamapps").resolve("libraryfolders.vdf");
        try {
            for (Path path : parseLibraryFoldersVdf(Files.readAllLines(vdf))) {
                if (!libraries.contains(path)) {
                    libraries.add(path);
                }
            }
        } catch (IOException e) {
            // Missing or unreadable vdf: the root alone still counts as a library.
        }
        return libraries;
    }

    /** Line scan for "path" entries; not a real VDF parser. Pure. */
    static List<Path> parseLibraryFoldersVdf(List<String> lines) {
        List<Path> paths = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = VDF_PATH.matcher(line);
            if (matcher.find()) {
                paths.add(Path.of(matcher.group(1).replace("\\\\", "\\")));
            }
        }
        return paths;
    }
}
