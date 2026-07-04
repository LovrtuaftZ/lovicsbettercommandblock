package com.zhuhongming.bettercommandblock.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class CommandBlockItemData {

    public static final String DISPLAY_NAME_KEY = "LBCBDisplayName";
    private static final String LEGACY_BCB_DISPLAY_NAME_KEY = "BCBDisplayName";
    private static final String LEGACY_DISPLAY_NAME_KEY = "CustomName";
    private static final String DEFAULT_COMMAND_SOURCE_NAME = "@";

    private CommandBlockItemData() {}

    public static boolean isCommandBlockItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == Items.COMMAND_BLOCK
                || item == Items.CHAIN_COMMAND_BLOCK
                || item == Items.REPEATING_COMMAND_BLOCK
                || item == Items.COMMAND_BLOCK_MINECART;
    }

    @Nullable
    public static CompoundTag getStoredDataTag(ItemStack stack) {
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag != null) {
            return blockEntityTag;
        }
        return stack.getTagElement("EntityTag");
    }

    @Nullable
    public static Component getDisplayName(ItemStack stack) {
        CompoundTag dataTag = getStoredDataTag(stack);
        if (dataTag == null) {
            return null;
        }
        return parseCustomDisplayName(dataTag);
    }

    @Nullable
    public static Component parseCustomDisplayName(@Nullable CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        if (tag.contains(DISPLAY_NAME_KEY, 8)) {
            return parseStoredDisplayNameString(tag.getString(DISPLAY_NAME_KEY));
        }
        if (tag.contains(LEGACY_BCB_DISPLAY_NAME_KEY, 8)) {
            return parseStoredDisplayNameString(tag.getString(LEGACY_BCB_DISPLAY_NAME_KEY));
        }
        if (tag.contains(LEGACY_DISPLAY_NAME_KEY, 8)) {
            return parseStoredDisplayNameString(tag.getString(LEGACY_DISPLAY_NAME_KEY));
        }
        return null;
    }

    @Nullable
    public static Component parseBlockEntityDisplayName(CompoundTag tag) {
        if (tag.contains(DISPLAY_NAME_KEY, 8)) {
            return parseStoredDisplayNameString(tag.getString(DISPLAY_NAME_KEY));
        }
        if (tag.contains(LEGACY_BCB_DISPLAY_NAME_KEY, 8)) {
            return parseStoredDisplayNameString(tag.getString(LEGACY_BCB_DISPLAY_NAME_KEY));
        }
        return null;
    }

    @Nullable
    private static Component parseStoredDisplayNameString(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        if (raw.startsWith("{")) {
            return normalizeCustomDisplayName(Component.Serializer.fromJson(raw));
        }
        return normalizeCustomDisplayName(Component.literal(raw));
    }

    public static boolean hasDisplayNameKey(CompoundTag tag) {
        return tag.contains(DISPLAY_NAME_KEY, 8)
                || tag.contains(LEGACY_BCB_DISPLAY_NAME_KEY, 8)
                || tag.contains(LEGACY_DISPLAY_NAME_KEY, 8);
    }

    public static void writeDisplayNameToTag(CompoundTag tag, @Nullable Component name) {
        if (name == null) {
            tag.remove(DISPLAY_NAME_KEY);
            tag.remove(LEGACY_BCB_DISPLAY_NAME_KEY);
            return;
        }
        tag.putString(DISPLAY_NAME_KEY, name.getString());
        tag.remove(LEGACY_BCB_DISPLAY_NAME_KEY);
    }

    @Nullable
    public static Component normalizeCustomDisplayName(@Nullable Component name) {
        if (name == null || name.getString().isBlank() || DEFAULT_COMMAND_SOURCE_NAME.equals(name.getString())) {
            return null;
        }
        return name;
    }

    public static String getCommand(ItemStack stack) {
        CompoundTag dataTag = getStoredDataTag(stack);
        if (dataTag == null || !dataTag.contains("Command", 8)) {
            return "";
        }
        return dataTag.getString("Command");
    }

    public static boolean hasStoredData(ItemStack stack) {
        CompoundTag dataTag = getStoredDataTag(stack);
        return dataTag != null && dataTag.contains("Command", 8);
    }

    public static CommandBlockEntity.Mode getMode(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.CHAIN_COMMAND_BLOCK) {
            return CommandBlockEntity.Mode.SEQUENCE;
        }
        if (item == Items.REPEATING_COMMAND_BLOCK) {
            return CommandBlockEntity.Mode.AUTO;
        }
        return CommandBlockEntity.Mode.REDSTONE;
    }

    public static boolean isConditional(ItemStack stack) {
        CompoundTag blockStateTag = stack.getTagElement("BlockStateTag");
        if (blockStateTag == null || !blockStateTag.contains("conditional", 8)) {
            return false;
        }
        return Boolean.parseBoolean(blockStateTag.getString("conditional"));
    }

    public static boolean isAutomatic(ItemStack stack) {
        CompoundTag dataTag = getStoredDataTag(stack);
        if (dataTag == null || !dataTag.contains("auto", 1)) {
            return false;
        }
        return dataTag.getBoolean("auto");
    }

    public static boolean tracksOutput(ItemStack stack) {
        CompoundTag dataTag = getStoredDataTag(stack);
        if (dataTag == null || !dataTag.contains("TrackOutput", 1)) {
            return true;
        }
        return dataTag.getBoolean("TrackOutput");
    }

    public static boolean needsRedstone(ItemStack stack) {
        return switch (getMode(stack)) {
            case REDSTONE -> true;
            case SEQUENCE -> false;
            case AUTO -> !isAutomatic(stack);
        };
    }

    public static boolean isMinecartItem(ItemStack stack) {
        return stack.is(Items.COMMAND_BLOCK_MINECART);
    }

    public static boolean isBlockItem(ItemStack stack) {
        return isCommandBlockItem(stack) && !isMinecartItem(stack);
    }

    public static CompoundTag getOrCreateStoredDataTag(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTag();
        String tagKey = isMinecartItem(stack) ? "EntityTag" : "BlockEntityTag";
        CompoundTag dataTag = root.getCompound(tagKey);
        root.put(tagKey, dataTag);
        return dataTag;
    }

    public static Item itemForMode(CommandBlockEntity.Mode mode) {
        return switch (mode) {
            case SEQUENCE -> Items.CHAIN_COMMAND_BLOCK;
            case AUTO -> Items.REPEATING_COMMAND_BLOCK;
            case REDSTONE -> Items.COMMAND_BLOCK;
        };
    }

    public static void loadIntoCommandBlock(ItemStack stack, BaseCommandBlock commandBlock) {
        CompoundTag dataTag = getStoredDataTag(stack);
        if (dataTag != null) {
            commandBlock.load(dataTag);
        }
    }

    public static ItemStack applyEditorState(
            ItemStack stack,
            BaseCommandBlock commandBlock,
            @Nullable CommandBlockEntity.Mode mode,
            boolean conditional,
            boolean autoexec,
            @Nullable String displayName) {
        ItemStack edited = stack.copy();
        CompoundTag dataTag = getOrCreateStoredDataTag(edited);
        commandBlock.save(dataTag);
        applyDisplayName(dataTag, displayName);

        if (isBlockItem(edited)) {
            dataTag.putBoolean("auto", autoexec);
            CompoundTag root = edited.getOrCreateTag();
            CompoundTag blockStateTag = root.getCompound("BlockStateTag");
            blockStateTag.putString("conditional", Boolean.toString(conditional));
            root.put("BlockStateTag", blockStateTag);
            if (mode != null) {
                Item targetItem = itemForMode(mode);
                if (edited.getItem() != targetItem) {
                    edited = new ItemStack(targetItem, edited.getCount());
                    edited.setTag(root);
                }
            }
        }

        return edited;
    }

    public static void applyDisplayName(CompoundTag dataTag, @Nullable String displayName) {
        if (displayName == null || displayName.isBlank()) {
            dataTag.remove(DISPLAY_NAME_KEY);
            dataTag.remove(LEGACY_BCB_DISPLAY_NAME_KEY);
            dataTag.remove(LEGACY_DISPLAY_NAME_KEY);
            return;
        }
        dataTag.putString(DISPLAY_NAME_KEY, displayName.trim());
        dataTag.remove(LEGACY_BCB_DISPLAY_NAME_KEY);
        dataTag.remove(LEGACY_DISPLAY_NAME_KEY);
    }
}
