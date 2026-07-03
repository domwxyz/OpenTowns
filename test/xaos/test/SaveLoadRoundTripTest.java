package xaos.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Save/load round-trip: saving a world and loading it back must reproduce
 * the same state hash. This exercises the entire hand-rolled Externalizable
 * chain (World, cells, entities), the highest-risk area when fields change.
 *
 * Two child JVMs share one sandbox user folder: run A generates, ticks,
 * saves and prints its hash; run B loads the save with --ticks=0 and prints
 * the hash of the freshly loaded state.
 */
class SaveLoadRoundTripTest {

    private static Path userFolder;

    @BeforeAll
    static void setup() throws Exception {
        if (!Files.exists(Path.of("towns.ini"))) {
            throw new IllegalStateException("Working directory must be src/ (run via gradlew test)");
        }
        userFolder = HeadlessRunner.newUserFolder();
    }

    @AfterAll
    static void cleanup() {
        HeadlessRunner.deleteRecursively(userFolder);
    }

    @Test
    void roundTripPreservesStateHash() throws Exception {
        HeadlessRunner.Result saved = HeadlessRunner.run(userFolder,
                "--seed=42", "--ticks=1500", "--map=normal", "--save=roundtrip");
        assertEquals(0, saved.exitCode, "save run failed:\n" + saved.output);
        assertNotNull(saved.stateHash, "no state hash in save run:\n" + saved.output);

        HeadlessRunner.Result loaded = HeadlessRunner.run(userFolder,
                "--load=roundtrip", "--ticks=0");
        assertEquals(0, loaded.exitCode, "load run failed:\n" + loaded.output);
        assertNotNull(loaded.stateHash, "no state hash in load run:\n" + loaded.output);

        assertEquals(saved.terrainHash, loaded.terrainHash, "terrain changed across save/load");
        assertEquals(saved.stateHash, loaded.stateHash, "world state changed across save/load");
    }

    @Test
    void loadedGameKeepsSimulating() throws Exception {
        // Not a determinism assertion (RNG state is not serialized, so a
        // resumed run legitimately diverges from an uninterrupted one); just
        // proves a loaded world can tick onward without blowing up.
        HeadlessRunner.Result saved = HeadlessRunner.run(userFolder,
                "--seed=77", "--ticks=500", "--map=normal", "--save=resume");
        assertEquals(0, saved.exitCode, "save run failed:\n" + saved.output);

        HeadlessRunner.Result resumed = HeadlessRunner.run(userFolder,
                "--load=resume", "--seed=77", "--ticks=500");
        assertEquals(0, resumed.exitCode, "resumed run failed:\n" + resumed.output);
        assertNotNull(resumed.stateHash, "no state hash in resumed run:\n" + resumed.output);
    }
}
