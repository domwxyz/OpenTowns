package xaos.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xaos.main.Game;
import xaos.main.World;
import xaos.tiles.entities.living.LivingEntity;
import xaos.utils.AStarQueue;
import xaos.utils.Utils;

/**
 * Long-run simulation smoke test: 20,000 ticks in-JVM on a seeded world,
 * then invariants over the surviving state. The run is deterministic, so
 * these assertions either always hold or always fail for this seed; there
 * is no flake margin.
 */
class LongRunSmokeTest {

    private static final long SEED = 4242;
    private static final long TICKS = 20000;

    private static Path userFolder;
    private static World world;

    private static int startYear;
    private static int startMonth;
    private static int startDay;
    private static int startTurn;

    @BeforeAll
    static void bootAndRun() throws Exception {
        if (!Files.exists(Path.of("towns.ini"))) {
            throw new IllegalStateException("Working directory must be src/ (run via gradlew test)");
        }
        userFolder = HeadlessRunner.newUserFolder();
        Game.initHeadless(userFolder.toString());
        AStarQueue.setSynchronousMode(true);
        Utils.setRandomSeed(SEED);
        Game.startGame("c1", "normal");
        world = Game.getWorld();

        startYear = world.getDate().getYear();
        startMonth = world.getDate().getMonth();
        startDay = world.getDate().getDay();
        startTurn = world.getTurn();

        for (long i = 0; i < TICKS; i++) {
            world.nextTurn();
            AStarQueue.drainSynchronously();
        }
    }

    @AfterAll
    static void cleanup() {
        HeadlessRunner.deleteRecursively(userFolder);
    }

    @Test
    void calendarAdvanced() {
        long before = ((long) startYear * 12 * 30 + (long) startMonth * 30 + startDay) * World.TIME_MODIFIER_DAY + startTurn;
        long after = ((long) world.getDate().getYear() * 12 * 30 + (long) world.getDate().getMonth() * 30 + world.getDate().getDay()) * World.TIME_MODIFIER_DAY + world.getTurn();
        assertTrue(after > before, "in-game time did not advance over " + TICKS + " ticks");
    }

    @Test
    void livingsHaveSaneState() {
        for (int d = 0; d < 2; d++) {
            for (LivingEntity le : World.getLivings(d == 0).values()) {
                assertNotNull(le.getIniHeader());
                assertTrue(le.getLivingEntityData().getHealthPoints() >= 0,
                        le.getIniHeader() + " has negative health");
                assertInBounds(le.getCoordinates().x, le.getCoordinates().y, le.getCoordinates().z,
                        "living " + le.getIniHeader());
            }
        }
    }

    @Test
    void citizenListConsistent() {
        for (Integer id : World.getCitizenIDs()) {
            assertNotNull(World.getLivingEntityByID(id.intValue()),
                    "citizen ID " + id + " has no living entity after " + TICKS + " ticks");
        }
    }

    @Test
    void itemsRemainInBounds() {
        for (xaos.tiles.entities.items.Item item : World.getItems().values()) {
            assertInBounds(item.getCoordinates().x, item.getCoordinates().y, item.getCoordinates().z,
                    "item " + item.getIniHeader());
        }
    }

    private static void assertInBounds(int x, int y, int z, String what) {
        assertTrue(x >= 0 && x < World.MAP_WIDTH
                && y >= 0 && y < World.MAP_HEIGHT
                && z >= 0 && z < World.MAP_DEPTH,
                what + " out of bounds at " + x + "," + y + "," + z);
    }
}
