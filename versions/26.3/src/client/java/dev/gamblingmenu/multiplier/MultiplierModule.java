package dev.gamblingmenu.multiplier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;

import com.google.gson.Gson;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.gamblingmenu.payment.PaymentModule;
import dev.gamblingmenu.payment.PaymentRules;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;

/** Session-bound, one-use payout choices created only from server payment receipts. */
public final class MultiplierModule {
  public static final MultiplierModule INSTANCE = new MultiplierModule();
  private record Offer(String player, List<BigDecimal> totals, Object connection, long expires) {}
  private final LinkedHashMap<String, Offer> offers = new LinkedHashMap<>();
  private final Map<Component, String> originals = new IdentityHashMap<>();
  private final Path path =
      FabricLoader.getInstance().getConfigDir().resolve("gamblingmenu-multipliers.json");
  private String[] slots = {"2x", "4x", ""};
  private boolean enabled;
  private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
  public boolean enabled() {
    return enabled;
  }
  public String slot(int i) {
    return slots[i];
  }
  public void toggle() {
    enabled = !enabled;
    offers.clear();
  }
  public void reset() {
    enabled = false;
    offers.clear();
    originals.clear();
  }
  private static BigDecimal factor(String text) {
    String value = text.strip().toLowerCase(Locale.ROOT);
    if (value.endsWith("x"))
      value = value.substring(0, value.length() - 1);
    if (!value.matches("[0-9]{1,4}(?:\\.[0-9]{1,2})?"))
      throw new IllegalArgumentException();
    BigDecimal n = new BigDecimal(value).stripTrailingZeros();
    if (n.signum() <= 0 || n.compareTo(new BigDecimal("1000")) > 0)
      throw new IllegalArgumentException();
    return n;
  }
  public boolean setSlot(int i, String value) {
    if (!value.isBlank())
      try {
        factor(value);
      } catch (IllegalArgumentException e) {
        return false;
      }
    slots[i] = value.strip();
    String json = new Gson().toJson(slots);
    writes = writes.handle((v, e) -> null).thenRunAsync(() -> {
      try {
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), "multipliers-", ".tmp");
        try {
          Files.writeString(tmp, json);
          Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          Files.deleteIfExists(tmp);
        }
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger("gamblingmenu").warn("Cannot save multipliers", e);
      }
    });
    return true;
  }
  public void initialize() {
    try {
      if (Files.isRegularFile(path) && Files.size(path) < 1024) {
        String[] saved = new Gson().fromJson(Files.readString(path), String[].class);
        if (saved != null && saved.length == 3) {
          for (String s : saved)
            if (s == null)
              throw new IllegalArgumentException();
            else if (!s.isBlank())
              factor(s);
          slots = saved;
        }
      }
    } catch (Exception ignored) {
    }
    ClientReceiveMessageEvents.MODIFY_GAME.register(
        (message, overlay) -> overlay ? message : decorate(message));
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, access)
            -> dispatcher.register(literal("gamblepayout")
                    .then(argument("receipt", StringArgumentType.word())
                            .then(argument("slot", IntegerArgumentType.integer(0, 2))
                                    .executes(ctx -> {
                                      redeem(StringArgumentType.getString(ctx, "receipt"),
                                          IntegerArgumentType.getInteger(ctx, "slot"));
                                      return 1;
                                    })))));
  }
  public String original(Component message) {
    String text = originals.remove(message);
    return text == null ? message.getString() : text;
  }
  public Component decorate(Component message) {
    var mc = Minecraft.getInstance();
    if (!enabled || mc.player == null || mc.getConnection() == null)
      return message;
    var receipt = PaymentRules.receipt(message.getString());
    if (receipt.isEmpty() || receipt.get().player().equalsIgnoreCase(mc.getUser().getName())
        || PaymentModule.INSTANCE.willRefund(receipt.get()))
      return message;
    var r = receipt.get();
    List<BigDecimal> totals = new ArrayList<>();
    var text = message.copy();
    String token = UUID.randomUUID().toString();
    int count = 0;
    for (int i = 0; i < 3; i++) {
      BigDecimal total = null;
      if (!slots[i].isBlank()) {
        total = r.amount().multiply(factor(slots[i])).stripTrailingZeros();
        if (total.scale() > 2 || total.compareTo(new BigDecimal("1000000000000000")) > 0)
          total = null;
      }
      totals.add(total);
      if (total == null)
        continue;
      int tagColour = switch (i) { case 0 -> 0xfcb500; case 1 -> 0x97e35e; default -> 0x00bdf8; };
      String label = "[" + factor(slots[i]).toPlainString() + "x]";
      String command = "/gamblepayout " + token + " " + i;
      Component hint = Component.literal(
          "Pay " + r.player() + " " + total.toPlainString() + " (one payout per payment)");
      text.append(Component.literal(" " + label)
              .withStyle(style
                  -> style.withColor(tagColour)
                      .withClickEvent(new ClickEvent.RunCommand(command))
                      .withHoverEvent(new HoverEvent.ShowText(hint))));
      count++;
    }
    if (count == 0)
      return message;
    long now = System.nanoTime();
    offers.values().removeIf(o -> o.expires() < now);
    if (offers.size() >= 100)
      offers.remove(offers.keySet().iterator().next());
    offers.put(token,
        new Offer(r.player(), Collections.unmodifiableList(totals), mc.getConnection(),
            now + 300_000_000_000L));
    if (originals.size() >= 128)
      originals.clear();
    originals.put(text, message.getString());
    return text;
  }
  public boolean redeem(String token, int slot) {
    var mc = Minecraft.getInstance();
    var offer = offers.get(token);
    if (!enabled || offer == null || offer.connection() != mc.getConnection()
        || offer.expires() < System.nanoTime() || slot < 0 || slot > 2
        || offer.totals().get(slot) == null) {
      mc.gui.hud.getChat().addClientSystemMessage(
          Component.literal("Payout unavailable: already used, expired, or module disabled.")
              .withColor(0xff9999));
      return false;
    }
    offers.remove(token);
    dev.gamblingmenu.profit.ProfitTracker.INSTANCE.intent(offer.player(), offer.totals().get(slot),
        dev.gamblingmenu.profit.ProfitLedger.Kind.MULTIPLIER);
    mc.getConnection().sendCommand(
        "pay " + offer.player() + " " + offer.totals().get(slot).toPlainString());
    return true;
  }
}
