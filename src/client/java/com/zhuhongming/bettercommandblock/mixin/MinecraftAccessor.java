package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("missTime")
    void bettercommandblock$setMissTime(int missTime);
}
