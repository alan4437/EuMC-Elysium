# EuMC Elysium

Open-source client-side Minecraft mod by utud. **Fabric only.** Version 1.0.0.

## Install

Choose ONE matching JAR from `releases/1.0.0/`, install Fabric Loader and the matching Fabric API, then put both mods in your instance's `mods` directory. Remove older copies of this mod (ID `gamblingmenu`).

| Minecraft | Java | Fabric Loader | Tested Fabric API | Source |
|---|---|---|---|---|
| 1.21.11 | 21 | 0.18.4+ | 0.141.1+1.21.11 | [source](versions/1.21.11) |
| 26.1 | 25 | 0.20.1+ | 0.145.1+26.1 | [source](versions/26.1) |
| 26.2 | 25 | 0.20.1+ | 0.161.0+26.2 | [source](versions/26.2) |
| 26.3 | 25 | 0.20.1+ | 0.166.2+26.3 | [source](versions/26.3) |

## Features

Right Shift opens/closes the non-pausing menu; Escape closes it. F5 retains Minecraft camera switching. The glass GUI uses bundled Inter, colour wheels, smooth animations, and customisable HUD layouts.

- **Gambling:** Payment Guard, Gamble Blacklist, Profit Tracker.
- **EuropeMC:** Payment Multipliers and Auction leaderboard with winner celebration.
- **Settings:** theme, colours, opacity, animation and HUD position editor.

There is no Discord authentication or backend dependency. The Discord icon opens a public invite in your browser. Historical `dev.discordlink` package and asset names remain for compatibility.

Read the guides in [docs](docs) for exact payment behaviour. Amounts accept K/M/B/T suffixes with exact decimal arithmetic. Receipts come from supported server chat formats, not a server payment API. Duplicate suppression is best-effort because chat has no transaction IDs. Gambling counts mean confirmed multiplier payouts, not verified wins/losses. Guard donations and refunds are excluded from profit; ordinary and auction receipts can still enter payment totals. Auctions do not transfer inventory items or provide escrow.

## Read the source online

No login is needed to browse the complete code: [1.21.11](versions/1.21.11/src), [26.1](versions/26.1/src), [26.2](versions/26.2/src), [26.3](versions/26.3/src). Each folder includes the client, shared logic, resources and tests.

## Build and test

Enter the matching `versions/<minecraft-version>` directory with the Java version above:

```sh
./gradlew build
# Optional real-client tests, requiring a graphical session:
./gradlew runClientGameTest -PclientTests
```

On Windows use `gradlew.bat`. Build output is in `build/libs/`. Deterministic money, auction and profit tests run during `build`. Enabling client tests accepts Minecraft's EULA for that test environment; review it before running.

All four versions were built and client-tested on Linux, including GUI inputs, fonts, payment routing, auction and HUD editing with synthetic receipts. Windows and Vulkan were not tested. No real server funds were used by tests. Version-specific rendering/API notes are in each port's README.

## Contributing and licensing

Code is MIT licensed; see [LICENSE](LICENSE). Existing copyright notices are retained. Inter is distributed under its SIL Open Font License, included in each source tree as `src/main/resources/Inter-OFL.txt`. Supplied artwork is included as project assets; third-party names/logos remain their owners' trademarks. This project is not affiliated with Mojang, Microsoft, Discord or EuropeMC.

The repository includes all current mod sources, packaged assets, asset-generation scripts, tests, Gradle wrappers and runtime JARs. Private server inspection reports, cancelled Discord backend/deployment tooling, personal configurations, world saves, build caches and downloaded Minecraft/toolchain binaries are excluded.
