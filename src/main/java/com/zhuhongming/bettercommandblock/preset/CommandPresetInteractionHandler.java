package com.zhuhongming.bettercommandblock.preset;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CommandPresetInteractionHandler {

    private CommandPresetInteractionHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register(CommandPresetInteractionHandler::onUseBlock);
    }

    private static InteractionResult onUseBlock(
            Player player,
            net.minecraft.world.level.Level world,
            InteractionHand hand,
            BlockHitResult hitResult) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (CommandPresetStacks.getPreset(heldStack).isEmpty()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();
        if (face == null) {
            return InteractionResult.PASS;
        }
        BlockHitResult useHit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        UseOnContext context = new UseOnContext(player, hand, useHit);
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        if (!(world instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockState clickedState = serverLevel.getBlockState(pos);
        if (!player.isSecondaryUseActive()) {
            InteractionResult blockUseResult = clickedState.use(serverLevel, player, hand, useHit);
            if (blockUseResult.consumesAction()) {
                return blockUseResult;
            }
        }
        if (!clickedState.canBeReplaced(placeContext)) {
            BlockPos placePos = pos.relative(face);
            if (!serverLevel.isInWorldBounds(placePos)
                    || !serverLevel.getBlockState(placePos).canBeReplaced(placeContext)) {
                return InteractionResult.PASS;
            }
        }

        InteractionResult result = CommandPresetStacks.placePresetFromStack(context);
        if (result.consumesAction()) {
            return result;
        }
        return InteractionResult.PASS;
    }
}
