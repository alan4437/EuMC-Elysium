package dev.discordlink;

import dev.discordlink.gui.MenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

/** Client GUI and server-economy receipt module; no Discord or external service. */
public final class GamblingMenuClient implements ClientModInitializer {
  @Override public void onInitializeClient() { dev.gamblingmenu.payment.PaymentModule.INSTANCE.initialize(); }
  public static void openMenu() {
    var mc = Minecraft.getInstance();
    if (mc.screen instanceof MenuScreen) mc.screen.onClose();
    else mc.setScreen(new MenuScreen(mc.screen));
  }
}
