# Contributing to OpenTowns

OpenTowns is currently a single-maintainer project. The maintainer ([@domwxyz](https://github.com/domwxyz)) is the only current active developer.

## Read this first: the frozen core

OpenTowns preserves the original Towns simulation exactly. The repo is mapped into three zones in [src/FROZEN.md](src/FROZEN.md): a **frozen** simulation core, a small **gray zone**, and a **free zone** (platform layer, UI, setup, tests) where you can work without worry.

The freeze is enforced by golden pins: recorded state hashes for fixed seed/map/tick scenarios (`test/xaos/test/Golden.java`). If your change makes worldgen or the simulation behave differently, a pin should fail.

## Build and run

You need a copy of [Towns on Steam](https://store.steampowered.com/app/221020/Towns/) for its proprietary assets **only to run the windowed game**. Clone, then `./gradlew run` (`gradlew.bat run` on Windows); Gradle bootstraps its own JDK, and first launch offers to copy the assets from your Steam install.

Headless mode and the whole test suite need no assets at all; repo files suffice, which is why CI can run them:

```
./gradlew test                          # the JUnit suite
./gradlew runHeadless -Pseed=42 -Pticks=3000   # a deterministic headless run
```

See the Testing section of [src/README.md](src/README.md) for how the harness works.

## Writing tests

One hard constraint: **one game per JVM, so one JVM per test class.** `Game`, `World`, and `UIPanel` are static god-objects; state like `World.maxEntityID` keeps counting across games, shifting entity IDs and HashMap iteration order. The Gradle `test` task already sets `forkEvery = 1`.

Seeded tests are fully deterministic. Please keep new tests on that standard.

## File conventions

- Java sources are **UTF-8 without a BOM** (javac rejects BOMs) with **LF** line endings. `.gitattributes` and your editor should handle this.
- Exception: `src/data/languages/*.properties` must stay **ISO-8859-1/ASCII**: `PropertyResourceBundle` reads that encoding by spec.
- Match the surrounding code's style. The game code is deliberately Java 6/7 era (no lambdas, no diamond operator, raw types); only `xaos/compat/` uses current Java. The `unchecked` compile warnings are expected.

## As contributors join

When there are regular contributors, the intended progression is boring and gradual: sustained good PRs earn review trust, review trust earns commit access, and if the group gets large enough to need more than "the maintainer plus people the maintainer trusts," this document gets rewritten with actual process.

## Decisions made

- **The source is immutable as received.** The GPLv3 code drop is the historical artifact this project preserves. There are no retroactive changes to what's already there: the simulation core stays frozen per [src/FROZEN.md](src/FROZEN.md), and behavior drift should be caught by the golden hash pins.
- **The baseline is v14f.** The received source self-identifies as `v14f` (`GAME_VERSION` in `TownsProperties.java`): the unreleased work-in-progress one step past the last shipped v14e, already containing the changes listed under "v15" in the [changelog](CHANGELOG.md). OpenTowns does not roll those back to reconstruct v14e, and does not treat them as OpenTowns content changes. What was received is what is preserved.
- **The original assets stay proprietary.** They are not in this repo, are never redistributed, and there is no plan to replace them with free equivalents (see the README's asset license notice).
