package com.zhuhongming.bettercommandblock.client;



import net.minecraft.client.Minecraft;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.item.ItemStack;



public final class CommandBlockItemInventorySync {



    private static final int HOTBAR_SLOT_START = 36;

    private static final int OFFHAND_SLOT = 45;



    private CommandBlockItemInventorySync() {}



    public static void applyEditedStack(Minecraft minecraft, InteractionHand hand, ItemStack stack) {

        ItemStack copy = stack.copy();

        minecraft.player.setItemInHand(hand, copy);



        int slot = hand == InteractionHand.OFF_HAND

                ? OFFHAND_SLOT

                : HOTBAR_SLOT_START + minecraft.player.getInventory().selected;



        if (minecraft.getSingleplayerServer() != null) {

            ServerPlayer serverPlayer =

                    minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID());

            if (serverPlayer != null) {

                serverPlayer.setItemInHand(hand, copy.copy());

            }

            // Also route through the serverbound slot packet so placement reads the same NBT on the server.

            minecraft.player.connection.send(new ServerboundSetCreativeModeSlotPacket(slot, copy));

            return;

        }



        if (minecraft.player.getAbilities().instabuild) {

            minecraft.player.connection.send(new ServerboundSetCreativeModeSlotPacket(slot, copy));

        }

    }

}

