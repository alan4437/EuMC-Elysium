package dev.discordlink.mixin;

import dev.discordlink.gui.MenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Increase vanilla blur only for this menu; never mutate or save global player options. */
@Mixin(Options.class)
public abstract class MenuBlurMixin {
  @Inject(method = "getMenuBackgroundBlurriness", at = @At("RETURN"), cancellable = true)
  private void gamblingmenu$glassBlur(CallbackInfoReturnable<Integer> result) {
    if (Minecraft.getInstance().screen instanceof MenuScreen && result.getReturnValue() > 0)
      result.setReturnValue(dev.discordlink.gui.GuiPreferences.blurStrength);
  }
}
