package com.zhuhongming.bettercommandblock.api;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * Implemented by {@link com.zhuhongming.bettercommandblock.mixin.CommandBlockEntityMixin}.
 * Must live outside the mixin package so the interface can be loaded at runtime.
 */
public interface CommandBlockEntityNameAccessor {

    @Nullable
    Component bettercommandblock$getStoredCustomName();

    void bettercommandblock$setStoredCustomName(@Nullable Component name);
}
