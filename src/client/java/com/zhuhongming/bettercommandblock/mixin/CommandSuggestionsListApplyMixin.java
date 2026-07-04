package com.zhuhongming.bettercommandblock.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import com.zhuhongming.bettercommandblock.client.command.CommandArgumentSpan;
import java.util.List;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.SuggestionsList.class)
public abstract class CommandSuggestionsListApplyMixin {
    @Shadow
    @Final
    net.minecraft.client.gui.components.CommandSuggestions field_21615;

    @Shadow
    private int current;

    @Shadow
    @Final
    private List<Suggestion> suggestionList;

    @Inject(method = "useSuggestion", at = @At("HEAD"), cancellable = true)
    private void betterCommandBlock$replaceEntireToken(CallbackInfo ci) {
        EditBox input = ((CommandSuggestionsInputAccessor) this.field_21615).bettercommandblock$getInput();
        if (input.isVisible()) {
            return;
        }
        if (this.current < 0 || this.current >= this.suggestionList.size()) {
            return;
        }

        Suggestion suggestion = this.suggestionList.get(this.current);
        String text = input.getValue();
        int cursor = input.getCursorPosition();
        CommandArgumentSpan.Range tokenRange = CommandArgumentSpan.atCursor(text, cursor);
        CommandArgumentSpan.Range replaceRange = tokenRange;
        if (replaceRange.length() == 0) {
            int start = Math.max(0, Math.min(suggestion.getRange().getStart(), text.length()));
            int end = Math.max(start, Math.min(suggestion.getRange().getEnd(), text.length()));
            replaceRange = new CommandArgumentSpan.Range(start, end);
        }

        String replacement = suggestion.getText();
        String newText = text.substring(0, replaceRange.start()) + replacement + text.substring(replaceRange.end());
        int newCursor = replaceRange.start() + replacement.length();

        input.setValue(newText);
        input.setCursorPosition(newCursor);
        input.setHighlightPos(newCursor);

        CommandSuggestionsInputAccessor outer = (CommandSuggestionsInputAccessor) this.field_21615;
        outer.bettercommandblock$setKeepSuggestions(false);
        ((CommandSuggestionsListStateAccessor) this).bettercommandblock$setTabCycles(true);
        ((CommandSuggestionsListAccessor) this).bettercommandblock$invokeSelect(this.current);
        ci.cancel();
    }
}
