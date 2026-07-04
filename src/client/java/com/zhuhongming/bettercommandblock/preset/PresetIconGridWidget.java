package com.zhuhongming.bettercommandblock.preset;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class PresetIconGridWidget extends AbstractScrollWidget {

    private static final int SLOT = 18;
    private static final int PAD = 2;
    private static final int HOVER_BG = 0x333F3F3F;
    private static final int NORMAL_BG = 0x332A2A2A;
    private static final int SELECT_BORDER = 0xFF9CCFE3;

    private final List<ResourceLocation> iconIds;
    private int selectedIndex;

    public PresetIconGridWidget(int x, int y, int width, int height, List<ResourceLocation> iconIds, int defaultIndex) {
        super(x, y, width, height, Component.literal("icon_grid"));
        this.iconIds = new ArrayList<>(iconIds);
        this.selectedIndex = Math.max(0, Math.min(defaultIndex, Math.max(0, iconIds.size() - 1)));
    }

    public ResourceLocation getSelectedIconId() {
        if (iconIds.isEmpty()) {
            ResourceLocation fallback = ResourceLocation.tryParse("minecraft:glass_pane");
            return fallback == null ? ResourceLocation.tryParse("minecraft:stone") : fallback;
        }
        return iconIds.get(selectedIndex);
    }

    public void setIconIds(List<ResourceLocation> filteredIds, ResourceLocation preferredSelected) {
        this.iconIds.clear();
        this.iconIds.addAll(filteredIds);
        if (this.iconIds.isEmpty()) {
            this.selectedIndex = 0;
            this.setScrollAmount(0);
            return;
        }
        int preferred = preferredSelected == null ? -1 : this.iconIds.indexOf(preferredSelected);
        this.selectedIndex = preferred >= 0 ? preferred : 0;
        this.setScrollAmount(Math.min(this.scrollAmount(), Math.max(0, this.getInnerHeight() - this.height)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.withinContentAreaPoint(mouseX, mouseY)) {
            int contentX = (int) (mouseX - this.getX() - this.innerPadding());
            int contentY = (int) (mouseY - this.getY() - this.innerPadding() + this.scrollAmount());
            int cols = columns();
            if (cols <= 0) {
                return true;
            }
            int col = contentX / SLOT;
            int row = contentY / SLOT;
            if (col < 0 || row < 0 || col >= cols) {
                return true;
            }
            int index = row * cols + col;
            if (index >= 0 && index < iconIds.size()) {
                selectedIndex = index;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int cols = columns();
        if (cols <= 0) {
            return;
        }
        int innerX = this.getX() + this.innerPadding();
        int innerY = this.getY() + this.innerPadding();
        int hover = hoveredIndex(mouseX, mouseY, cols);

        for (int i = 0; i < iconIds.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int x = innerX + col * SLOT;
            int y = innerY + row * SLOT;
            if (!withinContentAreaTopBottom(y, y + SLOT)) {
                continue;
            }
            int bg = i == hover ? HOVER_BG : NORMAL_BG;
            guiGraphics.fill(x, y, x + SLOT - PAD, y + SLOT - PAD, bg);
            if (i == selectedIndex) {
                int right = x + SLOT - PAD;
                int bottom = y + SLOT - PAD;
                guiGraphics.fill(x, y, right, y + 1, SELECT_BORDER);
                guiGraphics.fill(x, bottom - 1, right, bottom, SELECT_BORDER);
                guiGraphics.fill(x, y, x + 1, bottom, SELECT_BORDER);
                guiGraphics.fill(right - 1, y, right, bottom, SELECT_BORDER);
            }
            ItemStack stack = iconStack(iconIds.get(i));
            guiGraphics.renderItem(stack, x + 1, y + 1);
        }
    }

    @Override
    public int getInnerHeight() {
        int cols = columns();
        if (cols <= 0) {
            return this.height;
        }
        int rows = (int) Math.ceil(iconIds.size() / (double) cols);
        return rows * SLOT + this.totalInnerPadding();
    }

    @Override
    protected boolean scrollbarVisible() {
        return getInnerHeight() > this.height;
    }

    @Override
    protected double scrollRate() {
        return 12.0D;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("icon selector"));
    }

    private int columns() {
        return Math.max(1, (this.width - this.totalInnerPadding()) / SLOT);
    }

    private int hoveredIndex(int mouseX, int mouseY, int cols) {
        if (!withinContentAreaPoint(mouseX, mouseY)) {
            return -1;
        }
        int contentX = mouseX - this.getX() - this.innerPadding();
        int contentY = (int) (mouseY - this.getY() - this.innerPadding() + this.scrollAmount());
        int col = contentX / SLOT;
        int row = contentY / SLOT;
        if (col < 0 || row < 0 || col >= cols) {
            return -1;
        }
        int index = row * cols + col;
        return index >= 0 && index < iconIds.size() ? index : -1;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack iconStack(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.GLASS_PANE);
        }
        return new ItemStack(item);
    }
}

