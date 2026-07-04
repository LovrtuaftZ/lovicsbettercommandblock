package com.zhuhongming.bettercommandblock.network;

import com.zhuhongming.bettercommandblock.BetterCommandBlockMod;
import com.zhuhongming.bettercommandblock.preset.CommandPreset;
import com.zhuhongming.bettercommandblock.preset.CommandPresetManager;
import com.zhuhongming.bettercommandblock.preset.CommandPresetStacks;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CommandPresetNetworking {

    public static final ResourceLocation GIVE_PRESET_CHANNEL =
            new ResourceLocation(BetterCommandBlockMod.MOD_ID, "give_command_preset");

    private CommandPresetNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                GIVE_PRESET_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    GiveCommandPresetPacket packet = GiveCommandPresetPacket.decode(buf);
                    server.execute(() -> handleGivePreset(player, packet));
                });
    }

    private static void handleGivePreset(ServerPlayer player, GiveCommandPresetPacket packet) {
        if (player == null || !player.canUseGameMasterBlocks()) {
            return;
        }
        Optional<CommandPreset> preset = CommandPresetManager.getInstance().findByName(packet.presetName());
        if (preset.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("lovicsbettercommandblock.preset.give.not_found", packet.presetName()), false);
            return;
        }
        ItemStack stack = CommandPresetStacks.createPresetStack(preset.get());
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack, false);
        }
    }

    public record GiveCommandPresetPacket(String presetName) {

        public static void encode(GiveCommandPresetPacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.presetName(), 128);
        }

        public static GiveCommandPresetPacket decode(FriendlyByteBuf buf) {
            return new GiveCommandPresetPacket(buf.readUtf(128));
        }
    }
}
