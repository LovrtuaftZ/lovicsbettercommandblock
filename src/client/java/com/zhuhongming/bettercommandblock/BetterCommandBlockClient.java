package com.zhuhongming.bettercommandblock;

import com.zhuhongming.bettercommandblock.client.BlockCoordCopyKeyMappings;
import com.zhuhongming.bettercommandblock.client.CommandBlockItemTooltipHandler;
import com.zhuhongming.bettercommandblock.client.CommandBlockNameOverlay;
import com.zhuhongming.bettercommandblock.preset.CommandPresetMenuKeyMappings;
import net.fabricmc.api.ClientModInitializer;

public final class BetterCommandBlockClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CommandBlockNameOverlay.register();
        CommandBlockItemTooltipHandler.register();
        CommandPresetMenuKeyMappings.register();
        BlockCoordCopyKeyMappings.register();
    }
}
