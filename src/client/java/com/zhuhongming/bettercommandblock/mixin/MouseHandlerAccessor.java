package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("activeButton")
    void bettercommandblock$setActiveButton(int activeButton);

    @Accessor("isLeftPressed")
    void bettercommandblock$setLeftPressed(boolean pressed);

    @Accessor("isMiddlePressed")
    void bettercommandblock$setMiddlePressed(boolean pressed);

    @Accessor("isRightPressed")
    void bettercommandblock$setRightPressed(boolean pressed);

    @Accessor("clickDepth")
    void bettercommandblock$setClickDepth(int clickDepth);
}
