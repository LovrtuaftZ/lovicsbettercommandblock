package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.client.CommandBlockItemEditScreen;
import com.zhuhongming.bettercommandblock.client.CommandBlockScreenLayouts;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockItemEditScreen.class)
public abstract class CommandBlockItemEditScreenMixin {

    @Shadow
    private CycleButton<CommandBlockEntity.Mode> modeButton;

    @Shadow
    private CycleButton<Boolean> conditionalButton;

    @Shadow
    private CycleButton<Boolean> autoexecButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void betterCommandBlock$relayoutButtons(CallbackInfo ci) {
        AbstractCommandBlockEditScreenAccess editor = (AbstractCommandBlockEditScreenAccess) this;
        CommandBlockItemEditScreen itemScreen = (CommandBlockItemEditScreen) (Object) this;
        if (editor.bettercommandblock$shouldRestoreItemModeFields()) {
            itemScreen.bettercommandblock$restoreEditorMode(
                    editor.bettercommandblock$getSavedMode(),
                    editor.bettercommandblock$getSavedConditional(),
                    editor.bettercommandblock$getSavedAutomatic());
            if (this.modeButton != null) {
                this.modeButton.setValue(editor.bettercommandblock$getSavedMode());
            }
            if (this.conditionalButton != null) {
                this.conditionalButton.setValue(editor.bettercommandblock$getSavedConditional());
            }
            if (this.autoexecButton != null) {
                this.autoexecButton.setValue(editor.bettercommandblock$getSavedAutomatic());
            }
        }
        editor.bettercommandblock$applyExportReturnRestore();
        CommandBlockScreenLayouts.relayoutModeButtons(
                (ScreenAccessor) this, this.modeButton, this.conditionalButton, this.autoexecButton);
    }
}
