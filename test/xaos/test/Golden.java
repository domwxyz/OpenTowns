package xaos.test;

import java.util.Map;

/**
 * Golden pins: the expected world state for fixed seed/map/tick scenarios,
 * recorded while the source was at its original (pre-refactor) behavior.
 * They freeze vanilla behavior itself, not just run-to-run determinism: any
 * change to worldgen, the sim, or the hash definition in
 * TownsHeadless.computeStateHash shows up as a pin mismatch.
 *
 * Recorded on the original dev machine (Windows, Adoptium JDK 25). CI
 * (.github/workflows/test.yml) runs the suite on Windows, Linux and macOS
 * runners, and every OS is expected to reproduce these pins identically;
 * the matrix keeps fail-fast off so a per-OS divergence (file listing
 * order, default locale, floating point) reports on its own instead of
 * being cancelled by another OS's failure.
 */
final class Golden {

    private Golden() {
    }

    private static final Map<String, String> PINS = Map.ofEntries(
            // Worldgen only (seed 42, 0 ticks), one per map type
            Map.entry("worldgen.normal.terrain", "bb97e605343c497a"),
            Map.entry("worldgen.normal.state", "ee90565cace24678"),
            Map.entry("worldgen.desert.terrain", "211c03dc1d153068"),
            Map.entry("worldgen.desert.state", "722af7bfc32ca18c"),
            Map.entry("worldgen.jungle.terrain", "9c56d90214a70eb4"),
            Map.entry("worldgen.jungle.state", "1ba8290ae39cf3ca"),
            Map.entry("worldgen.mixed.terrain", "cca6f0fc839a22a6"),
            Map.entry("worldgen.mixed.state", "e675f859fd653803"),
            Map.entry("worldgen.snow.terrain", "3081a74ba00dad9c"),
            Map.entry("worldgen.snow.state", "5cbc12c385e2eaba"),
            Map.entry("worldgen.mountains.terrain", "52142894e964ca96"),
            Map.entry("worldgen.mountains.state", "8c2c7d1afbf3323d"),

            // Simulated scenarios (seed, map, ticks)
            Map.entry("sim.42.normal.3000.terrain", "2b013a1bce9c3174"),
            Map.entry("sim.42.normal.3000.state", "72fb1c57b4b1692f"),
            Map.entry("sim.42.normal.3000.summary", "date=1/1/1 citizens=11 livings=7435 items=1834 coins=71"),
            Map.entry("sim.1234.desert.3000.terrain", "80e3b1a604d089e6"),
            Map.entry("sim.1234.desert.3000.state", "149e801fe7c7aaf2"),
            Map.entry("sim.1234.desert.3000.summary", "date=1/1/1 citizens=8 livings=7253 items=843 coins=47"),
            Map.entry("sim.42.normal.20000.terrain", "a42a44b77e495f0a"),
            Map.entry("sim.42.normal.20000.state", "4d370c611873822a"),
            Map.entry("sim.42.normal.20000.summary", "date=3/1/1 citizens=11 livings=7435 items=1901 coins=71"),

            // Pinned from a process run; LongRunSmokeTest asserts them
            // in-JVM, which also proves the two drive paths are equivalent
            Map.entry("sim.4242.normal.20000.terrain", "dc29b967adf18d3f"),
            Map.entry("sim.4242.normal.20000.state", "5cf51429b84716cc"),

            // L1 scripted-directive scenario (ScenarioMineTest): one mine order
            // at 68,181,12, seed 42 / normal, 1000 ticks. Freezes active-play
            // behavior, not just free-run drift.
            Map.entry("scenario.mine.42.normal.t250.state", "9160bb88f30af7e5"),
            Map.entry("scenario.mine.42.normal.terrain", "1be34d248eb12b9"),
            Map.entry("scenario.mine.42.normal.state", "b03820dd46471ba2"));

    static String get(String key) {
        String value = PINS.get(key);
        if (value == null) {
            throw new IllegalStateException("no golden pin recorded for " + key);
        }
        return value;
    }
}
