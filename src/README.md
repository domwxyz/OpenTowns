# OpenTowns Source Map

Developer notes on the codebase. The game is roughly 8X,000 lines of Java in a single root package, `xaos`.


## Layout of src/

```
src/
├── xaos/           The entire game source (mapped below)
├── data/           Game data: XML definitions, campaigns, languages
│   ├── graphics/   (gitignored; copy from the Towns install)
│   ├── audio/      (gitignored; copy from the Towns install)
│   └── fonts/      (gitignored; copy from the Towns install)
├── lib/            (gitignored; jars and native DLLs from the Towns install)
├── towns.ini       Main config: window, FPS, folders, keybinds, mods
├── graphics.ini    Tile atlas coordinates for every sprite in the game
└── audio.ini       Sound-effect and music file mappings
```

`src/` doubles as the runtime working directory. The game resolves every path (the three .ini files, `data/`, `lib/native`) relative to the current working directory, not the classpath. The Gradle `run` task sets this up.

Since the LWJGL 3 port, only `jna.jar`, `platform.jar`, `pngdecoder.jar` and the `steam_api` DLLs in `lib/` are actually used. The LWJGL 2, slick-util and jinput files that come along when copying `lib/` from a Towns install are ignored by the build.

There is a second config layer: the game creates a user folder (`~/Towns` by default) and re-reads `towns.ini` from there on top of the local one. Saves, mods, and screenshots also live in the user folder.


## Startup flow

`xaos.Towns.main()` first runs the first-run asset setup (`xaos.setup.FirstRunSetup`: if `data/graphics`, `data/audio` or `data/fonts` are missing from the working directory, it locates a Steam Towns install or asks for one via native tinyfd dialogs and copies them in), then initializes the Steam API via JNA if present (failure is silently ignored; the game runs fine without Steam), then constructs `xaos.main.Game`. Game creates the user folder, reads config, opens a GLFW window through the compatibility layer (`xaos.compat.opengl.Display`), initializes OpenAL audio (`utils.UtilsAL`), and enters the main loop in `Game.run()`: poll input, advance the simulation one turn, render, swap buffers, cap the frame rate (`FPS_MAINMENU` and `FPS_INGAME` in towns.ini, both 30 by default).


## Package map

### Core

- `xaos`: `Towns.java` (entry point, .ini property access) and `TownsProperties.java`.
- `xaos.setup`: first-run asset setup, run from `Towns.main()` before anything else. `SteamLocator` finds a Steam Towns install (registry/default paths + a dumb `libraryfolders.vdf` line scan); `FirstRunSetup` validates, confirms via tinyfd native dialogs (message box + folder picker; no AWT, which would fight GLFW on macOS) and copies `data/graphics`, `data/audio`, `data/fonts` into the working directory (tmp-then-rename per folder, so an interrupted copy never looks complete). Imports nothing from the game packages: it must not class-initialize `Game` or draw RNG. `TownsHeadless` never calls it.
- `xaos.main`: the two central classes.
  - `Game.java` (2,300 ln): static god-object. Main loop, game states, display init, savegame versioning (`SAVEGAME_V9` through `V14e`), pause, speed, mods list, user folder.
  - `World.java` (4,700 ln): the map. A 200x200 grid, up to 64 Z-levels, the per-turn simulation tick, water/lava fluid simulation, in-game calendar, siege scheduling, and save serialization (`Externalizable`).

### Compatibility layer: xaos.compat

The LWJGL 2 to 3 port gave the game a small platform layer that reimplements the LWJGL 2 API surface it used, backed by GLFW. Game code kept its call sites and changed only imports.

- `compat.opengl.Display`, `DisplayMode`: the window. Buffers settings made before `create()` as LWJGL 2 allowed, swaps and polls in `update()`, reimplements the `sync()` frame limiter.
- `compat.input.Keyboard`, `Mouse`, `Cursor`: LWJGL 2 style polled input state, fed by GLFW callbacks registered in `Display.create()`. LWJGL 2 keycode values and key-name strings are preserved because towns.ini keybinds store them. `Mouse` keeps the LWJGL 2 bottom-left Y origin; game code flips Y itself.
- `compat.LWJGLException`: kept so existing catch blocks compile.

Input rules: game hotkeys match physical keys (layout-independent, as in the original); typed text comes from the GLFW char callback and follows the OS keyboard layout (consumed in `TypingPanel`).

### The world and its contents: xaos.tiles

- `Cell.java` (2,200 ln): everything occupying one map coordinate. `Tile.java`: a renderable tile definition.
- `terrain/`: terrain types (`terrain.xml`) plus animated specials (`Water`, `Lava`) and cursor/overlay pseudo-tiles.
- `entities/`: `Entity` base class, then:
  - `living/`: `LivingEntity` (4,600 ln; base creature, needs, movement, combat) and `Citizen` (4,100 ln; the colonist AI), with subfolders for `heroes/` (own behaviour/skill/task system), `enemies/`, `allies/`, `friendly/` (animals), and `projectiles/`.
  - `items/`: `Item` (2,300 ln), `Container`, and `military/` with prefix/suffix gear modifiers (`prefixsuffix.xml`).
  - `buildings/`: buildings (`buildings.xml`).
  - `special/`: status-bubble sprites (eating, sleeping, exclamation, red cross).

### Game logic

- `xaos.tasks`: the job system. `TaskManager` (3,300 ln) turns player orders into a work queue that citizen AI pulls from; the game's indirect control scheme lives here.
- `xaos.actions`: action definitions (`actions.xml`) and per-citizen priority management (`priorities.xml`).
- `xaos.generator`: procedural world generation, driven by `data/**/gen_*.xml`. `MapGenerator` builds terrain from height seeds and Bezier curves; `ItemGenerator` and `LivingEntityGenerator` populate it. `Generator` is the shared XML-parsing base.
- `xaos.stockpiles`, `xaos.zones`: storage piles; designated zones (barracks, hero rooms, personal rooms).
- `xaos.campaign`: campaigns, missions, objectives, and the tutorial flow (`campaigns.xml`, `data/campaigns/`).
- `xaos.caravans`, `xaos.dungeons`, `xaos.effects`, `xaos.events`, `xaos.gods`, `xaos.skills`: one subsystem each, all following the same manager + item pattern, each parsing its matching `data/*.xml` (trading and prices, dungeon generation, status effects, random events, gods, citizen skills).
- `xaos.data`: 26 small passive data classes used by savegames and XML loading.
- `xaos.property`: typed accessor layer over the .ini files.

### Presentation

- `xaos.panels`: the entire UI, hand-rolled in immediate-mode OpenGL; no toolkit. `MainMenuPanel` and `MainPanel` are the two top-level screens. `UIPanel` (the largest file in the game) is the in-game HUD: it multiplexes about a dozen logical sub-panels (livings, materials, piles, production, professions, priorities, trade, messages, bottom menu, tutorial, typing, images) as static state, each with its own active/locked flags and create/resize/render/mouse handlers. The ten sub-panels were split out into their own all-static `XyzUIPanel` classes by mechanical split (verbatim bodies, no behavior change), the same method used for the Utils decouple: `BottomMenuUIPanel` (build menu bar), `LivingsUIPanel` (citizens/soldiers/heroes roster and groups), `MatsUIPanel` (materials), `MessagesUIPanel` (message log HUD), `PileUIPanel` (stockpile config), `PrioritiesUIPanel` (job priorities), `ProductionUIPanel` (workshop production queue), `ProfessionsUIPanel` (professions/job groups), `RightMenuUIPanel` (right-edge menu), `TradeUIPanel` (caravan/town trading, including the headless guard in `createTradePanelContent`). UIPanel (down from 9,800 to ~5,200 lines) keeps the shared core: the big dispatchers (`render`, `mousePressed`, `isMouseOnAPanel`, `renderTooltip`, `initialize`, `generateTiles`), the `MOUSE_*` dispatch codes, shared tiles and geometry, the top-bar/info/date chrome, and the thin glue for the tutorial button and the typing/images/minimap panels (whose real implementations already live in their own classes). One rule the golden pins enforce here: `Tile(String)` consumes one `Utils.random` draw, so `new Tile(...)` field initializers never move out of `UIPanel.<clinit>`; the extracted classes keep their own class-init free of RNG draws. Also trade, command, minimap, messages and typing panels as separate files, plus the context/smart menus in `menus/`. Text entry goes through `TypingPanel`, which appends layout-aware characters from the keyboard shim and falls back to a physical-key table for presses that produce no character. The AWT `MainFrame` that once hosted the LWJGL 2 display was dead code (never instantiated) and was deleted during the port.
- `xaos.utils`: 32 files of everything else. `Utils` used to be a 2,200-line grab-bag; it was decoupled mechanically (verbatim code motion, no behavior change) into: `UtilsGeometry` (map bounds, distances, Bezier, integer sqrt), `UtilsDice` (dice rolls and dice-string parsing), `UtilsLineOfSight` (Bresenham line-of-sight, cell discovery, light propagation), `UtilsString` (number/list/color parsing, dynamic option strings), `UtilsFiles` (user folder creation, options saving, mod path resolution, language discovery), and `UtilsSavegame` (savegame zip save/load, bury towns, savegame listing). `Utils` itself keeps only the single game RNG (`Utils.random`, which every random draw funnels through) plus dice wrappers delegating to `UtilsDice`, so the ~350 dice call sites kept their form. `UtilsGL`: texture loading and immediate-mode draw helpers. `UtilsAL`: audio, OpenAL + STB Vorbis since the port; all 16 OGGs are decoded into buffers at load, one looping music source plus a pool of FX sources. `UtilsXML` (DOM helpers), `UtilsKeyboard` (keybind and function-key mapping), A* pathfinding (`AStar*`, binary-heap based), `Log`, `Messages` (i18n via `data/languages/messages*.properties`), `UtilsServer` (HTTP to the townsmods.net community server, which no longer responds; connection error at boot, which fails gracefully), `JNASteamAPI`.


## Data-driven design

Nearly all game content is defined in `data/*.xml` and parsed at load time by the matching manager class: items, buildings, creatures, terrain, skills, gods, events, heroes, caravans, menus. Modding works by overlaying these files (plus `graphics.ini` and the language files) from `~/Towns/mods/<modname>/`, controlled by the `MODS=` key in `towns.ini`.


## Dependencies

LWJGL 3 (3.3.6) comes from Maven Central via the Gradle build: core, glfw, opengl, openal and stb modules plus Windows natives. Rendering calls the same `GL11`/`GL13` binding classes the game always used, so the 750+ immediate-mode GL calls did not change in the port.

Still vendored in `lib/` (from a Steam Towns install):

| Jar | Provides |
|---|---|
| `jna.jar`, `platform.jar` | Steam API binding (JNA 3.3; optional, failure ignored) |
| `pngdecoder.jar` | PNG to GL texture decoding |
| `xaos.jar` | The original compiled game. Never put it on the classpath, it would shadow freshly built classes. The build excludes it. |


## State of the code

- Runs on a Java 25 toolchain (Gradle auto-provisions it) and LWJGL 3. The Java 8 to 25 jump required zero source changes. The `run` task carries two JVM flags: `--enable-native-access` (JNA and LWJGL load native code) and `--sun-misc-unsafe-memory-access=allow` (LWJGL 3 still uses Unsafe internally).
- Sources are UTF-8 (converted from the original ISO-8859-1 in a mechanical commit; due to accented Spanish comments throughout). Java files must stay UTF-8 without a BOM; javac rejects BOMs.
- Java 6/7 idioms in the game code: no lambdas, no diamond operator, raw types are common (hence the `unchecked` compile warnings; these are expected). Heavy use of static state; `Game`, `UIPanel` and most managers are effectively singletons. Only `xaos.compat` uses current Java.
- Automated tests cover the headless sim (see the Testing section): determinism, worldgen invariants, save/load round-trip, long-run smoke. The manual smoke test for the windowed game remains: main menu appears, new game, worldgen completes, save, reload.
- The source corresponds to the unreleased v15 work-in-progress, one step past the last shipped v14e.
- Savegame compatibility is versioned (`Game.SAVEGAME_VERSION`); `World` and entity serialization is hand-rolled `Externalizable`. Be careful editing fields on anything that gets saved.
- `data/languages/*.properties` must stay ISO-8859-1/ASCII; Java 8 era `PropertyResourceBundle` reads that encoding by spec.
- towns.ini keybinds store LWJGL 2 keycodes and key names; the input shim translates from GLFW, so configs from the original game keep working.

## Headless deterministic test mode

`xaos.TownsHeadless` runs worldgen and the simulation without a window, GL context, audio device, or any proprietary asset. Run it with:

```
.\gradlew runHeadless -Pseed=42 -Pticks=3000            # deterministic
.\gradlew runHeadless -Pticks=3000                      # headless, unseeded
```

Plus optional `-Pmap=normal|desert|jungle|mixed|snow|mountains` and `-PuserFolder=path`. A windowed "New game" is campaign `c1` with the map type as the mission id; `-Pmap` picks the same thing. The user folder defaults to a sandbox under the system temp dir, so test runs never touch `~/.towns`.

With a seed, the run is deterministic: the single game RNG (`Utils.random`, which every random draw funnels through) is seeded before worldgen, and pathfinding runs synchronously each tick (`AStarQueue.drainSynchronously`) instead of on the worker thread. Same seed + same tick count = identical world state; the runner prints a state hash over terrain, livings and items to compare runs. Without a seed the RNG and async A* worker behave exactly like the shipped game.

How it works: `Game.initHeadless()` reads the same config as the windowed constructor and forces audio, autosave, pause-at-start and bury off. A handful of `Game.isHeadless()` guards give the sim no-op paths where it brushes against presentation: texture loads return 1x1 stubs with unique IDs, font metrics are zero-width, message-log adds and the caravan trade panel return early, and alpha masks (mouse hit-testing) come back empty. The bury feature is excluded from the deterministic surface on purpose (`Point3DShort`-keyed HashMaps iterate by identity hash). Vanilla behavior is untouched: every guard is behind the headless flag, which only `TownsHeadless` sets.

## Testing

`.\gradlew test` runs the JUnit 5 suite in `test/` (a top-level directory; test sources cannot live under `src/`, the main sourceSet would compile them). The Gradle `test` task sets the working directory to `src/` and forks one JVM per test class (`forkEvery = 1`): the static god-objects do not support a second new game in the same JVM (`World.maxEntityID` keeps counting across games, shifting entity IDs and HashMap iteration order), so every test class that boots a game gets a fresh JVM.

Two kinds of tests, both built on headless mode:

- **Process-level** (`HeadlessRunner`): forks `xaos.TownsHeadless` as a child JVM and parses the printed state hashes. Used where two independent full runs must be compared: determinism regression (`DeterminismTest`, same seed = same hash) and the save/load round-trip (`SaveLoadRoundTripTest`, hash before save equals hash after load).
- **In-JVM** (`Worldgen*Test`, `LongRunSmokeTest`): boots the game once per class via `Game.initHeadless` + `Game.startGame` and asserts invariants directly on the `World` objects.

Every run sandboxes its user folder under the system temp dir. Repo assets suffice (no proprietary files needed), so the suite also runs in CI (`.github/workflows/test.yml`, `windows-latest`). The seeded tests are fully deterministic: a failure always means a real behavior change, never a flake. The bury feature stays outside the tested deterministic surface.

**Golden pins** (`test/xaos/test/Golden.java`) freeze vanilla behavior itself, not just run-to-run determinism: they record the expected state hashes and counters for fixed seed/map/tick scenarios (worldgen at tick 0 for all six map types, plus three simulated scenarios and the 20k-tick in-JVM run), captured while the source was at its original pre-refactor behavior. The hash definition lives in `TownsHeadless.computeStateHash` and is frozen along with them. A pin mismatch means worldgen or the sim changed behavior; updating a pin is a deliberate act that must be explained in the same commit (take the new value from the failing assertion message).

## Next intended steps
- Player-ready release. Self-contained portable builds via `jpackage` are in
  place (`./gradlew jpackageZip`, built per-OS by `.github/workflows/release.yml`
  on a version tag and uploaded as run artifacts; the GitHub release itself is
  published by hand from those artifacts). A packaged build sets `-Dtowns.home=$APPDIR`
  so the game roots its bundled content (the `.ini` files and `data/`) at the app
  dir instead of the working directory, which a jpackage launcher does not control
  (it is `/` on macOS). Still open before a general release: native installers
  (`.msi`/`.dmg`/`.deb`, which need a read-only-install asset model and a macOS
  signing/notarization story), and a per-OS windowed smoke test of the packaged
  build with real assets.

## Pie-in-the-sky desires
- Expose `data\*.xml` as a public moddable API, with load-time validation.
- General mod management improvements. On top of the existing overlay system: per-mod metadata, load order, conflict reporting.
