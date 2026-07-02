# OpenTowns Source Map

Developer notes on the codebase. The game is roughly 82,500 lines of Java across 181 files in a single root package, `xaos`.


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

`xaos.Towns.main()` initializes the Steam API via JNA if present (failure is silently ignored; the game runs fine without Steam), then constructs `xaos.main.Game`. Game creates the user folder, reads config, opens a GLFW window through the compatibility layer (`xaos.compat.opengl.Display`), initializes OpenAL audio (`utils.UtilsAL`), and enters the main loop in `Game.run()`: poll input, advance the simulation one turn, render, swap buffers, cap the frame rate (`FPS_MAINMENU` and `FPS_INGAME` in towns.ini, both 30 by default).


## Package map

### Core

- `xaos`: `Towns.java` (entry point, .ini property access) and `TownsProperties.java`.
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

- `xaos.panels`: the entire UI, hand-rolled in immediate-mode OpenGL; no toolkit. `MainMenuPanel` and `MainPanel` are the two top-level screens. `UIPanel` (9,800 lines, some 580 static fields and methods, the largest file in the game) is the in-game HUD: it multiplexes about a dozen logical sub-panels (livings, materials, piles, production, professions, priorities, trade, messages, bottom menu, tutorial, typing, images) as static state, each with its own active/locked flags and create/resize/render/mouse handlers. The seams between sub-panels are visible and consistent; splitting them out one at a time into their own classes, without behavior changes, is the intended next step of the modernization push. Also trade, command, minimap, messages and typing panels as separate files, plus the context/smart menus in `menus/`. Text entry goes through `TypingPanel`, which appends layout-aware characters from the keyboard shim and falls back to a physical-key table for presses that produce no character. The AWT `MainFrame` that once hosted the LWJGL 2 display was dead code (never instantiated) and was deleted during the port.
- `xaos.utils`: 26 files of everything else. `Utils` (2,200 ln) is the grab-bag: savegame zip save/load, Bresenham line-of-sight and light propagation, map bounds checks, distances, dice rolls, options saving, user folder creation. `UtilsGL`: texture loading and immediate-mode draw helpers. `UtilsAL`: audio, OpenAL + STB Vorbis since the port; all 16 OGGs are decoded into buffers at load, one looping music source plus a pool of FX sources. `UtilsXML` (DOM helpers), `UtilsKeyboard` (keybind and function-key mapping), A* pathfinding (`AStar*`, binary-heap based), `Log`, `Messages` (i18n via `data/languages/messages*.properties`), `UtilsServer` (HTTP to the townsmods.net community server, which no longer responds; connection error at boot, which fails gracefully), `JNASteamAPI`.


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
- No tests. Verification is running the game. The manual smoke test: main menu appears, new game, worldgen completes, save, reload.
- The source corresponds to the unreleased v15 work-in-progress, one step past the last shipped v14e.
- Savegame compatibility is versioned (`Game.SAVEGAME_VERSION`); `World` and entity serialization is hand-rolled `Externalizable`. Be careful editing fields on anything that gets saved.
- `data/languages/*.properties` must stay ISO-8859-1/ASCII; Java 8 era `PropertyResourceBundle` reads that encoding by spec.
- towns.ini keybinds store LWJGL 2 keycodes and key names; the input shim translates from GLFW, so configs from the original game keep working.

## Next intended steps
- Headless mode. Lets `World` + worldgen + the task system tick without a window. Goal to run in test without proprietary assets.
- Inject seed to RNG source, for deterministic test mode. Shipped game remains non-deterministic.
- Decouple `UIPanel`. Extraction of one sub-panel at a time, retaining original behavior.
- Decouple `Utils`. Extraction into mechanical pieces based on responsibility.
- Player-ready release. Self-contained builds using `jpackage`.
- Full test suite. For proper CI.

## Pie-in-the-sky desires
- Expose `data\*.xml` as a public moddable API, with load-time validation.
- General mod management improvements. On top of the existing overlay system: per-mod metadata, load order, conflict reporting.
