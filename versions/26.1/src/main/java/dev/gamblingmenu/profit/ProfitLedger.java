package dev.gamblingmenu.profit;

import java.math.BigDecimal;
import java.util.*;

/** Chat-confirmed totals; intents alone never change balances or processed gamble counts. */
public final class ProfitLedger {
  public record Totals(BigDecimal received, BigDecimal paid, BigDecimal refunds,
      BigDecimal donations, long gambles) {
    public static Totals zero() {
      return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
    public BigDecimal profit() {
      return received.subtract(paid.subtract(refunds));
    }
  }
  public enum Kind { OTHER, REFUND, MULTIPLIER }
  private record Intent(String player, BigDecimal amount, Kind kind, long expires) {}
  private final ArrayDeque<Intent> pending = new ArrayDeque<>();
  private final LinkedHashMap<String, Long> recent = new LinkedHashMap<>();
  private Totals session = Totals.zero(), lifetime = Totals.zero();
  public Totals session() {
    return session;
  }
  public Totals lifetime() {
    return lifetime;
  }
  public void restore(Totals totals) {
    Objects.requireNonNull(totals);
    for (var n : List.of(totals.received(), totals.paid(), totals.refunds(), totals.donations()))
      if (n == null || n.signum() < 0 || n.precision() > 128 || n.scale() > 2 || n.scale() < -128)
        throw new IllegalArgumentException("Invalid saved totals");
    if (totals.gambles() < 0 || totals.refunds().compareTo(totals.paid()) > 0)
      throw new IllegalArgumentException("Invalid saved totals");
    lifetime = totals;
  }
  public void resetSession() {
    session = Totals.zero();
  }
  public void connectionChanged() {
    pending.clear();
    recent.clear();
  }
  public void intent(String player, BigDecimal amount, Kind kind, long now) {
    pending.removeIf(p -> p.expires() < now);
    if (pending.size() == 128)
      pending.removeFirst();
    pending.addLast(
        new Intent(player.toLowerCase(Locale.ROOT), amount, kind, now + 60_000_000_000L));
  }
  public boolean accept(boolean outgoing, String player, BigDecimal amount, boolean donation,
      boolean excludedRefund, long now) {
    if (amount.signum() <= 0)
      return false;
    String name = player.toLowerCase(Locale.ROOT);
    String key = outgoing + ":" + name + ":" + amount.stripTrailingZeros().toPlainString();
    recent.entrySet().removeIf(e -> now - e.getValue() >= 2_000_000_000L);
    if (recent.containsKey(key))
      return false;
    if (recent.size() == 512)
      recent.remove(recent.keySet().iterator().next());
    recent.put(key, now);
    Kind kind = Kind.OTHER;
    if (outgoing) {
      pending.removeIf(p -> p.expires() < now);
      var it = pending.iterator();
      while (it.hasNext()) {
        var p = it.next();
        if (p.player().equals(name) && p.amount().compareTo(amount) == 0) {
          kind = p.kind();
          it.remove();
          break;
        }
      }
    }
    session = add(session, outgoing, amount, donation, excludedRefund, kind);
    lifetime = add(lifetime, outgoing, amount, donation, excludedRefund, kind);
    return true;
  }
  private static Totals add(
      Totals t, boolean out, BigDecimal n, boolean donation, boolean excludedRefund, Kind kind) {
    return new Totals((out || donation || excludedRefund) ? t.received() : t.received().add(n),
        out ? t.paid().add(n) : t.paid(), kind == Kind.REFUND ? t.refunds().add(n) : t.refunds(),
        !out && donation ? t.donations().add(n) : t.donations(),
        kind == Kind.MULTIPLIER && t.gambles() < Long.MAX_VALUE ? t.gambles() + 1 : t.gambles());
  }
}
