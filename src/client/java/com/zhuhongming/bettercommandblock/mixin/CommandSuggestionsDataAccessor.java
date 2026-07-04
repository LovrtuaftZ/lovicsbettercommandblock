package com.zhuhongming.bettercommandblock.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsDataAccessor {

    @Accessor("pendingSuggestions")
    @Nullable
    CompletableFuture<Suggestions> bettercommandblock$getPendingSuggestions();
}
