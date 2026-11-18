package dev.gamblingmenu.auction;
import java.math.BigDecimal;
public final class AuctionLedgerTest {
  private static void check(boolean b){if(!b)throw new AssertionError();}
  public static void main(String[] args){
    AuctionLedger a=new AuctionLedger();a.start(new BigDecimal("10000"),100,0);
    check(!a.accept("Bob_12",new BigDecimal("9999"),1));
    check(a.accept("Bob_12",new BigDecimal("10000"),2));
    check(a.accept("Bob_13",new BigDecimal("20000"),3));
    check(a.top().getFirst().player().equals("Bob_13"));
    check(a.accept("BOB_12",new BigDecimal("30000"),4));
    check(a.top().size()==2 && a.top().getFirst().amount().compareTo(new BigDecimal("30000"))==0);
    check(!a.accept("Bob_12",new BigDecimal("15000"),5));
    check(a.accept("Alice",new BigDecimal("30000"),6));
    check(a.top().getFirst().player().equals("BOB_12"));
    for(int i=0;i<10;i++)a.accept("User"+i,new BigDecimal("40000").add(BigDecimal.valueOf(i)),7+i);
    check(a.top().size()==5 && a.top().getFirst().player().equals("User9"));
    check(!a.accept("Late",new BigDecimal("900000"),100));check(a.finished(100));
    a.start(BigDecimal.ONE,10,200);check(a.top().isEmpty());a.cancel();check(!a.running(201)&&!a.finished(300));
    a.clear();check(a.top().isEmpty());
    System.out.println("Auction: highest-single-bid, minimum, case-folding, ties, top five, expiry and reset passed");
  }
}
