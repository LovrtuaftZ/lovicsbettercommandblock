package com.zhuhongming.bettercommandblock.preset;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public final class CommandPresetStacks {

    public static final String PRESET_NAME_TAG = "LBCBCommandPresetName";
    public static final String PRESET_MARKER_TAG = "LBCBCommandPresetItem";
    private static final String LEGACY_PRESET_NAME_TAG = "BCBCommandPresetName";
    private static final String LEGACY_PRESET_MARKER_TAG = "BCBCommandPresetItem";

    private CommandPresetStacks() {}

    public static ItemStack createPresetStack(CommandPreset preset) {
        ItemStack stack = createIconBackedStack(preset);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(PRESET_NAME_TAG, preset.getName());
        tag.putBoolean(PRESET_MARKER_TAG, true);
        stack.setHoverName(Component.literal(preset.getName()));
        applyGlintAndLore(stack);
        applyCommandBlockItemNbt(stack, preset);
        return stack;
    }

    public static Optional<CommandPreset> getPreset(ItemStack stack) {
        if (!stack.hasTag()) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        String presetName = null;
        boolean marked = false;
        if (tag.contains(PRESET_NAME_TAG, 8) && tag.getBoolean(PRESET_MARKER_TAG)) {
            presetName = tag.getString(PRESET_NAME_TAG);
            marked = true;
        } else if (tag.contains(LEGACY_PRESET_NAME_TAG, 8) && tag.getBoolean(LEGACY_PRESET_MARKER_TAG)) {
            presetName = tag.getString(LEGACY_PRESET_NAME_TAG);
            marked = true;
        }
        if (!marked || presetName == null || presetName.isBlank()) {
            return Optional.empty();
        }
        return CommandPresetManager.getInstance().findByName(presetName);
    }

    public static InteractionResult placePresetFromStack(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack heldStack = context.getItemInHand();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Optional<CommandPreset> optionalPreset = getPreset(heldStack);
        if (optionalPreset.isEmpty()) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = level.getBlockState(clickedPos).canBeReplaced(placeContext)
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());

        if (player != null && !player.mayUseItemAt(placePos, context.getClickedFace(), heldStack)) {
            return InteractionResult.FAIL;
        }
        if (!level.getWorldBorder().isWithinBounds(placePos)) {
            return InteractionResult.FAIL;
        }
        if (player != null && !level.mayInteract(player, placePos)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(placePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }

        CommandPreset preset = optionalPreset.get();
        BlockState commandBlockState = createCommandBlockState(
                preset.getBlockType(), context.getHorizontalDirection(), preset.isConditional());
        if (commandBlockState == null) {
            return InteractionResult.FAIL;
        }
        CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        if (!level.isUnobstructed(commandBlockState, placePos, collisionContext)) {
            return InteractionResult.FAIL;
        }
        if (!commandBlockState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (!level.setBlock(placePos, commandBlockState, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getBlockEntity(placePos) instanceof CommandBlockEntity commandBlockEntity) {
                commandBlockEntity.setAutomatic(preset.isAlwaysActive());
                commandBlockEntity.getCommandBlock().setCommand(preset.getCommand());
                if (!preset.getName().isBlank()) {
                    ((CommandBlockEntityNameAccessor) commandBlockEntity)
                            .bettercommandblock$setStoredCustomName(Component.literal(preset.getName()));
                }
                commandBlockEntity.setChanged();
                serverLevel.sendBlockUpdated(placePos, commandBlockState, commandBlockState, Block.UPDATE_ALL);
            }
        }

        if (player == null || !player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void applyCommandBlockItemNbt(ItemStack stack, CommandPreset preset) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag blockEntityTag = root.getCompound("BlockEntityTag");
        blockEntityTag.putString("Command", preset.getCommand());
        blockEntityTag.putBoolean("auto", preset.isAlwaysActive());
        CommandBlockItemData.applyDisplayName(blockEntityTag, preset.getName());
        root.put("BlockEntityTag", blockEntityTag);

        CompoundTag blockStateTag = root.getCompound("BlockStateTag");
        blockStateTag.putString("conditional", Boolean.toString(preset.isConditional()));
        root.put("BlockStateTag", blockStateTag);
    }

    @Nullable
    private static BlockState createCommandBlockState(String blockType, Direction playerFacing, boolean conditional) {
        Block block = switch (blockType) {
            case "repeat" -> Blocks.REPEATING_COMMAND_BLOCK;
            case "chain" -> Blocks.CHAIN_COMMAND_BLOCK;
            case "pulse" -> Blocks.COMMAND_BLOCK;
            default -> null;
        };
        if (block == null) {
            return null;
        }
        BlockState baseState = block.defaultBlockState();
        if (baseState.hasProperty(CommandBlock.FACING)) {
            baseState = baseState.setValue(CommandBlock.FACING, playerFacing.getOpposite());
        }
        if (baseState.hasProperty(CommandBlock.CONDITIONAL)) {
            baseState = baseState.setValue(CommandBlock.CONDITIONAL, conditional);
        }
        return baseState;
    }

    private static ItemStack createIconBackedStack(CommandPreset preset) {
        ResourceLocation iconId = ResourceLocation.tryParse(preset.getIcon());
        if (iconId == null) {
            return new ItemStack(Items.COMMAND_BLOCK);
        }
        Item iconItem = BuiltInRegistries.ITEM.get(iconId);
        if (iconItem == null || iconItem == Items.AIR) {
            return new ItemStack(Items.COMMAND_BLOCK);
        }
        return new ItemStack(iconItem);
    }

    private static void applyGlintAndLore(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag enchantments = new ListTag();
        CompoundTag enchant = new CompoundTag();
        enchant.putString("id", "minecraft:unbreaking");
        enchant.putShort("lvl", (short) 1);
        enchantments.add(enchant);
        tag.put("Enchantments", enchantments);
        tag.putInt("HideFlags", tag.getInt("HideFlags") | 1);

        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.translatable("lovicsbettercommandblock.preset.item.lore").withStyle(ChatFormatting.GRAY))));
        display.put("Lore", lore);
    }
}
