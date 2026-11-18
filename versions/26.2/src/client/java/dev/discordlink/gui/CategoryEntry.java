package dev.discordlink.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

/** Reusable category entry; only Gambling is registered by the menu. */
public final class CategoryEntry extends Button {
  private final boolean selected;
  private float hover;
  private long frame = System.nanoTime();
  public CategoryEntry(int x, int y, int width, String label, OnPress press) {
    this(x, y, width, label, press, true);
  }
  public CategoryEntry(int x, int y, int width, String label, OnPress press, boolean selected) {
    super(x, y, width, 30, Inter.text(label, Inter.Weight.SEMIBOLD), press, DEFAULT_NARRATION);
    this.selected = selected;
  }
  @Override
  protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
    long now = System.nanoTime();
    float dt = !GuiPreferences.animations ? 10 : Math.min(.1f, (now - frame) / 1e9f);
    frame = now;
    hover += ((isHoveredOrFocused() ? 1 : 0) - hover) * (1 - (float) Math.exp(-14 * dt));
    Glass.glow(g, getX(), getY(), getWidth(), getHeight(), 8, (selected ? .8f : 0) + hover * .6f);
    if (selected || hover > .01f) {
      MenuScreen.round(g, getX(), getY(), getWidth(), getHeight(), 8, 0x8ac4c4ca);
      Glass.gradient(
          g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 7, 0xb7626269, 0xb32c2d33);
    }
    if (getMessage().getString().equals("Settings")) {
      for (int row = 0; row < 3; row++)
        MenuScreen.round(g, getX() + 10, getY() + 9 + row * 5, 12, 2, 1, 0xffc7cbd6);
    } else if (getMessage().getString().equals("EuropeMC"))
      MenuScreen.icon(g,"europe",getX()+8,getY()+7,16,0xffffffff);
    else
      MenuScreen.icon(g, "dice", getX() + 8, getY() + 7, 16, 0xffc7cbd6);
    Inter.draw(
        g, getMessage().getString(), getX() + 29, getY() + 10, 0xffe4e5ec, Inter.Weight.SEMIBOLD);
  }
}
