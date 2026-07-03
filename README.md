# OpenTowns

OpenTowns is an open restoration and modernization of [Towns][Towns game], the 2012 town-building indie game. It builds on the official release of the game's full source code, released under the GPLv3.

The original game shipped against Java-6-era APIs and the long-abandoned LWJGL 2, which means today it cannot run at all on modern Macs and is increasingly fragile everywhere else.

**Mission Statement:** Upgrade the original Towns code to run on Java 25 and LWJGL 3, bringing a beloved game back to life on modern machines.

# Status

The game compiles and runs from source on a Java 25 toolchain with LWJGL 3. Windowing, input, and audio were ported (GLFW window, OpenAL audio); the rendering code unchanged.

Only one player-visible fix over the original: text entry now follows the OS keyboard layout instead of hard-coding QWERTY.

Hash-verified deterministic test suite was established in order to track potential variations from original simulation behavior.

See `src/README.md` for more information about source status.

# Building

You need a copy of the original game (available on [Steam][Towns game]) for its assets, which are proprietary and not included here.

1. Clone this repository.
2. From your Towns installation, copy `lib/` and the `data/graphics`, `data/audio`, and `data/fonts` folders into `src/`.
3. Run `./gradlew run` (or `gradlew.bat run` on Windows). Gradle bootstraps itself, including the correct JDK toolchain.

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
[LICENSE]: ./LICENSE
