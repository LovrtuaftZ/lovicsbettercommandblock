package com.zhuhongming.bettercommandblock.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.zhuhongming.bettercommandblock.mixin.MinecraftAccessor;
import com.zhuhongming.bettercommandblock.mixin.MouseHandlerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;

public final class MouseInputCleanup {

    private static int suppressCombatTicks;

    private MouseInputCleanup() {}

    public static void clearAllMouseState(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }

        for (int button = GLFW.GLFW_MOUSE_BUTTON_1; button <= GLFW.GLFW_MOUSE_BUTTON_3; button++) {
            KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), false);
        }

        MouseHandler mouseHandler = minecraft.mouseHandler;
        if (mouseHandler instanceof MouseHandlerAccessor accessor) {
            accessor.bettercommandblock$setActiveButton(-1);
            accessor.bettercommandblock$setLeftPressed(false);
            accessor.bettercommandblock$setMiddlePressed(false);
            accessor.bettercommandblock$setRightPressed(false);
            accessor.bettercommandblock$setClickDepth(0);
        }

        long window = minecraft.getWindow().getWindow();
        GLFW.glfwSetCursor(window, 0L);
    }

    public static void armCombatSuppression(Minecraft minecraft) {
        suppressCombatTicks = Math.max(suppressCombatTicks, 8);
        if (minecraft instanceof MinecraftAccessor accessor) {
            accessor.bettercommandblock$setMissTime(10);
        }
    }

    public static void onScreenTransition(Minecraft minecraft) {
        clearAllMouseState(minecraft);
    }

    public static void onReturningToGame(Minecraft minecraft) {
        clearAllMouseState(minecraft);
        armCombatSuppression(minecraft);
    }

    public static boolean shouldSuppressCombat() {
        return suppressCombatTicks > 0;
    }

    public static void tick() {
        if (suppressCombatTicks > 0) {
            suppressCombatTicks--;
        }
    }
}
