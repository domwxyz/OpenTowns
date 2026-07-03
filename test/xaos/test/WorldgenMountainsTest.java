package xaos.test;

import org.junit.jupiter.api.BeforeAll;

class WorldgenMountainsTest extends WorldgenInvariantsBase {

    @BeforeAll
    static void bootMap() throws Exception {
        boot("mountains");
    }
}
