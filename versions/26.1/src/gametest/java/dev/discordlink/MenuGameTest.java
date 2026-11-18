package dev.discordlink;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.discordlink.gui.*;
import dev.gamblingmenu.payment.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MenuGameTest implements FabricClientGameTest {
  private static void check(boolean value, String message) {
    if (!value)
      throw new AssertionError(message);
  }
  private static void click(
      ClientGameTestContext ctx, Predicate<AbstractWidget> predicate, int button) {
    ctx.runOnClient(mc -> {
      var target = (AbstractWidget) mc.screen.children()
                       .stream()
                       .filter(c -> c instanceof AbstractWidget w && predicate.test(w))
                       .findFirst()
                       .orElseThrow();
      double scale =
          Math.clamp(mc.getWindow().getWidth() / 1280f, .9f, 2f) / mc.getWindow().getGuiScale();
      if(mc.screen instanceof AuctionItemScreen || mc.screen instanceof dev.gamblingmenu.profit.ProfitDetailsScreen || mc.screen instanceof dev.gamblingmenu.auction.AuctionHudScreen || mc.screen instanceof HudEditorScreen) scale=1;
      var event = new net.minecraft.client.input.MouseButtonEvent((target.getX() + 5) * scale,
          (target.getY() + 5) * scale, new net.minecraft.client.input.MouseButtonInfo(button, 0));
      check(mc.screen.mouseClicked(event, false),
          "Widget click failed: " + target.getMessage().getString());
      mc.screen.mouseReleased(event);
    });
  }
  private static void receipt(String text) {
    ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(Component.literal(text), false);
  }
  @Override
  public void runTest(ClientGameTestContext ctx) {
    var paid = new ConcurrentLinkedQueue<String>();
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registry, environment)
            -> dispatcher.register(Commands.literal("pay").then(
                Commands.argument("target", StringArgumentType.word())
                    .then(
                        Commands.argument("amount", StringArgumentType.word()).executes(command -> {
                          paid.add(command.getInput());
                          return 1;
                        })))));
    ctx.runOnClient(mc -> mc.options.pauseOnLostFocus = false);
    try (var world = ctx.worldBuilder().create()) {
      world.getClientLevel().waitForChunksRender();
      var original = ctx.computeOnClient(mc -> mc.options.getCameraType());
      ctx.getInput().pressKey(GLFW.GLFW_KEY_F5);
      ctx.waitTicks(2);
      ctx.runOnClient(mc -> {
        check(mc.screen == null, "F5 opened custom GUI");
        check(mc.options.getCameraType() != original, "F5 switching failed");
      });
      var camera = ctx.computeOnClient(mc -> mc.options.getCameraType());
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);
      ctx.waitTicks(6);
      ctx.runOnClient(mc -> {
        check(
            !mc.screen.isPauseScreen() && mc.getConnection() != null, "GUI paused or disconnected");
        check(mc.options.getCameraType() == camera, "Right Shift changed camera");
        check(!PaymentModule.INSTANCE.enabled(), "Module enabled automatically");
      });
      click(ctx, w -> w instanceof SearchInput, 0);
      ctx.getInput().typeChars("unrelated");
      ctx.runOnClient(mc -> {
        var screen = (MenuScreen) mc.screen;
        check(screen.searchResults().isEmpty(), "Search did not filter");
        screen.setSearchQuery("");
      });
      ctx.getInput().resizeWindow(1920, 1080);
      ctx.waitTicks(5);
      ctx.takeScreenshot("payment-card-1080p");
      click(ctx, w -> w instanceof PaymentCard, 1);
      var originalLimits = ctx.computeOnClient(
          mc -> PaymentModule.INSTANCE.min() + ":" + PaymentModule.INSTANCE.max());
      ctx.runOnClient(mc -> {
        var screen = (MenuScreen) mc.screen;
        check(screen.paymentSettingsOpen(), "Right click did not open settings");
        check(mc.screen.children().stream().noneMatch(c
                  -> c instanceof AbstractWidget widget
                      && widget.getMessage().getString().equals("Apply settings")),
            "Manual apply button remains");
        screen.paymentDrafts("1b", "20k");
      });
      ctx.waitFor(mc -> !((MenuScreen) mc.screen).settingsValidationMessage().isEmpty(), 100);
      ctx.runOnClient(mc -> {
        check((PaymentModule.INSTANCE.min() + ":" + PaymentModule.INSTANCE.max())
                  .equals(originalLimits),
            "Invalid auto-save changed active limits");
        ((MenuScreen) mc.screen).paymentDrafts("20k", "1t");
      });
      ctx.waitFor(mc
          -> PaymentModule.INSTANCE.min().equals("20000")
              && PaymentModule.INSTANCE.max().equals("1000000000000"),
          100);
      ctx.runOnClient(mc
          -> check(((MenuScreen) mc.screen).settingsValidationMessage().isEmpty(),
              "Valid automatic save left an error"));
      ctx.waitTicks(3);
      ctx.takeScreenshot("payment-settings-1080p");
      click(ctx, w -> w.getMessage().getString().equals("Donation  v"), 0);
      ctx.waitTicks(2);
      ctx.takeScreenshot("payment-mode-dropdown");
      click(ctx, w -> w.getMessage().getString().equals("Pay back"), 0);
      ctx.runOnClient(mc -> {
        check(PaymentModule.INSTANCE.mode() == PaymentRules.Mode.PAY_BACK, "Dropdown failed");
      });
      click(ctx, w -> w instanceof PaymentCard, 0);
      ctx.runOnClient(mc -> {
        var module = PaymentModule.INSTANCE;
        check(module.enabled(), "Card did not toggle on");
        module.apply("10", "100", PaymentRules.Mode.PAY_BACK);
        receipt("ECONOMY ➜ You received 5 from ♟ Alice.");
        receipt("ECONOMY ➜ You received 10 from Alice.");
        receipt("ECONOMY ➜ You received 100 from Alice.");
        receipt("ECONOMY ➜ You received 1b from Bob_12.");
        receipt("ECONOMY ➜ You paid 5 to Alice.");
        receipt("<Eve> ECONOMY ➜ You received 1B from Eve.");
      });
      ctx.waitFor(mc -> paid.size() == 2, 220);
      ctx.waitTicks(15);
      check(paid.size() == 2 && paid.contains("pay Alice 5")
              && paid.contains("pay Bob_12 1000000000"),
          "Incorrect/reflected/duplicate refund commands: " + paid);
      click(ctx, w -> w.getMessage().getString().equals("Pay back  v"), 0);
      click(ctx, w -> w.getMessage().getString().equals("Donation"), 0);
      ctx.runOnClient(mc -> {
        var screen = (MenuScreen) mc.screen;
        screen.paymentDrafts("20k", "1t");
        screen.applyPaymentSettings();
        var module = PaymentModule.INSTANCE;
        String before = module.status();
        receipt("ECONOMY ➜ You received 50k from Alice.");
        check(module.status().equals(before), "In-range donation triggered thank-you");
        receipt("ECONOMY ➜ You received 41T from ♟ 20ka.");
        check(module.status().contains("20ka"), "Donation notification failed");
      });
      ctx.waitTicks(5);
      ctx.takeScreenshot("donation-announcement-menu");
      ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
      ctx.waitForScreen(null);
      ctx.waitTicks(2);
      ctx.takeScreenshot("donation-announcement-game");
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);
      ctx.runOnClient(mc -> {
        var screen = (MenuScreen) mc.screen;
        screen.openPaymentSettings();
        screen.paymentDrafts("20k", "1t");
      });
      check(paid.size() == 2, "Donation mode sent a refund");
      ctx.getInput().resizeWindow(2560, 1440);
      ctx.waitTicks(5);
      ctx.takeScreenshot("payment-settings-1440p");
      ctx.getInput().resizeWindow(640, 480);
      ctx.waitTicks(5);
      ctx.takeScreenshot("payment-settings-small");
      ctx.runOnClient(mc -> {
        mc.options.guiScale().set(1);
        mc.resizeGui();
      });
      ctx.waitTicks(3);
      ctx.takeScreenshot("payment-gui-scale-1");
      ctx.runOnClient(mc -> {
        mc.options.guiScale().set(3);
        mc.resizeGui();
      });
      ctx.waitTicks(3);
      ctx.takeScreenshot("payment-gui-scale-3");
      ctx.runOnClient(mc -> {
        PaymentModule.INSTANCE.toggle();
        check(!PaymentModule.INSTANCE.enabled(), "Card could not disable");
      });
      ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
      ctx.waitForScreen(null);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(null);
      ctx.runOnClient(mc -> {
        check(mc.options.getCameraType() == camera, "Menu changed camera");
        PaymentModule.INSTANCE.reset();
      });
      // New module: UI list management and shared refund priority on an isolated dummy economy.
      ctx.getInput().resizeWindow(1920, 1080);
      ctx.waitTicks(3);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);
      for (int i = 0; i < 6; i++) {
        ctx.takeScreenshot("anim-main-" + i);
        ctx.waitTicks(1);
      }
      click(ctx, w -> w instanceof PaymentCard card && card.blacklist(), 1);
      for (int i = 0; i < 8; i++) {
        ctx.takeScreenshot("anim-inspector-" + i);
        ctx.waitTicks(1);
      }
      ctx.runOnClient(mc
          -> check(((MenuScreen) mc.screen).blacklistSettingsOpen(),
              "Blacklist inspector did not open"));
      click(ctx, w -> w instanceof AmountInput, 0);
      ctx.getInput().typeChars("Alice");
      click(ctx, w -> w.getMessage().getString().equals("Add"), 0);
      click(ctx, w -> w instanceof AmountInput, 0);
      ctx.getInput().typeChars("Bob_12");
      click(ctx, w -> w.getMessage().getString().equals("Add"), 0);
      ctx.runOnClient(mc -> {
        var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
        check(blacklist.contains("ALICE") && blacklist.contains("bob_12"),
            "UI list additions failed");
        check(!blacklist.add("alice").startsWith("Added "), "Duplicate username accepted");
      });
      click(ctx, w -> w instanceof PaymentCard card && card.blacklist(), 0);
      for (int i = 0; i < 8; i++) {
        ctx.takeScreenshot("anim-toggle-" + i);
        ctx.waitTicks(1);
      }
      ctx.takeScreenshot("blacklist-settings-1080p");
      ctx.runOnClient(mc -> {
        check(!PaymentModule.INSTANCE.enabled(), "Payment Guard should be off");
        receipt("ECONOMY ➜ You received 50 from ALICE.");
      });
      ctx.waitFor(mc -> paid.size() == 3, 120);
      check(paid.contains("pay ALICE 50"), "Blacklist failed independently of Payment Guard");
      ctx.runOnClient(mc -> {
        PaymentModule.INSTANCE.toggle();
        PaymentModule.INSTANCE.apply("10", "100", PaymentRules.Mode.PAY_BACK);
        receipt("ECONOMY ➜ You received 1b from Bob_12.");
        receipt("ECONOMY ➜ You received 50 from Carol.");
      });
      ctx.waitFor(mc -> paid.size() == 4, 120);
      ctx.waitTicks(12);
      check(paid.size() == 4, "Overlapping modules duplicated a refund");
      ctx.runOnClient(mc -> {
        PaymentModule.INSTANCE.apply("10", "100", PaymentRules.Mode.DONATION);
        receipt("ECONOMY ➜ You received 1b from Bob_12.");
        check(!PaymentModule.INSTANCE.status().startsWith("Donation from"),
            "Blacklist produced a donation announcement");
      });
      ctx.waitFor(mc -> paid.size() == 5, 120);
      ctx.runOnClient(mc -> {
        dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.toggle();
        PaymentModule.INSTANCE.toggle();
        receipt("ECONOMY ➜ You received 50 from Alice.");
      });
      ctx.waitTicks(12);
      check(paid.size() == 5, "Disabled blacklist refunded money");
      ctx.runOnClient(mc -> {
        var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
        blacklist.toggle();
        receipt("ECONOMY ➜ You received 42 from Bob_12.");
        blacklist.remove("Bob_12");
        receipt("ECONOMY ➜ You received 100 from Alice.");
      });
      ctx.waitFor(mc -> paid.size() == 6, 120);
      ctx.waitTicks(10);
      check(!paid.contains("pay Bob_12 42") && paid.contains("pay Alice 100"),
          "Removed username was refunded");
      ctx.runOnClient(mc -> {
        dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE.add("Bob_12");
        ((MenuScreen) mc.screen).openBlacklistSettings();
      });
      ctx.waitTicks(5);
      ctx.takeScreenshot("blacklist-final");
      click(ctx, w -> w.getMessage().getString().equals("X"), 0);
      for (int i = 0; i < 6; i++) {
        ctx.takeScreenshot("anim-close-inspector-" + i);
        ctx.waitTicks(1);
      }
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(null);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);
      ctx.waitTicks(8);
      click(ctx, w -> w.getMessage().getString().equals("Settings"), 0);
      click(ctx, w -> w.getMessage().getString().startsWith("Speed:"), 0);
      ctx.runOnClient(mc -> check(dev.discordlink.gui.GuiPreferences.duration != 320, "Speed did not change"));
      click(ctx, w -> w.getMessage().getString().startsWith("Blur:"), 0);
      ctx.runOnClient(mc -> check(!dev.discordlink.gui.GuiPreferences.blur, "Blur did not disable"));
      click(ctx, w -> w.getMessage().getString().equals("Reset GUI settings"), 0);
      ctx.runOnClient(mc -> check(dev.discordlink.gui.GuiPreferences.blur && dev.discordlink.gui.GuiPreferences.duration == 320, "GUI reset failed"));
      ctx.runOnClient(mc -> {
        var wheel = (dev.discordlink.gui.ColourWheel) mc.screen.children().stream()
            .filter(w -> w instanceof dev.discordlink.gui.ColourWheel).findFirst().orElseThrow();
        double scale = Math.clamp(mc.getWindow().getWidth()/1280f,.9f,2f)/mc.getWindow().getGuiScale();
        var button = new net.minecraft.client.input.MouseButtonInfo(0,0);
        var start = new net.minecraft.client.input.MouseButtonEvent((wheel.getX()+69)*scale,(wheel.getY()+36)*scale,button);
        check(mc.screen.mouseClicked(start,false), "Wheel click failed");
        int first = dev.discordlink.gui.GuiPreferences.accent;
        var end = new net.minecraft.client.input.MouseButtonEvent((wheel.getX()+36)*scale,(wheel.getY()+3)*scale,button);
        mc.screen.mouseDragged(end, -33*scale,-33*scale);
        mc.screen.mouseReleased(end);
        check(first != dev.discordlink.gui.GuiPreferences.accent,"Wheel dragging did not change hue");
        var dark = new net.minecraft.client.input.MouseButtonEvent((wheel.getX()+88)*scale,(wheel.getY()+40)*scale,button);
        mc.screen.mouseClicked(dark,false); mc.screen.mouseReleased(dark);
        check(dev.discordlink.gui.GuiPreferences.accent==0,"Brightness minimum is not black");
        var light = new net.minecraft.client.input.MouseButtonEvent((wheel.getRight()-8)*scale,(wheel.getY()+40)*scale,button);
        mc.screen.mouseClicked(light,false); mc.screen.mouseReleased(light);
        check(dev.discordlink.gui.GuiPreferences.accent!=0,"Brightness did not restore colour");
      });
      ctx.waitTicks(3);
      click(ctx, w -> w.getMessage().getString().startsWith("Corners:"), 0);
      click(ctx, w -> w.getMessage().getString().startsWith("Gloss:"), 0);
      ctx.runOnClient(mc -> {
        check(dev.discordlink.gui.GuiPreferences.corners == 50, "Corner style failed");
        check(dev.discordlink.gui.GuiPreferences.gloss == 125, "Gloss failed");
      });
      ctx.runOnClient(mc -> {
        check(mc.screen.children().stream().anyMatch(w -> w instanceof dev.discordlink.gui.DiscordInviteButton), "Discord shortcut missing");
        check(dev.discordlink.gui.DiscordInviteButton.INVITE.toString().equals("https://discord.gg/3h9MAYQwUc"), "Incorrect invite");
        var opened = new java.util.concurrent.atomic.AtomicBoolean();
        var button = new dev.discordlink.gui.DiscordInviteButton(0,0, () -> opened.set(true));
        button.onClick(new net.minecraft.client.input.MouseButtonEvent(5,5,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
        check(opened.get(), "Invite click did not invoke browser action");
      });
      ctx.takeScreenshot("gui-settings-colour");
      click(ctx, w -> w.getMessage().getString().equals("Reset GUI settings"), 0);
      ctx.takeScreenshot("gui-settings");
      click(ctx, w -> w.getMessage().getString().equals("Gambling"), 0);
      ctx.runOnClient(mc -> check(mc.screen.children().stream().anyMatch(w -> w instanceof PaymentCard), "Gambling cards missing"));
      click(ctx, w -> w instanceof CategoryEntry && w.getMessage().getString().equals("EuropeMC"), 0);
      ctx.waitTicks(8);
      ctx.takeScreenshot("europemc-sidebar");
      click(ctx,w -> w instanceof CategoryEntry && w.getMessage().getString().equals("Gambling"),0);
      ctx.waitTicks(8);
      int[] previewColours = {0xeeeeef,0x55bbff,0x8b5cff,0xff72bf,0xff5656,0xffa64d,0xffdc55,0x65df89,0x45ded6,0x778bff};
      for (int i=0; i<previewColours.length; i++) {
        int colour = previewColours[i];
        ctx.runOnClient(mc -> dev.discordlink.gui.GuiPreferences.accent = colour);
        ctx.waitTicks(2);
        ctx.takeScreenshot("logo-colour-" + i);
      }
      ctx.runOnClient(mc -> {
        PaymentModule.INSTANCE.reset();
        dev.discordlink.gui.GuiPreferences.reset();
      });
      ctx.runOnClient(mc -> check(mc.screen.children().stream().noneMatch(w -> w instanceof PaymentCard card && card.kind()==2), "Multipliers still in Gambling"));
      click(ctx, w -> w instanceof CategoryEntry && w.getMessage().getString().equals("EuropeMC"), 0);
      ctx.runOnClient(mc -> check(mc.screen.children().stream().anyMatch(w -> w instanceof PaymentCard card && card.kind()==2), "Multipliers missing from EuropeMC"));
      click(ctx, w -> w instanceof PaymentCard card && card.kind()==2, 1);
      ctx.runOnClient(mc -> check(mc.screen.children().stream().anyMatch(w -> w instanceof PaymentCard card && card.kind()==3), "Opening multipliers left EuropeMC"));
      ctx.waitTicks(8);
      ctx.runOnClient(mc -> {
        for(var child: mc.screen.children()) if(child instanceof AmountInput input) {
          String label=input.getMessage().getString();
          if(label.equals("Multiplier slot 1")) input.setValue("2x");
          if(label.equals("Multiplier slot 2")) input.setValue("4x");
          if(label.equals("Multiplier slot 3")) input.setValue("");
        }
      });
      click(ctx, w -> w instanceof PaymentCard card && card.kind()==2, 0);
      ctx.waitTicks(8);
      ctx.takeScreenshot("payment-multipliers-settings");
      var payoutCommands = new java.util.ArrayList<String>();
      ctx.runOnClient(mc -> {
        var module=dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE;
        check(module.enabled(),"Multiplier did not enable");
        check(!module.setSlot(0,"-2x") && !module.setSlot(0,"NaN") && !module.setSlot(0,"1001x"),"Invalid multiplier accepted");
        Component incoming=Component.literal("ECONOMY ➜ You received 100 from Alice.");
        Component decorated=ClientReceiveMessageEvents.MODIFY_GAME.invoker().modifyReceivedGameMessage(incoming,false);
        check(decorated.getString().equals(incoming.getString()+" [2x] [4x]"),"Incorrect payout tags or blank slot visible");
        check(module.original(decorated).equals(incoming.getString()),"Original receipt not preserved");
        for(var tag:decorated.getSiblings()) if(tag.getStyle().getClickEvent() instanceof net.minecraft.network.chat.ClickEvent.RunCommand run) payoutCommands.add(run.command().substring(1));
        mc.gui.getChat().addClientSystemMessage(decorated);
        check(decorated.getSiblings().get(0).getStyle().getColor().getValue()==0xfcb500 && decorated.getSiblings().get(1).getStyle().getColor().getValue()==0x97e35e,"Wrong tag colours");
        check(payoutCommands.size()==2,"Incorrect clickable option count");
        mc.getConnection().sendCommand(payoutCommands.get(0));
        mc.getConnection().sendCommand(payoutCommands.get(1));
        var overlay=ClientReceiveMessageEvents.MODIFY_GAME.invoker().modifyReceivedGameMessage(incoming,true);
        check(overlay==incoming,"Overlay decorated");
        check(module.decorate(Component.literal("<Eve> ECONOMY ➜ You received 100 from Alice.")).getSiblings().isEmpty(),"Player spoof decorated");
      });
      ctx.waitFor(mc -> paid.size()==7,120);
      ctx.waitTicks(4);
      check(paid.size()==7 && paid.contains("pay Alice 200"),"Multiplier payout incorrect or receipt paid twice");
      ctx.runOnClient(mc -> {
        var module=dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE;
        Component decorated=module.decorate(Component.literal("ECONOMY ➜ You received 41T from Bob_12."));
        var click=(net.minecraft.network.chat.ClickEvent.RunCommand)decorated.getSiblings().get(1).getStyle().getClickEvent();
        mc.getConnection().sendCommand(click.command().substring(1));
        check(module.setSlot(2,"1.5x"),"Decimal multiplier rejected");
        var small=module.decorate(Component.literal("ECONOMY ➜ You received 0.01 from Alice."));
        check(!small.getString().contains("1.5x"),"Fractional-cent payout offered");
        module.setSlot(2,"");
        PaymentModule.INSTANCE.apply("1","10",PaymentRules.Mode.PAY_BACK);
        PaymentModule.INSTANCE.toggle();
        check(module.decorate(Component.literal("ECONOMY ➜ You received 100 from Alice.")).getSiblings().isEmpty(),"Automatic refund also offered payout");
        PaymentModule.INSTANCE.toggle();
      });
      ctx.waitFor(mc -> paid.size()==8,120);
      check(paid.contains("pay Bob_12 164000000000000"),"4x abbreviated amount incorrect");
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(null);
      ctx.runOnClient(mc -> {
        var module=dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE;
        mc.gui.getChat().clearMessages(false);
        module.setSlot(2,"8x");
        var shown=module.decorate(Component.literal("ECONOMY ➜ You received 1B from Alice."));
        check(shown.getSiblings().get(2).getStyle().getColor().getValue()==0x00bdf8,"Third tag is not blue");
        mc.gui.getChat().addClientSystemMessage(shown);
        mc.gui.getChat().addClientSystemMessage(module.decorate(Component.literal("ECONOMY ➜ You received 20K from Bob_12.")));
        mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("",false));
      });
      ctx.waitTicks(3);
      ctx.takeScreenshot("payment-multipliers-chat");
      ctx.runOnClient(mc -> dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.setSlot(2,""));
      ctx.runOnClient(mc -> {
        var module=dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE;
        module.reset();
        mc.getConnection().sendCommand(payoutCommands.get(0));
        mc.setScreen(null);
      });
      ctx.waitTicks(3);
      check(paid.size()==8,"Stale payout survived reset");
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(null);
      ctx.runOnClient(mc -> {
        PaymentModule.INSTANCE.reset();
        mc.player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
      });
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      ctx.waitForScreen(MenuScreen.class);ctx.waitTicks(8);
      click(ctx,w -> w instanceof CategoryEntry && w.getMessage().getString().equals("EuropeMC"),0);
      click(ctx,w -> w instanceof PaymentCard card && card.kind()==3,1);ctx.waitTicks(8);
      click(ctx,w -> w.getMessage().getString().equals("HUD appearance"),0);
      ctx.waitForScreen(dev.gamblingmenu.auction.AuctionHudScreen.class);
      ctx.runOnClient(mc -> {
        for(var child:mc.screen.children()) if(child instanceof AmountInput input) {
          switch(input.getMessage().getString()) {
            case "Auction HUD scale" -> input.setValue("0.85");
            case "Auction HUD opacity" -> input.setValue("75");
          }
        }
        var a=dev.gamblingmenu.auction.AuctionModule.INSTANCE;
        check(a.hudScale==.85f && a.hudOpacity==75,"Auction HUD preferences failed");
      });
      ctx.takeScreenshot("auction-hud-layout-settings");
      click(ctx,w -> w.getMessage().getString().equals("Back"),0);
      ctx.waitForScreen(MenuScreen.class);ctx.waitTicks(8);
      click(ctx,w -> w.getMessage().getString().equals("Choose item"),0);
      ctx.waitForScreen(AuctionItemScreen.class);
      ctx.takeScreenshot("auction-item-picker");
      click(ctx,w -> w.getMessage().getString().equals("Diamond Sword"),0);
      ctx.waitForScreen(MenuScreen.class);ctx.waitTicks(8);
      ctx.runOnClient(mc -> {
        var a=dev.gamblingmenu.auction.AuctionModule.INSTANCE;
        check(!a.item().isEmpty(),"Item selection failed");
        for(var child: mc.screen.children())if(child instanceof AmountInput input){
          if(input.getMessage().getString().equals("Auction minimum"))input.setValue("10k");
          if(input.getMessage().getString().equals("Auction duration seconds"))input.setValue("6");
        }
        check(a.seconds()==6 && a.minimum().equals("10k"),"Auction settings not applied");
        check(!a.minimum("invalid") && !a.duration("0"),"Invalid auction config accepted");
      });
      ctx.takeScreenshot("auction-settings");
      click(ctx,w -> w.getMessage().getString().equals("Start / Stop"),0);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);ctx.waitForScreen(null);
      ctx.runOnClient(mc -> {
        var a=dev.gamblingmenu.auction.AuctionModule.INSTANCE;
        check(a.enabled(),"Auction did not start");
        receipt("ECONOMY ➜ You received 10K from Bob_12.");
        receipt("ECONOMY ➜ You received 20K from Bob_13.");
        receipt("ECONOMY ➜ You received 15K from Alice.");
        receipt("ECONOMY ➜ You received 50K from Charlie.");
        receipt("ECONOMY ➜ You received 30K from Dylan.");
        receipt("ECONOMY ➜ You received 60K from Bob_12.");
        receipt("ECONOMY ➜ You received 5K from Bob_13.");
        receipt("<Eve> ECONOMY ➜ You received 1T from Eve.");
        check(a.top().size()==5 && a.top().getFirst().player().equals("Bob_12"),"Auction rankings incorrect");
        check(a.top().getFirst().amount().toPlainString().equals("60000"),"Bids were added together");
        check(mc.player.getInventory().getItem(0).is(net.minecraft.world.item.Items.DIAMOND_SWORD),"Item moved unexpectedly");
      });
      ctx.waitTicks(4);ctx.takeScreenshot("auction-leaderboard");
      ctx.waitFor(mc -> dev.gamblingmenu.auction.AuctionModule.INSTANCE.status().startsWith("Winner:"),200);
      for(int i=0;i<20;i++){ctx.takeScreenshot("auction-confetti-"+String.format("%02d",i));ctx.waitTicks(1);}
      ctx.waitFor(mc -> !dev.gamblingmenu.auction.AuctionModule.INSTANCE.shown(), 300);
      ctx.takeScreenshot("auction-results-auto-hidden");
      ctx.runOnClient(mc -> {
        var a=dev.gamblingmenu.auction.AuctionModule.INSTANCE;
        receipt("ECONOMY ➜ You received 1B from LateUser.");
        check(a.top().getFirst().player().equals("Bob_12"),"Late bid changed winner");
        check(a.status().contains("60K"),"Winner amount incorrect");
        a.duration("60");a.reset();
        check(!a.shown(),"Auction did not reset");
      });
      check(paid.size()==8,"Auction sent an unsolicited payout");
      ctx.runOnClient(mc -> {
        PaymentModule.INSTANCE.reset();
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        if(!p.enabled)p.toggle();p.resetSession();
        PaymentModule.INSTANCE.apply("10","100",PaymentRules.Mode.DONATION);
        PaymentModule.INSTANCE.toggle();
        receipt("ECONOMY ➜ You received 1b from DonorUser.");
        check(p.ledger.session().received().signum()==0 && p.ledger.session().donations().toPlainString().equals("1000000000"),"Donation counted as gambling income");
        PaymentModule.INSTANCE.apply("10","100",PaymentRules.Mode.PAY_BACK);
        receipt("ECONOMY ➜ You received 5 from RefundUser.");
      });
      ctx.waitFor(mc -> paid.size()==9,120);
      ctx.runOnClient(mc -> {
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        check(p.ledger.session().paid().signum()==0,"Refund command counted before receipt");
        receipt("ECONOMY ➜ You paid 5 to RefundUser.");
        receipt("ECONOMY ➜ You paid 5.00 to REFUNDUSER.");
        check(p.ledger.session().refunds().toPlainString().equals("5") && p.ledger.session().profit().signum()==0,"Refund affected profit or counted twice");
        PaymentModule.INSTANCE.reset();
        var m=dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE;
        m.setSlot(0,"2x");if(!m.enabled())m.toggle();
        var incoming=Component.literal("ECONOMY ➜ You received 50m from GambleUser.");
        var decorated=m.decorate(incoming);
        ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(decorated,false);
        var command=(net.minecraft.network.chat.ClickEvent.RunCommand)decorated.getSiblings().getFirst().getStyle().getClickEvent();
        mc.getConnection().sendCommand(command.command().substring(1));
      });
      ctx.waitFor(mc -> paid.size()==10,120);
      ctx.runOnClient(mc -> {
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        check(p.ledger.session().gambles()==0,"Unconfirmed multiplier counted");
        receipt("ECONOMY ➜ You paid 100m to GambleUser.");
        receipt("ECONOMY ➜ You received 12.45b from OrdinaryUser.");
        receipt("<Eve> ECONOMY ➜ You received 1t from SpoofUser.");
        check(p.ledger.session().gambles()==1,"Unrelated income counted as gamble");
        check(p.ledger.session().profit().compareTo(new java.math.BigDecimal("12400000000"))==0,"Exact net profit incorrect");
        p.positionX=.05f;p.positionY=.66f;p.showHud=true;
      });
      ctx.waitTicks(10);ctx.takeScreenshot("profit-hud-positive");
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);ctx.waitForScreen(MenuScreen.class);ctx.waitTicks(8);
      click(ctx,w -> w instanceof PaymentCard c && c.kind()==4,1);ctx.waitTicks(8);
      ctx.takeScreenshot("profit-tracker-settings");
      ctx.runOnClient(mc -> check(mc.screen.children().stream().noneMatch(c -> c instanceof AmountInput input && (input.getMessage().getString().equals("HUD X") || input.getMessage().getString().equals("HUD Y"))),"Position fields still in right-click settings"));
      click(ctx,w -> w instanceof CategoryEntry && w.getMessage().getString().equals("Settings"),0);
      click(ctx,w -> w.getMessage().getString().equals("Edit HUD positions"),0);
      ctx.waitForScreen(HudEditorScreen.class);ctx.waitTicks(8);
      ctx.runOnClient(mc -> {
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        float before=p.positionX;
        int w=mc.screen.width,h=mc.screen.height;
        double x=p.left(w,h)+4,y=p.top(w,h)+4;
        var info=new net.minecraft.client.input.MouseButtonInfo(0,0);
        var press=new net.minecraft.client.input.MouseButtonEvent(x,y,info);
        check(mc.screen.mouseClicked(press,false),"HUD drag did not start");
        var move=new net.minecraft.client.input.MouseButtonEvent(x+35,y-15,info);
        mc.screen.mouseDragged(move,35,-15);mc.screen.mouseReleased(move);
        check(p.positionX>before,"HUD position did not change");
        float old=p.scale;
        float logicalScale=(float)(Math.clamp(mc.getWindow().getWidth()/1280f,.9f,2f)/mc.getWindow().getGuiScale());
        x=p.left(w,h)+214*logicalScale*p.scale;y=p.top(w,h)+110*logicalScale*p.scale;
        var resize=new net.minecraft.client.input.MouseButtonEvent(x,y,info);
        check(mc.screen.mouseClicked(resize,false),"HUD resize did not start");
        var larger=new net.minecraft.client.input.MouseButtonEvent(x+20,y,info);
        mc.screen.mouseDragged(larger,20,0);mc.screen.mouseReleased(larger);
        check(p.scale>old,"HUD resize failed");
        var auction=dev.gamblingmenu.auction.AuctionModule.INSTANCE;
        float oldX=auction.hudX,oldY=auction.hudY;
        x=auction.hudLeft(w,h)+8;y=auction.hudTop(w,h)+8;
        var pressAuction=new net.minecraft.client.input.MouseButtonEvent(x,y,info);
        check(mc.screen.mouseClicked(pressAuction,false),"Auction drag did not start");
        var moveAuction=new net.minecraft.client.input.MouseButtonEvent(x+25,y+15,info);
        mc.screen.mouseDragged(moveAuction,25,15);mc.screen.mouseReleased(moveAuction);
        check(auction.hudX>oldX && auction.hudY>oldY && !auction.enabled(),"Auction editor moved nothing or started auction");
      });
      ctx.takeScreenshot("hud-layout-editor");
      click(ctx,w -> w.getMessage().getString().equals("Done"),0);ctx.waitForScreen(MenuScreen.class);
      click(ctx,w -> w instanceof CategoryEntry && w.getMessage().getString().equals("Gambling"),0);
      click(ctx,w -> w instanceof PaymentCard c && c.kind()==4,1);ctx.waitTicks(8);
      ctx.runOnClient(mc -> {
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        for(var child:mc.screen.children())if(child instanceof AmountInput input && input.getMessage().getString().equals("HUD opacity"))input.setValue("70");
        check(p.opacity==70,"HUD opacity setting failed");
        p.primary=0x55bbff;p.customColour=true;p.changed();
      });
      ctx.takeScreenshot("profit-tracker-customised");
      click(ctx,w -> w.getMessage().getString().equals("View detailed statistics"),0);
      ctx.waitForScreen(dev.gamblingmenu.profit.ProfitDetailsScreen.class);ctx.takeScreenshot("profit-statistics");
      click(ctx,w -> w.getMessage().getString().equals("Back"),0);ctx.waitForScreen(MenuScreen.class);ctx.waitTicks(8);
      click(ctx,w -> w.getMessage().getString().equals("Reset current session"),0);
      ctx.runOnClient(mc -> check(dev.gamblingmenu.profit.ProfitTracker.INSTANCE.ledger.session().gambles()==1,"Reset happened without confirmation"));
      click(ctx,w -> w.getMessage().getString().equals("Confirm session reset"),0);
      ctx.runOnClient(mc -> {
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        check(p.ledger.session().received().signum()==0 && p.ledger.lifetime().gambles()>=1,"Session reset erased lifetime");
        receipt("ECONOMY ➜ You paid 10k to ManualUser.");
        check(p.ledger.session().profit().toPlainString().equals("-10000") && p.ledger.session().gambles()==0,"Manual payout classification");
      });
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);ctx.waitForScreen(null);ctx.waitTicks(10);ctx.takeScreenshot("profit-hud-negative");
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);ctx.waitForScreen(MenuScreen.class);ctx.waitTicks(8);
      click(ctx,w -> w instanceof PaymentCard c && c.kind()==4,1);ctx.waitTicks(8);
      ctx.getInput().resizeWindow(640,480);ctx.waitTicks(8);
      ctx.runOnClient(mc -> {
        var close=(AbstractWidget)mc.screen.children().stream().filter(c -> c instanceof AbstractWidget w && w.getMessage().getString().equals("X")).findFirst().orElseThrow();
        float sc=(float)(.9/mc.getWindow().getGuiScale());
        check(mc.screen.mouseScrolled((close.getX()-30)*sc,200*sc,0,-10),"Profit settings scroll not handled");
        check(mc.screen.children().stream().anyMatch(c -> c instanceof AbstractWidget w && w.visible && w.getMessage().getString().equals("View detailed statistics")),"Small-window controls unreachable");
      });
      ctx.takeScreenshot("profit-small-window-settings");
      click(ctx,w -> w.getMessage().getString().equals("View detailed statistics"),0);
      ctx.waitForScreen(dev.gamblingmenu.profit.ProfitDetailsScreen.class);ctx.takeScreenshot("profit-small-window-details");
      click(ctx,w -> w.getMessage().getString().equals("Back"),0);ctx.waitForScreen(MenuScreen.class);
      ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);ctx.waitForScreen(null);
      ctx.runOnClient(mc -> {
        var p=dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
        p.toggle();var before=p.ledger.session();receipt("ECONOMY ➜ You received 1b from IgnoredUser.");
        check(p.ledger.session().equals(before),"Disabled tracker counted payment");
        p.showHud=false;p.changed();
      });
      ctx.runOnClient(mc -> PaymentModule.INSTANCE.reset());
    }
    ctx.runOnClient(
        mc -> check(!PaymentModule.INSTANCE.enabled(), "Disconnect retained enabled state"));
  }
}
