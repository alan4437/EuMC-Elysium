package dev.discordlink.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
public final class RoundedButton extends Button {
  private float hover = 0;
  private long frame = System.nanoTime();
  public RoundedButton(int x, int y, int w, String label, OnPress press) {
    super(x, y, w, 24, Inter.text(label, Inter.Weight.MEDIUM), press, DEFAULT_NARRATION);
  }
  @Override
  protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
    long now = System.nanoTime();
    float dt = !GuiPreferences.animations ? 10 : Math.min(.1f, (now - frame) / 1e9f);
    frame = now;
    hover += ((isHoveredOrFocused() ? 1 : 0) - hover) * (1 - (float) Math.exp(-14 * dt));
    if (active)
      Glass.glow(g, getX(), getY(), getWidth(), getHeight(), 7, hover * .65f);
    MenuScreen.round(
        g, getX(), getY(), getWidth(), getHeight(), 7, active ? 0x408a8a94 : 0x253b3b43);
    Glass.gradient(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 6,
        active && isHoveredOrFocused() ? 0xe54a4b52 : 0xe12c2d34, 0xf016171c);
    g.text(Minecraft.getInstance().font, getMessage(),
        getX() + (getWidth() - Minecraft.getInstance().font.width(getMessage())) / 2, getY() + 8,
        active ? 0xfff3f5ff : 0xff8890a0, false);
  }
}
