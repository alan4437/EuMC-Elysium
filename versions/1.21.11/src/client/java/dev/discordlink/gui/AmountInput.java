package dev.discordlink.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

public final class AmountInput extends EditBox {
  public AmountInput(int x, int y, int width, String label, String value) {
    super(Inter.editingFont(), x, y, width, 18, Inter.text(label, Inter.Weight.MEDIUM));
    setBordered(false);
    setTextShadow(false);
    setMaxLength(24);
    setTextColor(0xffe1e3eb);
    addFormatter(
        (text, index) -> FormattedCharSequence.forward(text, Inter.style(Inter.Weight.REGULAR)));
    setValue(value);
  }
}
