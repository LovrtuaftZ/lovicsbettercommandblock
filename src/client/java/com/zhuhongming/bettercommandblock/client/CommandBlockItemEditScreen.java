package com.zhuhongming.bettercommandblock.client;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.api.CommandBlockItemEditScreenAccess;
import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public class CommandBlockItemEditScreen extends AbstractCommandBlockEditScreen implements CommandBlockItemEditScreenAccess {

    private final InteractionHand editedHand;
    private final ItemStackCommandBlock commandBlock;
    private CycleButton<CommandBlockEntity.Mode> modeButton;
    private CycleButton<Boolean> conditionalButton;
    private CycleButton<Boolean> autoexecButton;
    private CommandBlockEntity.Mode mode = CommandBlockEntity.Mode.REDSTONE;
    private boolean conditional;
    private boolean autoexec;

    public CommandBlockItemEditScreen(InteractionHand hand) {
        this.editedHand = hand;
        this.commandBlock = new ItemStackCommandBlock(Minecraft.getInstance());
    }

    @Override
    public ItemStack bettercommandblock$getEditedItemStack() {
        return this.minecraft.player.getItemInHand(this.editedHand);
    }

    @Override
    public CommandBlockEntity.Mode bettercommandblock$getEditedMode() {
        return this.mode;
    }

    @Override
    public boolean bettercommandblock$isEditedConditional() {
        return this.conditional;
    }

    @Override
    public boolean bettercommandblock$isEditedAutomatic() {
        return this.autoexec;
    }

    public void bettercommandblock$restoreEditorMode(
            CommandBlockEntity.Mode mode, boolean conditional, boolean autoexec) {
        this.mode = mode;
        this.conditional = conditional;
        this.autoexec = autoexec;
        if (this.modeButton != null) {
            this.modeButton.setValue(mode);
        }
        if (this.conditionalButton != null) {
            this.conditionalButton.setValue(conditional);
        }
        if (this.autoexecButton != null) {
            this.autoexecButton.setValue(autoexec);
        }
    }

    @Override
    public BaseCommandBlock getCommandBlock() {
        return this.commandBlock;
    }

    @Override
    public int getPreviousY() {
        return 135;
    }

    @Override
    protected void init() {
        CommandBlockItemData.loadIntoCommandBlock(this.bettercommandblock$getEditedItemStack(), this.commandBlock);
        ItemStack stack = this.bettercommandblock$getEditedItemStack();
        this.mode = CommandBlockItemData.getMode(stack);
        this.conditional = CommandBlockItemData.isConditional(stack);
        this.autoexec = CommandBlockItemData.isAutomatic(stack);

        super.init();

        this.modeButton = this.addRenderableWidget(CycleButton.builder((CommandBlockEntity.Mode mode) -> {
                    if (mode == CommandBlockEntity.Mode.SEQUENCE) {
                        return Component.translatable("advMode.mode.sequence");
                    }
                    if (mode == CommandBlockEntity.Mode.AUTO) {
                        return Component.translatable("advMode.mode.auto");
                    }
                    return Component.translatable("advMode.mode.redstone");
                })
                .withValues(CommandBlockEntity.Mode.values())
                .displayOnlyValue()
                .withInitialValue(this.mode)
                .create(
                        this.width / 2 - 50 - 100 - 4,
                        165,
                        100,
                        20,
                        Component.translatable("advMode.mode"),
                        (cycleButton, value) -> this.mode = value));
        this.conditionalButton = this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("advMode.mode.conditional"),
                        Component.translatable("advMode.mode.unconditional"))
                .displayOnlyValue()
                .withInitialValue(this.conditional)
                .create(
                        this.width / 2 - 50,
                        165,
                        100,
                        20,
                        Component.translatable("advMode.type"),
                        (cycleButton, value) -> this.conditional = value));
        this.autoexecButton = this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("advMode.mode.autoexec.bat"),
                        Component.translatable("advMode.mode.redstoneTriggered"))
                .displayOnlyValue()
                .withInitialValue(this.autoexec)
                .create(
                        this.width / 2 + 50 + 4,
                        165,
                        100,
                        20,
                        Component.translatable("advMode.triggering"),
                        (cycleButton, value) -> this.autoexec = value));

        this.commandEdit.setValue(this.commandBlock.getCommand());
        this.outputButton.setValue(this.commandBlock.isTrackOutput());
        this.updatePreviousOutput(this.commandBlock.isTrackOutput());
        this.setButtonsActive(true);
        ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$refreshEditorFieldsFromSource();
    }

    private void setButtonsActive(boolean active) {
        this.doneButton.active = active;
        this.outputButton.active = active;
        this.modeButton.active = active;
        this.conditionalButton.active = active;
        this.autoexecButton.active = active;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.setButtonsActive(true);
    }

    @Override
    protected void populateAndSendPacket(BaseCommandBlock baseCommandBlock) {
        baseCommandBlock.setCommand(this.commandEdit.getValue());
        baseCommandBlock.setTrackOutput(this.outputButton.getValue());
        String customName = ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$getCustomNameValue();
        ItemStack updated = CommandBlockItemData.applyEditorState(
                this.bettercommandblock$getEditedItemStack(),
                baseCommandBlock,
                this.mode,
                this.conditional,
                this.autoexec,
                customName);
        CommandBlockItemInventorySync.applyEditedStack(this.minecraft, this.editedHand, updated);
    }
}
