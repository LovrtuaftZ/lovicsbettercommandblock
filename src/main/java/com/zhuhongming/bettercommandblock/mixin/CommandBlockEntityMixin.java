package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import com.zhuhongming.bettercommandblock.util.CommandBlockCustomNamePersistence;
import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

@Mixin(CommandBlockEntity.class)
public abstract class CommandBlockEntityMixin implements CommandBlockEntityNameAccessor {

    @Unique
    @Nullable
    private Component bettercommandblock$customName;

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void bettercommandblock$saveCustomName(CompoundTag tag, CallbackInfo ci) {
        CommandBlockCustomNamePersistence.writeToTag(tag, this.bettercommandblock$customName);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void bettercommandblock$loadCustomName(CompoundTag tag, CallbackInfo ci) {
        CommandBlockCustomNamePersistence.readIntoEntity((CommandBlockEntity) (Object) this, tag);
    }

    @Override
    @Nullable
    public Component bettercommandblock$getStoredCustomName() {
        return this.bettercommandblock$customName;
    }

    @Override
    public void bettercommandblock$setStoredCustomName(@Nullable Component name) {
        this.bettercommandblock$customName = CommandBlockItemData.normalizeCustomDisplayName(name);
        CommandBlockEntity self = (CommandBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide()) {
            self.setChanged();
        }
    }
}
