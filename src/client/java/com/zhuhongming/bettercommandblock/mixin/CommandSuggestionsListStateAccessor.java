package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface CommandSuggestionsListStateAccessor {

    @Accessor("tabCycles")
    void bettercommandblock$setTabCycles(boolean tabCycles);
}
