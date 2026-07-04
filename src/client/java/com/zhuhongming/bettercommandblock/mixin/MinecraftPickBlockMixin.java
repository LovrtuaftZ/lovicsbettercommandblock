package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.client.CommandBlockMiddleClickHandler;
import com.zhuhongming.bettercommandblock.client.input.MouseInputCleanup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftPickBlockMixin {

    @Shadow
    public Screen screen;

    @Unique
    private Screen bettercommandblock$screenBeforeChange;

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void bettercommandblock$blockPickWhenHoldingCommandBlock(CallbackInfo ci) {
        if (CommandBlockMiddleClickHandler.shouldBlockPickBlock()) {
            ci.cancel();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void bettercommandblock$captureScreenBeforeChange(Screen screen, CallbackInfo ci) {
        this.bettercommandblock$screenBeforeChange = this.screen;
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void bettercommandblock$clearMouseKeysAfterScreenChange(Screen screen, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        MouseInputCleanup.onScreenTransition(minecraft);
        if (screen == null && this.bettercommandblock$screenBeforeChange != null) {
            MouseInputCleanup.onReturningToGame(minecraft);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bettercommandblock$tickMouseCleanup(CallbackInfo ci) {
        MouseInputCleanup.tick();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void bettercommandblock$suppressGhostStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (MouseInputCleanup.shouldSuppressCombat()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void bettercommandblock$suppressGhostContinueAttack(CallbackInfo ci) {
        if (MouseInputCleanup.shouldSuppressCombat()) {
            ci.cancel();
        }
    }
}
