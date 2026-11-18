package dev.discordlink.gui;
import dev.gamblingmenu.auction.AuctionModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Reads the 36 inventory/hotbar slots; selection never moves an item. */
public final class AuctionItemScreen extends Screen {
  private final Screen parent;
  public AuctionItemScreen(Screen parent) {
    super(Component.literal("Choose auction item"));
    this.parent = parent;
  }
  @Override
  protected void init() {
    int left = width / 2 - 126, top = height / 2 - 70;
    if (minecraft.player != null)
      for (int slot = 0; slot < 36; slot++) {
        final int index = slot;
        var stack = minecraft.player.getInventory().getItem(slot).copy();
        var button = new Button(
            left + (slot % 9) * 28, top + (slot / 9) * 28, 26, 26, stack.getHoverName(), b -> {
              AuctionModule.INSTANCE.select(index);
              onClose();
            }, supplier -> supplier.get()) {
          @Override
          protected void renderContents(GuiGraphics g, int mx, int my, float d) {
            Glass.inset(g, getX(), getY(), 26, 26, 6);
            g.renderItem(stack, getX() + 5, getY() + 5);
            if (isHoveredOrFocused())
              MenuScreen.round(g, getX(), getY(), 26, 2, 0, 0xffeeeeef);
          }
        };
        button.active = !stack.isEmpty();
        button.setTooltip(Tooltip.create(stack.getHoverName()));
        addRenderableWidget(button);
      }
    addRenderableWidget(new RoundedButton(width / 2 - 60, top + 128, 120, "Back", b -> onClose()));
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
  public void renderBackground(GuiGraphics g, int mx, int my, float d) {
    renderBlurredBackground(g);
    g.fill(0, 0, width, height, 0xa0000000);
  }
  @Override
  public void render(GuiGraphics g, int mx, int my, float d) {
    Inter.draw(g, "Choose an item stack from your inventory", width / 2 - 125, height / 2 - 98,
        0xffeeeeef, Inter.Weight.MEDIUM);
    super.render(g, mx, my, d);
  }
}
