package com.zhuhongming.bettercommandblock.mixin;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsStateAccessor {

    @Accessor("suggestions")
    @Nullable
    CommandSuggestions.SuggestionsList bettercommandblock$getSuggestionsList();
}
