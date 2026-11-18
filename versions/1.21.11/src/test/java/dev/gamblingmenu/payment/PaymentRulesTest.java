package dev.gamblingmenu.payment;

import java.math.BigDecimal;
public final class PaymentRulesTest {
  private static int checks;
  private static void check(boolean value, String label) {
    checks++;
    if (!value)
      throw new AssertionError(label);
  }
  public static void main(String[] args) {
    for (String[] pair : new String[][] {{"1b", "1000000000"}, {"20k", "20000"},
             {"1t", "1000000000000"}, {"41T", "41000000000000"}, {"1.5m", "1500000"}, {"1.00", "1"},
             {"0.01", "0.01"}, {"1000t", "1000000000000000"}})
      check(PaymentRules.amount(pair[0]).compareTo(new BigDecimal(pair[1])) == 0,
          "Amount " + pair[0]);
    for (String invalid : new String[] {
             "-1", "1e6", "NaN", "Infinity", "1;op alan", "1,000", "1.001", "1bb", "", "1001t"}) {
      boolean rejected = false;
      try {
        PaymentRules.amount(invalid);
      } catch (IllegalArgumentException e) {
        rejected = true;
      }
      check(rejected, "Reject " + invalid);
    }
    for (String text : new String[] {"ECONOMY ➜ You received 41T from ♟ 20ka.",
             "ECONOMY » You received 1.00 from Alice.", "[ECONOMY] ➜ Bob_12 paid you 1b!"})
      check(PaymentRules.receipt(text).isPresent(), "Receipt " + text);
    var receipt = PaymentRules.receipt("ECONOMY ➜ You received 41T from ♟ 20ka.").orElseThrow();
    check(receipt.player().equals("20ka"), "Player icon removal");
    check(PaymentRules.refundCommand(receipt).equals("pay 20ka 41000000000000"),
        "Exact refund command");
    for (String text : new String[] {"ECONOMY ➜ You paid 1.00 to Alice.",
             "<Bob> ECONOMY ➜ You received 1B from Alice.", "Alice paid you 1B",
             "ECONOMY ➜ You received -1 from Alice.", "ECONOMY ➜ You received 0 from Alice.",
             "ECONOMY ➜ You received 1b from Alice;op me.",
             "ECONOMY ➜ You received 1b from Alice.\nextra"})
      check(PaymentRules.receipt(text).isEmpty(), "Reject receipt " + text);
    var low = BigDecimal.TEN;
    var high = new BigDecimal("100");
    check(!PaymentRules.outside(low, low, high), "Inclusive min");
    check(!PaymentRules.outside(high, low, high), "Inclusive max");
    check(PaymentRules.outside(new BigDecimal("9.99"), low, high), "Below minimum");
    check(PaymentRules.outside(new BigDecimal("100.01"), low, high), "Above maximum");
    var blacklist = new dev.gamblingmenu.blacklist.UsernameBlacklist();
    check(blacklist.add("Alice").startsWith("Added "), "Add blacklist username");
    check(blacklist.contains("aLiCe"), "Case-insensitive blacklist match");
    check(!blacklist.add("ALICE").startsWith("Added "), "Blacklist duplicate rejected");
    check(
        !blacklist.add("Alice;op me").startsWith("Added "), "Blacklist command injection rejected");
    check(!blacklist.add("ab").startsWith("Added "), "Short username rejected");
    check(blacklist.names().getFirst().equals("Alice"), "Original case preserved");
    check(blacklist.remove("ALICE") && !blacklist.contains("Alice"), "Case-insensitive removal");
    for (int i = 0; i < 200; i++) blacklist.add("user_" + i);
    check(blacklist.names().size() == 200, "Blacklist capacity");
    check(!blacklist.add("overflow").startsWith("Added "), "Blacklist overflow rejected");
    System.out.println("Payment rules: " + checks + " checks passed");
  }
}
