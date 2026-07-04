# OpenTowns

OpenTowns is an open restoration and modernization of [Towns][Towns game], the 2012 town-building indie game. It builds on the official release of the game's full source code, released under the GPLv3.

The original game shipped against Java-6-era APIs and the long-abandoned LWJGL 2, which means today it cannot run at all on modern Macs and is increasingly fragile everywhere else.

**Mission Statement:** Upgrade the original Towns code to run on Java 25 and LWJGL 3, bringing a beloved game back to life on modern machines.

# Status

The game compiles and runs from source on a Java 25 toolchain with LWJGL 3. Windowing, input, and audio were ported (GLFW window, OpenAL audio); the rendering code unchanged.

Only one player-visible fix over the original: text entry now follows the OS keyboard layout instead of hard-coding QWERTY.

Hash-verified deterministic test suite was established in order to track potential variations from original simulation behavior.

See `src/README.md` for more information about source status.

# Download and Play

Prebuilt, self-contained release candidates are published on the [Releases page][Releases] for Windows, macOS and Linux. They bundle their own Java runtime, so no separate Java install is needed.

1. Download the zip for your OS and unzip it to any writable location (for example your Desktop; do not run it from inside a read-only folder).
2. Launch `OpenTowns` (`OpenTowns.exe` on Windows, `OpenTowns.app` on macOS, the `bin/OpenTowns` launcher on Linux).
3. On first launch the game copies the proprietary assets from your own Towns install, exactly as the from-source build does (see below). You still need a copy of the original game for those assets.

**macOS:** the builds are not yet code-signed, so the first launch is blocked by Gatekeeper as coming from an unidentified developer. Right-click (or Control-click) the app and choose **Open**, then confirm; macOS remembers the choice for later launches.

# Building

You need a copy of the original game (available on [Steam][Towns game]) for its assets, which are proprietary and not included here.

1. Clone this repository.
2. Run `./gradlew run` (or `gradlew.bat run` on Windows). Gradle bootstraps itself, including the correct JDK toolchain.

On first launch the game looks for a Steam installation of Towns and offers to copy its `data/graphics`, `data/audio` and `data/fonts` folders over automatically; if none is found, it asks you to point at your Towns folder. Copying those three folders into `src/data/` by hand still works too.

# Code License

The source code is released under the [GNU GPL v3 license][LICENSE], as was the original code drop this project is forked from:

- You can use, study, and modify the code
- You can redistribute it
- If you distribute a modified version, you must also release it under GPL and make the source code available

See the [LICENSE][] file for the full text.

# Original Assets License Notice

The original game assets (graphics, audio, and other media) are **not** covered by the code license. They remain the property of their respective authors:

- They must NOT be included in this repository or any fork of it
- They must NOT be redistributed without permission from their original authors

This repository contains only code and data that is safe to redistribute.

# Community & Credits

Towns was created by its original developers, who graciously released the source code in 2026. This project would not exist without that decision.

The Towns community Discord is the place to discuss ideas, mods, and the future of the game:

[[Discord invite link](https://discord.gg/wAW28PkrwF)]

[Towns game]: https://store.steampowered.com/app/221020/Towns/
[Releases]: https://github.com/OpenTowns/OpenTowns/releases
[LICENSE]: ./LICENSE
