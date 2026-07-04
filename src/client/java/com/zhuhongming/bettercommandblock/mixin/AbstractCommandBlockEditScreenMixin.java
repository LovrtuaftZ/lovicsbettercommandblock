package com.zhuhongming.bettercommandblock.mixin;

import com.zhuhongming.bettercommandblock.client.command.CommandArgumentSpan;
import com.zhuhongming.bettercommandblock.client.command.CommandSyntaxHintResolver;
import com.zhuhongming.bettercommandblock.client.widget.BetterCommandMultiLineEditBox;
import com.zhuhongming.bettercommandblock.client.widget.BetterCommandSingleLineEditBox;
import com.zhuhongming.bettercommandblock.client.widget.BetterReadonlyWrapPane;
import com.zhuhongming.bettercommandblock.client.widget.BetterSuggestionPane;
import com.zhuhongming.bettercommandblock.client.CommandBlockItemEditScreen;
import com.zhuhongming.bettercommandblock.api.CommandBlockItemEditScreenAccess;
import com.zhuhongming.bettercommandblock.api.AbstractCommandBlockEditScreenAccess;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.network.chat.Component;
import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import com.zhuhongming.bettercommandblock.api.CommandBlockEditScreenModeAccess;
import com.zhuhongming.bettercommandblock.preset.CommandPreset;
import com.zhuhongming.bettercommandblock.preset.CommandPresetExportContext;
import com.zhuhongming.bettercommandblock.client.input.MouseInputCleanup;
import com.zhuhongming.bettercommandblock.preset.CommandPresetExportScreen;
import com.zhuhongming.bettercommandblock.preset.CommandPresetMenuScreen;
import com.zhuhongming.bettercommandblock.util.CommandBlockItemData;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AbstractCommandBlockEditScreen.class)
public abstract class AbstractCommandBlockEditScreenMixin implements AbstractCommandBlockEditScreenAccess {
    @Unique
    private static final int PANEL_BG = 0xD92A2A2A;
    @Unique
    private static final int HOVER_BG = 0x664C4C4C;
    @Unique
    private static final int TITLE_COLOR = 0x9CCFE3;
    @Unique
    private static final int TEXT_WHITE = 0xFFFFFF;
    @Unique
    private static final int TEXT_GREEN = 0x9EEA9E;
    @Unique
    private static final int TEXT_GRAY = 0xBFBFBF;
    @Unique
    private static final int SYNTAX_ACTIVE_COLOR = 0xFFFF55;
    @Unique
    private static final int SYNTAX_INACTIVE_COLOR = 0xBFBFBF;
    @Unique
    private static final Component EXIT_FOCUS_HINT = Component.literal("[Esc] 退出聚焦输入框");
    @Unique
    private static final Component EXIT_FOCUS_BUTTON = Component.literal("x");
    @Unique
    private static final Pattern SYNTAX_ELEMENT_PATTERN = Pattern.compile("<[^<>]+>");

    @Shadow
    protected EditBox commandEdit;

    @Shadow
    CommandSuggestions commandSuggestions;

    @Shadow
    protected EditBox previousEdit;

    @Shadow
    protected net.minecraft.client.gui.components.Button doneButton;

    @Shadow
    protected net.minecraft.client.gui.components.Button cancelButton;

    @Shadow
    protected net.minecraft.client.gui.components.CycleButton<Boolean> outputButton;

    @Shadow
    abstract int getPreviousY();

    @Shadow
    public abstract BaseCommandBlock getCommandBlock();

    @Unique
    private boolean betterCommandBlock$exportReturnRestorePending;
    @Unique
    private String betterCommandBlock$savedCommandForExportReturn = "";
    @Unique
    private String betterCommandBlock$savedNameForExportReturn = "";
    @Unique
    private CommandBlockEntity.Mode betterCommandBlock$savedMode = CommandBlockEntity.Mode.REDSTONE;
    @Unique
    private boolean betterCommandBlock$savedConditional;
    @Unique
    private boolean betterCommandBlock$savedAutomatic;
    @Unique
    private boolean betterCommandBlock$savedModeFieldsValid;

    @Unique
    private CommandPreset betterCommandBlock$pendingImportPreset;

    @Unique
    private BetterCommandMultiLineEditBox betterCommandBlock$multilineEdit;

    @Unique
    private BetterCommandSingleLineEditBox betterCommandBlock$nameEdit;

    @Unique
    private int betterCommandBlock$commandLabelY;

    @Unique
    private boolean betterCommandBlock$supportsCustomName;

    @Unique
    private boolean betterCommandBlock$syncing;
    @Unique
    private boolean betterCommandBlock$wasInputFocused;
    @Unique
    private int betterCommandBlock$lastSyncedCursor = -1;
    @Unique
    private String betterCommandBlock$lastSyncedCommand = "";
    @Unique
    private CommandSyntaxHintResolver.State betterCommandBlock$cachedSyntaxHints = CommandSyntaxHintResolver.State.empty();
    @Unique
    private String betterCommandBlock$cachedSyntaxHintCommand = "";
    @Unique
    private int betterCommandBlock$cachedSyntaxHintCursor = -1;
    @Unique
    private List<String> betterCommandBlock$lastDetailLines = List.of();
    @Unique
    private int betterCommandBlock$lastDetailHighlight = -2;

    @Unique
    private int betterCommandBlock$panelX;
    @Unique
    private int betterCommandBlock$panelY;
    @Unique
    private int betterCommandBlock$panelW;
    @Unique
    private int betterCommandBlock$panelH;
    @Unique
    private int betterCommandBlock$hintBandY;
    @Unique
    private BetterSuggestionPane betterCommandBlock$suggestionPane;
    @Unique
    private BetterReadonlyWrapPane betterCommandBlock$suggestionDetailPane;
    @Unique
    private int betterCommandBlock$exitButtonX;
    @Unique
    private int betterCommandBlock$exitButtonY;
    @Unique
    private int betterCommandBlock$exitButtonW;
    @Unique
    private int betterCommandBlock$exitButtonH;
    @Unique
    private Button betterCommandBlock$commandTabButton;
    @Unique
    private Button betterCommandBlock$exportTabButton;
    @Unique
    private Button betterCommandBlock$importTabButton;

    @Unique
    @Override
    public CommandPresetExportContext bettercommandblock$buildExportContext() {
        String command = this.betterCommandBlock$getEditedCommandValue();
        String name = this.bettercommandblock$getCustomNameValue();
        if ((Object) this instanceof CommandBlockEditScreen) {
            CommandBlockEntity entity =
                    ((CommandBlockEditScreenAccessor) this).bettercommandblock$getCommandBlockEntity();
            return new CommandPresetExportContext(
                    name,
                    command,
                    betterCommandBlock$modeToBlockType(entity.getMode()),
                    entity.isConditional(),
                    entity.isAutomatic());
        }
        if ((Object) this instanceof CommandBlockItemEditScreenAccess itemScreen) {
            return new CommandPresetExportContext(
                    name,
                    command,
                    betterCommandBlock$modeToBlockType(itemScreen.bettercommandblock$getEditedMode()),
                    itemScreen.bettercommandblock$isEditedConditional(),
                    itemScreen.bettercommandblock$isEditedAutomatic());
        }
        return new CommandPresetExportContext(name, command, "pulse", false, false);
    }

    @Unique
    private static String betterCommandBlock$modeToBlockType(CommandBlockEntity.Mode mode) {
        if (mode == CommandBlockEntity.Mode.SEQUENCE) {
            return "chain";
        }
        if (mode == CommandBlockEntity.Mode.AUTO) {
            return "repeat";
        }
        return "pulse";
    }

    @Unique
    @Override
    public String bettercommandblock$getCustomNameValue() {
        if (this.betterCommandBlock$nameEdit == null) {
            return "";
        }
        return this.betterCommandBlock$nameEdit.getValue();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void betterCommandBlock$injectInit(CallbackInfo ci) {
        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        int screenWidth = screenAccessor.bettercommandblock$getWidth();
        int screenHeight = screenAccessor.bettercommandblock$getHeight();
        this.betterCommandBlock$supportsCustomName = (Object) this instanceof CommandBlockEditScreen
                || (Object) this instanceof CommandBlockItemEditScreenAccess;
        int nameBandHeight = this.betterCommandBlock$supportsCustomName ? 18 : 0;
        this.betterCommandBlock$panelW = 392;
        this.betterCommandBlock$panelH = 252 + nameBandHeight;
        this.betterCommandBlock$panelX = (screenWidth - this.betterCommandBlock$panelW) / 2;
        this.betterCommandBlock$panelY = (screenHeight - this.betterCommandBlock$panelH) / 2;
        int boxX = this.betterCommandBlock$panelX + 12;
        int boxWidth = this.betterCommandBlock$panelW - 24;
        int boxHeight = 96;

        if (this.betterCommandBlock$supportsCustomName) {
            int nameBoxY = this.betterCommandBlock$panelY + 22;
            this.betterCommandBlock$nameEdit = new BetterCommandSingleLineEditBox(
                    screenAccessor.bettercommandblock$getMinecraft(),
                    screenAccessor.bettercommandblock$getFont(),
                    boxX,
                    nameBoxY,
                    boxWidth,
                    Component.translatable("lovicsbettercommandblock.command_block.name.placeholder"),
                    Component.translatable("lovicsbettercommandblock.command_block.name.placeholder"));
            screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$nameEdit);
            screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$nameEdit);
            screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$nameEdit);

            Component storedName = this.betterCommandBlock$resolveStoredCustomName();
            if (storedName != null) {
                this.betterCommandBlock$nameEdit.setValue(storedName.getString());
            }
            this.betterCommandBlock$commandLabelY = nameBoxY + 18 + 4;
        } else {
            this.betterCommandBlock$commandLabelY = this.betterCommandBlock$panelY + 28;
        }

        int boxY = this.betterCommandBlock$commandLabelY + 12;

        this.betterCommandBlock$multilineEdit = new BetterCommandMultiLineEditBox(
                screenAccessor.bettercommandblock$getMinecraft(),
                screenAccessor.bettercommandblock$getFont(),
                boxX,
                boxY,
                boxWidth,
                boxHeight,
                Component.empty(),
                Component.translatable("advMode.command"));
        screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$multilineEdit);
        screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$multilineEdit);
        screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$multilineEdit);
        this.betterCommandBlock$multilineEdit.setCharacterLimit(32500);
        this.betterCommandBlock$multilineEdit.setValue(this.commandEdit.getValue());
        this.betterCommandBlock$multilineEdit.resetUndoBaseline();
        this.betterCommandBlock$multilineEdit.setValueListener(this::betterCommandBlock$onMultilineEdited);
        int paneY = boxY + boxHeight + 18;
        int paneHeight = Math.max(24, this.betterCommandBlock$panelY + this.betterCommandBlock$panelH - 12 - paneY);
        int leftPaneWidth = Math.min((this.betterCommandBlock$panelW - 32) / 2, 176);
        this.betterCommandBlock$suggestionPane = new BetterSuggestionPane(
                screenAccessor.bettercommandblock$getFont(),
                boxX,
                paneY,
                leftPaneWidth,
                paneHeight);
        this.betterCommandBlock$suggestionPane.visible = false;
        this.betterCommandBlock$suggestionPane.setClickSuggestionListener(this::betterCommandBlock$applySuggestionByMouseClick);
        screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$suggestionPane);
        screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$suggestionPane);
        screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$suggestionPane);
        this.betterCommandBlock$suggestionDetailPane = new BetterReadonlyWrapPane(
                screenAccessor.bettercommandblock$getFont(),
                boxX + leftPaneWidth + 8,
                paneY,
                boxWidth - leftPaneWidth - 8,
                paneHeight);
        this.betterCommandBlock$suggestionDetailPane.visible = false;
        screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$suggestionDetailPane);
        screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$suggestionDetailPane);
        screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$suggestionDetailPane);

        this.commandEdit.setVisible(false);
        this.commandEdit.setFocused(false);
        this.commandEdit.setEditable(true);
        this.commandEdit.setX(boxX);
        this.betterCommandBlock$hintBandY = boxY + boxHeight + 6;
        this.commandEdit.setY(this.betterCommandBlock$hintBandY);
        this.commandEdit.setWidth(boxWidth);
        ((Screen) (Object) this).setFocused(this.betterCommandBlock$multilineEdit);

        this.commandSuggestions = new CommandSuggestions(
                screenAccessor.bettercommandblock$getMinecraft(),
                (Screen) (Object) this,
                this.commandEdit,
                screenAccessor.bettercommandblock$getFont(),
                true,
                true,
                0,
                6,
                false,
                Integer.MIN_VALUE);
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();

        int previousLabelY = this.betterCommandBlock$hintBandY + 16;
        this.previousEdit.setX(boxX);
        this.previousEdit.setY(previousLabelY + 10);
        this.previousEdit.setWidth(boxWidth - 34);
        this.outputButton.setX(boxX + boxWidth - 24);
        this.outputButton.setY(this.previousEdit.getY());
        int doneWidth = 86;
        int cancelWidth = 86;
        this.doneButton.setWidth(doneWidth);
        this.cancelButton.setWidth(cancelWidth);
        this.doneButton.setX(this.betterCommandBlock$panelX + this.betterCommandBlock$panelW - 12 - doneWidth);
        this.doneButton.setY(this.betterCommandBlock$panelY + this.betterCommandBlock$panelH - 28);
        this.cancelButton.setX(this.doneButton.getX() - 8 - cancelWidth);
        this.cancelButton.setY(this.doneButton.getY());
        this.betterCommandBlock$setupTextButton(this.doneButton);
        this.betterCommandBlock$setupTextButton(this.cancelButton);
        this.betterCommandBlock$setupTextButton(this.outputButton);
        this.previousEdit.setBordered(false);
        this.betterCommandBlock$initTabButtons(screenAccessor);
        if ((Object) this instanceof CommandBlockEditScreen) {
            this.bettercommandblock$applyExportReturnRestore();
        }
        this.betterCommandBlock$applyPendingPresetImport();
        this.bettercommandblock$refreshEditorFieldsFromSource();
    }

    @Override
    @Unique
    public void bettercommandblock$refreshEditorFieldsFromSource() {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        String commandEditValue = this.commandEdit.getValue();
        String blockCommand = this.getCommandBlock().getCommand();
        if (blockCommand == null) {
            blockCommand = "";
        }

        if (commandEditValue.isEmpty() && !blockCommand.isEmpty()) {
            this.betterCommandBlock$syncing = true;
            try {
                this.commandEdit.setValue(blockCommand);
                commandEditValue = blockCommand;
            } finally {
                this.betterCommandBlock$syncing = false;
            }
        }

        String multilineValue = this.betterCommandBlock$multilineEdit.getValue();
        if (multilineValue.isEmpty() && !commandEditValue.isEmpty()) {
            this.betterCommandBlock$syncing = true;
            try {
                this.betterCommandBlock$multilineEdit.setValueFromSync(commandEditValue);
            } finally {
                this.betterCommandBlock$syncing = false;
            }
        }

        this.betterCommandBlock$hydrateNameFieldIfEmpty();
    }

    @Unique
    private void betterCommandBlock$hydrateNameFieldIfEmpty() {
        if (this.betterCommandBlock$nameEdit == null || !this.betterCommandBlock$supportsCustomName) {
            return;
        }
        if (!this.betterCommandBlock$nameEdit.getValue().isEmpty()) {
            return;
        }

        Component storedName = this.betterCommandBlock$resolveStoredCustomName();
        if (storedName != null) {
            this.betterCommandBlock$nameEdit.setValue(storedName.getString());
        }
    }

    @Unique
    @Nullable
    private Component betterCommandBlock$resolveStoredCustomName() {
        if ((Object) this instanceof CommandBlockItemEditScreenAccess itemScreen) {
            return CommandBlockItemData.getDisplayName(itemScreen.bettercommandblock$getEditedItemStack());
        }
        if ((Object) this instanceof CommandBlockEditScreen) {
            CommandBlockEntity commandBlockEntity =
                    ((CommandBlockEditScreenAccessor) this).bettercommandblock$getCommandBlockEntity();
            return ((CommandBlockEntityNameAccessor) commandBlockEntity).bettercommandblock$getStoredCustomName();
        }
        return null;
    }

    @Unique
    private String betterCommandBlock$getEditedCommandValue() {
        if (this.betterCommandBlock$multilineEdit != null) {
            return this.betterCommandBlock$multilineEdit.getValue();
        }
        return this.commandEdit.getValue();
    }

    @Unique
    private void betterCommandBlock$saveEditorStateBeforeExport() {
        String command = this.betterCommandBlock$getEditedCommandValue();
        if (command == null) {
            command = "";
        }
        this.betterCommandBlock$savedCommandForExportReturn = command;
        this.betterCommandBlock$savedNameForExportReturn = this.bettercommandblock$getCustomNameValue();
        this.betterCommandBlock$exportReturnRestorePending = true;
        this.getCommandBlock().setCommand(command);
        if ((Object) this instanceof CommandBlockItemEditScreen itemScreen) {
            this.betterCommandBlock$savedMode = itemScreen.bettercommandblock$getEditedMode();
            this.betterCommandBlock$savedConditional = itemScreen.bettercommandblock$isEditedConditional();
            this.betterCommandBlock$savedAutomatic = itemScreen.bettercommandblock$isEditedAutomatic();
            this.betterCommandBlock$savedModeFieldsValid = true;
        } else {
            this.betterCommandBlock$savedModeFieldsValid = false;
        }
    }

    @Override
    @Unique
    public void bettercommandblock$applyExportReturnRestore() {
        if (!this.betterCommandBlock$exportReturnRestorePending) {
            return;
        }
        String command = this.betterCommandBlock$savedCommandForExportReturn;
        this.commandEdit.setValue(command);
        this.getCommandBlock().setCommand(command);
        if (this.betterCommandBlock$multilineEdit != null) {
            this.betterCommandBlock$multilineEdit.setValue(command);
            this.betterCommandBlock$multilineEdit.resetUndoBaseline();
        }
        if (this.betterCommandBlock$nameEdit != null) {
            this.betterCommandBlock$nameEdit.setValue(this.betterCommandBlock$savedNameForExportReturn);
        }
        this.betterCommandBlock$exportReturnRestorePending = false;
    }

    @Override
    @Unique
    public boolean bettercommandblock$shouldRestoreItemModeFields() {
        return this.betterCommandBlock$exportReturnRestorePending && this.betterCommandBlock$savedModeFieldsValid;
    }

    @Override
    @Unique
    public CommandBlockEntity.Mode bettercommandblock$getSavedMode() {
        return this.betterCommandBlock$savedMode;
    }

    @Override
    @Unique
    public boolean bettercommandblock$getSavedConditional() {
        return this.betterCommandBlock$savedConditional;
    }

    @Override
    @Unique
    public boolean bettercommandblock$getSavedAutomatic() {
        return this.betterCommandBlock$savedAutomatic;
    }

    @Override
    @Unique
    public void bettercommandblock$schedulePresetImport(CommandPreset preset) {
        this.betterCommandBlock$pendingImportPreset = preset;
    }

    @Unique
    private void betterCommandBlock$applyPendingPresetImport() {
        CommandPreset preset = this.betterCommandBlock$pendingImportPreset;
        if (preset == null) {
            return;
        }
        this.betterCommandBlock$pendingImportPreset = null;
        this.betterCommandBlock$applyImportedPreset(preset);
    }

    @Unique
    private void betterCommandBlock$applyImportedPreset(CommandPreset preset) {
        String command = preset.getCommand();
        this.commandEdit.setValue(command);
        this.getCommandBlock().setCommand(command);
        if (this.betterCommandBlock$multilineEdit != null) {
            this.betterCommandBlock$multilineEdit.setValue(command);
            this.betterCommandBlock$multilineEdit.resetUndoBaseline();
        }
        if (this.betterCommandBlock$nameEdit != null) {
            this.betterCommandBlock$nameEdit.setValue(preset.getName());
        }

        CommandBlockEntity.Mode mode = this.betterCommandBlock$blockTypeToMode(preset.getBlockType());
        boolean conditional = preset.isConditional();
        boolean autoexec = preset.isAlwaysActive();
        if ((Object) this instanceof CommandBlockEditScreenModeAccess modeAccess) {
            modeAccess.bettercommandblock$applyImportedModeFields(mode, conditional, autoexec);
        } else if ((Object) this instanceof CommandBlockItemEditScreen itemScreen) {
            itemScreen.bettercommandblock$restoreEditorMode(mode, conditional, autoexec);
        }
    }

    @Unique
    private static CommandBlockEntity.Mode betterCommandBlock$blockTypeToMode(String blockType) {
        if ("chain".equals(blockType)) {
            return CommandBlockEntity.Mode.SEQUENCE;
        }
        if ("repeat".equals(blockType)) {
            return CommandBlockEntity.Mode.AUTO;
        }
        return CommandBlockEntity.Mode.REDSTONE;
    }

    @Unique
    private void betterCommandBlock$initTabButtons(ScreenAccessor screenAccessor) {
        int tabY = this.betterCommandBlock$panelY + 6;
        int tabWidth = 60;
        int exportTabX = this.betterCommandBlock$panelX + this.betterCommandBlock$panelW - 12 - tabWidth;
        int importTabX = exportTabX - 4 - tabWidth;
        int commandTabX = importTabX - 4 - tabWidth;
        Screen screen = (Screen) (Object) this;
        this.betterCommandBlock$commandTabButton = Button.builder(
                        Component.translatable("lovicsbettercommandblock.tab.command_edit"),
                        button -> {})
                .bounds(commandTabX, tabY, tabWidth, 16)
                .build();
        this.betterCommandBlock$importTabButton = Button.builder(
                        Component.translatable("lovicsbettercommandblock.tab.import_template"),
                        button -> this.betterCommandBlock$openImportScreen())
                .bounds(importTabX, tabY, tabWidth, 16)
                .build();
        this.betterCommandBlock$exportTabButton = Button.builder(
                        Component.translatable("lovicsbettercommandblock.tab.export_template"),
                        button -> this.betterCommandBlock$openExportScreen())
                .bounds(exportTabX, tabY, tabWidth, 16)
                .build();
        screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$commandTabButton);
        screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$commandTabButton);
        screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$commandTabButton);
        screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$importTabButton);
        screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$importTabButton);
        screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$importTabButton);
        screenAccessor.bettercommandblock$getRenderables().add(this.betterCommandBlock$exportTabButton);
        screenAccessor.bettercommandblock$getChildren().add(this.betterCommandBlock$exportTabButton);
        screenAccessor.bettercommandblock$getNarratables().add(this.betterCommandBlock$exportTabButton);
    }

    @Unique
    private void betterCommandBlock$openImportScreen() {
        Screen screen = (Screen) (Object) this;
        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        screenAccessor.bettercommandblock$getMinecraft()
                .setScreen(new CommandPresetMenuScreen(screen, CommandPresetMenuScreen.Mode.IMPORT_TO_EDITOR));
    }

    @Unique
    private void betterCommandBlock$openExportScreen() {
        this.betterCommandBlock$saveEditorStateBeforeExport();
        Screen screen = (Screen) (Object) this;
        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        screenAccessor.bettercommandblock$getMinecraft()
                .setScreen(new CommandPresetExportScreen(screen, this.bettercommandblock$buildExportContext()));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void betterCommandBlock$injectTick(CallbackInfo ci) {
        if (this.betterCommandBlock$nameEdit != null) {
            this.betterCommandBlock$nameEdit.tick();
        }
        if (this.betterCommandBlock$multilineEdit != null) {
            this.betterCommandBlock$multilineEdit.tick();
            boolean focused = this.betterCommandBlock$shouldShowHints();
            this.betterCommandBlock$syncLowerAreaVisibility(focused);
            if (focused && !this.betterCommandBlock$wasInputFocused) {
                this.betterCommandBlock$cachedSyntaxHintCursor = -1;
                this.betterCommandBlock$syncCommandEditFromMultiline(true);
                this.commandSuggestions.updateCommandInfo();
                this.commandSuggestions.showSuggestions(false);
            } else if (focused) {
                this.betterCommandBlock$refreshSuggestionsIfCursorMoved();
            }
            this.betterCommandBlock$updateCachedSyntaxHints();
            this.betterCommandBlock$refreshSuggestionPane();
            this.betterCommandBlock$wasInputFocused = focused;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void betterCommandBlock$injectRenderSync(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.betterCommandBlock$syncing || this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        String commandEditValue = this.commandEdit.getValue();
        String multilineValue = this.betterCommandBlock$multilineEdit.getValue();
        if (commandEditValue.equals(multilineValue)) {
            return;
        }
        if (commandEditValue.isEmpty() && !multilineValue.isEmpty()) {
            this.betterCommandBlock$syncCommandEditFromMultiline(false);
            return;
        }
        if (!commandEditValue.isEmpty()) {
            this.betterCommandBlock$syncing = true;
            try {
                this.betterCommandBlock$multilineEdit.setValueFromSync(commandEditValue);
            } finally {
                this.betterCommandBlock$syncing = false;
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void betterCommandBlock$injectRenderCustom(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        ScreenAccessor accessor = (ScreenAccessor) this;
        guiGraphics.fill(0, 0, accessor.bettercommandblock$getWidth(), accessor.bettercommandblock$getHeight(), 0x90000000);
        guiGraphics.fill(
                this.betterCommandBlock$panelX,
                this.betterCommandBlock$panelY,
                this.betterCommandBlock$panelX + this.betterCommandBlock$panelW,
                this.betterCommandBlock$panelY + this.betterCommandBlock$panelH,
                PANEL_BG);

        guiGraphics.drawString(
                accessor.bettercommandblock$getFont(),
                Component.translatable("advMode.setCommand"),
                this.betterCommandBlock$panelX + 12,
                this.betterCommandBlock$panelY + 8,
                TITLE_COLOR);
        if (this.betterCommandBlock$commandTabButton != null
                && this.betterCommandBlock$importTabButton != null
                && this.betterCommandBlock$exportTabButton != null) {
            this.betterCommandBlock$renderTextButton(
                    guiGraphics, accessor, this.betterCommandBlock$commandTabButton, mouseX, mouseY, TITLE_COLOR);
            this.betterCommandBlock$renderTextButton(
                    guiGraphics, accessor, this.betterCommandBlock$importTabButton, mouseX, mouseY, TEXT_WHITE);
            this.betterCommandBlock$renderTextButton(
                    guiGraphics, accessor, this.betterCommandBlock$exportTabButton, mouseX, mouseY, TEXT_WHITE);
        }
        guiGraphics.drawString(
                accessor.bettercommandblock$getFont(),
                Component.translatable("advMode.command"),
                this.betterCommandBlock$panelX + 12,
                this.betterCommandBlock$commandLabelY,
                TEXT_GRAY);

        boolean focusedInput = this.betterCommandBlock$shouldShowHints();
        this.betterCommandBlock$renderExitFocusHints(guiGraphics, accessor, mouseX, mouseY, focusedInput);
        if (this.betterCommandBlock$nameEdit != null) {
            this.betterCommandBlock$nameEdit.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.betterCommandBlock$multilineEdit.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!focusedInput) {
            guiGraphics.drawString(
                    accessor.bettercommandblock$getFont(),
                    Component.translatable("advMode.previousOutput"),
                    this.betterCommandBlock$panelX + 12,
                    this.previousEdit.getY() - 10,
                    TEXT_GRAY);
            this.previousEdit.render(guiGraphics, mouseX, mouseY, partialTick);
            for (Renderable renderable : accessor.bettercommandblock$getRenderables()) {
                if (renderable instanceof AbstractWidget widget
                        && widget != this.betterCommandBlock$multilineEdit
                        && widget != this.betterCommandBlock$nameEdit
                        && widget != this.betterCommandBlock$suggestionPane
                        && widget != this.betterCommandBlock$suggestionDetailPane
                        && widget != this.betterCommandBlock$commandTabButton
                        && widget != this.betterCommandBlock$importTabButton
                        && widget != this.betterCommandBlock$exportTabButton
                        && widget != this.previousEdit
                        && widget != this.commandEdit) {
                    int color = widget instanceof CycleButton<?> ? TEXT_GREEN : TEXT_WHITE;
                    this.betterCommandBlock$renderTextButton(guiGraphics, accessor, widget, mouseX, mouseY, color);
                }
            }
        } else {
            this.betterCommandBlock$resetDefaultCursor();
            this.betterCommandBlock$updateCachedSyntaxHints();
            this.betterCommandBlock$renderSyntaxUsageAboveSuggestions(guiGraphics, accessor);
            if (this.betterCommandBlock$suggestionPane != null) {
                this.betterCommandBlock$suggestionPane.render(guiGraphics, mouseX, mouseY, partialTick);
                this.betterCommandBlock$suggestionPane.applyCursorStyle(mouseX, mouseY);
            }
            if (this.betterCommandBlock$suggestionDetailPane != null) {
                this.betterCommandBlock$suggestionDetailPane.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        ci.cancel();
    }

    @Unique
    private void betterCommandBlock$onMultilineEdited(String value) {
        if (this.betterCommandBlock$syncing) {
            return;
        }

        this.betterCommandBlock$syncCommandEditFromMultiline(true);
    }

    @Unique
    private void betterCommandBlock$syncCommandEditFromMultiline(boolean refreshSuggestions) {
        if (this.betterCommandBlock$syncing || this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        String value = this.betterCommandBlock$multilineEdit.getValue();
        int cursor = this.betterCommandBlock$multilineEdit.getCursorPosition();
        this.betterCommandBlock$syncing = true;
        try {
            if (!value.equals(this.commandEdit.getValue())) {
                this.commandEdit.setValue(value);
            }
            this.commandEdit.setCursorPosition(Math.min(cursor, value.length()));
        } finally {
            this.betterCommandBlock$syncing = false;
        }

        this.betterCommandBlock$lastSyncedCommand = value;
        this.betterCommandBlock$lastSyncedCursor = cursor;
        if (refreshSuggestions && this.betterCommandBlock$shouldShowHints()) {
            this.commandSuggestions.updateCommandInfo();
            this.commandSuggestions.showSuggestions(false);
        }
    }

    @Unique
    private void betterCommandBlock$refreshSuggestionsIfCursorMoved() {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        String value = this.betterCommandBlock$multilineEdit.getValue();
        int cursor = this.betterCommandBlock$multilineEdit.getCursorPosition();
        if (value.equals(this.betterCommandBlock$lastSyncedCommand)
                && cursor == this.betterCommandBlock$lastSyncedCursor) {
            return;
        }

        this.betterCommandBlock$syncCommandEditFromMultiline(true);
    }

    @Unique
    private void betterCommandBlock$syncMultilineFromCommandEdit() {
        if (this.betterCommandBlock$syncing || this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        String value = this.commandEdit.getValue();
        int cursor = this.commandEdit.getCursorPosition();
        this.betterCommandBlock$syncing = true;
        try {
            if (!value.equals(this.betterCommandBlock$multilineEdit.getValue())) {
                this.betterCommandBlock$multilineEdit.setValueFromSync(value);
            }
            this.betterCommandBlock$multilineEdit.setCursorPosition(cursor);
        } finally {
            this.betterCommandBlock$syncing = false;
        }

        this.betterCommandBlock$lastSyncedCommand = value;
        this.betterCommandBlock$lastSyncedCursor = cursor;
    }

    @Unique
    private void betterCommandBlock$setupTextButton(AbstractWidget widget) {
        widget.setAlpha(0.0F);
    }

    @Unique
    private void betterCommandBlock$renderTextButton(
            GuiGraphics guiGraphics,
            ScreenAccessor accessor,
            AbstractWidget widget,
            int mouseX,
            int mouseY,
            int textColor) {
        if (!widget.visible) {
            return;
        }
        if (widget.isMouseOver(mouseX, mouseY)) {
            guiGraphics.fill(widget.getX(), widget.getY(), widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight(), HOVER_BG);
        }
        int y = widget.getY() + (widget.getHeight() - 8) / 2;
        int textWidth = accessor.bettercommandblock$getFont().width(widget.getMessage());
        int x = widget.getX() + (widget.getWidth() - textWidth) / 2;
        guiGraphics.drawString(accessor.bettercommandblock$getFont(), widget.getMessage(), x, y, textColor);
    }

    @Unique
    private void betterCommandBlock$resetDefaultCursor() {
        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        long window = screenAccessor.bettercommandblock$getMinecraft().getWindow().getWindow();
        GLFW.glfwSetCursor(window, 0L);
    }

    @Unique
    private void betterCommandBlock$updateCachedSyntaxHints() {
        if (this.betterCommandBlock$multilineEdit == null || !this.betterCommandBlock$shouldShowHints()) {
            return;
        }

        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        if (screenAccessor.bettercommandblock$getMinecraft().player == null
                || screenAccessor.bettercommandblock$getMinecraft().player.connection == null) {
            return;
        }

        String command = this.betterCommandBlock$multilineEdit.getValue();
        int cursor = this.betterCommandBlock$multilineEdit.getCursorPosition();
        if (command.equals(this.betterCommandBlock$cachedSyntaxHintCommand)
                && cursor == this.betterCommandBlock$cachedSyntaxHintCursor) {
            return;
        }

        CommandSyntaxHintResolver.State previous = this.betterCommandBlock$cachedSyntaxHints;
        CommandSyntaxHintResolver.State resolved =
                CommandSyntaxHintResolver.resolve(
                        command,
                        cursor,
                        screenAccessor.bettercommandblock$getMinecraft().player.connection.getCommands(),
                        screenAccessor.bettercommandblock$getMinecraft().player.connection.getSuggestionsProvider());
        if (resolved.isPresent()) {
            this.betterCommandBlock$cachedSyntaxHints = resolved;
        } else if (previous.isPresent()
                && command.equals(this.betterCommandBlock$cachedSyntaxHintCommand)) {
            int activeIndex =
                    CommandSyntaxHintResolver.resolveActiveIndexOnly(
                            command,
                            cursor,
                            screenAccessor.bettercommandblock$getMinecraft().player.connection.getCommands(),
                            screenAccessor
                                    .bettercommandblock$getMinecraft()
                                    .player
                                    .connection
                                    .getSuggestionsProvider());
            if (activeIndex < 0 || activeIndex >= previous.syntaxElements().size()) {
                activeIndex = CommandArgumentSpan.argumentIndexAt(command, cursor);
            }
            if (activeIndex >= 0 && activeIndex < previous.syntaxElements().size()) {
                this.betterCommandBlock$cachedSyntaxHints =
                        new CommandSyntaxHintResolver.State(
                                previous.usageLine(), previous.syntaxElements(), activeIndex);
            } else {
                this.betterCommandBlock$cachedSyntaxHints = resolved;
            }
        } else {
            this.betterCommandBlock$cachedSyntaxHints = resolved;
        }
        this.betterCommandBlock$cachedSyntaxHintCommand = command;
        this.betterCommandBlock$cachedSyntaxHintCursor = cursor;
    }

    @Unique
    private void betterCommandBlock$renderSyntaxUsageAboveSuggestions(GuiGraphics guiGraphics, ScreenAccessor accessor) {
        CommandSyntaxHintResolver.State hints = this.betterCommandBlock$cachedSyntaxHints;
        if (!hints.isPresent()) {
            List<FormattedCharSequence> usage =
                    ((CommandSuggestionsAccessor) (Object) this.commandSuggestions).bettercommandblock$getCommandUsage();
            if (usage.isEmpty()) {
                return;
            }
            hints = new CommandSyntaxHintResolver.State(
                    this.betterCommandBlock$toPlainString(usage.get(0)),
                    this.betterCommandBlock$extractSyntaxElements(usage),
                    this.betterCommandBlock$resolveActiveSyntaxIndex(
                            this.betterCommandBlock$extractSyntaxElements(usage)));
        }

        String plainUsage = hints.usageLine();
        int baseX = this.commandEdit.getX();
        int y = this.betterCommandBlock$hintBandY;
        int width = accessor.bettercommandblock$getFont().width(plainUsage);
        guiGraphics.fill(baseX - 1, y - 1, baseX + width + 3, y + 10, 0x552A2A2A);
        if (hints.syntaxElements().isEmpty() || hints.activeArgumentIndex() < 0) {
            guiGraphics.drawString(accessor.bettercommandblock$getFont(), plainUsage, baseX + 1, y, SYNTAX_INACTIVE_COLOR);
            return;
        }
        this.betterCommandBlock$renderUsageWithActiveArgument(
                guiGraphics,
                accessor,
                plainUsage,
                hints.syntaxElements(),
                hints.activeArgumentIndex(),
                baseX + 1,
                y);
    }

    @Unique
    private void betterCommandBlock$renderUsageWithActiveArgument(
            GuiGraphics guiGraphics,
            ScreenAccessor accessor,
            String usage,
            List<String> syntaxElements,
            int activeIndex,
            int x,
            int y) {
        Matcher matcher = SYNTAX_ELEMENT_PATTERN.matcher(usage);
        int drawX = x;
        int lastEnd = 0;
        int placeholderIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String literal = usage.substring(lastEnd, matcher.start());
                guiGraphics.drawString(
                        accessor.bettercommandblock$getFont(), literal, drawX, y, SYNTAX_INACTIVE_COLOR);
                drawX += accessor.bettercommandblock$getFont().width(literal);
            }
            String token = matcher.group();
            int color = placeholderIndex == activeIndex ? SYNTAX_ACTIVE_COLOR : SYNTAX_INACTIVE_COLOR;
            guiGraphics.drawString(accessor.bettercommandblock$getFont(), token, drawX, y, color);
            drawX += accessor.bettercommandblock$getFont().width(token);
            lastEnd = matcher.end();
            placeholderIndex++;
        }
        if (lastEnd < usage.length()) {
            guiGraphics.drawString(
                    accessor.bettercommandblock$getFont(),
                    usage.substring(lastEnd),
                    drawX,
                    y,
                    SYNTAX_INACTIVE_COLOR);
        }
    }

    @Unique
    private void betterCommandBlock$renderExitFocusHints(
            GuiGraphics guiGraphics, ScreenAccessor accessor, int mouseX, int mouseY, boolean focusedInput) {
        if (!focusedInput) {
            this.betterCommandBlock$exitButtonW = 0;
            this.betterCommandBlock$exitButtonH = 0;
            return;
        }
        int rowY = this.betterCommandBlock$commandLabelY;
        int buttonSize = 12;
        int rightPadding = 12;
        int gap = 6;
        int rowRight = this.betterCommandBlock$panelX + this.betterCommandBlock$panelW - rightPadding;
        this.betterCommandBlock$exitButtonW = buttonSize;
        this.betterCommandBlock$exitButtonH = buttonSize;
        this.betterCommandBlock$exitButtonX = rowRight - buttonSize;
        this.betterCommandBlock$exitButtonY = rowY - 2;
        int hintColor = this.betterCommandBlock$isOverExitFocusButton(mouseX, mouseY) ? TEXT_WHITE : TEXT_GRAY;
        int hintX =
                this.betterCommandBlock$exitButtonX
                        - gap
                        - accessor.bettercommandblock$getFont().width(EXIT_FOCUS_HINT);
        guiGraphics.drawString(accessor.bettercommandblock$getFont(), EXIT_FOCUS_HINT, hintX, rowY, hintColor);
        guiGraphics.fill(
                this.betterCommandBlock$exitButtonX,
                this.betterCommandBlock$exitButtonY,
                this.betterCommandBlock$exitButtonX + this.betterCommandBlock$exitButtonW,
                this.betterCommandBlock$exitButtonY + this.betterCommandBlock$exitButtonH,
                this.betterCommandBlock$isOverExitFocusButton(mouseX, mouseY) ? HOVER_BG : 0x552A2A2A);
        guiGraphics.drawCenteredString(
                accessor.bettercommandblock$getFont(),
                EXIT_FOCUS_BUTTON,
                this.betterCommandBlock$exitButtonX + this.betterCommandBlock$exitButtonW / 2,
                this.betterCommandBlock$exitButtonY + 2,
                TEXT_WHITE);
    }

    @Unique
    private boolean betterCommandBlock$shouldShowHints() {
        if (this.betterCommandBlock$multilineEdit == null) {
            return false;
        }
        Screen screen = (Screen) (Object) this;
        return screen.getFocused() == this.betterCommandBlock$multilineEdit && this.betterCommandBlock$multilineEdit.isFocused();
    }

    @Unique
    private void betterCommandBlock$syncLowerAreaVisibility(boolean focusedInput) {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }
        boolean showLower = !focusedInput;
        if (this.betterCommandBlock$suggestionPane != null) {
            this.betterCommandBlock$suggestionPane.visible = focusedInput;
            this.betterCommandBlock$suggestionPane.active = focusedInput;
        }
        if (this.betterCommandBlock$suggestionDetailPane != null) {
            this.betterCommandBlock$suggestionDetailPane.visible = focusedInput;
            this.betterCommandBlock$suggestionDetailPane.active = focusedInput;
        }
        this.previousEdit.setVisible(showLower);
        this.outputButton.visible = showLower;
        this.outputButton.active = showLower;
        this.doneButton.visible = showLower;
        this.doneButton.active = showLower;
        this.cancelButton.visible = showLower;
        this.cancelButton.active = showLower;
        ScreenAccessor accessor = (ScreenAccessor) this;
        int lowerThreshold = this.previousEdit.getY() - 12;
        for (Renderable renderable : accessor.bettercommandblock$getRenderables()) {
            if (renderable instanceof AbstractWidget widget
                    && widget != this.betterCommandBlock$multilineEdit
                    && widget != this.betterCommandBlock$nameEdit
                    && widget != this.previousEdit
                    && widget != this.commandEdit
                    && widget.getY() >= lowerThreshold) {
                widget.visible = showLower;
                widget.active = showLower;
            }
        }
    }

    @Unique
    private void betterCommandBlock$clearInputFocusAndHints() {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        screen.setFocused((GuiEventListener) null);
        this.betterCommandBlock$multilineEdit.setFocused(false);
        this.betterCommandBlock$syncLowerAreaVisibility(false);
        if (this.betterCommandBlock$suggestionPane != null) {
            this.betterCommandBlock$suggestionPane.clear();
        }
        if (this.betterCommandBlock$suggestionDetailPane != null) {
            this.betterCommandBlock$suggestionDetailPane.clear();
        }
        this.commandSuggestions.hide();
        this.betterCommandBlock$wasInputFocused = false;
    }

    @Unique
    private void betterCommandBlock$focusNameEdit() {
        if (this.betterCommandBlock$nameEdit == null) {
            return;
        }
        this.betterCommandBlock$clearInputFocusAndHints();
        Screen screen = (Screen) (Object) this;
        screen.setFocused(this.betterCommandBlock$nameEdit);
        this.betterCommandBlock$nameEdit.setFocused(true);
    }

    @Unique
    private void betterCommandBlock$focusInputAndKeepHints() {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        screen.setFocused(this.betterCommandBlock$multilineEdit);
        this.betterCommandBlock$multilineEdit.setFocused(true);
    }

    @Unique
    private boolean betterCommandBlock$isOverHintPane(double mouseX, double mouseY) {
        return (this.betterCommandBlock$suggestionPane != null
                        && this.betterCommandBlock$suggestionPane.visible
                        && this.betterCommandBlock$suggestionPane.isMouseOver(mouseX, mouseY))
                || (this.betterCommandBlock$suggestionDetailPane != null
                        && this.betterCommandBlock$suggestionDetailPane.visible
                        && this.betterCommandBlock$suggestionDetailPane.isMouseOver(mouseX, mouseY));
    }

    @Unique
    private boolean betterCommandBlock$isOverExitFocusButton(double mouseX, double mouseY) {
        return this.betterCommandBlock$exitButtonW > 0
                && mouseX >= this.betterCommandBlock$exitButtonX
                && mouseX < this.betterCommandBlock$exitButtonX + this.betterCommandBlock$exitButtonW
                && mouseY >= this.betterCommandBlock$exitButtonY
                && mouseY < this.betterCommandBlock$exitButtonY + this.betterCommandBlock$exitButtonH;
    }

    @Unique
    private void betterCommandBlock$refreshSuggestionPane() {
        if (this.betterCommandBlock$suggestionPane == null) {
            return;
        }
        if (!this.betterCommandBlock$shouldShowHints()) {
            this.betterCommandBlock$suggestionPane.clear();
            if (this.betterCommandBlock$suggestionDetailPane != null) {
                this.betterCommandBlock$suggestionDetailPane.clear();
            }
            return;
        }

        if (this.betterCommandBlock$suggestionDetailPane != null) {
            CommandSyntaxHintResolver.State hints = this.betterCommandBlock$cachedSyntaxHints;
            List<String> detailLines = new ArrayList<>(hints.syntaxElements());
            if (detailLines.isEmpty() && hints.isPresent()) {
                detailLines.add(hints.usageLine());
            }
            if (detailLines.isEmpty()) {
                List<FormattedCharSequence> usage =
                        ((CommandSuggestionsAccessor) (Object) this.commandSuggestions).bettercommandblock$getCommandUsage();
                for (FormattedCharSequence sequence : usage) {
                    detailLines.add(this.betterCommandBlock$toPlainString(sequence));
                }
            }
            if (!detailLines.isEmpty()) {
                if (!detailLines.equals(this.betterCommandBlock$lastDetailLines)) {
                    this.betterCommandBlock$suggestionDetailPane.setLines(detailLines);
                    this.betterCommandBlock$lastDetailLines = List.copyOf(detailLines);
                }
                int highlight = hints.activeArgumentIndex();
                if (highlight != this.betterCommandBlock$lastDetailHighlight) {
                    this.betterCommandBlock$suggestionDetailPane.setHighlightedLine(highlight);
                    this.betterCommandBlock$lastDetailHighlight = highlight;
                }
            }
        }

        CompletableFuture<Suggestions> pending =
                ((CommandSuggestionsDataAccessor) (Object) this.commandSuggestions).bettercommandblock$getPendingSuggestions();
        if (pending != null && pending.isDone()) {
            Suggestions suggestions = pending.join();
            List<String> lines = new ArrayList<>();
            for (Suggestion suggestion : suggestions.getList()) {
                lines.add(suggestion.getText());
            }
            this.betterCommandBlock$suggestionPane.setSuggestions(lines);
            int selected = -1;
            CommandSuggestions.SuggestionsList list =
                    ((CommandSuggestionsStateAccessor) (Object) this.commandSuggestions).bettercommandblock$getSuggestionsList();
            if (list != null) {
                selected = ((CommandSuggestionsListAccessor) (Object) list).bettercommandblock$getCurrent();
            }
            this.betterCommandBlock$suggestionPane.setSelectedIndex(selected);
        }
    }

    @Unique
    private void betterCommandBlock$applySuggestionByMouseClick(int selectedIndex) {
        if (selectedIndex < 0 || this.betterCommandBlock$multilineEdit == null) {
            return;
        }
        CommandSuggestions.SuggestionsList list =
                ((CommandSuggestionsStateAccessor) (Object) this.commandSuggestions).bettercommandblock$getSuggestionsList();
        if (list == null) {
            return;
        }
        CommandSuggestionsListAccessor listAccessor = (CommandSuggestionsListAccessor) (Object) list;
        listAccessor.bettercommandblock$setCurrent(selectedIndex);
        this.betterCommandBlock$multilineEdit.recordUndoBeforeExternalChange();
        this.betterCommandBlock$syncCommandEditFromMultiline(false);
        listAccessor.bettercommandblock$invokeUseSuggestion();
        this.betterCommandBlock$syncMultilineFromCommandEdit();
        this.betterCommandBlock$focusInputAndKeepHints();
        ((CommandSuggestionsInputAccessor) (Object) this.commandSuggestions).bettercommandblock$setKeepSuggestions(false);
        this.commandSuggestions.updateCommandInfo();
        this.commandSuggestions.showSuggestions(false);
    }

    @Unique
    private int betterCommandBlock$resolveActiveSyntaxIndex(List<String> syntaxElements) {
        if (syntaxElements.isEmpty() || this.betterCommandBlock$multilineEdit == null) {
            return -1;
        }

        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        if (screenAccessor.bettercommandblock$getMinecraft().player == null
                || screenAccessor.bettercommandblock$getMinecraft().player.connection == null) {
            return -1;
        }

        String command = this.betterCommandBlock$multilineEdit.getValue();
        int cursor = this.betterCommandBlock$multilineEdit.getCursorPosition();
        int argumentIndex =
                CommandSyntaxHintResolver.resolveActiveIndexOnly(
                        command,
                        cursor,
                        screenAccessor.bettercommandblock$getMinecraft().player.connection.getCommands(),
                        screenAccessor.bettercommandblock$getMinecraft().player.connection.getSuggestionsProvider());
        if (argumentIndex < 0 || argumentIndex >= syntaxElements.size()) {
            return -1;
        }
        return argumentIndex;
    }

    @Unique
    private String betterCommandBlock$toPlainString(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    @Unique
    private List<String> betterCommandBlock$extractSyntaxElements(List<FormattedCharSequence> usage) {
        List<String> elements = new ArrayList<>();
        for (FormattedCharSequence sequence : usage) {
            String plain = this.betterCommandBlock$toPlainString(sequence);
            Matcher matcher = SYNTAX_ELEMENT_PATTERN.matcher(plain);
            while (matcher.find()) {
                elements.add(matcher.group());
            }
        }
        return elements;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void betterCommandBlock$injectKeyPressed(
            int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        if (keyCode == 256 && this.betterCommandBlock$shouldShowHints()) {
            this.betterCommandBlock$clearInputFocusAndHints();
            cir.setReturnValue(true);
            return;
        }

        if (keyCode == 258 || keyCode == 264 || keyCode == 265) {
            Screen screen = (Screen) (Object) this;
            GuiEventListener previousFocus = screen.getFocused();
            this.betterCommandBlock$syncCommandEditFromMultiline(false);
            screen.setFocused(this.commandEdit);
            boolean handledBySuggestions = this.commandSuggestions.keyPressed(keyCode, scanCode, modifiers);
            screen.setFocused(this.betterCommandBlock$multilineEdit);
            if (handledBySuggestions) {
                String value = this.commandEdit.getValue();
                int cursor = this.commandEdit.getCursorPosition();
                if (!value.equals(this.betterCommandBlock$multilineEdit.getValue())
                        || cursor != this.betterCommandBlock$multilineEdit.getCursorPosition()) {
                    this.betterCommandBlock$multilineEdit.recordUndoBeforeExternalChange();
                    this.betterCommandBlock$syncMultilineFromCommandEdit();
                }
                ((CommandSuggestionsInputAccessor) (Object) this.commandSuggestions).bettercommandblock$setKeepSuggestions(false);
                this.commandSuggestions.updateCommandInfo();
                this.commandSuggestions.showSuggestions(false);
                cir.setReturnValue(true);
                return;
            }
            if (previousFocus != null && previousFocus != this.commandEdit) {
                screen.setFocused(previousFocus);
            }
        }
    }

    @Inject(method = "onClose()V", at = @At("HEAD"))
    private void betterCommandBlock$onClose(CallbackInfo ci) {
        this.betterCommandBlock$resetDefaultCursor();
        if (this.betterCommandBlock$suggestionPane != null) {
            this.betterCommandBlock$suggestionPane.resetInteractionState();
        }
        MouseInputCleanup.clearAllMouseState(Minecraft.getInstance());
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void betterCommandBlock$injectMouseReleased(
            double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || this.betterCommandBlock$multilineEdit == null) {
            return;
        }

        boolean handled = false;
        if (this.betterCommandBlock$suggestionPane != null && this.betterCommandBlock$suggestionPane.visible) {
            handled = this.betterCommandBlock$suggestionPane.mouseReleased(mouseX, mouseY, button) || handled;
        }
        if (this.betterCommandBlock$suggestionDetailPane != null && this.betterCommandBlock$suggestionDetailPane.visible) {
            handled = this.betterCommandBlock$suggestionDetailPane.mouseReleased(mouseX, mouseY, button) || handled;
        }
        if (handled) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void betterCommandBlock$injectMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || this.betterCommandBlock$multilineEdit == null) {
            return;
        }
        if (this.betterCommandBlock$nameEdit != null && this.betterCommandBlock$nameEdit.isMouseOver(mouseX, mouseY)) {
            this.betterCommandBlock$focusNameEdit();
            cir.setReturnValue(true);
            return;
        }
        if (this.betterCommandBlock$shouldShowHints() && this.betterCommandBlock$isOverExitFocusButton(mouseX, mouseY)) {
            this.betterCommandBlock$clearInputFocusAndHints();
            cir.setReturnValue(true);
            return;
        }
        if (this.betterCommandBlock$shouldShowHints() && this.betterCommandBlock$isOverHintPane(mouseX, mouseY)) {
            boolean handled = false;
            if (this.betterCommandBlock$suggestionPane != null && this.betterCommandBlock$suggestionPane.visible) {
                handled = this.betterCommandBlock$suggestionPane.mouseClicked(mouseX, mouseY, button) || handled;
            }
            if (this.betterCommandBlock$suggestionDetailPane != null && this.betterCommandBlock$suggestionDetailPane.visible) {
                handled = this.betterCommandBlock$suggestionDetailPane.mouseClicked(mouseX, mouseY, button) || handled;
            }
            this.betterCommandBlock$focusInputAndKeepHints();
            cir.setReturnValue(handled);
            return;
        }
        boolean overMultiline = this.betterCommandBlock$multilineEdit.isMouseOver(mouseX, mouseY);
        boolean overName = this.betterCommandBlock$nameEdit != null && this.betterCommandBlock$nameEdit.isMouseOver(mouseX, mouseY);
        if (!overMultiline && !overName) {
            this.betterCommandBlock$clearInputFocusAndHints();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void betterCommandBlock$injectMouseScrolled(
            double mouseX, double mouseY, double scrollDelta, CallbackInfoReturnable<Boolean> cir) {
        if (this.betterCommandBlock$multilineEdit == null || !this.betterCommandBlock$shouldShowHints()) {
            return;
        }
        boolean handled = false;
        if (this.betterCommandBlock$suggestionPane != null && this.betterCommandBlock$suggestionPane.visible) {
            handled = this.betterCommandBlock$suggestionPane.mouseScrolled(mouseX, mouseY, scrollDelta) || handled;
        }
        if (this.betterCommandBlock$suggestionDetailPane != null && this.betterCommandBlock$suggestionDetailPane.visible) {
            handled = this.betterCommandBlock$suggestionDetailPane.mouseScrolled(mouseX, mouseY, scrollDelta) || handled;
        }
        if (handled) {
            this.betterCommandBlock$focusInputAndKeepHints();
            cir.setReturnValue(true);
        }
    }
}
