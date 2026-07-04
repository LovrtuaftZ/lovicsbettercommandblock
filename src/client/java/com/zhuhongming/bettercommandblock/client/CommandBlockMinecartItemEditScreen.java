package com.zhuhongming.bettercommandblock.client;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.api.CommandBlockItemEditScreenAccess;
import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public class CommandBlockMinecartItemEditScreen extends AbstractCommandBlockEditScreen
        implements CommandBlockItemEditScreenAccess {

    private final InteractionHand editedHand;
    private final ItemStackCommandBlock commandBlock;

    public CommandBlockMinecartItemEditScreen(InteractionHand hand) {
        this.editedHand = hand;
        this.commandBlock = new ItemStackCommandBlock(net.minecraft.client.Minecraft.getInstance());
    }

    @Override
    public ItemStack bettercommandblock$getEditedItemStack() {
        return this.minecraft.player.getItemInHand(this.editedHand);
    }

    @Override
    public CommandBlockEntity.Mode bettercommandblock$getEditedMode() {
        return CommandBlockEntity.Mode.REDSTONE;
    }

    @Override
    public boolean bettercommandblock$isEditedConditional() {
        return false;
    }

    @Override
    public boolean bettercommandblock$isEditedAutomatic() {
        return false;
    }

    @Override
    public BaseCommandBlock getCommandBlock() {
        return this.commandBlock;
    }

    @Override
    public int getPreviousY() {
        return 150;
    }

    @Override
    protected void init() {
        CommandBlockItemData.loadIntoCommandBlock(this.bettercommandblock$getEditedItemStack(), this.commandBlock);
        super.init();
        this.commandEdit.setValue(this.commandBlock.getCommand());
        this.outputButton.setValue(this.commandBlock.isTrackOutput());
        this.updatePreviousOutput(this.commandBlock.isTrackOutput());
        this.doneButton.active = true;
        this.outputButton.active = true;
        ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$refreshEditorFieldsFromSource();
    }

    @Override
    protected void populateAndSendPacket(BaseCommandBlock baseCommandBlock) {
        baseCommandBlock.setCommand(this.commandEdit.getValue());
        baseCommandBlock.setTrackOutput(this.outputButton.getValue());
        String customName = ((AbstractCommandBlockEditScreenAccess) this).bettercommandblock$getCustomNameValue();
        ItemStack updated = CommandBlockItemData.applyEditorState(
                this.bettercommandblock$getEditedItemStack(),
                baseCommandBlock,
                null,
                false,
                false,
                customName);
        CommandBlockItemInventorySync.applyEditedStack(this.minecraft, this.editedHand, updated);
    }
}
