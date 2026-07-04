package com.zhuhongming.bettercommandblock.preset;

import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import com.zhuhongming.bettercommandblock.network.CommandPresetClientNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommandPresetMenuScreen extends Screen {

    public enum Mode {
        GIVE_ITEM,
        IMPORT_TO_EDITOR
    }

    private static final int BG_COLOR = 0xD92A2A2A;
    private static final int BUTTON_COLOR = 0x00000000;
    private static final int BUTTON_HOVER_COLOR = 0x663F3F3F;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_BLUE = 0xFF8FB7FF;
    private static final int TEXT_GRAY = 0xFF9A9A9A;
    private static final int TEXT_PATTERN = 0xFF9BFFA3;
    private static final int TEXT_ANIMATION = 0xFFFFA6E7;
    private static final int TEXT_TEXT = 0xFFFFD27A;
    private static final int TEXT_COMMAND = 0xFF7EC8E8;
    private static final int TEXT_BUILTIN = 0xFFBFC9FF;
    private static final int TEXT_CUSTOM = 0xFF7FD58A;
    private static final int THUMB_SIZE = 16;
    private static final int THUMB_LEFT_PADDING = 5;

    private final List<CommandPreset> presets = new ArrayList<>();
    private final List<CommandPreset> filteredPresets = new ArrayList<>();
    private final List<FlatPresetButton> presetButtons = new ArrayList<>();
    private final Map<CommandPreset, ResourceLocation> presetThumbnails = new HashMap<>();
    private final PresetThumbnailResolver thumbnailResolver = new PresetThumbnailResolver();
    private int scrollOffset;
    private SourceFilter sourceFilter = SourceFilter.ALL;
    private TagFilter tagFilter = TagFilter.ALL;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listHeight;
    private int visibleRows;
    private static final int ROW_HEIGHT = 22;

    private final Screen parent;
    private final Mode mode;

    public CommandPresetMenuScreen() {
        this(null, Mode.GIVE_ITEM);
    }

    public CommandPresetMenuScreen(Screen parent, Mode mode) {
        super(Component.translatable("screen.lovicsbettercommandblock.preset_menu.title"));
        this.parent = parent;
        this.mode = mode;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.panelWidth = Math.min(420, this.width - 40);
        this.panelHeight = Math.min(320, this.height - 40);
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;

        this.listTop = panelY + 78;
        this.listHeight = panelHeight - 92;
        this.visibleRows = Math.max(1, listHeight / ROW_HEIGHT);

        CommandPresetManager.getInstance().reloadAllPresets();
        this.presets.clear();
        this.presets.addAll(CommandPresetManager.getInstance().getAllPresets());
        reloadThumbnails();
        rebuildFilteredPresets();
        this.scrollOffset = Math.min(this.scrollOffset, maxScrollOffset());

        this.addRenderableWidget(new FlatActionButton(
                panelX + 14,
                panelY + 24,
                (panelWidth - 32) / 2,
                18,
                Component.translatable("screen.lovicsbettercommandblock.preset_menu.open_folder"),
                button -> openCustomPresetFolder()));
        this.addRenderableWidget(new FlatActionButton(
                panelX + 18 + (panelWidth - 32) / 2,
                panelY + 24,
                (panelWidth - 32) / 2,
                18,
                Component.translatable("screen.lovicsbettercommandblock.preset_menu.export_custom"),
                button -> openExportScreen()));
        this.addRenderableWidget(new SourceFilterButton(panelX + 14, panelY + 46, 40, SourceFilter.ALL));
        this.addRenderableWidget(new SourceFilterButton(panelX + 56, panelY + 46, 48, SourceFilter.BUILTIN));
        this.addRenderableWidget(new SourceFilterButton(panelX + 106, panelY + 46, 48, SourceFilter.CUSTOM));
        this.addRenderableWidget(new TagFilterFunnelButton(panelX + panelWidth - 32, panelY + 46, 18, 18));
        rebuildPresetButtons();
    }

    private void reloadThumbnails() {
        thumbnailResolver.releaseCustomTextures(this.minecraft);
        presetThumbnails.clear();
        if (this.minecraft == null) {
            return;
        }
        for (CommandPreset preset : presets) {
            presetThumbnails.put(preset, thumbnailResolver.loadThumbnail(this.minecraft, preset));
        }
    }

    private void rebuildPresetButtons() {
        for (FlatPresetButton button : presetButtons) {
            this.removeWidget(button);
        }
        presetButtons.clear();

        int rows = Math.min(visibleRows, Math.max(0, filteredPresets.size() - scrollOffset));
        for (int i = 0; i < rows; i++) {
            CommandPreset preset = filteredPresets.get(scrollOffset + i);
            FlatPresetButton button = new FlatPresetButton(
                    panelX + 14,
                    listTop + i * ROW_HEIGHT,
                    panelWidth - 28,
                    ROW_HEIGHT - 2,
                    preset,
                    b -> onPresetSelected(preset));
            presetButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    private void rebuildFilteredPresets() {
        filteredPresets.clear();
        for (CommandPreset preset : presets) {
            if (!sourceFilter.matches(preset)) {
                continue;
            }
            if (!tagFilter.matches(preset)) {
                continue;
            }
            filteredPresets.add(preset);
        }
        filteredPresets.sort(
                Comparator.comparing((CommandPreset p) -> !p.isBuiltin())
                        .thenComparing(CommandPreset::getName, String.CASE_INSENSITIVE_ORDER));
        scrollOffset = Math.min(scrollOffset, maxScrollOffset());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (filteredPresets.size() <= visibleRows) {
            return super.mouseScrolled(mouseX, mouseY, scrollY);
        }
        if (scrollY > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (scrollY < 0) {
            scrollOffset = Math.min(maxScrollOffset(), scrollOffset + 1);
        }
        rebuildPresetButtons();
        return true;
    }

    private int maxScrollOffset() {
        return Math.max(0, filteredPresets.size() - visibleRows);
    }

    private void openCustomPresetFolder() {
        Path customDir = CommandPresetManager.getCustomPresetDir();
        try {
            java.nio.file.Files.createDirectories(customDir);
            Util.getPlatform().openFile(customDir.toFile());
        } catch (Exception exception) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("lovicsbettercommandblock.preset.open_folder_failed", customDir)
                                .withStyle(ChatFormatting.RED),
                        false);
            }
        }
    }

    private void onPresetSelected(CommandPreset preset) {
        if (mode == Mode.IMPORT_TO_EDITOR && parent instanceof AbstractCommandBlockEditScreenAccess editorAccess) {
            editorAccess.bettercommandblock$schedulePresetImport(preset);
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
            return;
        }
        givePresetToPlayer(preset);
    }

    private void givePresetToPlayer(CommandPreset preset) {
        if (minecraft == null || minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        CommandPresetClientNetworking.sendGivePreset(preset.getName());
        this.onClose();
    }

    private void openExportScreen() {
        if (minecraft != null) {
            minecraft.setScreen(new CommandPresetExportScreen(this));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG_COLOR);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.lovicsbettercommandblock.preset_menu.title"),
                panelX + 14,
                panelY + 10,
                TEXT_BLUE);
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        mode == Mode.IMPORT_TO_EDITOR
                                ? "screen.lovicsbettercommandblock.preset_menu.hint_import"
                                : "screen.lovicsbettercommandblock.preset_menu.hint"),
                panelX + 14,
                panelY + panelHeight - 12,
                TEXT_GRAY);
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        "screen.lovicsbettercommandblock.preset_menu.filter_status",
                        sourceFilter.label,
                        tagFilter.label),
                panelX + 160,
                panelY + 50,
                TEXT_GRAY);

        if (filteredPresets.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.lovicsbettercommandblock.preset_menu.empty"),
                    panelX + 14,
                    listTop + 6,
                    TEXT_GRAY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        thumbnailResolver.releaseCustomTextures(this.minecraft);
        presetThumbnails.clear();
        super.removed();
    }

    private final class FlatActionButton extends Button {
        private FlatActionButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 6, this.getY() + 5, isHoveredOrFocused() ? TEXT_BLUE : TEXT_WHITE);
        }
    }

    private final class FlatPresetButton extends Button {
        private final CommandPreset preset;

        private FlatPresetButton(int x, int y, int width, int height, CommandPreset preset, OnPress onPress) {
            super(x, y, width, height, Component.literal(preset.getName()), onPress, DEFAULT_NARRATION);
            this.preset = preset;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
            int color = resolveNameColor(preset);
            int thumbX = this.getX() + THUMB_LEFT_PADDING;
            int thumbY = this.getY() + (this.height - THUMB_SIZE) / 2;
            ResourceLocation thumbnail = presetThumbnails.get(preset);
            if (thumbnail != null) {
                guiGraphics.blit(thumbnail, thumbX, thumbY, 0, 0, THUMB_SIZE, THUMB_SIZE, THUMB_SIZE, THUMB_SIZE);
            } else {
                drawPlaceholder(guiGraphics, thumbX, thumbY, THUMB_SIZE);
            }
            int textX = thumbX + THUMB_SIZE + 6;
            guiGraphics.drawString(font, this.getMessage(), textX, this.getY() + 6, isHoveredOrFocused() ? TEXT_BLUE : color);
            Component sourceLabel = Component.translatable(
                    preset.isBuiltin()
                            ? "screen.lovicsbettercommandblock.preset_menu.source.builtin"
                            : "screen.lovicsbettercommandblock.preset_menu.source.custom");
            int sourceColor = preset.isBuiltin() ? TEXT_BUILTIN : TEXT_CUSTOM;
            int labelWidth = font.width(sourceLabel);
            guiGraphics.drawString(font, sourceLabel, this.getX() + this.width - labelWidth - 6, this.getY() + 6, sourceColor);
        }

        private int resolveNameColor(CommandPreset preset) {
            if (preset.isAnimationTag()) {
                return TEXT_ANIMATION;
            }
            if (preset.isTextTag()) {
                return TEXT_TEXT;
            }
            if (preset.isCommandTag()) {
                return TEXT_COMMAND;
            }
            if (preset.isPatternTag()) {
                return TEXT_PATTERN;
            }
            return TEXT_WHITE;
        }

        private void drawPlaceholder(GuiGraphics guiGraphics, int x, int y, int size) {
            int borderColor = 0xFF5A5A5A;
            int innerColor = 0x33242424;
            int accent = 0x55888888;
            guiGraphics.fill(x, y, x + size, y + size, borderColor);
            guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, innerColor);
            guiGraphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, accent);
        }
    }

    private final class SourceFilterButton extends Button {
        private final SourceFilter filter;

        private SourceFilterButton(int x, int y, int width, SourceFilter filter) {
            super(x, y, width, 18, Component.literal(filter.label), b -> {}, DEFAULT_NARRATION);
            this.filter = filter;
        }

        @Override
        public void onPress() {
            sourceFilter = filter;
            scrollOffset = 0;
            rebuildFilteredPresets();
            rebuildPresetButtons();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean active = sourceFilter == filter;
            int bg = active ? 0x66404C6B : (isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            int color = active ? TEXT_BLUE : TEXT_WHITE;
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 6, this.getY() + 5, color);
        }
    }

    private final class TagFilterFunnelButton extends Button {
        private TagFilterFunnelButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.literal(""), b -> {}, DEFAULT_NARRATION);
        }

        @Override
        public void onPress() {
            tagFilter = tagFilter.next();
            scrollOffset = 0;
            rebuildFilteredPresets();
            rebuildPresetButtons();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            int color = tagFilter == TagFilter.ALL ? TEXT_GRAY : TEXT_BLUE;
            drawFunnelIcon(guiGraphics, this.getX() + 4, this.getY() + 3, color);
        }

        private void drawFunnelIcon(GuiGraphics guiGraphics, int x, int y, int color) {
            guiGraphics.fill(x, y, x + 10, y + 2, color);
            guiGraphics.fill(x + 1, y + 2, x + 9, y + 4, color);
            guiGraphics.fill(x + 3, y + 4, x + 7, y + 7, color);
            guiGraphics.fill(x + 4, y + 7, x + 6, y + 11, color);
        }
    }

    private enum SourceFilter {
        ALL("全部"),
        BUILTIN("内置"),
        CUSTOM("自定义");

        private final String label;

        SourceFilter(String label) {
            this.label = label;
        }

        private boolean matches(CommandPreset preset) {
            return switch (this) {
                case ALL -> true;
                case BUILTIN -> preset.isBuiltin();
                case CUSTOM -> !preset.isBuiltin();
            };
        }
    }

    private enum TagFilter {
        ALL("全部"),
        PATTERN("图案"),
        ANIMATION("动画"),
        TEXT("文本"),
        COMMAND("指令");

        private final String label;

        TagFilter(String label) {
            this.label = label;
        }

        private TagFilter next() {
            return switch (this) {
                case ALL -> PATTERN;
                case PATTERN -> ANIMATION;
                case ANIMATION -> TEXT;
                case TEXT -> COMMAND;
                case COMMAND -> ALL;
            };
        }

        private boolean matches(CommandPreset preset) {
            return switch (this) {
                case ALL -> true;
                case PATTERN -> preset.isPatternTag();
                case ANIMATION -> preset.isAnimationTag();
                case TEXT -> preset.isTextTag();
                case COMMAND -> preset.isCommandTag();
            };
        }
    }
}
