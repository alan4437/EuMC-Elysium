package dev.discordlink.gui;

import dev.gamblingmenu.payment.PaymentModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Left click toggles. Right click opens settings. Native focus/narration and keyboard activation.
 */
public final class PaymentCard extends Button {
  private final Runnable settings;
  private final boolean blacklist;
  private final int kind;
  public int kind() {
    return kind;
  }
  public boolean blacklist() {
    return blacklist;
  }
  private boolean enabled() {
    return kind == 4 ? dev.gamblingmenu.profit.ProfitTracker.INSTANCE.enabled
        : kind == 3  ? dev.gamblingmenu.auction.AuctionModule.INSTANCE.enabled()
        : kind == 2  ? dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.enabled()
        : blacklist  ? dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.enabled()
                     : PaymentModule.INSTANCE.enabled();
  }
  private void toggle() {
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
  }
  private float enabledAnimation = PaymentModule.INSTANCE.enabled() ? 1 : 0;
  private float hover;
  private long lastFrame = System.nanoTime();
  public PaymentCard(int x, int y, int width, Runnable settings) {
    this(x, y, width, settings, false);
  }
  public PaymentCard(int x, int y, int width, Runnable settings, boolean blacklist) {
    this(x, y, width, settings, blacklist ? 1 : 0);
  }
  public PaymentCard(int x, int y, int width, Runnable settings, int kind) {
    super(x, y, width, 48,
        Inter.text(kind == 4 ? "Profit Tracker"
                : kind == 3  ? "Auction"
                : kind == 2  ? "Payment Multipliers"
                : kind == 1  ? "Gamble Blacklist"
                             : "Payment Guard",
            Inter.Weight.MEDIUM),
        b
        -> {
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
        },
        DEFAULT_NARRATION);
    this.settings = settings;
    this.blacklist = kind == 1;
    this.kind = kind;
    enabledAnimation = enabled() ? 1 : 0;
  }
  @Override
  protected boolean isValidClickButton(MouseButtonInfo info) {
    return info.button() == 0 || info.button() == 1;
  }
  @Override
  public void onClick(MouseButtonEvent event, boolean doubleClick) {
    if (event.button() == 1)
      settings.run();
    else
      toggle();
  }
  @Override
  protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
    boolean enabled = enabled();
    long now = System.nanoTime();
    float dt = !GuiPreferences.animations ? 10 : Math.min(.1f, (now - lastFrame) / 1e9f);
    lastFrame = now;
    float blend = 1 - (float) Math.exp(-11 * dt);
    enabledAnimation += ((enabled ? 1 : 0) - enabledAnimation) * blend;
    hover += ((isHoveredOrFocused() ? 1 : 0) - hover) * blend;
    Glass.shadow(g, getX(), getY(), getWidth(), getHeight(), 12);
    Glass.glow(
        g, getX(), getY(), getWidth(), getHeight(), 12, enabledAnimation * .8f + hover * .5f);
    MenuScreen.round(g, getX(), getY(), getWidth(), getHeight(), 12,
        blendColor(0x43585862, 0x99c4c4cb, enabledAnimation));
    Glass.gradient(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 11,
        blendColor(0xf2232429, 0xe14a4a50, enabledAnimation), 0xf20d0e12);
    g.enableScissor(getX() + 10, getY() + 5, getRight() - 48, getY() + 23);
    Inter.draw(g,
        kind == 4       ? "Profit Tracker"
            : kind == 3 ? "Auction"
            : kind == 2 ? "Payment Multipliers"
            : blacklist ? "Gamble Blacklist"
                        : "Payment Guard",
        getX() + 12, getY() + 8, 0xffe6e8ef, Inter.Weight.SEMIBOLD);
    g.disableScissor();
    Glass.toggle(g, getRight() - 40, getY() + 16, enabledAnimation);
    g.enableScissor(getX() + 12, getY() + 24, getRight() - 12, getY() + 42);
    String description =
        Inter.editingFont().plainSubstrByWidth(kind == 4 ? "Live session & lifetime statistics"
                : kind == 3                              ? "Timed auctions & top five bids"
                : kind == 2                              ? "Clickable chat payout options"
                : blacklist                              ? "Refund payments from listed players"
                                                         : "Payment limits & donations",
            getWidth() - 24);
    Inter.draw(g, description, getX() + 12, getY() + 28, 0xff989ca8, Inter.Weight.REGULAR);
    g.disableScissor();
  }
  private static int blendColor(int from, int to, float progress) {
    int result = 0;
    for (int shift = 0; shift <= 24; shift += 8) {
      int a = (from >>> shift) & 255, b = (to >>> shift) & 255;
      result |= Math.round(a + (b - a) * progress) << shift;
    }
    return result;
  }
}
