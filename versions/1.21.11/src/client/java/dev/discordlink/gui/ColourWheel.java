package dev.discordlink.gui;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;

/** Cached hue/saturation texture; brightness and marker updates require no texture uploads. */
public final class ColourWheel extends Button {
  private final java.util.function.IntSupplier readColour;
  private final java.util.function.IntConsumer writeColour;
  private final Runnable saveColour;
  private float hue, saturation, brightness;
  private int dragging;
  public ColourWheel(int x, int y, int width, boolean accent) {
    this(x, y, width, accent ? "Accent colour wheel" : "Panel colour wheel",
        () -> accent ? GuiPreferences.accent : GuiPreferences.surface, rgb -> {
          if (accent)
            GuiPreferences.accent = rgb;
          else
            GuiPreferences.surface = rgb;
        }, GuiPreferences::save);
  }
  public ColourWheel(int x, int y, int width, String label, java.util.function.IntSupplier read,
      java.util.function.IntConsumer write, Runnable save) {
    super(x, y, width, 78, Inter.text(label, Inter.Weight.MEDIUM), b -> {}, DEFAULT_NARRATION);
    readColour = read;
    writeColour = write;
    saveColour = save;
    float[] hsv = Color.RGBtoHSB((rgb() >> 16) & 255, (rgb() >> 8) & 255, rgb() & 255, null);
    hue = hsv[0];
    saturation = hsv[1];
    brightness = hsv[2];
  }
  private int rgb() {
    return readColour.getAsInt();
  }
  private void choose(double x, double y) {
    if (dragging == 1) {
      double dx = x - (getX() + 36), dy = y - (getY() + 36);
      hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
      saturation = (float) Math.min(1, Math.hypot(dx, dy) / 35);
    } else if (dragging == 2)
      brightness = (float) Math.clamp((x - getX() - 88) / (getWidth() - 96), 0, 1);
    int colour = Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff;
    writeColour.accept(colour);
  }
  @Override
  public void onClick(MouseButtonEvent e, boolean twice) {
    double dx = e.x() - (getX() + 36), dy = e.y() - (getY() + 36);
    dragging = dx * dx + dy * dy <= 36 * 36                                    ? 1
        : e.x() >= getX() + 88 && e.y() >= getY() + 30 && e.y() <= getY() + 54 ? 2
                                                                               : 0;
    if (dragging != 0) {
      choose(e.x(), e.y());
      saveColour.run();
    }
  }
  @Override
  protected void onDrag(MouseButtonEvent e, double dx, double dy) {
    if (dragging != 0)
      choose(e.x(), e.y());
  }
  @Override
  public void onRelease(MouseButtonEvent e) {
    if (dragging != 0)
      saveColour.run();
    dragging = 0;
  }
  @Override
  public void onPress(net.minecraft.client.input.InputWithModifiers input) {
    hue = (hue + .04f) % 1;
    saturation = Math.max(.5f, saturation);
    brightness = Math.max(.5f, brightness);
    int colour = Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff;
    writeColour.accept(colour);
    saveColour.run();
  }
  @Override
  protected void renderContents(GuiGraphics g, int mx, int my, float delta) {
    int tint = Math.round(brightness * 255);
    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "discordlink", "textures/gui/colour-wheel.png"),
        getX(), getY(), 0, 0, 72, 72, 256, 256, 256, 256,
        0xff000000 | (tint << 16) | (tint << 8) | tint);
    int px = getX() + 36 + Math.round((float) Math.cos(hue * 2 * Math.PI) * saturation * 35);
    int py = getY() + 36 + Math.round((float) Math.sin(hue * 2 * Math.PI) * saturation * 35);
    MenuScreen.round(g, px - 4, py - 4, 8, 8, 4, 0xff111111);
    MenuScreen.round(g, px - 3, py - 3, 6, 6, 3, 0xffffffff);
    int left = getX() + 88, width = getWidth() - 96;
    Inter.draw(g, "Brightness", left, getY() + 5, 0xffbbbbc5, Inter.Weight.REGULAR);
    for (int i = 0; i < width; i++)
      g.fill(left + i, getY() + 34, left + i + 1, getY() + 46,
          Color.HSBtoRGB(hue, saturation, i / (float) Math.max(1, width - 1)));
    MenuScreen.round(
        g, left + Math.round(brightness * width) - 2, getY() + 31, 4, 18, 2, 0xffeeeeef);
    Inter.draw(g, GuiPreferences.hex(rgb()), left, getY() + 59, 0xffeeeeef, Inter.Weight.MEDIUM);
  }
}
