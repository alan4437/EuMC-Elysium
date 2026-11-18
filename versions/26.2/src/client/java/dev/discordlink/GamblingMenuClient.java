package dev.discordlink;

import dev.discordlink.gui.MenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

/** Client GUI and server-economy receipt module; no Discord or external service. */
public final class GamblingMenuClient implements ClientModInitializer {
  @Override public void onInitializeClient() { dev.gamblingmenu.payment.PaymentModule.INSTANCE.initialize(); }
  public static void openMenu() {
    var mc = Minecraft.getInstance();
    if (mc.gui.screen() instanceof MenuScreen) mc.gui.screen().onClose();
    else mc.gui.setScreen(new MenuScreen(mc.gui.screen()));
  }
}
