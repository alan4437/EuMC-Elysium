package dev.gamblingmenu.payment;

import com.google.gson.Gson;
import dev.discordlink.gui.Inter;
import dev.discordlink.gui.MenuScreen;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

/**
 * No external networking. Acts only on strictly matched non-overlay server receipts while enabled.
 */
public final class PaymentModule {
  public static final PaymentModule INSTANCE = new PaymentModule();
  private record Settings(String min, String max, String mode) {}
  private record
      Refund(PaymentRules.Receipt receipt, Object connection, long queued, boolean blacklist) {}
  private final Path path =
      FabricLoader.getInstance().getConfigDir().resolve("gamblingmenu-payments.json");
  private final ArrayDeque<Refund> refunds = new ArrayDeque<>();
  private BigDecimal min = BigDecimal.ONE, max = new BigDecimal("1000000000");
  private PaymentRules.Mode mode = PaymentRules.Mode.DONATION;
  private boolean enabled;
  private long nextRefund, notificationUntil;
  private String notification = "", notificationAmount = "", status = "Disabled";
  private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
  private PaymentModule() {}
  public boolean enabled() {
    return enabled;
  }
  public String min() {
    return min.toPlainString();
  }
  public String max() {
    return max.toPlainString();
  }
  public PaymentRules.Mode mode() {
    return mode;
  }
  public String status() {
    return status;
  }
  public void toggle() {
    enabled = !enabled;
    refunds.removeIf(refund -> !refund.blacklist());
    status = enabled ? "Listening for server payment receipts" : "Disabled";
  }
  public String apply(String low, String high, PaymentRules.Mode selected) {
    try {
      var minimum = PaymentRules.amount(low);
      var maximum = PaymentRules.amount(high);
      if (minimum.signum() < 0 || maximum.signum() <= 0 || minimum.compareTo(maximum) > 0)
        return "Minimum must be at most maximum; maximum must be positive.";
      if (min.compareTo(minimum) == 0 && max.compareTo(maximum) == 0 && mode == selected)
        return "Settings applied";
      min = minimum;
      max = maximum;
      mode = selected;
      refunds.removeIf(refund -> !refund.blacklist());
      status = enabled ? "Settings applied; listening for receipts"
                       : "Settings applied; module disabled";
      var snapshot = new Settings(min(), max(), mode.name());
      writes = writes.handle((v, e) -> null).thenRunAsync(() -> {
        try {
          Files.createDirectories(path.getParent());
          var temporary = Files.createTempFile(path.getParent(), "payments-", ".tmp");
          try {
            Files.writeString(temporary, new Gson().toJson(snapshot));
            try {
              Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
              Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
          } finally {
            Files.deleteIfExists(temporary);
          }
        } catch (Exception e) {
          Minecraft.getInstance().execute(() -> status = "Settings could not be saved to disk");
        }
      });
      return "Settings applied";
    } catch (IllegalArgumentException error) {
      return error.getMessage();
    }
  }
  public void initialize() {
    dev.gamblingmenu.profit.ProfitTracker.INSTANCE.initialize();
    dev.gamblingmenu.auction.AuctionModule.INSTANCE.initialize();
    dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.initialize();
    dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.initialize();
    try {
      if (Files.isRegularFile(path) && Files.size(path) < 4096) {
        var settings = new Gson().fromJson(Files.readString(path), Settings.class);
        var low = PaymentRules.amount(settings.min());
        var high = PaymentRules.amount(settings.max());
        var selected = PaymentRules.Mode.valueOf(settings.mode());
        if (low.compareTo(high) <= 0 && high.signum() > 0) {
          min = low;
          max = high;
          mode = selected;
        }
      }
    } catch (Exception ignored) {
      status = "Invalid saved settings; using defaults";
    }
    ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
      if (!overlay)
        acceptServerReceipt(dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.original(message));
    });
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> reset());
    ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
    HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
        Identifier.fromNamespaceAndPath("gamblingmenu", "donation"), (g, timer) -> {
          if (Minecraft.getInstance().gui.screen() == null)
            renderNotification(g);
        });
    net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register(
        (mc, screen, width, height)
            -> net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(screen).register(
                (current, g, mx, my, delta) -> {
                  if (!(current instanceof MenuScreen) && !(current instanceof dev.discordlink.gui.HudEditorScreen)) renderNotification(g);
                }));
  }
  public void renderNotification(net.minecraft.client.gui.GuiGraphicsExtractor g) {
    if (System.nanoTime() > notificationUntil || notification.isEmpty())
      return;
    var mc = Minecraft.getInstance();
    var title = Inter.text(notification, Inter.Weight.SEMIBOLD);
    var amount = Inter.text(notificationAmount + " DONATED", Inter.Weight.MEDIUM);
    float physical = mc.getWindow().getWidth() / 1920f;
    float gui = (float) mc.getWindow().getGuiScale();
    float titleScale =
        Math.min(6f * physical / gui, g.guiWidth() * .86f / Math.max(1, mc.font.width(title)));
    float amountScale =
        Math.min(2.7f * physical / gui, g.guiWidth() * .8f / Math.max(1, mc.font.width(amount)));
    float gap = 14f * physical / gui;
    float blockHeight = 11 * titleScale + gap + 11 * amountScale;
    float top = g.guiHeight() * .49f - blockHeight / 2;
    long remaining = notificationUntil - System.nanoTime();
    float fade = Math.clamp(
        Math.min((5_000_000_000L - remaining) / 180_000_000f, remaining / 350_000_000f), 0, 1);
    int alpha = Math.max(4, Math.round(255 * fade)) << 24;
    g.pose().pushMatrix();
    g.pose().translate(g.guiWidth() / 2f, top);
    g.pose().scale(titleScale, titleScale);
    g.text(mc.font, title, -mc.font.width(title) / 2, 0, alpha | 0x55ff55, true);
    g.pose().popMatrix();
    g.pose().pushMatrix();
    g.pose().translate(g.guiWidth() / 2f, top + 11 * titleScale + gap);
    g.pose().scale(amountScale, amountScale);
    g.text(mc.font, amount, -mc.font.width(amount) / 2, 0, alpha | 0xffffff, true);
    g.pose().popMatrix();
  }
  public void reset() {
    enabled = false;
    dev.gamblingmenu.profit.ProfitTracker.INSTANCE.connectionChanged();
    dev.gamblingmenu.auction.AuctionModule.INSTANCE.reset();
    dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.reset();
    dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.disable();
    refunds.clear();
    notification = "";
    notificationAmount = "";
    notificationUntil = 0;
    nextRefund = 0;
    status = "Disabled — enable for this session";
  }
  /** Public for deterministic tests; production calls this only from GAME, never player CHAT. */
  public void acceptServerReceipt(String text) {
    dev.gamblingmenu.profit.ProfitTracker.INSTANCE.accept(text);
    dev.gamblingmenu.auction.AuctionModule.INSTANCE.accept(text);
    var mc = Minecraft.getInstance();
    if ((!enabled && !dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.enabled())
        || mc.player == null || mc.getConnection() == null)
      return;
    PaymentRules.receipt(text).ifPresent(receipt -> {
      if (receipt.player().equalsIgnoreCase(mc.getUser().getName()))
        return;
      var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
      if (blacklist.enabled() && blacklist.contains(receipt.player())) {
        queueRefund(receipt, mc, true);
        return;
      }
      if (!enabled)
        return;
      if (mode == PaymentRules.Mode.DONATION && PaymentRules.outside(receipt.amount(), min, max)) {
        notification = "NEW DONATION FROM " + receipt.player();
        notificationAmount = receipt.displayAmount();
        notificationUntil = System.nanoTime() + 5_000_000_000L;
        mc.player.playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.15f);
        status = "Donation from " + receipt.player() + ": " + receipt.displayAmount();
      } else if (PaymentRules.outside(receipt.amount(), min, max)) {
        queueRefund(receipt, mc, false);
      }
    });
  }
  public boolean isDonation(PaymentRules.Receipt r) {
    return enabled && mode == PaymentRules.Mode.DONATION && PaymentRules.outside(r.amount(), min, max);
  }
  public boolean willRefund(PaymentRules.Receipt receipt) {
    var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
    return (blacklist.enabled() && blacklist.contains(receipt.player()))
        || (enabled && mode == PaymentRules.Mode.PAY_BACK && PaymentRules.outside(receipt.amount(), min, max));
  }
  public void cancelBlacklistRefunds() {
    refunds.removeIf(Refund::blacklist);
  }
  private void queueRefund(PaymentRules.Receipt receipt, Minecraft mc, boolean blacklist) {
    if (refunds.size() >= 32) {
      enabled = false;
      dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.disable();
      refunds.clear();
      status = "Refund queue full; refund modules disabled";
      return;
    }
    refunds.addLast(new Refund(receipt, mc.getConnection(), System.nanoTime(), blacklist));
    status = "Queued refund to " + receipt.player();
  }
  private void tick(Minecraft mc) {
    if (refunds.isEmpty() || mc.getConnection() == null)
      return;
    long now = System.nanoTime();
    if (now < nextRefund)
      return;
    var refund = refunds.removeFirst();
    if (refund.connection() != mc.getConnection() || now - refund.queued() > 60_000_000_000L)
      return;
    if (refund.blacklist()) {
      var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
      if (!blacklist.enabled() || !blacklist.contains(refund.receipt().player()))
        return;
    } else if (!enabled)
      return;
    dev.gamblingmenu.profit.ProfitTracker.INSTANCE.intent(refund.receipt().player(),
        refund.receipt().amount(), dev.gamblingmenu.profit.ProfitLedger.Kind.REFUND);
    mc.getConnection().sendCommand(PaymentRules.refundCommand(refund.receipt()));
    nextRefund = now + 1_500_000_000L;
    status = "Refund command sent to " + refund.receipt().player() + " (not confirmed)";
  }
}
