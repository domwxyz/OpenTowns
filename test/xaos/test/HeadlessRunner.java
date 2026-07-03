package xaos.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Forks xaos.TownsHeadless as a child JVM and parses its summary output.
 *
 * Each child gets a fresh JVM on purpose: the game's static state (Game,
 * World, the managers, World.maxEntityID) does not reset cleanly for a
 * second new game in the same JVM, so run-to-run comparisons are only
 * meaningful across processes. The child reuses this JVM's java binary and
 * classpath and inherits the test working directory (src/), where towns.ini
 * and data/ resolve.
 */
public final class HeadlessRunner {

    /** Generous cap; a 20k-tick seeded run takes ~15 s on the dev machine. */
    private static final long TIMEOUT_SECONDS = 300;

    private HeadlessRunner() {
    }

    public static final class Result {
        public final int exitCode;
        public final String output;
        public final String stateHash;
        public final String terrainHash;

        Result(int exitCode, String output, String stateHash, String terrainHash) {
            this.exitCode = exitCode;
            this.output = output;
            this.stateHash = stateHash;
            this.terrainHash = terrainHash;
        }
    }

    /**
     * Runs TownsHeadless with the given arguments plus a --user-folder
     * pointing at the given sandbox. Pass args like "--seed=42",
     * "--ticks=3000", "--map=desert", "--save=name", "--load=name".
     */
    public static Result run(Path userFolder, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("--sun-misc-unsafe-memory-access=allow");
        command.add("xaos.TownsHeadless");
        command.add("--user-folder=" + userFolder.toAbsolutePath());
        for (String arg : args) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read stdout fully before waiting: the pipe buffer is small and the
        // child would block on a full buffer otherwise.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            in.transferTo(buffer);
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("TownsHeadless timed out after " + TIMEOUT_SECONDS + "s. Output:\n" + buffer);
        }

        String output = buffer.toString();
        return new Result(process.exitValue(), output,
                parseValue(output, "state-hash="),
                parseValue(output, "terrain-hash="));
    }

    private static String parseValue(String output, String key) {
        for (String line : output.split("\\R")) {
            int index = line.indexOf(key);
            if (index != -1) {
                return line.substring(index + key.length()).trim();
            }
        }
        return null;
    }

    /** Fresh sandbox user folder for a child run (or several sharing saves). */
    public static Path newUserFolder() throws IOException {
        return Files.createTempDirectory("opentowns-test");
    }

    /** Best-effort recursive delete of a sandbox user folder. */
    public static void deleteRecursively(Path folder) {
        if (folder == null || !Files.exists(folder)) {
            return;
        }
        try (var paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
