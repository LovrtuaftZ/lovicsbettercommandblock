package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.api.CommandBlockEditScreenModeAccess;
import com.zhuhongming.bettercommandblock.client.CommandBlockScreenLayouts;
import com.zhuhongming.bettercommandblock.network.BetterCommandBlockClientNetworking;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Unique;

@Mixin(CommandBlockEditScreen.class)
public abstract class CommandBlockEditScreenMixin implements CommandBlockEditScreenModeAccess {

    @Shadow
    private CycleButton<CommandBlockEntity.Mode> modeButton;

    @Shadow
    private CycleButton<Boolean> conditionalButton;

    @Shadow
    private CycleButton<Boolean> autoexecButton;

    @Inject(method = "populateAndSendPacket", at = @At("TAIL"))
    private void betterCommandBlock$sendCustomName(BaseCommandBlock commandBlock, CallbackInfo ci) {
        String customName = ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$getCustomNameValue();
        CommandBlockEntity commandBlockEntity =
                ((CommandBlockEditScreenAccessor) this).bettercommandblock$getCommandBlockEntity();
        BetterCommandBlockClientNetworking.sendCustomName(commandBlockEntity.getBlockPos(), customName);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void betterCommandBlock$relayoutButtons(CallbackInfo ci) {
        CommandBlockScreenLayouts.relayoutModeButtons(
                (ScreenAccessor) this, this.modeButton, this.conditionalButton, this.autoexecButton);
    }

    @Inject(method = "updateGui", at = @At("TAIL"))
    private void betterCommandBlock$syncCustomFieldsAfterUpdateGui(CallbackInfo ci) {
        ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$refreshEditorFieldsFromSource();
    }

    @Override
    @Unique
    public void bettercommandblock$applyImportedModeFields(
            CommandBlockEntity.Mode mode, boolean conditional, boolean autoexec) {
        if (this.modeButton != null) {
            this.modeButton.setValue(mode);
        }
        if (this.conditionalButton != null) {
            this.conditionalButton.setValue(conditional);
        }
        if (this.autoexecButton != null) {
            this.autoexecButton.setValue(autoexec);
        }
    }
}
