# Live Profit Tracker

Under Gambling, left-click Profit Tracker to enable/pause tracking. Right-click for
its inspector. Show/hide the HUD, select Session/Lifetime, toggle individual rows,
change text/panel colours with wheels, or follow the GUI palette. Enter scale and
opacity. To move HUDs, open Settings → Edit HUD positions. Drag the Profit Tracker
and Auction previews into place, then press Done. Profit Tracker's bottom-right
handle also resizes it. Previewing does not enable modules or start auctions. Saved
positions are used next time the HUDs appear and stay inside the viewport on resize. The inspector scrolls on small windows.
Reset current session requires a second confirmation click and never resets lifetime.

The HUD uses bundled Inter, themed rounded translucent panels and eased visibility.
It uses the menu's existing blur while editing; gameplay uses a lightweight translucent
surface and cached shadows rather than an additional full-screen blur pass.

## What the numbers mean

- Received: server-reported incoming payments while tracking is enabled, excluding
  Payment Guard donations and payments identified for automatic payback/blacklist refund.
- Paid Out: all server-reported outgoing payments while enabled, including refunds.
- Refunds: the subset of outgoing confirmations matching an automatic refund command
  sent by Payment Guard or Gamble Blacklist. A command alone does not count.
- Guard donations: incoming payments classified as donations by Payment Guard;
  recorded separately, excluded from Received, Profit and Gambles.
- Profit: Received minus (Paid Out minus Refunds). Refund money is excluded from
  both sides of gambling profit, per the requested Payment Guard exclusion.
- Gambles: confirmed multiplier payouts, one per matched outgoing confirmation.
  Ordinary receipts, donations, refunds and Auction bids never increment this count.
  It is a count of processed multiplier transactions, not server-verified gambling
  outcomes. Losses or rounds without a payout cannot be inferred from payment chat.

Incoming ordinary payments still contribute to Received. This is a payment-based
cash-flow tracker; it cannot determine whether an arbitrary payment was a bet.
Auction activity is not given a gamble classification; its incoming receipts remain
in payment totals unless an automatic refund/donation rule excludes them.

## Chat and confirmation limits

Reuses PaymentModule's single non-overlay GAME handler, using original text before
local multiplier tags. It accepts the existing anchored incoming ECONOMY patterns
and the observed outgoing format `ECONOMY ➜ You paid 1.00 to PlayerName.`
K/M/B/T suffixes, case-insensitive usernames and exact decimal arithmetic are supported.
Player-prefixed messages, unrelated text, outgoing failures, zero and invalid amounts
are rejected. No server API, external requests or polling is used.

The server format supplies no transaction ID. Same direction/player/exact amount
within two seconds is conservatively treated as duplicate, even with different
abbreviations/case. Two genuine identical transfers in that window may be suppressed;
repeats outside it cannot reliably be distinguished. This is bounded duplicate
suppression, not an exactly-once guarantee.

Refund/multiplier intent correlation lasts 60 seconds and requires the same player
and exact amount. It is cleared on reconnect or pause. A manually sent identical
payment inside that interval is ambiguous. Missing/changed server receipts cannot be
counted. No failed command is assumed paid and no payout is retried by this tracker.
A payment marked for refund is excluded from income even if its refund later fails;
only the confirmed outgoing refund enters the separate refund counter.

Session statistics begin at zero on Minecraft startup and survive server reconnects.
Lifetime totals, enable state and appearance save locally in
`config/gamblingmenu-profit.json`. Tracking is off by default on a fresh installation.
All matched servers share these local lifetime/session totals. Saves are throttled,
serialized, atomic where supported, and performed off the render thread; shutdown
flushes pending statistics. Unreadable existing data is preserved and a warning is
logged rather than overwriting it. Exact stored values are BigDecimal; HUD formatting
abbreviates for display only.

## Validation

Dependency-free tests cover exact arithmetic, very large totals, duplicate suppression,
intent vs confirmation, refunds/donations excluded from profit, manual/expired/cross-
connection classification, session reset and lifetime restore, and outgoing spoof parsing.
Minecraft client tests exercise real module command hooks with synthetic server chat,
positive/negative HUDs, duplicate refunds, coloured settings, dragging/resizing,
opacity, details screen and reset confirmation. No public-server money was used.
