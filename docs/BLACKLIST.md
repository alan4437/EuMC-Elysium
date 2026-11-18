# Gamble Blacklist — 1.6.0

Open the menu with Right Shift. Left-click **Gamble Blacklist** to toggle it; right-click for its separate settings inspector. The inspector's Enabled switch controls the same state. Enter a Java Minecraft username, click Add, and remove entries using each row's X. Scroll inside the list for additional entries.

Matching is case-insensitive, preserves original spelling for display and rejects duplicates, invalid names and command-like strings. Usernames must contain 3–16 letters/digits/underscores. The list supports up to 200 entries and persists in `config/gamblingmenu-blacklist.json`; saves are asynchronous and atomic where supported. Invalid saved entries are ignored and load/save failures are shown in the inspector. Enabled state is session-only; joining or disconnecting disables both refund modules and clears pending work.

When enabled, Gamble Blacklist returns the full amount of each matching incoming economy receipt from a listed player, including payments inside Payment Guard's min/max range. Payment Guard does not need to be enabled. The same server receipt parser and command dispatcher are used. A blacklisted receipt takes priority over ordinary refund/donation rules and is queued once, so both enabled modules do not duplicate a refund and blocked receipts do not show donation announcements.

Normal `/pay <sender> <expanded amount>` commands are paced 1.5 seconds apart, limited to 32 pending entries and a 60-second lifetime, bound to the current connection and never automatically retried. Disabling the blacklist cancels its pending refunds; removing a name also prevents its queued blacklist refund from being sent. Payment Guard settings/toggle changes leave pending blacklist work intact. Overflow disables both refund modules and clears their queues.

The server still decides whether a payment command succeeds. This mod does not bypass economy permissions/cooldowns or confirm account balances. It acts on supported server GAME receipt messages only; it does not scan historical chat or trust ordinary player-prefixed chat. See PAYMENTS.md for receipt formats and protocol limitations.

## Animation

The interface retains timed menu transitions, hover easing, glowing switches and adds a timed inspector slide-in/slide-out. Inspector widgets and pointer coordinates use the same animation offset, keeping clicks aligned. List rendering uses clipping and only creates visible rows. Animations use elapsed time on the existing render/tick loop, with no animation threads or external networking.

The delivered GIF/MP4 use actual frame captures from the isolated Minecraft test world. Alice and Bob_12 in those previews are example usernames; no real players were paid for testing.
