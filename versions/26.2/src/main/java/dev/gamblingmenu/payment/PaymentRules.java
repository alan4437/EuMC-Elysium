package dev.gamblingmenu.payment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Exact decimal amounts and anchored server receipt parsing; parses incoming and outgoing server
 * confirmations separately.
 */
public final class PaymentRules {
  public enum Mode { DONATION, PAY_BACK }
  public record Receipt(String player, BigDecimal amount, String displayAmount) {}
  private static final Pattern AMOUNT =
      Pattern.compile("(?i)([0-9]{1,16}(?:\\.[0-9]{1,2})?)([KMBT]?)");
  private static final String PREFIX =
      "(?i)^\\[?ECONOMY\\]?\\s*(?:»|➜|➤|→|›|:|\\||➡|➔|➞|➠|➟|➢|➣)\\s*";
  private static final String NUMBER = "\\$?(?<amount>[0-9]{1,16}(?:\\.[0-9]{1,2})?[KMBT]?)";
  private static final String PLAYER =
      "(?:[^A-Za-z0-9_\\s]{1,3}\\s+)?(?<sender>[A-Za-z0-9_]{3,16})";
  private static final Pattern RECEIVED =
      Pattern.compile(PREFIX + "You received " + NUMBER + " from " + PLAYER + "[.!]?$");
  private static final Pattern RECEIPT =
      Pattern.compile(PREFIX + PLAYER + " paid you " + NUMBER + "[.!]?$");
  private static final Pattern SENT =
      Pattern.compile(PREFIX + "You paid " + NUMBER + " to " + PLAYER + "[.!]?$");
  public static Optional<Receipt> outgoing(String text) {
    if (text.length() > 200)
      return Optional.empty();
    var match = SENT.matcher(text.replace('\u00a0', ' ').strip());
    if (!match.matches())
      return Optional.empty();
    try {
      var value = amount(match.group("amount"));
      return value.signum() > 0
          ? Optional.of(new Receipt(match.group("sender"), value, match.group("amount")))
          : Optional.empty();
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
  public static BigDecimal amount(String text) {
    var match = AMOUNT.matcher(text.strip());
    if (!match.matches())
      throw new IllegalArgumentException("Use an amount like 1, 1.00, 10K or 1B.");
    int power = switch (match.group(2).toUpperCase(java.util.Locale.ROOT)) {
      case "K" -> 3;
      case "M" -> 6;
      case "B" -> 9;
      case "T" -> 12;
      default -> 0;
    };
    var value = new BigDecimal(match.group(1)).scaleByPowerOfTen(power).stripTrailingZeros();
    if (value.compareTo(new BigDecimal("1000000000000000")) > 0)
      throw new IllegalArgumentException("Amount is too large.");
    return value;
  }
  public static Optional<Receipt> receipt(String text) {
    if (text.length() > 200)
      return Optional.empty();
    String normalized = text.replace('\u00a0', ' ').strip();
    var match = RECEIVED.matcher(normalized);
    if (!match.matches()) {
      match = RECEIPT.matcher(normalized);
      if (!match.matches())
        return Optional.empty();
    }
    try {
      var amount = amount(match.group("amount"));
      if (amount.signum() <= 0)
        return Optional.empty();
      return Optional.of(new Receipt(match.group("sender"), amount, match.group("amount")));
    } catch (IllegalArgumentException error) {
      return Optional.empty();
    }
  }
  public static boolean outside(BigDecimal amount, BigDecimal min, BigDecimal max) {
    return amount.compareTo(min) < 0 || amount.compareTo(max) > 0;
  }
  public static String refundCommand(Receipt receipt) {
    if (!receipt.player().matches("[A-Za-z0-9_]{3,16}"))
      throw new IllegalArgumentException("Invalid username");
    return "pay " + receipt.player() + " " + receipt.amount().toPlainString();
  }
  private PaymentRules() {}
}
