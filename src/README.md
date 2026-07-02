# OpenTowns Source Map

Developer notes on the codebase. The game is roughly 81,500 lines of Java across 176 files in a single root package, `xaos`.


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

There is a second config layer: the game creates a user folder (`~/Towns` by default) and re-reads `towns.ini` from there on top of the local one. Saves, mods, and screenshots also live in the user folder.


## Startup flow

`xaos.Towns.main()` initializes the Steam API via JNA if present (failure is silently ignored; the game runs fine without Steam), then constructs `xaos.main.Game`. Game creates the user folder, reads config, opens the window (`panels.MainFrame`, an AWT `Frame` hosting the LWJGL 2 OpenGL `Display` through `Display.setParent` on a `Canvas`), and enters the main loop.


## Package map

### Core

- `xaos`: `Towns.java` (entry point, .ini property access) and `TownsProperties.java`.
- `xaos.main`: the two central classes.
  - `Game.java` (2,300 ln): static god-object. Main loop, game states, display init, savegame versioning (`SAVEGAME_V9` through `V14e`), pause, speed, mods list, user folder.
  - `World.java` (4,700 ln): the map. A 200x200 grid, up to 64 Z-levels, the per-turn simulation tick, water/lava fluid simulation, in-game calendar, siege scheduling, and save serialization (`Externalizable`).

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

- `xaos.panels`: the entire UI, hand-rolled in immediate-mode OpenGL; no toolkit. `MainFrame` is the OS window; `MainMenuPanel` and `MainPanel` are the two top-level screens; `UIPanel` (9,800 lines, the largest file in the game) is the in-game HUD monolith. Also trade, command, minimap, messages, and typing panels, plus the context/smart menus in `menus/`.
- `xaos.utils`: 26 files of everything else. `UtilsGL` (texture/draw helpers), `UtilsAL` (audio), `UtilsXML` (DOM helpers), `UtilsKeyboard`, A* pathfinding (`AStar*`, binary-heap based), `Log`, `Messages` (i18n via `data/languages/messages*.properties`), `UtilsServer` (HTTP to the townsmods.net community server), `JNASteamAPI`.


## Data-driven design

Nearly all game content is defined in `data/*.xml` and parsed at load time by the matching manager class: items, buildings, creatures, terrain, skills, gods, events, heroes, caravans, menus. Modding works by overlaying these files (plus `graphics.ini` and the language files) from `~/Towns/mods/<modname>/`, controlled by the `MODS=` key in `towns.ini`.


## Dependencies (vendored in lib/)

| Jar | Provides |
|---|---|
| `lwjgl.jar`, `lwjgl_util.jar` | LWJGL 2.x: window, OpenGL, input, OpenAL context (`lib/native/` holds its DLLs) |
| `slick-util.jar` | OGG loading and audio playback on OpenAL (`org.newdawn.slick.openal`) |
| `pngdecoder.jar` | PNG to GL texture decoding |
| `jna.jar`, `platform.jar` | Steam API binding (old JNA 3/4-style interface mapping) |
| `xaos.jar` | The original compiled game; kept only as a reference binary. Never put it on the classpath, it would shadow freshly built classes. The build excludes it. |


## State of the code

- Sources are UTF-8 (converted from the original ISO-8859-1 in a mechanical commit; accented Spanish comments throughout). Java files must stay UTF-8 without a BOM — javac rejects BOMs.
- Comments are mostly Spanish; sparse but generally accurate.
- Java 6/7 idioms everywhere: no lambdas, no diamond operator, raw types are common (hence the `unchecked` compile warnings; these are expected). Heavy use of static state; `Game` and most managers are effectively singletons.
- No tests. Verification is running the game. The manual smoke test: main menu appears, new game, worldgen completes, save, reload.
- The source corresponds to the unreleased v15 work-in-progress, one step past the last shipped v14e (see `CHANGELOG.md`).
- Savegame compatibility is versioned (`Game.SAVEGAME_VERSION`); `World` and entity serialization is hand-rolled `Externalizable`. Be careful editing fields on anything that gets saved.
- `data/languages/*.properties` must stay ISO-8859-1/ASCII; Java 8 `PropertyResourceBundle` reads that encoding by spec.
