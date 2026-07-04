package com.zhuhongming.bettercommandblock.client;

import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public final class CommandBlockItemTooltipHandler {

    private static final int DETAIL_COLOR = 0xAAAAAA;

    private CommandBlockItemTooltipHandler() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> appendTooltip(stack, lines));
    }

    private static void appendTooltip(ItemStack stack, java.util.List<Component> lines) {
        if (!CommandBlockItemData.isCommandBlockItem(stack) || !CommandBlockItemData.hasStoredData(stack)) {
            return;
        }

        if (!Screen.hasControlDown()) {
            lines.add(Component.translatable("lovicsbettercommandblock.item.tooltip.hold_ctrl")
                    .withStyle(style -> style.withColor(DETAIL_COLOR)));
            return;
        }

        String command = CommandBlockItemData.getCommand(stack);
        lines.add(Component.translatable("lovicsbettercommandblock.item.tooltip.command", command.isEmpty() ? "-" : command));

        lines.add(Component.translatable(
                "lovicsbettercommandblock.item.tooltip.mode",
                Component.translatable(switch (CommandBlockItemData.getMode(stack)) {
                    case REDSTONE -> "lovicsbettercommandblock.item.tooltip.mode.impulse";
                    case SEQUENCE -> "lovicsbettercommandblock.item.tooltip.mode.chain";
                    case AUTO -> "lovicsbettercommandblock.item.tooltip.mode.repeat";
                })));

        lines.add(Component.translatable(
                "lovicsbettercommandblock.item.tooltip.conditional",
                Component.translatable(CommandBlockItemData.isConditional(stack)
                        ? "lovicsbettercommandblock.item.tooltip.yes"
                        : "lovicsbettercommandblock.item.tooltip.no")));

        Component redstoneText = switch (CommandBlockItemData.getMode(stack)) {
            case REDSTONE -> Component.translatable(
                    CommandBlockItemData.needsRedstone(stack)
                            ? "lovicsbettercommandblock.item.tooltip.redstone.required"
                            : "lovicsbettercommandblock.item.tooltip.redstone.not_required");
            case SEQUENCE -> Component.translatable("lovicsbettercommandblock.item.tooltip.redstone.chain");
            case AUTO -> Component.translatable(
                    CommandBlockItemData.needsRedstone(stack)
                            ? "lovicsbettercommandblock.item.tooltip.redstone.required"
                            : "lovicsbettercommandblock.item.tooltip.redstone.not_required");
        };
        lines.add(Component.translatable("lovicsbettercommandblock.item.tooltip.redstone", redstoneText));

        lines.add(Component.translatable(
                "lovicsbettercommandblock.item.tooltip.track_output",
                Component.translatable(CommandBlockItemData.tracksOutput(stack)
                        ? "lovicsbettercommandblock.item.tooltip.yes"
                        : "lovicsbettercommandblock.item.tooltip.no")));

        for (int i = lines.size() - 5; i < lines.size(); i++) {
            lines.set(i, lines.get(i).copy().withStyle(style -> style.withColor(DETAIL_COLOR)));
        }
    }
}
