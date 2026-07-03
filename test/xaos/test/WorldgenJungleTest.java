package xaos.test;

import org.junit.jupiter.api.BeforeAll;

class WorldgenJungleTest extends WorldgenInvariantsBase {

    @BeforeAll
    static void bootMap() throws Exception {
        boot("jungle");
    }
}
