package com.zhuhongming.bettercommandblock.mixin;

import com.mojang.brigadier.ParseResults;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsParseAccessor {

    @Accessor("currentParse")
    @Nullable
    ParseResults<SharedSuggestionProvider> bettercommandblock$getCurrentParse();
}
