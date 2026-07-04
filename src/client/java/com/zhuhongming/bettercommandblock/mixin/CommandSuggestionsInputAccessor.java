package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsInputAccessor {

    @Accessor("input")
    EditBox bettercommandblock$getInput();

    @Accessor("keepSuggestions")
    void bettercommandblock$setKeepSuggestions(boolean keepSuggestions);
}
