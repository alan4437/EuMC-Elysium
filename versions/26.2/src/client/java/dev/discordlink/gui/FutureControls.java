package dev.discordlink.gui;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;

/** Optional reusable controls; none are displayed by the empty module registry. */
public final class FutureControls {
  public static final class Toggle extends net.minecraft.client.gui.components.Button {
    private boolean value;
    private final Consumer<Boolean> changed;
    public Toggle(
        int x, int y, int width, String label, boolean initial, Consumer<Boolean> changed) {
      super(x, y, width, 28, Inter.text(label, Inter.Weight.MEDIUM), b -> {}, DEFAULT_NARRATION);
      value = initial;
      this.changed = changed;
    }
    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
      value = !value;
      changed.accept(value);
    }
    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
      MenuScreen.round(g, getX(), getY(), getWidth(), getHeight(), 5,
          isHoveredOrFocused() ? 0xff363a46 : 0xff292c35);
      Inter.draw(
          g, getMessage().getString(), getX() + 10, getY() + 10, 0xffe1e4ed, Inter.Weight.MEDIUM);
      MenuScreen.round(g, getRight() - 28, getY() + 8, 20, 10, 5, value ? 0xffa6b6e8 : 0xff454954);
      MenuScreen.round(g, getRight() - (value ? 16 : 26), getY() + 10, 6, 6, 3, 0xfff0f1f5);
    }
  }
  public static final class Slider extends AbstractSliderButton {
    private final String label;
    private final DoubleConsumer changed;
    public Slider(int x, int y, int width, String label, double initial, DoubleConsumer changed) {
      super(x, y, width, 28, Inter.text(label, Inter.Weight.MEDIUM), Math.clamp(initial, 0, 1));
      this.label = label;
      this.changed = changed;
      updateMessage();
    }
    @Override
    protected void updateMessage() {
      if (label != null)
        setMessage(Inter.text(label + "  " + Math.round(value * 100) + "%", Inter.Weight.MEDIUM));
    }
    @Override
    protected void applyValue() {
      if (changed != null)
        changed.accept(value);
    }
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      Inter.draw(g, getMessage().getString(), getX(), getY(), 0xffd4d9e5, Inter.Weight.MEDIUM);
      MenuScreen.round(g, getX(), getY() + 19, getWidth(), 3, 1, 0xff383c48);
      MenuScreen.round(g, getX(), getY() + 19, (int) (value * getWidth()), 3, 1, 0xff9baedc);
      MenuScreen.round(
          g, getX() + (int) (value * (getWidth() - 7)), getY() + 17, 7, 7, 3, 0xffdce4ff);
    }
  }
  public record SettingsPanel(int x, int y, int width, int height, String title) {
    public void extractRenderState(GuiGraphicsExtractor g) {
      MenuScreen.round(g, x, y, width, height, 7, 0xff22252d);
      Inter.draw(g, title, x + 12, y + 12, 0xffe4e7f0, Inter.Weight.SEMIBOLD);
    }
    public void beginContent(GuiGraphicsExtractor g) {
      g.enableScissor(x + 8, y + 30, x + width - 8, y + height - 8);
    }
    public void endContent(GuiGraphicsExtractor g) {
      g.disableScissor();
    }
  }
  public record ModuleCard(MenuScreen.ModuleDefinition module) {
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, int width, boolean hovered) {
      MenuScreen.round(g, x, y, width, 62, 6, hovered ? 0xff2c3039 : 0xff23262e);
      Inter.draw(g, module.name(), x + 12, y + 12, 0xffe2e6ef, Inter.Weight.MEDIUM);
      g.enableScissor(x + 12, y + 28, x + width - 12, y + 54);
      g.textWithWordWrap(net.minecraft.client.Minecraft.getInstance().font,
          Inter.text(module.description(), Inter.Weight.REGULAR), x + 12, y + 30, width - 24,
          0xff979ead, false);
      g.disableScissor();
    }
  }
  private FutureControls() {}
}
