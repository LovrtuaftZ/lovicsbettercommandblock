package com.zhuhongming.bettercommandblock.mixin;

import java.util.List;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsAccessor {

    @Accessor("commandUsage")
    List<FormattedCharSequence> bettercommandblock$getCommandUsage();
}
