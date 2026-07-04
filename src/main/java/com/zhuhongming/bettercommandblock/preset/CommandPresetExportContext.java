package com.zhuhongming.bettercommandblock.preset;

public record CommandPresetExportContext(
        String name,
        String command,
        String blockType,
        boolean conditional,
        boolean alwaysActive) {

    public static CommandPresetExportContext empty() {
        return new CommandPresetExportContext("", "", "pulse", false, false);
    }
}
