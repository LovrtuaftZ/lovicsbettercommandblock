package com.zhuhongming.bettercommandblock.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zhuhongming.bettercommandblock.preset.CommandPresetExportContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandPresetExportScreen extends Screen {

    private static final int BG_COLOR = 0xD92A2A2A;
    private static final int BUTTON_COLOR = 0x00000000;
    private static final int BUTTON_HOVER_COLOR = 0x663F3F3F;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_BLUE = 0xFF8FB7FF;
    private static final int TEXT_GRAY = 0xFF9A9A9A;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Screen parent;
    private final CommandPresetExportContext exportContext;
    private final List<ResourceLocation> minecraftItemIds = new ArrayList<>();

    private EditBox fileNameField;
    private PresetCommandMultiLineEditBox commandField;
    private EditBox iconSearchField;
    private int iconIndex = 0;
    private String blockType = "pulse";
    private boolean conditional = false;
    private boolean alwaysActive = false;
    private String tag = CommandPreset.TAG_PATTERN;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int viewportTop;
    private int viewportHeight;
    private int contentHeight;
    private double scrollAmount;
    private boolean draggingScrollBar;
    private int scrollDragOffset;

    private FlatActionButton blockTypeButton;
    private FlatActionButton conditionalButton;
    private FlatActionButton modeButton;
    private FlatActionButton tagButton;
    private PresetIconGridWidget iconGridWidget;
    private FlatActionButton exportButton;
    private FlatActionButton backButton;
    private final Map<AbstractWidget, Integer> scrollableWidgetY = new HashMap<>();
    private final Map<ResourceLocation, String> iconLocalizedNameMap = new HashMap<>();

    public CommandPresetExportScreen(Screen parent) {
        this(parent, CommandPresetExportContext.empty());
    }

    public CommandPresetExportScreen(Screen parent, CommandPresetExportContext exportContext) {
        super(Component.translatable("screen.lovicsbettercommandblock.preset_export.title"));
        this.parent = parent;
        this.exportContext = exportContext == null ? CommandPresetExportContext.empty() : exportContext;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(468, this.width - 40);
        panelHeight = Math.min(320, this.height - 40);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        viewportTop = panelY + 34;
        viewportHeight = panelHeight - 48;
        contentHeight = 456;
        scrollAmount = clampScroll(scrollAmount);
        scrollableWidgetY.clear();

        collectMinecraftItems();

        fileNameField = new EditBox(this.font, panelX + 120, viewportTop + 8, panelWidth - 154, 18, Component.literal("file_name"));
        fileNameField.setValue(sanitizeFileName(exportContext.name()));
        commandField = new PresetCommandMultiLineEditBox(
                this.minecraft,
                this.font,
                panelX + 120,
                viewportTop + 32,
                panelWidth - 154,
                78,
                Component.literal("输入完整指令"),
                Component.literal("command"));
        commandField.setValue(exportContext.command());
        commandField.setCharacterLimit(2048);
        blockType = exportContext.blockType();
        conditional = exportContext.conditional();
        alwaysActive = exportContext.alwaysActive();
        addScrollableWidget(fileNameField, 8);
        addScrollableWidget(commandField, 32);

        blockTypeButton = new FlatActionButton(panelX + 120, viewportTop + 116, 108, 18, Component.literal("方块:" + blockType), b -> {
            blockType = nextBlockType(blockType);
            b.setMessage(Component.literal("方块:" + blockType));
        });
        addScrollableWidget(blockTypeButton, 116);

        conditionalButton = new FlatActionButton(panelX + 232, viewportTop + 116, 70, 18, Component.literal("条件:" + (conditional ? "是" : "否")), b -> {
            conditional = !conditional;
            b.setMessage(Component.literal("条件:" + (conditional ? "是" : "否")));
        });
        addScrollableWidget(conditionalButton, 116);

        modeButton = new FlatActionButton(panelX + 306, viewportTop + 116, 78, 18, Component.literal(alwaysActive ? "模式:常开" : "模式:红石"), b -> {
            alwaysActive = !alwaysActive;
            b.setMessage(Component.literal(alwaysActive ? "模式:常开" : "模式:红石"));
        });
        addScrollableWidget(modeButton, 116);

        tagButton = new FlatActionButton(
                panelX + 120,
                viewportTop + 140,
                92,
                18,
                Component.literal("类型:" + CommandPreset.tagDisplayLabel(tag)),
                b -> {
            tag = CommandPreset.cycleExportTag(tag);
            b.setMessage(Component.literal("类型:" + CommandPreset.tagDisplayLabel(tag)));
        });
        addScrollableWidget(tagButton, 140);

        iconGridWidget = new PresetIconGridWidget(
                panelX + 120,
                viewportTop + 188,
                panelWidth - 154,
                150,
                minecraftItemIds,
                iconIndex);
        iconSearchField = new EditBox(this.font, panelX + 120, viewportTop + 166, panelWidth - 154, 18, Component.literal("icon_search"));
        iconSearchField.setValue("");
        iconSearchField.setResponder(value -> refreshIconFilter());
        addScrollableWidget(iconSearchField, 166);
        addScrollableWidget(iconGridWidget, 188);

        exportButton = new FlatActionButton(panelX + panelWidth - 180, viewportTop + 374, 80, 20, Component.literal("确认导出"), b -> exportPreset());
        backButton = new FlatActionButton(panelX + panelWidth - 90, viewportTop + 374, 70, 20, Component.literal("返回"), b -> onClose());
        addScrollableWidget(exportButton, 374);
        addScrollableWidget(backButton, 374);

        refreshIconFilter();
        applyScrollLayout();
    }

    private void addScrollableWidget(AbstractWidget widget, int contentY) {
        this.addRenderableWidget(widget);
        scrollableWidgetY.put(widget, contentY);
    }

    private void applyScrollLayout() {
        int viewportBottom = viewportTop + viewportHeight;
        int offset = (int) Math.round(scrollAmount);
        for (Map.Entry<AbstractWidget, Integer> entry : scrollableWidgetY.entrySet()) {
            AbstractWidget widget = entry.getKey();
            int y = viewportTop + entry.getValue() - offset;
            widget.setY(y);
            boolean visible = y + widget.getHeight() >= viewportTop && y <= viewportBottom;
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private double maxScroll() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    private double clampScroll(double value) {
        return Math.max(0, Math.min(maxScroll(), value));
    }

    private void setScrollAmount(double value) {
        scrollAmount = clampScroll(value);
        applyScrollLayout();
    }

    private int scrollbarX() {
        return panelX + panelWidth - 12;
    }

    private int scrollbarWidth() {
        return 6;
    }

    private int scrollbarThumbHeight() {
        if (maxScroll() <= 0) {
            return viewportHeight;
        }
        int height = (int) Math.round((viewportHeight * viewportHeight) / (double) contentHeight);
        return Math.max(24, Math.min(viewportHeight, height));
    }

    private int scrollbarThumbY() {
        if (maxScroll() <= 0) {
            return viewportTop;
        }
        int travel = viewportHeight - scrollbarThumbHeight();
        return viewportTop + (int) Math.round((scrollAmount / maxScroll()) * travel);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int x = scrollbarX();
        return mouseX >= x && mouseX <= x + scrollbarWidth()
                && mouseY >= viewportTop && mouseY <= viewportTop + viewportHeight;
    }

    private boolean isOverThumb(double mouseX, double mouseY) {
        int x = scrollbarX();
        int thumbY = scrollbarThumbY();
        int thumbH = scrollbarThumbHeight();
        return mouseX >= x && mouseX <= x + scrollbarWidth()
                && mouseY >= thumbY && mouseY <= thumbY + thumbH;
    }

    @SuppressWarnings("deprecation")
    private void collectMinecraftItems() {
        minecraftItemIds.clear();
        iconLocalizedNameMap.clear();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (!"minecraft".equals(id.getNamespace())) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) {
                continue;
            }
            minecraftItemIds.add(id);
            iconLocalizedNameMap.put(id, item.getDescription().getString().toLowerCase(Locale.ROOT));
        }
        minecraftItemIds.sort(Comparator.comparing(ResourceLocation::toString));
        ResourceLocation preferredId = ResourceLocation.tryParse("minecraft:glass_pane");
        int preferred = preferredId == null ? -1 : minecraftItemIds.indexOf(preferredId);
        iconIndex = preferred >= 0 ? preferred : 0;
    }

    private void refreshIconFilter() {
        if (iconGridWidget == null) {
            return;
        }
        ResourceLocation previous = iconGridWidget.getSelectedIconId();
        String query = iconSearchField == null ? "" : iconSearchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> filtered = new ArrayList<>();
        for (ResourceLocation id : minecraftItemIds) {
            if (query.isEmpty()) {
                filtered.add(id);
                continue;
            }
            String idText = id.toString().toLowerCase(Locale.ROOT);
            String localized = iconLocalizedNameMap.getOrDefault(id, "");
            if (idText.contains(query) || localized.contains(query)) {
                filtered.add(id);
            }
        }
        if (filtered.isEmpty()) {
            filtered.addAll(minecraftItemIds);
        }
        iconGridWidget.setIconIds(filtered, previous);
    }

    private void exportPreset() {
        String fileName = sanitizeFileName(fileNameField.getValue());
        String command = commandField.getValue() == null ? "" : commandField.getValue().trim();
        if (fileName.isBlank()) {
            showError("文件名不能为空，只能包含小写字母/数字/_/-");
            return;
        }
        if (command.isBlank()) {
            showError("指令内容不能为空");
            return;
        }

        Path initialDir = PresetExportHistory.loadLastDirectory();
        if (initialDir == null) {
            initialDir = CommandPresetManager.getCustomPresetDir();
        }
        Path selectedDirectory = chooseDirectory(initialDir);
        if (selectedDirectory == null) {
            return;
        }

        ResourceLocation iconId = getSelectedIconId();
        ExportPresetData data = new ExportPresetData();
        data.name = fileName;
        data.command = command;
        data.blockType = blockType;
        data.icon = iconId.toString();
        data.tag = tag;
        data.conditional = conditional;
        data.alwaysActive = alwaysActive;

        try {
            Files.createDirectories(selectedDirectory);
            Path output = selectedDirectory.resolve(fileName + ".pvic");
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            PresetExportHistory.saveLastDirectory(selectedDirectory);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("已导出: " + output).withStyle(ChatFormatting.GREEN),
                        false);
            }
        } catch (IOException exception) {
            showError("导出失败: " + exception.getMessage());
        }
    }

    private Path chooseDirectory(Path initialDir) {
        String defaultPath;
        if (initialDir != null && Files.isDirectory(initialDir)) {
            defaultPath = initialDir.toAbsolutePath().toString();
        } else {
            defaultPath = CommandPresetManager.getCustomPresetDir().toAbsolutePath().toString();
        }
        String selected = TinyFileDialogs.tinyfd_selectFolderDialog("选择导出目录", defaultPath);
        if (selected == null || selected.isBlank()) {
            return null;
        }
        return Path.of(selected);
    }

    private ResourceLocation getSelectedIconId() {
        if (iconGridWidget != null) {
            return iconGridWidget.getSelectedIconId();
        }
        if (minecraftItemIds.isEmpty()) {
            ResourceLocation fallback = ResourceLocation.tryParse("minecraft:glass_pane");
            return fallback == null ? ResourceLocation.tryParse("minecraft:stone") : fallback;
        }
        return minecraftItemIds.get(iconIndex);
    }

    private void showError(String message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
        }
    }

    private static String nextBlockType(String current) {
        return switch (current) {
            case "pulse" -> "repeat";
            case "repeat" -> "chain";
            default -> "pulse";
        };
    }

    private static String sanitizeFileName(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        // Keep Chinese and other Unicode letters, only strip characters invalid for Windows file names.
        return trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverThumb(mouseX, mouseY)) {
            draggingScrollBar = true;
            scrollDragOffset = (int) mouseY - scrollbarThumbY();
            return true;
        }
        if (button == 0 && isOverScrollbar(mouseX, mouseY) && maxScroll() > 0) {
            int target = (int) mouseY - scrollbarThumbHeight() / 2;
            int travel = viewportHeight - scrollbarThumbHeight();
            double ratio = travel <= 0 ? 0 : (target - viewportTop) / (double) travel;
            setScrollAmount(maxScroll() * ratio);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollBar && button == 0) {
            int travel = viewportHeight - scrollbarThumbHeight();
            if (travel <= 0) {
                setScrollAmount(0);
            } else {
                int top = (int) mouseY - scrollDragOffset;
                double ratio = (top - viewportTop) / (double) travel;
                setScrollAmount(maxScroll() * ratio);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollBar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, scrollY);
        if (handled) {
            return true;
        }
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= viewportTop && mouseY <= viewportTop + viewportHeight) {
            setScrollAmount(scrollAmount - scrollY * 16.0D);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG_COLOR);
        guiGraphics.drawString(this.font, Component.translatable("screen.lovicsbettercommandblock.preset_export.title"), panelX + 12, panelY + 12, TEXT_BLUE);

        int offset = (int) Math.round(scrollAmount);
        int yFile = viewportTop + 13 - offset;
        int yCommand = viewportTop + 37 - offset;
        int yIconSearch = viewportTop + 170 - offset;
        int yIcon = viewportTop + 192 - offset;
        int yHint = viewportTop + 360 - offset;
        if (yFile >= viewportTop - 10 && yFile <= viewportTop + viewportHeight) {
            guiGraphics.drawString(this.font, Component.literal("文件名"), panelX + 20, yFile, TEXT_WHITE);
        }
        if (yCommand >= viewportTop - 10 && yCommand <= viewportTop + viewportHeight) {
            guiGraphics.drawString(this.font, Component.literal("指令"), panelX + 20, yCommand, TEXT_WHITE);
        }
        if (yIconSearch >= viewportTop - 10 && yIconSearch <= viewportTop + viewportHeight) {
            guiGraphics.drawString(this.font, Component.literal("图标搜索"), panelX + 20, yIconSearch, TEXT_WHITE);
        }
        if (yIcon >= viewportTop - 10 && yIcon <= viewportTop + viewportHeight) {
            guiGraphics.drawString(this.font, Component.literal("图标"), panelX + 20, yIcon, TEXT_WHITE);
        }
        if (yHint >= viewportTop - 10 && yHint <= viewportTop + viewportHeight) {
            guiGraphics.drawString(this.font, Component.translatable("screen.lovicsbettercommandblock.preset_export.hint"), panelX + 20, yHint, TEXT_GRAY);
        }

        int clipLeft = panelX + 8;
        int clipTop = viewportTop;
        int clipRight = panelX + panelWidth - 14;
        int clipBottom = viewportTop + viewportHeight;
        guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        int barX = scrollbarX();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();

        guiGraphics.fill(barX, viewportTop, barX + scrollbarWidth(), viewportTop + viewportHeight, 0x332A2A2A);
        int thumbY = scrollbarThumbY();
        int thumbH = scrollbarThumbHeight();
        int thumbColor = draggingScrollBar || isOverThumb(mouseX, mouseY) ? 0xFF7B8FB5 : 0xFF5C667A;
        guiGraphics.fill(barX, thumbY, barX + scrollbarWidth(), thumbY + thumbH, thumbColor);
    }

    @Override
    public void tick() {
        super.tick();
        if (commandField != null) {
            commandField.tick();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private final class FlatActionButton extends Button {
        private FlatActionButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 6, this.getY() + 6, isHoveredOrFocused() ? TEXT_BLUE : TEXT_WHITE);
        }
    }

    @SuppressWarnings("unused")
    private static final class ExportPresetData {
        String name;
        String command;
        String blockType;
        String icon;
        String tag;
        boolean conditional;
        boolean alwaysActive;
    }
}

