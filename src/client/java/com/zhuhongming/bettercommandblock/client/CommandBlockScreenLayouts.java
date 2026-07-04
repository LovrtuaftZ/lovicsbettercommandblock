package com.zhuhongming.bettercommandblock.client;

import com.zhuhongming.bettercommandblock.mixin.ScreenAccessor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public final class CommandBlockScreenLayouts {

    private CommandBlockScreenLayouts() {}

    public static void relayoutModeButtons(
            ScreenAccessor accessor,
            CycleButton<CommandBlockEntity.Mode> modeButton,
            CycleButton<Boolean> conditionalButton,
            CycleButton<Boolean> autoexecButton) {
        int centerX = accessor.bettercommandblock$getWidth() / 2;
        int panelY = (accessor.bettercommandblock$getHeight() - 268) / 2;
        int y = panelY + 210;
        modeButton.setX(centerX - 152);
        modeButton.setY(y);
        modeButton.setWidth(96);
        modeButton.setAlpha(0.0F);

        conditionalButton.setX(centerX - 48);
        conditionalButton.setY(y);
        conditionalButton.setWidth(96);
        conditionalButton.setAlpha(0.0F);

        autoexecButton.setX(centerX + 56);
        autoexecButton.setY(y);
        autoexecButton.setWidth(96);
        autoexecButton.setAlpha(0.0F);
    }
}
