package dev.gamblingmenu.profit;

import dev.discordlink.gui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Read-only breakdown explains the exclusions rather than implying server-verified outcomes. */
public final class ProfitDetailsScreen extends Screen {
  private final Screen parent;
  public ProfitDetailsScreen(Screen parent) {
    super(Inter.text("Profit statistics", Inter.Weight.SEMIBOLD));
    this.parent = parent;
  }
  @Override
  protected void init() {
    addRenderableWidget(
        new RoundedButton(width / 2 - 60, height - 40, 120, "Back", b -> onClose()));
  }
  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }
  @Override
  public boolean isPauseScreen() {
    return false;
  }
  @Override
  public void render(GuiGraphics g, int mx, int my, float delta) {
    float s = Math.min(1f, Math.min(width / 500f, (height - 50) / 310f));
    int w = 470;
    g.pose().pushMatrix();
    g.pose().translate((width - w * s) / 2, 12);
    g.pose().scale(s, s);
    Glass.panel(g, 0, 0, w, Math.round((height - 64) / s), 14, true);
    Inter.draw(g, "PROFIT TRACKER · CHAT-CONFIRMED TOTALS", 14, 14,
        0xff000000 | GuiPreferences.accent, Inter.Weight.SEMIBOLD);
    var p = ProfitTracker.INSTANCE;
    draw(g, 14, 36, "CURRENT SESSION", p.ledger.session());
    draw(g, w / 2, 36, "LIFETIME", p.ledger.lifetime());
    Inter.wrap(g,
        "Received excludes Payment Guard donations and payments marked for automatic refund. Paid "
            + "Out includes confirmed refunds; Profit excludes refunds. Gambles counts confirmed "
            + "multiplier payouts only, not gambling wins/losses.",
        14, 190, w - 28, 0xffb8bbc7);
    if ((height - 64) / s > 330)
      Inter.wrap(g,
          "Chat has no transaction IDs. Identical player/amount/direction receipts within 2 "
              + "seconds are treated as repeats. Missing receipts cannot be counted; ambiguous "
                + "manual payouts are unclassified.",
          14, 260, w - 28, 0xff969aa5);
    g.pose().popMatrix();
    super.render(g, mx, my, delta);
  }
  private void draw(GuiGraphics g, int x, int y, String title, ProfitLedger.Totals t) {
    Inter.draw(g, title, x, y, 0xff000000 | GuiPreferences.accent, Inter.Weight.SEMIBOLD);
    String[] lines = {"Received: $" + ProfitTracker.money(t.received()),
        "Paid Out: $" + ProfitTracker.money(t.paid()),
        "Profit: " + (t.profit().signum() < 0 ? "−$" : "+$")
            + ProfitTracker.money(t.profit().abs()),
        "Processed gambles: " + t.gambles(), "Refunds: $" + ProfitTracker.money(t.refunds()),
        "Guard donations: $" + ProfitTracker.money(t.donations())};
    for (int i = 0; i < lines.length; i++)
      Inter.draw(g, lines[i], x, y + 21 + i * 20, 0xffeeeeef, Inter.Weight.REGULAR);
  }
}
