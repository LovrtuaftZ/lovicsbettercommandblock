package com.zhuhongming.bettercommandblock.preset;

import com.mojang.blaze3d.platform.InputConstants;
import com.zhuhongming.bettercommandblock.BetterCommandBlockMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class CommandPresetMenuKeyMappings {

    private static final String KEY_OPEN_PRESET_MENU = "key." + BetterCommandBlockMod.MOD_ID + ".open_preset_menu";
    private static final String CATEGORY = "key.categories." + BetterCommandBlockMod.MOD_ID;

    public static final KeyMapping OPEN_PRESET_MENU = new KeyMapping(
            KEY_OPEN_PRESET_MENU,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_BACKSLASH,
            CATEGORY);

    private CommandPresetMenuKeyMappings() {}

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_PRESET_MENU);
        ClientTickEvents.END_CLIENT_TICK.register(CommandPresetMenuKeyMappings::onClientTickEnd);
    }

    private static void onClientTickEnd(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        while (OPEN_PRESET_MENU.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new CommandPresetMenuScreen());
            }
        }
    }
}
