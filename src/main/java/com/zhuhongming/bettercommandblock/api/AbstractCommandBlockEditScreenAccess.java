package com.zhuhongming.bettercommandblock.api;

import com.zhuhongming.bettercommandblock.preset.CommandPreset;
import com.zhuhongming.bettercommandblock.preset.CommandPresetExportContext;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public interface AbstractCommandBlockEditScreenAccess {

    String bettercommandblock$getCustomNameValue();

    CommandPresetExportContext bettercommandblock$buildExportContext();

    void bettercommandblock$applyExportReturnRestore();

    boolean bettercommandblock$shouldRestoreItemModeFields();

    CommandBlockEntity.Mode bettercommandblock$getSavedMode();

    boolean bettercommandblock$getSavedConditional();

    boolean bettercommandblock$getSavedAutomatic();

    void bettercommandblock$schedulePresetImport(CommandPreset preset);

    void bettercommandblock$refreshEditorFieldsFromSource();
}
