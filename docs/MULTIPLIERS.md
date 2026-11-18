# Payment Multipliers

Enable Payment Multipliers in EuropeMC; right-click its card for three slots.
Enter positive multipliers such as `2x`, `4x`, or `1.5x`. Empty slots are hidden.
Valid edits apply and save automatically; red invalid edits leave the previous valid
value active. Multipliers must be between 0.01 and 1000, with up to two decimal places.
Settings persist in `config/gamblingmenu-multipliers.json`. Enable the module each
server session, as with the other payment modules.

Recognised incoming server economy receipts receive clickable local chat suffixes,
for example `ECONOMY ➜ You received 100 from Alice. [2x] [4x]`.
Hover shows the sender and exact payout. Clicking sends `/pay Alice 200` for 2x,
or `/pay Alice 400` for 4x. Choose only one payout per receipt. Tags expire after
five minutes, on disabling the module, or after disconnecting; at most 100 receipts
are retained. Old chat remains visible but its tags cannot send another payment.

Receipts automatically refunded by Gamble Blacklist or Payment Guard's Pay back
mode have no multiplier options. Donation mode may still show both its notification
and clickable payout options. Only matched non-overlay server messages qualify;
player chat, outgoing payments and the player's own receipts are excluded.

Payouts use exact BigDecimal arithmetic and are capped at 1,000 trillion. Options
that would require fractions smaller than a cent are omitted rather than rounded.
Sending a command does not guarantee server acceptance: balance, permissions and
server limits still apply. Commands are not automatically retried.

No real public economy-server funds were used for tests. The Minecraft client test
uses an isolated dummy /pay command to verify transmitted usernames and amounts.

Tag colours follow slot order: yellow, green, blue. Blank slots remain hidden.

Reference-matched tag shades: gold #FCB500, green #97E35E, cyan-blue #00BDF8.
