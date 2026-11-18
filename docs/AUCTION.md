# EuropeMC Auction

Right-click Auction under EuropeMC to configure it. Choose any non-empty stack in
the 36 inventory/hotbar slots, set a minimum such as 10k/1b/1t, and enter a duration
in seconds (1–86400). The whole selected stack is shown; it stays in your inventory.
Start/Stop starts a fresh auction or cancels the current one. The card also toggles
it. An unchanged matching stack must still be in its slot when starting.

The top-five HUD ranks each player's highest single qualifying incoming payment,
not their cumulative payments. Matching is case-insensitive. Equal bids keep the
order in which that amount was first reached. Below-minimum bids, late bids,
player chat and payments automatically refunded by other modules do not count.
Minimum is inclusive. At most 1000 unique bidders are retained per auction.

The timer uses elapsed monotonic time. At expiry the leaderboard freezes. A local
winner announcement shows the winning player and amount, with rainbow confetti
from both sides and a firework sound. Confetti uses continuous elapsed-time motion,
air drag, gentle flutter and individual eased fades, independent of the tick rate. No qualifying bids means no winner. Results
automatically hide five seconds after the six-second confetti phase ends. The winner
text stays visible during that delay and fades out at the end. Hide results dismisses
them earlier; disconnect clears the auction.
Cancelling does not announce a winner or send refunds.

Primary colour styles the heading, ranks and winner name; secondary styles amounts
and the timer. Both use the existing colour wheels and persist with the minimum
and duration in config/gamblingmenu-auction.json. Minimum, time and item selection
are locked while running. Colour changes remain live. Items, bids and live auctions
are not restored across reconnects or restarts.

This module records server economy chat receipts locally. It does not transfer the
item, advertise publicly, escrow money or automatically pay/refund bidders. Final
item delivery and refunds are handled by the player. Payment Guard and Gamble
Blacklist retain their existing automatic refund behavior.

Tests use the isolated integrated server and synthetic economy receipts, with no
public-server payments. Covered: top five ordering, noncumulative bids, case matching,
ties, inclusive minimum, deadline, cancellation/reset, inventory selection and no
item movement, winner amount, confetti, and no unsolicited payment commands.

HUD appearance: right-click Auction and choose HUD appearance. Set scale (0.5–2),
background opacity and visibility. Changes save automatically. Move it using the
shared Settings → Edit HUD positions screen, even while inactive. Drag its preview
and press Done; the saved location is used next time the auction appears. Editing
the layout never starts an auction. The Right Shift menu renders above all mod HUDs.
