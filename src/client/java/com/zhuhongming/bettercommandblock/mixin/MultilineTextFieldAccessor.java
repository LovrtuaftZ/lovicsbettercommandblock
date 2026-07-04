package com.zhuhongming.bettercommandblock.mixin;

import java.util.List;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {

    @Accessor("displayLines")
    List<?> bettercommandblock$getDisplayLines();

    @Accessor("cursor")
    int bettercommandblock$getCursor();

    @Accessor("cursor")
    @Mutable
    void bettercommandblock$setCursor(int cursor);

    @Accessor("selectCursor")
    int bettercommandblock$getSelectCursor();

    @Accessor("selectCursor")
    @Mutable
    void bettercommandblock$setSelectCursor(int selectCursor);
}
