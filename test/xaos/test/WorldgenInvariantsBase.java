package xaos.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import xaos.generator.MapGenerator;
import xaos.main.Game;
import xaos.main.World;
import xaos.tiles.Cell;
import xaos.tiles.entities.items.Item;
import xaos.tiles.entities.living.LivingEntity;
import xaos.utils.AStarQueue;
import xaos.utils.Utils;

/**
 * Invariants that must hold for a freshly generated world, asserted in-JVM
 * directly against the game objects. One subclass per map type boots the
 * game once in @BeforeAll; the test methods are read-only.
 *
 * Each subclass needs its own JVM (gradle test sets forkEvery = 1): the
 * static god-objects do not support a second new game in the same JVM.
 */
abstract class WorldgenInvariantsBase {

    private static final long SEED = 42;

    private static Path userFolder;
    private static World world;

    /** Called from each subclass's @BeforeAll with its map type. */
    static void boot(String map) throws Exception {
        if (!Files.exists(Path.of("towns.ini"))) {
            throw new IllegalStateException("Working directory must be src/ (run via gradlew test)");
        }
        userFolder = HeadlessRunner.newUserFolder();
        // Same order as TownsHeadless: init, then sync + seed, then worldgen.
        Game.initHeadless(userFolder.toString());
        AStarQueue.setSynchronousMode(true);
        Utils.setRandomSeed(SEED);
        Game.startGame("c1", map);
        world = Game.getWorld();
    }

    @AfterAll
    static void cleanup() {
        HeadlessRunner.deleteRecursively(userFolder);
    }

    @Test
    void mapHasExpectedDimensions() {
        Cell[][][] cells = World.getCells();
        assertEquals(World.MAP_WIDTH, cells.length);
        assertEquals(World.MAP_HEIGHT, cells[0].length);
        assertEquals(World.MAP_DEPTH, cells[0][0].length);
        assertTrue(World.MAP_DEPTH > 0, "map has no Z levels");
    }

    @Test
    void everyCellHasTerrain() {
        Cell[][][] cells = World.getCells();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[0].length; y++) {
                for (int z = 0; z < cells[0][0].length; z++) {
                    Cell cell = cells[x][y][z];
                    assertNotNull(cell, "null cell at " + x + "," + y + "," + z);
                    assertNotNull(cell.getTerrain(), "null terrain at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void citizenCountMatchesGenerator() {
        assertTrue(MapGenerator.NUM_CITIZENS > 0, "generator reported no citizens");
        assertEquals(MapGenerator.NUM_CITIZENS, World.getCitizenIDs().size());
    }

    @Test
    void everyCitizenIDResolvesToALiving() {
        for (Integer id : World.getCitizenIDs()) {
            assertNotNull(World.getLivingEntityByID(id.intValue()), "citizen ID " + id + " has no living entity");
        }
    }

    @Test
    void allLivingsInBoundsWithValidData() {
        int total = 0;
        for (int d = 0; d < 2; d++) {
            for (LivingEntity le : World.getLivings(d == 0).values()) {
                assertNotNull(le.getIniHeader(), "living without ini header");
                assertInBounds(le.getCoordinates().x, le.getCoordinates().y, le.getCoordinates().z,
                        "living " + le.getIniHeader());
                total++;
            }
        }
        assertTrue(total > 0, "world has no living entities");
    }

    @Test
    void allItemsInBoundsWithValidData() {
        assertTrue(World.getItems().size() > 0, "world has no items");
        for (Item item : World.getItems().values()) {
            assertNotNull(item.getIniHeader(), "item without ini header");
            assertInBounds(item.getCoordinates().x, item.getCoordinates().y, item.getCoordinates().z,
                    "item " + item.getIniHeader());
        }
    }

    @Test
    void coinsWithinStartingDiceRange() {
        // generateAll rolls launchDice(NUM_CITIZENS, 10): one d10 per citizen.
        int n = MapGenerator.NUM_CITIZENS;
        assertTrue(world.getCoins() >= n && world.getCoins() <= n * 10,
                "coins " + world.getCoins() + " outside [" + n + ", " + (n * 10) + "]");
    }

    @Test
    void calendarInitialized() {
        assertNotNull(world.getDate());
        assertTrue(world.getDate().getDay() >= 1);
        assertTrue(world.getDate().getMonth() >= 1);
        assertTrue(world.getDate().getYear() >= 1);
        assertTrue(World.FRAMES_PER_TURN > 0, "turn cadence not set");
    }

    private static void assertInBounds(int x, int y, int z, String what) {
        assertTrue(x >= 0 && x < World.MAP_WIDTH
                && y >= 0 && y < World.MAP_HEIGHT
                && z >= 0 && z < World.MAP_DEPTH,
                what + " out of bounds at " + x + "," + y + "," + z);
    }
}
