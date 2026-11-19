# EuMC Elysium — Fabric 26.2

Port of Gambling Menu 1.0.0. Mod ID stays `gamblingmenu`, retaining local configuration
filenames and all existing features. Use only the JAR matching your Minecraft version.

Requires Minecraft Java 26.2, Java 25+, Fabric Loader 0.20.1+, and matching Fabric API
(tested dependency: 0.161.0+26.2). Original 1.21.11 uses Java 21 and remains a separate build.

Build with Java 25 and the included Gradle 9.6 wrapper:

```sh
./gradlew build
./gradlew build runClientGameTest -PclientTests
```

On Windows use `gradlew.bat`. Runtime JAR:
`build/libs/EuMC-Elysium-1.0.0-mc26.2-fabric.jar`.
The Gradle JVM must be Java 25; the source targets Java 25 bytecode.

Right Shift opens/closes the menu, Escape closes it, and F5 retains camera switching.
Gambling: Payment Guard, Gamble Blacklist and Profit Tracker.
EuropeMC: Payment Multipliers and Auction.
Settings: appearance controls and Edit HUD positions for Auction/Profit Tracker.

Chat money parsing and exact arithmetic are preserved. Client tests use an isolated
Minecraft world and synthetic economy receipts, never public-server payments.
Lifetime statistics and HUD layout save locally; reset confirmation only resets the
current session. Guard donations/refunds are excluded from gambling profit. Gambles
counts confirmed multiplier payouts, not server-verified outcomes.

The source adapts Minecraft's extraction-based GUI APIs and Fabric's renamed client
commands/screen events. The 26.2+ builds use Gui.screen()/Gui.hud. The 26.3 build also
uses SDL InputConstants for keyboard/mouse and Blaze3D.openUri for browser links.
Rendering uses Minecraft's graphics abstraction; no custom OpenGL calls are added.

Inter uses 4x oversampling in 26.2/26.3 to retain smooth text while avoiding missing
glyphs observed with the older 6x setting in the new rendering backend.
