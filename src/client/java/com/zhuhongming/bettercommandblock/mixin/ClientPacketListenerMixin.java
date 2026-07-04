package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "handleBlockEntityData", at = @At("TAIL"))
    private void bettercommandblock$applyCommandBlockDisplayNameFromPacket(
            ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        if (this.minecraft.level == null) {
            return;
        }

        BlockPos pos = packet.getPos();
        BlockEntity blockEntity = this.minecraft.level.getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity)) {
            return;
        }

        CompoundTag tag = packet.getTag();
        if (tag == null) {
            return;
        }

        Component displayName = CommandBlockItemData.parseBlockEntityDisplayName(tag);
        if (displayName == null) {
            displayName = CommandBlockItemData.parseCustomDisplayName(tag);
        }
        if (displayName != null) {
            ((CommandBlockEntityNameAccessor) blockEntity).bettercommandblock$setStoredCustomName(displayName);
        }

        if (this.minecraft.screen instanceof CommandBlockEditScreen commandBlockEditScreen
                && this.minecraft.screen instanceof AbstractCommandBlockEditScreenAccess screenAccess) {
            CommandBlockEntity openEntity =
                    ((CommandBlockEditScreenAccessor) commandBlockEditScreen).bettercommandblock$getCommandBlockEntity();
            if (openEntity.getBlockPos().equals(pos)) {
                screenAccess.bettercommandblock$refreshEditorFieldsFromSource();
            }
        }
    }
}
