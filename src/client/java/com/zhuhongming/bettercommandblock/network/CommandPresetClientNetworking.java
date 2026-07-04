package com.zhuhongming.bettercommandblock.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public final class CommandPresetClientNetworking {

    private CommandPresetClientNetworking() {}

    public static void sendGivePreset(String presetName) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        CommandPresetNetworking.GiveCommandPresetPacket.encode(
                new CommandPresetNetworking.GiveCommandPresetPacket(presetName), buf);
        ClientPlayNetworking.send(CommandPresetNetworking.GIVE_PRESET_CHANNEL, buf);
    }
}
