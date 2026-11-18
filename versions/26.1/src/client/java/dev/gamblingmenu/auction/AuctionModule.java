package dev.gamblingmenu.auction;
import com.google.gson.Gson;
import dev.discordlink.gui.*;
import dev.gamblingmenu.payment.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
public final class AuctionModule {
  public static final AuctionModule INSTANCE = new AuctionModule();
  private record Settings(String minimum, int seconds, int primary, int secondary, float hudScale,
      float hudX, float hudY, int hudOpacity, boolean hudHidden) {}
  public float hudScale = 1, hudX = .02f, hudY = .04f;
  public int hudOpacity = 90;
  public boolean hudHidden;
  public final AuctionLedger ledger = new AuctionLedger();
  private String minimum = "10k";
  private int seconds = 60, primary = 0xfcb500, secondary = 0x97e35e, slot = -1;
  private ItemStack item = ItemStack.EMPTY;
  private boolean shown, announced;
  private long celebration;
  private static final float CONFETTI_SECONDS = 6f;
  private static final float RESULTS_SECONDS = CONFETTI_SECONDS + 5f;
  private static final int[] CONFETTI_COLOURS = {
      0xff5555, 0xffb347, 0xffff55, 0x55ff88, 0x55ccff, 0xa779ff, 0xff77cc};
  private List<AuctionLedger.Bid> top = List.of();
  private String status = "Choose an item to begin";
  private final Path path =
      FabricLoader.getInstance().getConfigDir().resolve("gamblingmenu-auction.json");
  private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
  public String minimum() {
    return minimum;
  }
  public int seconds() {
    return seconds;
  }
  public int primary() {
    return primary;
  }
  public int secondary() {
    return secondary;
  }
  public ItemStack item() {
    return item.copy();
  }
  public String status() {
    return status;
  }
  public boolean enabled() {
    return ledger.running(System.nanoTime());
  }
  public boolean shown() {
    return shown;
  }
  public List<AuctionLedger.Bid> top() {
    return top;
  }
  public void colour(boolean first, int rgb) {
    if (first)
      primary = rgb;
    else
      secondary = rgb;
  }
  public boolean minimum(String text) {
    if (enabled())
      return false;
    try {
      if (PaymentRules.amount(text).signum() <= 0)
        return false;
      minimum = text.strip();
      save();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  public boolean duration(String text) {
    if (enabled())
      return false;
    try {
      int n = Integer.parseInt(text.strip());
      if (n < 1 || n > 86400)
        return false;
      seconds = n;
      save();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  public void select(int index) {
    var mc = Minecraft.getInstance();
    if (enabled() || mc.player == null || index < 0 || index >= 36)
      return;
    var stack = mc.player.getInventory().getItem(index);
    if (stack.isEmpty())
      return;
    slot = index;
    item = stack.copy();
    status = "Ready to start";
  }
  public boolean start() {
    var mc = Minecraft.getInstance();
    if (enabled())
      return false;
    if (mc.player == null || mc.getConnection() == null || slot < 0 || item.isEmpty()) {
      status = "Choose an inventory item first";
      return false;
    }
    var live = mc.player.getInventory().getItem(slot);
    if (!ItemStack.isSameItemSameComponents(live, item) || live.getCount() < item.getCount()) {
      status = "Selected item changed — choose it again";
      return false;
    }
    ledger.start(PaymentRules.amount(minimum), seconds * 1_000_000_000L, System.nanoTime());
    top = List.of();
    shown = true;
    announced = false;
    celebration = 0;
    status = "Auction running";
    return true;
  }
  public void toggle() {
    if (enabled()) {
      ledger.cancel();
      shown = false;
      status = "Auction cancelled";
    } else if (!start())
      Minecraft.getInstance().gui.getChat().addClientSystemMessage(
          Component.literal("Auction: " + status).withColor(0xffcc66));
  }
  public void hide() {
    if (enabled())
      ledger.cancel();
    shown = false;
    status = "Leaderboard hidden";
  }
  public void reset() {
    ledger.clear();
    top = List.of();
    shown = false;
    announced = false;
    celebration = 0;
    slot = -1;
    item = ItemStack.EMPTY;
    status = "Choose an item to begin";
  }
  public void accept(String text) {
    if (!enabled())
      return;
    var mc = Minecraft.getInstance();
    PaymentRules.receipt(text).ifPresent(receipt -> {
      if (receipt.player().equalsIgnoreCase(mc.getUser().getName())
          || PaymentModule.INSTANCE.willRefund(receipt))
        return;
      if (ledger.accept(receipt.player(), receipt.amount(), System.nanoTime()))
        top = ledger.top();
    });
  }
  public void save() {
    String json = new Gson().toJson(new Settings(
        minimum, seconds, primary, secondary, hudScale, hudX, hudY, hudOpacity, hudHidden));
    writes = writes.handle((v, e) -> null).thenRunAsync(() -> {
      try {
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), "auction-", ".tmp");
        try {
          Files.writeString(tmp, json);
          Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          Files.deleteIfExists(tmp);
        }
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger("gamblingmenu").warn("Cannot save auction settings", e);
      }
    });
  }
  public void initialize() {
    try {
      if (Files.isRegularFile(path) && Files.size(path) < 4096) {
        var s = new Gson().fromJson(Files.readString(path), Settings.class);
        if (PaymentRules.amount(s.minimum()).signum() > 0 && s.seconds() > 0
            && s.seconds() <= 86400) {
          minimum = s.minimum();
          seconds = s.seconds();
          primary = s.primary() & 0xffffff;
          secondary = s.secondary() & 0xffffff;
          if (Float.isFinite(s.hudScale()) && s.hudScale() >= .5f) {
            hudScale = Math.clamp(s.hudScale(), .5f, 2f);
            hudX = Float.isFinite(s.hudX()) ? Math.clamp(s.hudX(), 0, 1) : .02f;
            hudY = Float.isFinite(s.hudY()) ? Math.clamp(s.hudY(), 0, 1) : .04f;
            hudOpacity = Math.clamp(s.hudOpacity(), 20, 100);
            hudHidden = s.hudHidden();
          }
        }
      }
    } catch (Exception ignored) {
    }
    ClientTickEvents.END_CLIENT_TICK.register(mc -> {
      if (shown && !announced && ledger.finished(System.nanoTime())) {
        announced = true;
        status = top.isEmpty()
            ? "Ended — no qualifying bids"
            : "Winner: " + top.getFirst().player() + " — " + amount(top.getFirst().amount());
        mc.gui.getChat().addClientSystemMessage(Component.literal("Auction " + status).withColor(primary));
        celebration = System.nanoTime();
        if (!top.isEmpty()) {
          if (mc.player != null)
            mc.player.playSound(net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, .7f, 1.1f);
        }
      }
      if (shown && announced && celebration != 0
          && (System.nanoTime() - celebration) / 1e9 >= RESULTS_SECONDS) {
        shown = false;
      }
    });
    HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
        Identifier.fromNamespaceAndPath("gamblingmenu", "auction"), (g, t) -> {
          if (Minecraft.getInstance().screen == null)
            extractRenderState(g);
        });
    net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register(
        (mc, screen, w, h)
            -> net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(screen).register(
                (s, g, mx, my, d) -> {
                  if (!(s instanceof MenuScreen) && !(s instanceof HudEditorScreen))
                    extractRenderState(g);
                }));
  }
  public static String amount(BigDecimal n) {
    String[] suffix = {"T", "B", "M", "K"};
    int[] powers = {12, 9, 6, 3};
    for (int i = 0; i < 4; i++)
      if (n.compareTo(BigDecimal.TEN.pow(powers[i])) >= 0)
        return n.scaleByPowerOfTen(-powers[i])
                   .setScale(2, java.math.RoundingMode.DOWN)
                   .stripTrailingZeros()
                   .toPlainString()
            + suffix[i];
    return n.stripTrailingZeros().toPlainString();
  }
  public void extractRenderState(GuiGraphicsExtractor g) {
    if (!shown)
      return;
    if (Minecraft.getInstance().screen instanceof HudEditorScreen)
      return;
    if (hudHidden) {
      renderWinner(g);
      return;
    }
    renderBoard(g, false);
    renderWinner(g);
  }
  public float hudRenderScale(int width, int height) {
    return Math.min(1.25f * hudScale, Math.min(width / 230f, height / 174f));
  }
  public float hudLeft(int width, int height) {
    return hudX * Math.max(0, width - 230 * hudRenderScale(width, height));
  }
  public float hudTop(int width, int height) {
    return hudY * Math.max(0, height - 174 * hudRenderScale(width, height));
  }
  public void renderBoard(GuiGraphicsExtractor g, boolean preview) {
    // Layout preferences stay in the viewport at every Minecraft GUI scale.
    float scale = hudRenderScale(g.guiWidth(), g.guiHeight());
    g.pose().pushMatrix();
    g.pose().translate(hudX * Math.max(0, g.guiWidth() - 230 * scale),
        hudY * Math.max(0, g.guiHeight() - 174 * scale));
    g.pose().scale(scale, scale);
    int x = 0, y = 0, w = 230;
    MenuScreen.round(g, x, y, w, 174, 12, (Math.round(hudOpacity * 2.55f) << 24) | 0x101117);
    MenuScreen.round(g, x + 12, y + 12, 3, 18, 1, 0xff000000 | primary);
    Inter.draw(g, "AUCTION", x + 24, y + 14, 0xff000000 | primary, Inter.Weight.SEMIBOLD);
    String timer = preview ? "PREVIEW"
        : enabled() ? ((ledger.remaining(System.nanoTime()) + 999_999_999L) / 1_000_000_000L) + "s"
                    : "ENDED";
    Inter.draw(g, timer, x + w - 63, y + 14, 0xff000000 | secondary, Inter.Weight.MEDIUM);
    g.item(item, x + 13, y + 38);
    String title = Inter.editingFont().plainSubstrByWidth(item.isEmpty()
            ? "Choose an auction item"
            : item.getHoverName().getString() + " ×" + item.getCount(),
        w - 52);
    Inter.draw(g, title, x + 36, y + 41, 0xffeeeeef, Inter.Weight.MEDIUM);
    Inter.draw(g, "Minimum: " + minimum, x + 14, y + 62, 0xffaaaebb, Inter.Weight.REGULAR);
    for (int i = 0; i < 5; i++) {
      int rowY = y + 82 + i * 16;
      Inter.draw(g, (i + 1) + ".", x + 14, rowY, 0xff000000 | primary, Inter.Weight.SEMIBOLD);
      if (i < top.size()) {
        var bid = top.get(i);
        Inter.draw(g, bid.player(), x + 35, rowY, 0xffeeeeef, Inter.Weight.MEDIUM);
        String value = amount(bid.amount());
        Inter.draw(g, value, x + w - 14 - Inter.editingFont().width(value), rowY,
            0xff000000 | secondary, Inter.Weight.MEDIUM);
      } else
        Inter.draw(g, "—", x + 35, rowY, 0xff656b79, Inter.Weight.REGULAR);
    }
    g.pose().popMatrix();
  }
  private static float smoothFade(float value) {
    float v = Math.clamp(value, 0f, 1f);
    return v * v * (3f - 2f * v);
  }
  private void renderWinner(GuiGraphicsExtractor g) {
    if (celebration == 0 || top.isEmpty())
      return;
    float t = (System.nanoTime() - celebration) / 1e9f;
    if (t >= RESULTS_SECONDS)
      return;
    float scale = Math.min(g.guiWidth() / 520f, Math.min(1.7f, g.guiHeight() / 240f));
    float fade = smoothFade(t / .3f) * smoothFade((RESULTS_SECONDS - t) / .5f);
    int alpha = Math.max(1, Math.round(255 * fade)) << 24;
    float cx = g.guiWidth() / 2f, cy = g.guiHeight() * .38f;
    var winner = top.getFirst();
    g.pose().pushMatrix();
    g.pose().translate(cx, cy);
    g.pose().scale(scale, scale);
    String title = winner.player() + " WON!", price = "Winning bid: " + amount(winner.amount());
    g.pose().pushMatrix();
    g.pose().scale(1.7f, 1.7f);
    Inter.draw(
        g, title, -Inter.editingFont().width(title) / 2, 0, alpha | primary, Inter.Weight.SEMIBOLD);
    g.pose().popMatrix();
    Inter.draw(g, price, -Inter.editingFont().width(price) / 2, 27, alpha | secondary,
        Inter.Weight.MEDIUM);
    if (t >= CONFETTI_SECONDS) {
      g.pose().popMatrix();
      return;
    }
    // Elapsed-time trajectories stay smooth at any extractRenderState rate, with drag and gentle flutter.
    float origin = Inter.editingFont().width(title) * .85f + 12;
    for (int i = 0; i < 100; i++) {
      float age = t - (i / 2 % 10) * .035f;
      float life = 4.3f + (i % 7) * .15f;
      if (age <= 0 || age >= life)
        continue;
      int side = i % 2 == 0 ? -1 : 1;
      float drag = (float) -Math.expm1(-age * .85f) / .85f;
      float phase = i * 2.39996f;
      float flutter = (float) Math.sin(age * 2.1f + phase) - (float) Math.sin(phase);
      float x = side * origin + side * (24 + i * 31 % 65) * drag + flutter * 4f * smoothFade(age);
      float y = 12 - (32 + i * 17 % 58) * drag + 12f * age * age;
      float particleFade = smoothFade(age / .16f) * smoothFade((life - age) / 1.3f);
      int particleAlpha = Math.round(255 * particleFade * fade);
      if (particleAlpha == 0)
        continue;
      g.pose().pushMatrix();
      g.pose().translate(x, y);
      g.pose().rotate(phase + age * (i % 7 - 3) * .7f + flutter * .15f);
      // Slowly turning paper faces, never snapping to zero width.
      g.pose().scale(.65f + .35f * (float) Math.cos(age * 2f + phase), 1f);
      g.fill(-2, -1, 2, 2, (particleAlpha << 24) | CONFETTI_COLOURS[i % CONFETTI_COLOURS.length]);
      g.pose().popMatrix();
    }
    g.pose().popMatrix();
  }
}
