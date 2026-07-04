package com.zhuhongming.bettercommandblock.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Inject(method = "getUpdateTag", at = @At("HEAD"), cancellable = true)
    private void bettercommandblock$commandBlockFullUpdateTag(CallbackInfoReturnable<CompoundTag> cir) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity instanceof CommandBlockEntity commandBlockEntity) {
            cir.setReturnValue(commandBlockEntity.saveWithoutMetadata());
        }
    }
}
