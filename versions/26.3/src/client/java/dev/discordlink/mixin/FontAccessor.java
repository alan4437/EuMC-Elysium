package dev.discordlink.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access lets the search field reuse Minecraft's live, reloadable glyph atlas. */
@Mixin(Font.class)
public interface FontAccessor {
  @Accessor("provider") Font.Provider discordlink$provider();
}
