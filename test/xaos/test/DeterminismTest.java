package xaos.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Determinism regression: the core guarantee of headless mode. Same seed and
 * tick count must produce an identical world state (compared via the state
 * hash TownsHeadless prints); different seeds must diverge. Each run is a
 * fresh child JVM (see HeadlessRunner for why).
 *
 * These tests protect every future refactor (UIPanel/Utils decoupling and
 * beyond): if one of them fails, a change leaked nondeterminism into the sim
 * or altered vanilla behavior.
 */
class DeterminismTest {

    // Seeded runs of the same configuration are memoized so tests can share
    // them instead of re-running a ~6 s child JVM.
    private static final Map<String, HeadlessRunner.Result> CACHE = new HashMap<String, HeadlessRunner.Result>();

    @BeforeAll
    static void checkWorkingDirectory() {
        if (!Files.exists(Path.of("towns.ini"))) {
            throw new IllegalStateException("Working directory must be src/ (run via gradlew test)");
        }
    }

    private static HeadlessRunner.Result seededRun(long seed, long ticks, String map, int repetition) throws Exception {
        String key = seed + ":" + ticks + ":" + map + ":" + repetition;
        HeadlessRunner.Result cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Path userFolder = HeadlessRunner.newUserFolder();
        try {
            HeadlessRunner.Result result = HeadlessRunner.run(userFolder,
                    "--seed=" + seed, "--ticks=" + ticks, "--map=" + map);
            assertEquals(0, result.exitCode, "TownsHeadless failed:\n" + result.output);
            assertNotNull(result.stateHash, "no state hash in output:\n" + result.output);
            assertNotNull(result.terrainHash, "no terrain hash in output:\n" + result.output);
            CACHE.put(key, result);
            return result;
        } finally {
            HeadlessRunner.deleteRecursively(userFolder);
        }
    }

    private static void assertSameSeedSameState(long seed, long ticks, String map) throws Exception {
        HeadlessRunner.Result first = seededRun(seed, ticks, map, 1);
        HeadlessRunner.Result second = seededRun(seed, ticks, map, 2);
        assertEquals(first.terrainHash, second.terrainHash,
                "terrain diverged for seed " + seed + " map " + map);
        assertEquals(first.stateHash, second.stateHash,
                "world state diverged for seed " + seed + " map " + map);
    }

    @Test
    void sameSeedSameState_seed42_normal() throws Exception {
        assertSameSeedSameState(42, 3000, "normal");
    }

    @Test
    void sameSeedSameState_seed1234_desert() throws Exception {
        assertSameSeedSameState(1234, 3000, "desert");
    }

    @Test
    void sameSeedSameState_longRun() throws Exception {
        assertSameSeedSameState(42, 20000, "normal");
    }

    @Test
    void differentSeedsDifferentState() throws Exception {
        HeadlessRunner.Result seed42 = seededRun(42, 3000, "normal", 1);
        HeadlessRunner.Result seed1234 = seededRun(1234, 3000, "normal", 1);
        assertNotEquals(seed42.stateHash, seed1234.stateHash, "different seeds produced the same world");
    }

    @Test
    void unseededRunCompletes() throws Exception {
        // Vanilla configuration: time-seeded RNG, async A* worker. Only
        // asserts the run finishes cleanly and reports a state.
        Path userFolder = HeadlessRunner.newUserFolder();
        try {
            HeadlessRunner.Result result = HeadlessRunner.run(userFolder, "--ticks=500");
            assertEquals(0, result.exitCode, "unseeded run failed:\n" + result.output);
            assertNotNull(result.stateHash, "no state hash in output:\n" + result.output);
        } finally {
            HeadlessRunner.deleteRecursively(userFolder);
        }
    }
}
