package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface CommandSuggestionsListAccessor {

    @Accessor("current")
    int bettercommandblock$getCurrent();

    @Accessor("current")
    void bettercommandblock$setCurrent(int current);

    @Invoker("useSuggestion")
    void bettercommandblock$invokeUseSuggestion();

    @Invoker("select")
    void bettercommandblock$invokeSelect(int index);
}
