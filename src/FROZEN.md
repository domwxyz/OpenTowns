# Frozen files: a mindfulness guide

The point of this list is to know which edits can shift simulation behavior, and which edits are in the free zone where you can work without worry.

See `src/README.md` for the package-by-package map and the testing section for how the pins and the headless harness work.

## Frozen: simulation core

Whole packages under `src/xaos/` (every file):

- `main/` (`World.java`, `Game.java`)
- `tiles/` (`Cell.java`, `Tile.java`, `terrain/**` including fluid `Water`/`Lava`, `entities/**`: livings, items, buildings, special)
- `tasks/`, `actions/`, `generator/`, `stockpiles/`, `zones/`
- `caravans/`, `dungeons/`, `effects/`, `events/`, `gods/`, `skills/`
- `campaign/` (mission selection drives worldgen)
- `data/` (the serialized + hashed data carriers)

Root files: `Towns.java`, `TownsProperties.java`.

Frozen subset of `src/xaos/utils/` (the rest of this package is free, see below):

`Utils`, `UtilsDice`, `UtilsGeometry`, `UtilsLineOfSight`, `UtilsSavegame`, `UtilsString`, `UtilsXML`, `UtilsIniHeaders`, `UtilsFiles`, `Names` (draws RNG for entity names), `Date`, `Point3D`, `Point3DShort`, `AStarQueue`, `AStarBinaryHeap`, `AStarNodo`, `AStarQueueItem`.

## Frozen: data inputs (not `.java`, but they define behavior)

The pins hash the *result* of loading these, so they are just as load-bearing:

- `src/data/**` (the repo-tracked part) : all game XML (`items`, `buildings`, `livingentities`, `terrain`, `actions`, `effects`, `events`, `gods`, `heroes`, `caravans`, `skills`), every `gen_*.xml`, `campaigns.xml`, and `data/languages/*.properties` (which must also stay ISO-8859-1).
- The proprietary asset folders (`data/graphics/`, `data/audio/`, `data/fonts/`, gitignored, from a base Towns install) are NOT part of the pinned surface: headless runs never read them (textures come back as stubs, audio is forced off). They are render/audio inputs only, so they are not frozen; they just cannot be committed.
- `src/graphics.ini` : **partly frozen.** A tile's `animationFrameDelay` sets the range of the `Tile` constructor's RNG draw, so changing frame delays shifts the stream. Atlas *coordinates* are render-only and free.
- `src/towns.ini` : the values that select folders, mods and campaign feed the sim; window/FPS/keybind values do not.

## Gray zone: mostly free, but watch one thing

- Panel classes with `static Tile ... = new Tile(...)` field initializers: panel *behavior* is free, but each such field draws one RNG number when the class first initializes, so the field lists themselves are frozen. The complete set (verified with `-Xlog:class+init` on a seeded headless run):
  - `UIPanel.java` (16 initializers) and `MiniMapPanel.java` (1) : both class-initialize mid-sim inside the pinned headless surface; the golden pins catch edits here.
  - `MainPanel.java` (8) : initializes during the scenario tests; the scenario pins catch it.
  - `TradePanel.java` (8) and `menus/SmartMenu.java` (1) : never load in headless runs, so **no pin guards them**; editing their Tile field lists still shifts the windowed game's mid-game RNG stream. Treat the lists as frozen anyway.
  - The split-out `*UIPanel` classes and `MainMenuPanel`/`MessagesPanel` (which also initialize during pinned runs) currently have RNG-free class init: `static Tile` fields there are declared but not initialized at class init. Keep it that way; do not move a `new Tile(...)` into any of their field initializers or static blocks.
- `src/xaos/property/**` : config plumbing that parses the `.ini` files; you will rarely touch it, but the parser feeds sim inputs.
- `src/xaos/TownsHeadless.java` : the harness is free to extend, but its two hash methods are the pin oracle and are frozen.

## Free zone: edit freely (QoL, graphics, camera, minimap, modding UX)

- `src/xaos/compat/**` (the platform layer, meant to be edited)
- `src/xaos/panels/**` behavior (UI, minimap, menus, info), minus the two `<clinit>` Tile lists noted above
- `src/xaos/setup/**` (first-run asset copy; not in the headless path)
- `src/xaos/launcher/**` (pre-launch settings window; not in the headless path)
- Free `src/xaos/utils/`: `UtilsGL`, `UtilsAL`, `UtilsKeyboard`, `UtilsServer`, `JNASteamAPI`, `Log`, `Messages`, `LanguageData`, `LocalResourceClassLoader`, `PropertiesWriter`, `UtilFont`, `CharDef`, `ImageData`, `TextureData`, `ColorGL`
  - One watch-point in `UtilsServer`: the bury-download path draws one `Utils.getRandomBetween` to pick a random bury file.
- `test/**` (the guard itself)
- `src/audio.ini`, and atlas coordinates in `graphics.ini`
