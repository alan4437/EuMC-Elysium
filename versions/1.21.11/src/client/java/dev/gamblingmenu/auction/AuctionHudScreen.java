package dev.gamblingmenu.auction;
import dev.discordlink.gui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Auction layout preferences, independent of live bids and timer state. */
public final class AuctionHudScreen extends Screen {
  private final Screen parent;
  private int x, y, w;
  public AuctionHudScreen(Screen parent) {
    super(Inter.text("Auction HUD appearance", Inter.Weight.SEMIBOLD));
    this.parent = parent;
  }
  @Override
  public boolean isPauseScreen() {
    return false;
  }
  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }
  @Override
  protected void init() {
    var a = AuctionModule.INSTANCE;
    w = Math.min(320, width - 20);
    x = (width - w) / 2;
    y = Math.max(6, (height - 164) / 2);
    input("Auction HUD scale", a.hudScale, 38, .5f, 2, v -> a.hudScale = v);
    input("Auction HUD opacity", a.hudOpacity, 70, 20, 100, v -> a.hudOpacity = Math.round(v));
    addRenderableWidget(new RoundedButton(
        x + 12, y + 102, w / 2 - 18, a.hudHidden ? "HUD: Hidden" : "HUD: Shown", b -> {
          a.hudHidden = !a.hudHidden;
          a.save();
          clearWidgets();
          init();
        }));
    addRenderableWidget(
        new RoundedButton(x + w / 2 + 6, y + 102, w / 2 - 18, "Reset appearance", b -> {
          a.hudScale = 1;
          a.hudOpacity = 90;
          a.hudHidden = false;
          a.save();
          clearWidgets();
          init();
        }));
    addRenderableWidget(new RoundedButton(x + 12, y + 134, w - 24, "Back", b -> onClose()));
  }
  private void input(String label, float value, int offset, float min, float max,
      java.util.function.Consumer<Float> set) {
    var field = addRenderableWidget(new AmountInput(x + w - 102, y + offset, 82, label,
        java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()));
    field.setMaxLength(6);
    field.setResponder(text -> {
      try {
        float n = Float.parseFloat(text);
        if (!Float.isFinite(n) || n < min || n > max)
          throw new IllegalArgumentException();
        set.accept(n);
        AuctionModule.INSTANCE.save();
        field.setTextColor(0xffeeeeef);
      } catch (Exception e) {
        field.setTextColor(0xffff7777);
      }
    });
  }
  @Override
  public void render(GuiGraphics g, int mx, int my, float delta) {
    Glass.panel(g, x, y, w, 164, 12, true);
    Inter.draw(g, "Auction HUD appearance", x + 12, y + 13, 0xff000000 | GuiPreferences.accent,
        Inter.Weight.SEMIBOLD);
    String[] labels = {"Scale (0.5–2)", "Opacity (%)"};
    for (int i = 0; i < labels.length; i++) {
      Glass.inset(g, x + w - 108, y + 35 + i * 32, 90, 26, 7);
      Inter.draw(g, labels[i], x + 12, y + 43 + i * 32, 0xffc5c8d2, Inter.Weight.REGULAR);
    }
    super.render(g, mx, my, delta);
  }
}
