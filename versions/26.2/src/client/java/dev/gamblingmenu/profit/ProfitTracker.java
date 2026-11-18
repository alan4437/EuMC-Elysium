package dev.gamblingmenu.profit;

import com.google.gson.Gson;
import dev.discordlink.gui.*;
import dev.gamblingmenu.payment.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Uses PaymentModule's original, non-overlay GAME receipts; no second chat listener. */
public final class ProfitTracker {
  public static final ProfitTracker INSTANCE = new ProfitTracker();
  public final ProfitLedger ledger = new ProfitLedger();
  public boolean enabled, showHud = true, lifetimeView, customColour;
  public boolean[] rows = {true, true, true, true};
  public float scale = 1, positionX = .05f, positionY = .72f;
  public int opacity = 82, primary = 0xeeeeef, surface = 0x202127;
  private final Path path =
      FabricLoader.getInstance().getConfigDir().resolve("gamblingmenu-profit.json");
  private record Saved(int schema, ProfitLedger.Totals lifetime, boolean enabled, boolean showHud,
      boolean customColour, boolean[] rows, float scale, float x, float y, int opacity, int primary,
      int surface) {}
  private CompletableFuture<Void> write = CompletableFuture.completedFuture(null);
  private boolean dirty, savingAllowed = true, dragging, resizing;
  private long saveAt;
  private float grabX, grabY, startX, startScale, appear;
  private long frame = System.nanoTime();
  public void toggle() {
    enabled = !enabled;
    ledger.connectionChanged();
    changed();
  }
  public void changed() {
    if (!dirty)
      saveAt = System.nanoTime() + 1_000_000_000L;
    dirty = true;
  }
  public void resetSession() {
    ledger.resetSession();
  }
  public void connectionChanged() {
    ledger.connectionChanged();
    dragging = resizing = false;
  }
  public void intent(String player, BigDecimal amount, ProfitLedger.Kind kind) {
    if (enabled)
      ledger.intent(player, amount, kind, System.nanoTime());
  }
  public void accept(String text) {
    var mc = Minecraft.getInstance();
    if (!enabled || mc.player == null || mc.getConnection() == null)
      return;
    var incoming = PaymentRules.receipt(text);
    boolean out = incoming.isEmpty();
    var parsed = out ? PaymentRules.outgoing(text) : incoming;
    if (parsed.isEmpty())
      return;
    var r = parsed.get();
    if (r.player().equalsIgnoreCase(mc.getUser().getName()))
      return;
    boolean refund = !out && PaymentModule.INSTANCE.willRefund(r);
    boolean donation = !out && !refund && PaymentModule.INSTANCE.isDonation(r);
    if (ledger.accept(out, r.player(), r.amount(), donation, refund, System.nanoTime()))
      changed();
  }
  public void initialize() {
    try {
      if (Files.isRegularFile(path)) {
        if (Files.size(path) > 16384)
          throw new IllegalArgumentException();
        var s = new Gson().fromJson(Files.readString(path), Saved.class);
        if (s.schema() != 1 || s.rows() == null || s.rows().length != 4
            || !Float.isFinite(s.scale()) || !Float.isFinite(s.x()) || !Float.isFinite(s.y()))
          throw new IllegalArgumentException();
        ledger.restore(s.lifetime());
        enabled = s.enabled();
        showHud = s.showHud();
        customColour = s.customColour();
        rows = s.rows();
        scale = Math.clamp(s.scale(), .6f, 2f);
        positionX = Math.clamp(s.x(), 0, 1);
        positionY = Math.clamp(s.y(), 0, 1);
        opacity = Math.clamp(s.opacity(), 20, 100);
        primary = s.primary() & 0xffffff;
        surface = s.surface() & 0xffffff;
      }
    } catch (Exception e) {
      savingAllowed = false;
      org.slf4j.LoggerFactory.getLogger("gamblingmenu")
          .warn("Profit data unreadable; preserving original file", e);
    }
    ClientTickEvents.END_CLIENT_TICK.register(mc -> {
      if (dirty && System.nanoTime() >= saveAt)
        save();
    });
    ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
      try {
        write.get(3, java.util.concurrent.TimeUnit.SECONDS);
        if (dirty)
          save();
        write.get(3, java.util.concurrent.TimeUnit.SECONDS);
      } catch (Exception ignored) {
      }
    });
    HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
        Identifier.fromNamespaceAndPath("gamblingmenu", "profit"), (g, t) -> {
          if (Minecraft.getInstance().gui.screen() == null)
            extractRenderState(g, false);
        });
  }
  private void save() {
    if (!savingAllowed || !write.isDone())
      return;
    dirty = false;
    String json = new Gson().toJson(new Saved(1, ledger.lifetime(), enabled, showHud, customColour,
        rows.clone(), scale, positionX, positionY, opacity, primary, surface));
    write = CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), "profit-", ".tmp");
        try {
          Files.writeString(tmp, json);
          try {
            Files.move(
                tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
          } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
          }
        } finally {
          Files.deleteIfExists(tmp);
        }
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger("gamblingmenu").warn("Cannot save profit statistics", e);
        Minecraft.getInstance().execute(() -> {
          dirty = true;
          saveAt = System.nanoTime() + 5_000_000_000L;
        });
      }
    });
  }
  public ProfitLedger.Totals totals() {
    return lifetimeView ? ledger.lifetime() : ledger.session();
  }
  private int height() {
    int n = 0;
    for (boolean r : rows)
      if (r)
        n++;
    return 47 + n * 17;
  }
  private float fittedScale(int w, int h) {
    var mc = Minecraft.getInstance();
    float physical = Math.min(Math.clamp(mc.getWindow().getWidth() / 1280f, .9f, 2f),
        Math.min(mc.getWindow().getWidth() / 640f, mc.getWindow().getHeight() / 480f));
    return Math.min(scale * physical / (float) mc.getWindow().getGuiScale(),
        Math.min(w / 220f, h / (float) height()));
  }
  public float left(int w, int h) {
    return positionX * Math.max(0, w - 220 * fittedScale(w, h));
  }
  public float top(int w, int h) {
    return positionY * Math.max(0, h - height() * fittedScale(w, h));
  }
  public boolean click(double mx, double my, int button, int w, int h) {
    if (button != 0)
      return false;
    float s = fittedScale(w, h), x = left(w, h), y = top(w, h);
    if (mx < x || mx > x + 220 * s || my < y || my > y + height() * s)
      return false;
    resizing = mx > x + 204 * s && my > y + (height() - 16) * s;
    dragging = !resizing;
    grabX = (float) mx - x;
    grabY = (float) my - y;
    startX = (float) mx;
    startScale = scale;
    return true;
  }
  public boolean drag(double mx, double my, int w, int h) {
    if (!dragging && !resizing)
      return false;
    if (resizing)
      scale = Math.clamp(startScale + ((float) mx - startX) / 220f, .6f, 2f);
    else {
      float s = fittedScale(w, h);
      positionX = Math.clamp(((float) mx - grabX) / Math.max(1, w - 220 * s), 0, 1);
      positionY = Math.clamp(((float) my - grabY) / Math.max(1, h - height() * s), 0, 1);
    }
    return true;
  }
  public boolean release() {
    boolean active = dragging || resizing;
    if (active) {
      dragging = resizing = false;
      changed();
    }
    return active;
  }
  public void extractRenderState(GuiGraphicsExtractor g, boolean editing) {
    if ((!showHud && !editing) || Minecraft.getInstance().player == null)
      return;
    long now = System.nanoTime();
    float dt = Math.min(.1f, (now - frame) / 1e9f);
    frame = now;
    appear += ((enabled || editing ? 1f : 0f) - appear)
        * (GuiPreferences.animations ? 1 - (float) Math.exp(-12 * dt) : 1);
    if (appear < .01f)
      return;
    float s = fittedScale(g.guiWidth(), g.guiHeight());
    g.pose().pushMatrix();
    g.pose().translate(
        left(g.guiWidth(), g.guiHeight()), top(g.guiWidth(), g.guiHeight()) + 4 * (1 - appear));
    g.pose().scale(s, s);
    int h = height(), alpha = Math.max(1, Math.round(opacity * 2.55f * appear));
    int colour = customColour ? primary : GuiPreferences.accent;
    int bg = customColour ? surface : GuiPreferences.surface;
    Glass.shadow(g, 0, 0, 220, h, 11);
    MenuScreen.round(g, 0, 0, 220, h, 11, (alpha << 24) | bg);
    MenuScreen.round(g, 10, 11, 3, 14, 1, 0xff000000 | colour);
    Inter.draw(g, lifetimeView ? "GAMBLING LIFETIME" : "GAMBLING SESSION", 20, 13,
        0xff000000 | colour, Inter.Weight.SEMIBOLD);
    var t = totals();
    String[] labels = {"Received", "Paid Out", "Profit", "Gambles"};
    String[] values = {"$" + money(t.received()), "$" + money(t.paid()),
        (t.profit().signum() < 0 ? "−$" : "+$") + money(t.profit().abs()),
        Long.toString(t.gambles())};
    int y = 36;
    for (int i = 0; i < 4; i++)
      if (rows[i]) {
        Inter.draw(g, labels[i], 12, y, 0xffb8bbc7, Inter.Weight.REGULAR);
        int rgb = i == 2 ? (t.profit().signum() < 0 ? 0xff707c : 0x65df89) : colour;
        String value = Inter.editingFont().plainSubstrByWidth(values[i], 132);
        Inter.draw(g, value, 208 - Inter.editingFont().width(value), y, 0xff000000 | rgb,
            Inter.Weight.MEDIUM);
        y += 17;
      }
    if (editing) {
      g.fill(208, h - 7, 215, h - 6, 0xffaaaaaa);
      g.fill(212, h - 11, 215, h - 10, 0xffaaaaaa);
    }
    g.pose().popMatrix();
  }
  public static String money(BigDecimal n) {
    return dev.gamblingmenu.auction.AuctionModule.amount(n);
  }
}
