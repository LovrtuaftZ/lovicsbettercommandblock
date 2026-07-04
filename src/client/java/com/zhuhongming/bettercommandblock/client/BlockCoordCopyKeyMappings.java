package com.zhuhongming.bettercommandblock.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BlockCoordCopyKeyMappings {

    private static boolean altCHeld;

    private BlockCoordCopyKeyMappings() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BlockCoordCopyKeyMappings::onClientTickEnd);
    }

    private static void onClientTickEnd(Minecraft minecraft) {
        if (minecraft.player == null) {
            altCHeld = false;
            return;
        }

        long window = minecraft.getWindow().getWindow();
        boolean altC = Screen.hasAltDown() && InputConstants.isKeyDown(window, InputConstants.KEY_C);

        if (altC && !altCHeld) {
            copyTargetedBlockCoords(minecraft);
        }
        altCHeld = altC;
    }

    private static void copyTargetedBlockCoords(Minecraft minecraft) {
        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        minecraft.keyboardHandler.setClipboard(pos.getX() + " " + pos.getY() + " " + pos.getZ());
        minecraft.gui.getChat().addMessage(
                Component.empty()
                        .append(Component.translatable("debug.prefix")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                        .append(CommonComponents.SPACE)
                        .append(Component.translatable("lovicsbettercommandblock.copy_block_coords.success")));
    }
}
