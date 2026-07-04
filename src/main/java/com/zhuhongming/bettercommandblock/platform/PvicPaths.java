package com.zhuhongming.bettercommandblock.platform;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Shared with VicColor: {@code config/viccolor/}. */
public final class PvicPaths {

    private PvicPaths() {}

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("viccolor");
    }
}
