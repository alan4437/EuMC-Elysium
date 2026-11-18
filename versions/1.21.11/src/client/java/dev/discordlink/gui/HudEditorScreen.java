package dev.discordlink.gui;

import dev.gamblingmenu.auction.AuctionModule;
import dev.gamblingmenu.profit.ProfitTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

/** One layout editor for all mod HUDs. Previewing never enables modules or creates transactions. */
public final class HudEditorScreen extends Screen {
  private final Screen parent;
  private boolean auctionDragging;
  private float grabX, grabY;
  public HudEditorScreen(Screen parent) {
    super(Inter.text("Edit HUD positions", Inter.Weight.SEMIBOLD));
    this.parent = parent;
  }
  @Override
  public boolean isPauseScreen() {
    return false;
  }
  @Override
  protected void init() {
    addRenderableWidget(
        new RoundedButton(width / 2 - 112, height - 34, 108, "Reset positions", b -> {
          release();
          var a = AuctionModule.INSTANCE;
          var p = ProfitTracker.INSTANCE;
          a.hudX = .02f;
          a.hudY = .04f;
          p.positionX = .05f;
          p.positionY = .72f;
          a.save();
          p.changed();
        }));
    addRenderableWidget(new RoundedButton(width / 2 + 4, height - 34, 108, "Done", b -> onClose()));
  }
  private void release() {
    ProfitTracker.INSTANCE.release();
    if (auctionDragging) {
      auctionDragging = false;
      AuctionModule.INSTANCE.save();
    }
  }
  @Override
  public void onClose() {
    release();
    minecraft.setScreen(parent);
  }
  @Override
  public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
    if (super.mouseClicked(e, twice))
      return true;
    if (ProfitTracker.INSTANCE.click(e.x(), e.y(), e.button(), width, height))
      return true;
    var a = AuctionModule.INSTANCE;
    float x = a.hudLeft(width, height), y = a.hudTop(width, height),
          s = a.hudRenderScale(width, height);
    if (e.button() == 0 && e.x() >= x && e.x() <= x + 230 * s && e.y() >= y
        && e.y() <= y + 174 * s) {
      auctionDragging = true;
      grabX = (float) e.x() - x;
      grabY = (float) e.y() - y;
      return true;
    }
    return false;
  }
  @Override
  public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
    if (ProfitTracker.INSTANCE.drag(e.x(), e.y(), width, height))
      return true;
    if (auctionDragging) {
      var a = AuctionModule.INSTANCE;
      float s = a.hudRenderScale(width, height);
      a.hudX = Math.clamp(((float) e.x() - grabX) / Math.max(1, width - 230 * s), 0, 1);
      a.hudY = Math.clamp(((float) e.y() - grabY) / Math.max(1, height - 174 * s), 0, 1);
      return true;
    }
    return super.mouseDragged(e, dx, dy);
  }
  @Override
  public boolean mouseReleased(MouseButtonEvent e) {
    boolean dragging = auctionDragging;
    boolean profit = ProfitTracker.INSTANCE.release();
    release();
    return dragging || profit || super.mouseReleased(e);
  }
  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
    g.fill(0, 0, width, height, 0x33000000);
  }
  @Override
  public void render(GuiGraphics g, int mx, int my, float delta) {
    AuctionModule.INSTANCE.renderBoard(g, true);
    ProfitTracker.INSTANCE.render(g, true);
    int panelW = Math.min(width - 16, 360), px = (width - panelW) / 2;
    MenuScreen.round(g, px, 6, panelW, 31, 8, 0xe5101117);
    Inter.draw(g, "HUD LAYOUT EDITOR · PREVIEWS", px + 10, 12, 0xff000000 | GuiPreferences.accent,
        Inter.Weight.SEMIBOLD);
    Inter.draw(g, "Drag HUDs into place. Positions save automatically.", px + 10, 24, 0xffbbbfc9,
        Inter.Weight.REGULAR);
    super.render(g, mx, my, delta);
  }
}
