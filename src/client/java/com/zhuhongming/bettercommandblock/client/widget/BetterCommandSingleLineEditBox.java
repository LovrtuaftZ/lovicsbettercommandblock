package com.zhuhongming.bettercommandblock.client.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Single-line variant of {@link BetterCommandMultiLineEditBox} with the same visual style.
 */
public class BetterCommandSingleLineEditBox extends BetterCommandMultiLineEditBox {

    public BetterCommandSingleLineEditBox(
            Minecraft minecraft,
            Font font,
            int x,
            int y,
            int width,
            Component placeholder,
            Component narration) {
        super(minecraft, font, x, y, width, 18, placeholder, narration);
        this.setCharacterLimit(128);
    }

    @Override
    public int getInnerHeight() {
        return 9;
    }

    @Override
    protected boolean scrollbarVisible() {
        return false;
    }

    @Override
    protected void refreshSyntaxState(String command) {
        // Name field uses plain white text, not command syntax highlighting.
    }

    @Override
    protected Style plainLiteralStyle() {
        return Style.EMPTY.withColor(ChatFormatting.WHITE);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == '\n' || codePoint == '\r') {
            return false;
        }
        return super.charTyped(codePoint, modifiers);
    }
}
