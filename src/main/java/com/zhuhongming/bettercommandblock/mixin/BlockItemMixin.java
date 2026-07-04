package com.zhuhongming.bettercommandblock.mixin;



import com.zhuhongming.bettercommandblock.util.CommandBlockPlacementHelper;

import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.BlockItem;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



@Mixin(BlockItem.class)

public class BlockItemMixin {



    @Inject(method = "updateCustomBlockEntityTag", at = @At("RETURN"))

    private void bettercommandblock$applyCommandBlockItemData(

            BlockPos pos,

            Level level,

            @Nullable Player player,

            ItemStack stack,

            BlockState state,

            CallbackInfoReturnable<Boolean> cir) {

        CommandBlockPlacementHelper.applyItemStackToPlacedBlock(level, pos, player, stack);

    }

}

