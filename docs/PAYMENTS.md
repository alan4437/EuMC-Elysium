# Payment Guard

Open with Right Shift. Left-click Payment Guard to toggle it. Right-click to open the right-side settings. Set Minimum, Maximum, and Mode. Valid numeric edits save automatically after a short pause; mode selection applies immediately. X closes the settings pane. Searching payment/refund/donation matches the card.

Examples: minimum `20k`, maximum `1t`. A receipt of `19k` or `41T` triggers the selected mode. Exactly `20k`, exactly `1t`, and amounts between them produce no automatic action. Letter suffixes are case-insensitive and support two decimal places before the suffix, including `1.5m`. Negative/scientific/comma-formatted input is rejected; amounts are capped at 1,000,000,000,000,000. Invalid ranges are rejected without changing active settings.

## Modes

**Donation**: the money remains in the balance. A five-second centered Inter announcement displays green `NEW DONATION FROM <sender>` with `<amount> DONATED` in white underneath; Minecraft's firework rocket blast sound plays. Notifications render above the custom menu and other screens rather than being blurred with gameplay. This is a local display, not a public chat message. Only out-of-range payments qualify.

**Pay back**: the client sends `pay <sender> <expanded exact decimal amount>` through Minecraft's normal server command connection. Example: `1b` becomes `1000000000`, with no floating-point rounding or scientific notation. The client must be online with the module enabled when the receipt arrives. The server still controls the balance, permissions, command syntax and cooldowns. The status reports “command sent (not confirmed)”; this client does not claim the economy transaction completed.

Limits and mode persist in the instance's `config/gamblingmenu-payments.json`. Saving is asynchronous and uses atomic replacement where supported. The enabled state is deliberately session-only: joining/disconnecting clears pending work and requires enabling again. Changed settings or disabling clears pending refunds; identical values do not clear pending work. Invalid saved configuration falls back to defaults. Default range is 1–1B, Donation mode, disabled.

## Receipt matching

Supported examples:

```
ECONOMY ➜ You received 41T from ♟ 20ka.
ECONOMY » You received 1.00 from Alice.
[ECONOMY] ➜ Bob_12 paid you 1b!
```

An optional single player icon is accepted before the Java username. Matching tolerates common arrow separators, capitalization and nonbreaking spaces. The actual Unicode icon/arrow must belong to the supported format; if the server uses different text, copy the incoming chat line for a parser update. Outgoing `You paid ... to ...`, player-prefixed chat, action-bar messages, invalid usernames and nonpositive amounts are ignored.

The listener is registered only for Fabric's server GAME messages, never CHAT messages from players. Text matching cannot supply server transaction IDs or cryptographic proof of a payment. The connected server remains the authority; an administrative/plugin system message impersonating this exact economy format can look identical to a receipt. Historical receipts are not scraped from the chat UI.

At most 32 refund commands can wait; they are paced 1.5 seconds apart, tied to the current connection, expire after 60 seconds, and are sent once without retries. Overflow disables the module and clears the queue. Keeping a disconnected queue or retrying a rejected payment could send funds later or twice, so neither happens. Multiple identical legitimate receipts are distinct messages; there is no invented transaction-ID deduplication.

## Implementation

- `PaymentRules`: pure exact-decimal parsing, anchored receipt matching, inclusive boundaries and command construction.
- `PaymentModule`: settings persistence, session lifecycle, server receipt listener, bounded command queue, HUD/screen notification and sound.
- `PaymentCard`: left-click toggle / right-click settings, plus keyboard activation.
- `AmountInput`: native editing and Inter text/cursor measurements.
- `MenuScreen`: settings layout, validation feedback and two-choice mode dropdown.

No Discord code or external service was reintroduced. No live public-server payment was used in testing.
