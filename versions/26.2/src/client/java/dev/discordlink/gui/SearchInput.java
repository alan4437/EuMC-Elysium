package dev.discordlink.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

/** Uses native editing, selection, clipboard, keyboard navigation and narration. */
public final class SearchInput extends EditBox {
  public SearchInput(Font font, int x, int y, int width) {
    super(font, x, y, width, 16, Inter.text("Search modules", Inter.Weight.MEDIUM));
    setBordered(false);
    setTextShadow(false);
    setMaxLength(128);
    setTextColor(0xffd7dae2);
    setHint(Inter.text("Search modules...", Inter.Weight.REGULAR)
            .withStyle(style -> style.withColor(0xff858995)));
    addFormatter(
        (text, index) -> FormattedCharSequence.forward(text, Inter.style(Inter.Weight.REGULAR)));
  }
}
