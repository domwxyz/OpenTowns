package xaos.test;

import org.junit.jupiter.api.BeforeAll;

class WorldgenNormalTest extends WorldgenInvariantsBase {

    @BeforeAll
    static void bootMap() throws Exception {
        boot("normal");
    }
}
