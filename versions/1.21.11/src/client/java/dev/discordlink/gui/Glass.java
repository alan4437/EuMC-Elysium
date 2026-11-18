package dev.discordlink.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Batched translucent surfaces, continuous gradients and cached antialiased corner masks. */
public final class Glass {
  private static final net.minecraft.resources.Identifier HALO =
      net.minecraft.resources.Identifier.fromNamespaceAndPath(
          "discordlink", "textures/gui/halo.png");
  private static void halo(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
    int spread = 12;
    int[] targetX = {x - spread, x + r, x + w - r, x + w + spread};
    int[] targetY = {y - spread, y + r, y + h - r, y + h + spread};
    int[] source = {0, 48, 80, 128};
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 3; col++) {
        int tw = targetX[col + 1] - targetX[col], th = targetY[row + 1] - targetY[row];
        if (tw <= 0 || th <= 0)
          continue;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, HALO, targetX[col],
            targetY[row], source[col], source[row], tw, th, source[col + 1] - source[col],
            source[row + 1] - source[row], 128, 128, color);
      }
  }
  public static void shadow(GuiGraphics g, int x, int y, int w, int h, int r) {
    if (GuiPreferences.shadows)
      halo(g, x, y + 4, w, h, r, 0x80000000);
  }
  public static void glow(GuiGraphics g, int x, int y, int w, int h, int r, float strength) {
    int alpha = Math.min(120, Math.round(62 * strength * GuiPreferences.glow / 100f));
    if (alpha > 0)
      halo(g, x, y, w, h, r, (alpha << 24) | GuiPreferences.accent);
  }
  public static void panel(GuiGraphics g, int x, int y, int w, int h, int r, boolean raised) {
    shadow(g, x, y, w, h, r);
    MenuScreen.round(g, x, y, w, h, r, raised ? 0x70d8d8df : 0x48c8c8d0);
    int alpha = Math.round(GuiPreferences.opacity * 2.55f);
    int rgb = GuiPreferences.surface;
    int dark = mix(rgb, ((rgb & 0xfefefe) >>> 1), Math.min(1, GuiPreferences.gloss / 100f));
    gradient(g, x + 1, y + 1, w - 2, h - 2, r - 1, (alpha << 24) | rgb, (alpha << 24) | dark);
  }
  public static void inset(GuiGraphics g, int x, int y, int w, int h, int r) {
    MenuScreen.round(g, x, y, w, h, r, 0x3a9999a2);
    gradient(g, x + 1, y + 1, w - 2, h - 2, r - 1, 0xf00c0d10, 0xe51a1b20);
  }
  private static int mix(int a, int b, float t) {
    int result = 0;
    for (int shift = 0; shift <= 24; shift += 8)
      result |= Math.round(((a >>> shift) & 255) * (1 - t) + ((b >>> shift) & 255) * t) << shift;
    return result;
  }
  public static void gradient(
      GuiGraphics g, int x, int y, int w, int h, int r, int top, int bottom) {
    if (w <= 0 || h <= 0)
      return;
    r = Math.max(0, Math.min(GuiPreferences.radius(r), Math.min(w, h) / 2));
    g.fillGradient(x + r, y, x + w - r, y + h, top, bottom);
    if (r == 0)
      return;
    int midTop = mix(top, bottom, r / (float) h), midBottom = mix(top, bottom, 1 - r / (float) h);
    g.fillGradient(x, y + r, x + r, y + h - r, midTop, midBottom);
    g.fillGradient(x + w - r, y + r, x + w, y + h - r, midTop, midBottom);
    var texture = net.minecraft.resources.Identifier.fromNamespaceAndPath(
        "discordlink", "textures/gui/circle.png");
    for (int corner = 0; corner < 4; corner++) {
      boolean right = (corner & 1) != 0, lower = (corner & 2) != 0;
      int color = mix(top, bottom, lower ? 1 - r / (2f * h) : r / (2f * h));
      g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture,
          right ? x + w - r : x, lower ? y + h - r : y, right ? 64 : 0, lower ? 64 : 0, r, r, 64,
          64, 128, 128, color);
    }
  }
  public static void toggle(GuiGraphics g, int x, int y, float on) {
    glow(g, x, y, 28, 15, 7, on * 1.8f);
    MenuScreen.round(g, x, y, 28, 15, 7, 0x55c8c8cf);
    int shade = Math.round(48 + on * 178);
    gradient(g, x + 1, y + 1, 26, 13, 6,
        0xff000000 | (shade << 16) | (shade << 8) | Math.min(255, shade + 3),
        0xff000000 | ((shade - 12) << 16) | ((shade - 12) << 8) | Math.max(0, shade - 9));
    g.pose().pushMatrix();
    g.pose().translate(on * 13, 0);
    int knobX = x + 2;
    MenuScreen.round(g, knobX - 1, y + 1, 13, 13, 6, 0x40000000);
    int knob = Math.round(210 - on * 187);
    MenuScreen.round(g, knobX, y + 2, 11, 11, 5,
        0xff000000 | (knob << 16) | (knob << 8) | Math.min(255, knob + 4));
    g.pose().popMatrix();
  }
  private Glass() {}
}
