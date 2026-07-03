package xaos.test;

import org.junit.jupiter.api.BeforeAll;

class WorldgenSnowTest extends WorldgenInvariantsBase {

    @BeforeAll
    static void bootMap() throws Exception {
        boot("snow");
    }
}
