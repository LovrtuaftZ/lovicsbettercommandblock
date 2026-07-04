package com.zhuhongming.bettercommandblock;

import com.mojang.logging.LogUtils;
import com.zhuhongming.bettercommandblock.network.BetterCommandBlockNetwork;
import com.zhuhongming.bettercommandblock.network.CommandPresetNetworking;
import com.zhuhongming.bettercommandblock.preset.CommandPresetInteractionHandler;
import com.zhuhongming.bettercommandblock.preset.CommandPresetManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public final class BetterCommandBlockMod implements ModInitializer {

    public static final String MOD_ID = "lovicsbettercommandblock";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        BetterCommandBlockNetwork.register();
        CommandPresetNetworking.register();
        CommandPresetInteractionHandler.register();
        CommandPresetManager.getInstance().reloadAllPresets();
        LOGGER.info("{} (Fabric) loaded", MOD_ID);
    }
}
