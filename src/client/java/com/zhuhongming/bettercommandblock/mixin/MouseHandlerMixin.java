package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.client.CommandBlockMiddleClickHandler;
import com.zhuhongming.bettercommandblock.client.input.MouseInputCleanup;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private static final int MODIFIER_MASK =
            GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER;

    @Unique
    private int bettercommandblock$currentModifiers;

    @Inject(method = "onPress", at = @At("HEAD"))
    private void bettercommandblock$captureModifiers(long window, int button, int action, int modifiers, CallbackInfo ci) {
        this.bettercommandblock$currentModifiers = modifiers;
    }

    @Redirect(
            method = "onPress",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"))
    private void bettercommandblock$redirectMiddleClick(InputConstants.Key key) {
        if (key.getValue() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && (this.bettercommandblock$currentModifiers & MODIFIER_MASK) == 0
                && CommandBlockMiddleClickHandler.handleMiddleClick()) {
            return;
        }
        KeyMapping.click(key);
    }

    @Inject(method = "grabMouse()V", at = @At("HEAD"))
    private void bettercommandblock$beforeGrabMouse(CallbackInfo ci) {
        MouseInputCleanup.onReturningToGame(this.minecraft);
    }

    @Inject(method = "releaseMouse()V", at = @At("TAIL"))
    private void bettercommandblock$afterReleaseMouse(CallbackInfo ci) {
        MouseInputCleanup.clearAllMouseState(this.minecraft);
    }
}
