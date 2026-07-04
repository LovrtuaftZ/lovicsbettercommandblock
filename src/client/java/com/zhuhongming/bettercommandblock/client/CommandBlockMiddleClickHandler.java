package com.zhuhongming.bettercommandblock.client;

import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.zhuhongming.bettercommandblock.client.CommandBlockItemEditScreen;
import com.zhuhongming.bettercommandblock.client.CommandBlockMinecartItemEditScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CommandBlockMiddleClickHandler {

    private CommandBlockMiddleClickHandler() {}

    /**
     * Returns whether the middle-click should be consumed instead of triggering vanilla pick block.
     * When holding a command block item (without Ctrl), middle click opens the in-hand item editor.
     */
    public static boolean handleMiddleClick() {
        if (!shouldOverrideMiddleClick()) {
            return false;
        }
        openHeldItemEditor();
        return true;
    }

    public static boolean shouldBlockPickBlock() {
        return shouldOverrideMiddleClick();
    }

    private static boolean shouldOverrideMiddleClick() {
        if (Screen.hasControlDown()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || minecraft.getOverlay() != null) {
            return false;
        }
        return resolveEditedHand(player) != null;
    }

    private static void openHeldItemEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.canUseGameMasterBlocks()) {
            return;
        }

        InteractionHand hand = resolveEditedHand(player);
        if (hand == null) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (CommandBlockItemData.isMinecartItem(stack)) {
            minecraft.setScreen(new CommandBlockMinecartItemEditScreen(hand));
        } else {
            minecraft.setScreen(new CommandBlockItemEditScreen(hand));
        }
    }

    private static InteractionHand resolveEditedHand(Player player) {
        if (CommandBlockItemData.isCommandBlockItem(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (CommandBlockItemData.isCommandBlockItem(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
