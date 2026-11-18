package dev.discordlink.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Vanilla's cached, oversampled TrueType glyph atlas; no per-frame font rasterization. */
public final class Inter {
  public enum Weight { REGULAR, MEDIUM, SEMIBOLD }
  private static final FontDescription[] FONTS = {
      resource("regular"), resource("medium"), resource("semibold")};
  private static FontDescription resource(String name) {
    return new FontDescription.Resource(
        Identifier.fromNamespaceAndPath("discordlink", "inter_" + name));
  }
  public static Style style(Weight weight) {
    return Style.EMPTY.withFont(FONTS[weight.ordinal()]);
  }
  public static MutableComponent text(String text, Weight weight) {
    return Component.literal(text).setStyle(style(weight));
  }
  public static void draw(GuiGraphicsExtractor g, String text, int x, int y, int color, Weight weight) {
    g.text(Minecraft.getInstance().font, text(text, weight), x, y, color, false);
  }
  public static void heading(GuiGraphicsExtractor g, String text, int x, int y) {
    g.pose().pushMatrix();
    g.pose().translate(x, y);
    g.pose().scale(1.35f, 1.35f);
    draw(g, text, 0, 0, 0xfff0f1f5, Weight.SEMIBOLD);
    g.pose().popMatrix();
  }
  public static void wrap(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
    var font = Minecraft.getInstance().font;
    for (var line : font.split(text(text, Weight.REGULAR), width)) {
      g.text(font, line, x, y, color, false);
      y += 14;
    }
  }
  private static net.minecraft.client.gui.Font editingFont;
  /**
   * Native EditBox must measure cursor, selection and mouse positions using the actual Inter
   * advances.
   */
  public static net.minecraft.client.gui.Font editingFont() {
    if (editingFont == null) {
      var delegate = ((dev.discordlink.mixin.FontAccessor) (Object) Minecraft.getInstance().font)
                         .discordlink$provider();
      editingFont = new net.minecraft.client.gui.Font(new net.minecraft.client.gui.Font.Provider() {
        public net.minecraft.client.gui.GlyphSource glyphs(FontDescription ignored) {
          return delegate.glyphs(FONTS[0]);
        }
        public net.minecraft.client.gui.font.glyphs.EffectGlyph effect() {
          return delegate.effect();
        }
      });
    }
    return editingFont;
  }
  private Inter() {}
}
