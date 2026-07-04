package com.zhuhongming.bettercommandblock.util;

import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class CommandBlockCustomNamePersistence {

    private CommandBlockCustomNamePersistence() {}

    public static void writeToTag(CompoundTag tag, @Nullable Component name) {
        CommandBlockItemData.writeDisplayNameToTag(tag, name);
    }

    public static void readIntoEntity(CommandBlockEntity entity, CompoundTag tag) {
        if (!CommandBlockItemData.hasDisplayNameKey(tag)) {
            return;
        }
        Component parsed = CommandBlockItemData.parseBlockEntityDisplayName(tag);
        if (parsed == null) {
            parsed = CommandBlockItemData.parseCustomDisplayName(tag);
        }
        CommandBlockEntityNameAccessor accessor = (CommandBlockEntityNameAccessor) entity;
        accessor.bettercommandblock$setStoredCustomName(parsed);
    }
}
