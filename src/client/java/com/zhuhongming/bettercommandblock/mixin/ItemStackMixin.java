package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void bettercommandblock$customCommandBlockHoverName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!CommandBlockItemData.isCommandBlockItem(stack)) {
            return;
        }
        Component displayName = CommandBlockItemData.getDisplayName(stack);
        if (displayName != null) {
            cir.setReturnValue(displayName);
        }
    }
}
