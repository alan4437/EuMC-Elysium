package dev.gamblingmenu.auction;
import java.math.BigDecimal;
import java.util.*;
/** Highest single bid per player; ties preserve the first time that amount was reached. */
public final class AuctionLedger {
  public record Bid(String player,BigDecimal amount,long order) {}
  private final Map<String,Bid> bids=new HashMap<>();
  private BigDecimal minimum=BigDecimal.ONE;
  private long deadline, sequence;
  private boolean started,cancelled;
  public void start(BigDecimal min,long durationNanos,long now) {
    if(min.signum()<=0 || durationNanos<=0)throw new IllegalArgumentException();
    bids.clear();sequence=0;minimum=min;deadline=now+durationNanos;started=true;cancelled=false;
  }
  public boolean running(long now){return started && !cancelled && now<deadline;}
  public boolean finished(long now){return started && !cancelled && now>=deadline;}
  public long remaining(long now){return Math.max(0,deadline-now);}
  public void cancel(){cancelled=true;}
  public void clear(){bids.clear();started=false;cancelled=false;}
  public boolean accept(String player,BigDecimal amount,long now) {
    if(!running(now)||!player.matches("[A-Za-z0-9_]{3,16}")||amount.compareTo(minimum)<0)return false;
    String key=player.toLowerCase(Locale.ROOT);Bid old=bids.get(key);
    if(old!=null && old.amount().compareTo(amount)>=0)return false;
    if(old==null && bids.size()>=1000)return false;
    bids.put(key,new Bid(player,amount,sequence++));return true;
  }
  public List<Bid> top(){return bids.values().stream().sorted(Comparator.comparing(Bid::amount).reversed().thenComparingLong(Bid::order)).limit(5).toList();}
}
