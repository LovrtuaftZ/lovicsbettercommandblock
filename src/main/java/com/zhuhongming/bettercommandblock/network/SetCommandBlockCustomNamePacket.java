package com.zhuhongming.bettercommandblock.network;

import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public record SetCommandBlockCustomNamePacket(BlockPos pos, String name) {

    public static void encode(SetCommandBlockCustomNamePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.name, 128);
    }

    public static SetCommandBlockCustomNamePacket decode(FriendlyByteBuf buf) {
        return new SetCommandBlockCustomNamePacket(buf.readBlockPos(), buf.readUtf(128));
    }

    public static void handleOnServer(SetCommandBlockCustomNamePacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }

        Level level = player.level();
        if (!level.isLoaded(packet.pos)) {
            return;
        }
        if (!player.canUseGameMasterBlocks()) {
            return;
        }
        if (player.blockPosition().distSqr(packet.pos) > 64.0D * 64.0D) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(packet.pos);
        if (!(blockEntity instanceof CommandBlockEntity commandBlockEntity)) {
            return;
        }

        CommandBlockEntityNameAccessor nameAccessor = (CommandBlockEntityNameAccessor) commandBlockEntity;
        String trimmed = packet.name.trim();
        if (trimmed.isEmpty()) {
            nameAccessor.bettercommandblock$setStoredCustomName(null);
        } else {
            nameAccessor.bettercommandblock$setStoredCustomName(Component.literal(trimmed));
        }
        commandBlockEntity.setChanged();
        if (level instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket updatePacket =
                    ClientboundBlockEntityDataPacket.create(commandBlockEntity);
            for (ServerPlayer trackingPlayer : PlayerLookup.tracking(serverLevel, packet.pos)) {
                trackingPlayer.connection.send(updatePacket);
            }
        }
    }
}
