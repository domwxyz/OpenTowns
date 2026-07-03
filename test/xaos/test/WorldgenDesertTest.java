package xaos.test;

import org.junit.jupiter.api.BeforeAll;

class WorldgenDesertTest extends WorldgenInvariantsBase {

    @BeforeAll
    static void bootMap() throws Exception {
        boot("desert");
    }
}
