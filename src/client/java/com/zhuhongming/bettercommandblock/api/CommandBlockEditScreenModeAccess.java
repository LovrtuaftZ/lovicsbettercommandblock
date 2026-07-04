package com.zhuhongming.bettercommandblock.api;

import net.minecraft.world.level.block.entity.CommandBlockEntity;

public interface CommandBlockEditScreenModeAccess {

    void bettercommandblock$applyImportedModeFields(
            CommandBlockEntity.Mode mode, boolean conditional, boolean autoexec);
}
