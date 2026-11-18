package dev.gamblingmenu.profit;
import dev.gamblingmenu.payment.PaymentRules;
import java.math.BigDecimal;
public final class ProfitLedgerTest {
  private static void check(boolean b,String s) { if(!b)throw new AssertionError(s); }
  private static BigDecimal n(String s) { return PaymentRules.amount(s); }
  public static void main(String[] args) {
    var l=new ProfitLedger();
    l.accept(false,"Alice",n("1b"),false,false,0);
    check(!l.accept(false,"ALICE",n("1000m"),false,false,1),"duplicate denomination counted");
    l.intent("Alice",n("2b"),ProfitLedger.Kind.MULTIPLIER,2);
    check(l.session().paid().signum()==0 && l.session().gambles()==0,"intent counted as confirmed");
    l.accept(true,"Alice",n("2b"),false,false,3);
    check(l.session().gambles()==1 && l.session().profit().compareTo(n("1b").negate())==0,"multiplier confirmation");
    l.accept(false,"Donor",n("41t"),true,false,4);
    l.accept(false,"RefundGuy",n("20k"),false,true,5);
    l.intent("RefundGuy",n("20k"),ProfitLedger.Kind.REFUND,6);
    l.accept(true,"RefundGuy",n("20k"),false,false,7);
    check(l.session().received().compareTo(n("1b"))==0,"guard money included");
    check(l.session().refunds().compareTo(n("20k"))==0 && l.session().donations().compareTo(n("41t"))==0,"excluded breakdown");
    check(l.session().profit().compareTo(n("1b").negate())==0,"refund affected profit");
    l.accept(true,"ManualUser",n("0.01"),false,false,8);
    check(l.session().gambles()==1,"manual payment counted gamble");
    l.intent("ExpiredUser",n("1"),ProfitLedger.Kind.MULTIPLIER,0);
    l.accept(true,"ExpiredUser",n("1"),false,false,61_000_000_000L);
    check(l.session().gambles()==1,"expired intent matched");
    l.intent("NewServer",n("1"),ProfitLedger.Kind.MULTIPLIER,62_000_000_000L);l.connectionChanged();
    l.accept(true,"NewServer",n("1"),false,false,63_000_000_000L);
    check(l.session().gambles()==1,"cross-server intent matched");
    var lifetime=l.lifetime();l.resetSession();
    check(l.session().equals(ProfitLedger.Totals.zero()) && l.lifetime().equals(lifetime),"reset destroyed lifetime");
    var restored=new ProfitLedger();restored.restore(lifetime);
    check(restored.session().received().signum()==0 && restored.lifetime().equals(lifetime),"restart persistence");
    for(int i=0;i<100;i++)restored.accept(false,"LargeUser",n("1000t"),false,false,i*3_000_000_000L);
    check(restored.lifetime().received().compareTo(new BigDecimal("100000000000000000"))>0,"overflow");
    check(PaymentRules.outgoing("ECONOMY ➜ You paid 1.25b to ♟ Alice.").isPresent(),"outgoing parser");
    for(String bad:new String[]{"<Eve> ECONOMY ➜ You paid 1b to Alice.","ECONOMY ➜ You paid NaN to Alice.","You paid 1b to Alice.","ECONOMY ➜ You paid 0 to Alice."})
      check(PaymentRules.outgoing(bad).isEmpty(),"spoof accepted");
    System.out.println("Profit ledger checks passed");
  }
}
