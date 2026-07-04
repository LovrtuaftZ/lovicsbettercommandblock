package com.zhuhongming.bettercommandblock.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.MultilineTextField$StringView")
public interface MultilineTextFieldStringViewAccessor {

    @Accessor("beginIndex")
    int bettercommandblock$beginIndex();

    @Accessor("endIndex")
    int bettercommandblock$endIndex();
}
