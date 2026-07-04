package com.zhuhongming.bettercommandblock.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public interface CommandBlockItemEditScreenAccess {

    ItemStack bettercommandblock$getEditedItemStack();

    CommandBlockEntity.Mode bettercommandblock$getEditedMode();

    boolean bettercommandblock$isEditedConditional();

    boolean bettercommandblock$isEditedAutomatic();
}
