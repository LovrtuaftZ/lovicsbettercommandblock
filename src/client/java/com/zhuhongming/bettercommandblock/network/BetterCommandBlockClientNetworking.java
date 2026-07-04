package com.zhuhongming.bettercommandblock.network;

import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class BetterCommandBlockClientNetworking {

    private BetterCommandBlockClientNetworking() {}

    public static void sendCustomName(BlockPos pos, String name) {
        String payloadName = name == null ? "" : name;
        applyCustomNameLocally(pos, payloadName);

        if (!ClientPlayNetworking.canSend(BetterCommandBlockNetwork.CHANNEL)) {
            return;
        }

        FriendlyByteBuf buf = PacketByteBufs.create();
        SetCommandBlockCustomNamePacket.encode(new SetCommandBlockCustomNamePacket(pos, payloadName), buf);
        ClientPlayNetworking.send(BetterCommandBlockNetwork.CHANNEL, buf);
    }

    private static void applyCustomNameLocally(BlockPos pos, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity)) {
            return;
        }

        CommandBlockEntityNameAccessor nameAccessor = (CommandBlockEntityNameAccessor) blockEntity;
        nameAccessor.bettercommandblock$setStoredCustomName(toCustomName(name));
    }

    @Nullable
    private static Component toCustomName(String name) {
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : Component.literal(trimmed);
    }
}
