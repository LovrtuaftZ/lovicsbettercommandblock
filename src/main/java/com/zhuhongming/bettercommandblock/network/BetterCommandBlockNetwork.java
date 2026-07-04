package com.zhuhongming.bettercommandblock.network;

import com.zhuhongming.bettercommandblock.BetterCommandBlockMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public final class BetterCommandBlockNetwork {

    public static final ResourceLocation CHANNEL =
            new ResourceLocation(BetterCommandBlockMod.MOD_ID, "set_custom_name");

    private BetterCommandBlockNetwork() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    SetCommandBlockCustomNamePacket packet = SetCommandBlockCustomNamePacket.decode(buf);
                    server.execute(() -> SetCommandBlockCustomNamePacket.handleOnServer(packet, player));
                });
    }
}
