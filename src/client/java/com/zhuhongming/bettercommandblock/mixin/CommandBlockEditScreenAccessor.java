package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;

@Mixin(CommandBlockEditScreen.class)
public interface CommandBlockEditScreenAccessor {

    @Accessor("autoCommandBlock")
    CommandBlockEntity bettercommandblock$getCommandBlockEntity();
}
