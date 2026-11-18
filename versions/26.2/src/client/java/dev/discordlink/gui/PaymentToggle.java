package dev.discordlink.gui;

import dev.gamblingmenu.payment.PaymentModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

/** Settings-header switch, sharing the card's live enabled state. */
public final class PaymentToggle extends Button {
  private float on = PaymentModule.INSTANCE.enabled() ? 1 : 0;
  private long frame = System.nanoTime();
  public PaymentToggle(int x, int y, int width) {
    this(x, y, width, false);
  }
  private final boolean blacklist;
  private final int kind;
  public PaymentToggle(int x, int y, int width, boolean blacklist) {
    this(x, y, width, blacklist ? 1 : 0);
  }
  public PaymentToggle(int x, int y, int width, int kind) {
    super(x, y, width, 28, Inter.text("Enabled", Inter.Weight.REGULAR), b -> {
      if (kind == 4)
        dev.gamblingmenu.profit.ProfitTracker.INSTANCE.toggle();
      else if (kind == 3)
        dev.gamblingmenu.auction.AuctionModule.INSTANCE.toggle();
      else if (kind == 2)
        dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.toggle();
      else if (kind == 1)
        dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.toggle();
      else
        PaymentModule.INSTANCE.toggle();
    }, DEFAULT_NARRATION);
    this.blacklist = kind == 1;
    this.kind = kind;
    on = enabled() ? 1 : 0;
  }
  private boolean enabled() {
    return kind == 4 ? dev.gamblingmenu.profit.ProfitTracker.INSTANCE.enabled
        : kind == 3  ? dev.gamblingmenu.auction.AuctionModule.INSTANCE.enabled()
        : kind == 2  ? dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.enabled()
        : blacklist  ? dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.enabled()
                     : PaymentModule.INSTANCE.enabled();
  }
  @Override
  protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
    long now = System.nanoTime();
    float dt = !GuiPreferences.animations ? 10 : Math.min(.1f, (now - frame) / 1e9f);
    frame = now;
    on += ((enabled() ? 1 : 0) - on) * (1 - (float) Math.exp(-11 * dt));
    Inter.draw(g, "Enabled", getX(), getY() + 9, 0xffc6c7ce, Inter.Weight.REGULAR);
    Glass.toggle(g, getRight() - 30, getY() + 7, on);
  }
}
