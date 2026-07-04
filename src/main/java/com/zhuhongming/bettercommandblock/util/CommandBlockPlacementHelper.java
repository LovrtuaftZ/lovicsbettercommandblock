package com.zhuhongming.bettercommandblock.util;

import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class CommandBlockPlacementHelper {

    private CommandBlockPlacementHelper() {}

    public static void applyItemStackToPlacedBlock(
            Level level, BlockPos pos, @Nullable Player player, ItemStack stack) {
        if (!CommandBlockItemData.isBlockItem(stack)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity commandBlockEntity)) {
            return;
        }

        CompoundTag itemData = CommandBlockItemData.getStoredDataTag(stack);
        if (itemData != null) {
            CompoundTag merged = commandBlockEntity.saveWithoutMetadata();
            CompoundTag before = merged.copy();
            merged.merge(itemData);
            if (!merged.equals(before)) {
                commandBlockEntity.load(merged);
            }
        }

        Component displayName = CommandBlockItemData.getDisplayName(stack);
        if (displayName != null) {
            ((CommandBlockEntityNameAccessor) commandBlockEntity).bettercommandblock$setStoredCustomName(displayName);
        }

        commandBlockEntity.setChanged();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket updatePacket =
                    ClientboundBlockEntityDataPacket.create(commandBlockEntity);
            if (player instanceof ServerPlayer placer) {
                placer.connection.send(updatePacket);
            }
            for (ServerPlayer trackingPlayer : PlayerLookup.tracking(serverLevel, pos)) {
                if (player == null || trackingPlayer != player) {
                    trackingPlayer.connection.send(updatePacket);
                }
            }
        }
    }
}
