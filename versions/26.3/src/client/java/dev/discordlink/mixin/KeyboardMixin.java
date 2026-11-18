package dev.discordlink.mixin;
import dev.discordlink.GamblingMenuClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
  private void discordlink$key(long window, int action, KeyEvent event, CallbackInfo ci) {
    if (window == Minecraft.getInstance().getWindow().handle()
        && event.key() == InputConstants.KEY_RSHIFT) {
      ci.cancel(); // Consume only Right Shift; vanilla F5 perspective handling remains untouched.
      if (action == InputConstants.PRESS
          && (Minecraft.getInstance().gui.screen() instanceof dev.discordlink.gui.MenuScreen
              || (Minecraft.getInstance().level != null && Minecraft.getInstance().gui.screen() == null)))
        GamblingMenuClient.openMenu();
    }
  }
}
