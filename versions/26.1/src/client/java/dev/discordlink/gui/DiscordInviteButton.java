package dev.discordlink.gui;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;

/** A public invite shortcut; no Discord account integration or credentials. */
public final class DiscordInviteButton extends Button {
  public static final URI INVITE = URI.create("https://discord.gg/3h9MAYQwUc");
  private float hover;
  private long frame = System.nanoTime();
  public DiscordInviteButton(int x, int y) {
    this(x, y, () -> CompletableFuture.runAsync(() -> Util.getPlatform().openUri(INVITE)));
  }
  public DiscordInviteButton(int x, int y, Runnable open) {
    super(x, y, 32, 32, Inter.text("Open Discord invite", Inter.Weight.MEDIUM),
        button -> open.run(), DEFAULT_NARRATION);
    setTooltip(Tooltip.create(Inter.text("Join our Discord", Inter.Weight.MEDIUM)));
  }
  @Override
  protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
    long now = System.nanoTime();
    float dt = Math.min(.1f, (now - frame) / 1e9f);
    frame = now;
    hover += ((isHoveredOrFocused() ? 1 : 0) - hover)
        * (GuiPreferences.animations ? 1 - (float) Math.exp(-14 * dt) : 1);
    Glass.glow(g, getX(), getY(), 32, 32, 9, hover * .7f);
    MenuScreen.round(g, getX(), getY(), 32, 32, 9, (Math.round(30 + hover * 35) << 24) | 0xffffff);
    MenuScreen.icon(g, "discord", getX() + 5, getY() + 5, 22, 0xffffffff);
  }
}
