package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.client.CommandBlockMinecartItemEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockMinecartItemEditScreen.class)
public abstract class CommandBlockMinecartItemEditScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void betterCommandBlock$restoreAfterExport(CallbackInfo ci) {
        ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$applyExportReturnRestore();
    }
}
