package com.zhuhongming.bettercommandblock.preset;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ParsedArgument;
import com.zhuhongming.bettercommandblock.mixin.MultilineTextFieldAccessor;
import com.zhuhongming.bettercommandblock.mixin.MultilineTextFieldStringViewAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lovic's Better Command Block style multiline command editor.
 */
public final class PresetCommandMultiLineEditBox extends AbstractScrollWidget {
    private static final int CURSOR_INSERT_COLOR = -3092272;
    private static final int TEXT_COLOR = -2039584;
    private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
    private static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
    private static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final List<Style> ARGUMENT_STYLES = List.of(
            Style.EMPTY.withColor(ChatFormatting.AQUA),
            Style.EMPTY.withColor(ChatFormatting.YELLOW),
            Style.EMPTY.withColor(ChatFormatting.GREEN),
            Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE),
            Style.EMPTY.withColor(ChatFormatting.GOLD));

    private final Minecraft minecraft;
    private final Font font;
    private final Component placeholder;
    private final MultilineTextField textField;
    private final List<StyledRange> styledRanges = new ArrayList<>();

    private Consumer<String> valueListener = value -> {};
    private int frame;
    private boolean hasError;

    public PresetCommandMultiLineEditBox(
            Minecraft minecraft,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component placeholder,
            Component narration) {
        super(x, y, width, height, narration);
        this.minecraft = minecraft;
        this.font = font;
        this.placeholder = placeholder;
        this.textField = new MultilineTextField(font, width - this.totalInnerPadding());
        this.textField.setCursorListener(this::scrollToCursor);
        this.textField.setValueListener(value -> {
            this.refreshSyntaxState(value);
            this.valueListener.accept(value);
        });
    }

    public void tick() {
        ++this.frame;
    }

    public void setCharacterLimit(int limit) {
        this.textField.setCharacterLimit(limit);
    }

    public void setValueListener(Consumer<String> listener) {
        this.valueListener = listener;
    }

    public void setValue(String value) {
        this.textField.setValue(value);
        this.refreshSyntaxState(this.textField.value());
    }

    public String getValue() {
        return this.textField.value();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.setFocused(true);
            this.textField.setSelecting(false);
            this.seekCursorScreen(mouseX, mouseY);
            this.textField.setSelecting(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.textField.setSelecting(true);
            this.seekCursorScreen(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.textField.setSelecting(false);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.textField.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.visible && this.isFocused() && SharedConstants.isAllowedChatCharacter(codePoint)) {
            this.textField.insertText(Character.toString(codePoint));
            return true;
        }
        return false;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        MultilineTextFieldAccessor accessor = this.fieldAccessor();
        String text = this.textField.value();
        if (text.isEmpty() && !this.isFocused()) {
            guiGraphics.drawWordWrap(
                    this.font,
                    this.placeholder,
                    this.getX() + this.innerPadding(),
                    this.getY() + this.innerPadding(),
                    this.width - this.totalInnerPadding(),
                    PLACEHOLDER_TEXT_COLOR);
            return;
        }

        int cursor = this.textField.cursor();
        boolean drawCursor = this.isFocused() && this.frame / 6 % 2 == 0;
        boolean cursorInText = cursor < text.length();
        int cursorX = 0;
        int cursorY = 0;
        int lineY = this.getY() + this.innerPadding();

        for (Object line : accessor.bettercommandblock$getDisplayLines()) {
            boolean lineVisible = this.withinContentAreaTopBottom(lineY, lineY + 9);
            if (lineVisible) {
                int drawX = this.getX() + this.innerPadding();
                int lineStart = this.getLineBeginIndex(line);
                int lineEnd = this.getLineEndIndex(line);
                if (drawCursor && cursorInText && cursor >= lineStart && cursor <= lineEnd) {
                    drawX = this.drawStyledRange(guiGraphics, text, lineStart, cursor, drawX, lineY);
                    cursorX = drawX - 1;
                    guiGraphics.fill(cursorX, lineY - 1, cursorX + 1, lineY + 10, CURSOR_INSERT_COLOR);
                    drawX = this.drawStyledRange(guiGraphics, text, cursor, lineEnd, drawX, lineY);
                    cursorY = lineY;
                } else {
                    drawX = this.drawStyledRange(guiGraphics, text, lineStart, lineEnd, drawX, lineY);
                    cursorX = drawX - 1;
                    cursorY = lineY;
                }
            }
            lineY += 9;
        }

        if (drawCursor && !cursorInText && this.withinContentAreaTopBottom(cursorY, cursorY + 9)) {
            guiGraphics.drawString(this.font, "_", cursorX, cursorY, CURSOR_INSERT_COLOR);
        }

        if (this.textField.hasSelection()) {
            int selectedBegin = Math.min(accessor.bettercommandblock$getCursor(), accessor.bettercommandblock$getSelectCursor());
            int selectedEnd = Math.max(accessor.bettercommandblock$getCursor(), accessor.bettercommandblock$getSelectCursor());
            int baseX = this.getX() + this.innerPadding();
            int y = this.getY() + this.innerPadding();
            for (Object line : accessor.bettercommandblock$getDisplayLines()) {
                int lineBegin = this.getLineBeginIndex(line);
                int lineEnd = this.getLineEndIndex(line);
                if (selectedBegin > lineEnd) {
                    y += 9;
                    continue;
                }
                if (lineBegin > selectedEnd) {
                    break;
                }
                if (this.withinContentAreaTopBottom(y, y + 9)) {
                    int left = this.font.width(text.substring(lineBegin, Math.max(selectedBegin, lineBegin)));
                    int right = selectedEnd > lineEnd
                            ? this.width - this.innerPadding()
                            : this.font.width(text.substring(lineBegin, selectedEnd));
                    guiGraphics.fill(RenderType.guiTextHighlight(), baseX + left, y, baseX + right, y + 9, -16776961);
                }
                y += 9;
            }
        }
    }

    @Override
    public int getInnerHeight() {
        return 9 * this.textField.getLineCount();
    }

    @Override
    protected boolean scrollbarVisible() {
        return (double) this.textField.getLineCount() > this.getDisplayableLineCount();
    }

    @Override
    protected double scrollRate() {
        return 4.5D;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(
                NarratedElementType.TITLE,
                Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
    }

    private void refreshSyntaxState(String command) {
        this.styledRanges.clear();
        this.hasError = false;
        if (this.minecraft.player == null || this.minecraft.player.connection == null) {
            return;
        }

        StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }

        CommandDispatcher<SharedSuggestionProvider> dispatcher = this.minecraft.player.connection.getCommands();
        ParseResults<SharedSuggestionProvider> parsed =
                dispatcher.parse(reader, this.minecraft.player.connection.getSuggestionsProvider());
        if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty()) {
            this.hasError = true;
            return;
        }

        int colorIndex = 0;
        for (ParsedArgument<SharedSuggestionProvider, ?> argument :
                parsed.getContext().getLastChild().getArguments().values()) {
            int start = Math.max(0, argument.getRange().getStart());
            int end = Math.min(command.length(), argument.getRange().getEnd());
            if (end > start) {
                Style style = ARGUMENT_STYLES.get(colorIndex++ % ARGUMENT_STYLES.size());
                this.styledRanges.add(new StyledRange(start, end, style));
            }
        }
    }

    private int drawStyledRange(GuiGraphics guiGraphics, String fullText, int rangeStart, int rangeEnd, int x, int y) {
        if (rangeEnd <= rangeStart) {
            return x;
        }
        int cursor = rangeStart;
        while (cursor < rangeEnd) {
            Style style = this.styleAt(cursor);
            int segmentEnd = cursor + 1;
            while (segmentEnd < rangeEnd && this.styleAt(segmentEnd).equals(style)) {
                segmentEnd++;
            }
            String segment = fullText.substring(cursor, segmentEnd);
            int color = style.getColor() != null ? style.getColor().getValue() : TEXT_COLOR;
            guiGraphics.drawString(this.font, segment, x, y, color);
            x += this.font.width(segment);
            cursor = segmentEnd;
        }
        return x;
    }

    private Style styleAt(int index) {
        if (this.hasError) {
            return UNPARSED_STYLE;
        }
        for (StyledRange range : this.styledRanges) {
            if (index >= range.start && index < range.end) {
                return range.style;
            }
        }
        return LITERAL_STYLE;
    }

    private void scrollToCursor() {
        MultilineTextFieldAccessor accessor = this.fieldAccessor();
        double scroll = this.scrollAmount();
        Object topLine = this.getLineViewObject((int) (scroll / 9.0D), accessor);
        if (this.textField.cursor() <= this.getLineBeginIndex(topLine)) {
            scroll = this.textField.getLineAtCursor() * 9.0D;
        } else {
            Object bottomLine = this.getLineViewObject((int) ((scroll + this.height) / 9.0D) - 1, accessor);
            if (this.textField.cursor() > this.getLineEndIndex(bottomLine)) {
                scroll = this.textField.getLineAtCursor() * 9.0D - this.height + 9 + this.totalInnerPadding();
            }
        }
        this.setScrollAmount(scroll);
    }

    private double getDisplayableLineCount() {
        return (double) (this.height - this.totalInnerPadding()) / 9.0D;
    }

    private void seekCursorScreen(double mouseX, double mouseY) {
        double x = mouseX - this.getX() - this.innerPadding();
        double y = mouseY - this.getY() - this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(x, y);
    }

    private Object getLineViewObject(int lineIndex, MultilineTextFieldAccessor accessor) {
        List<?> lines = accessor.bettercommandblock$getDisplayLines();
        if (lines.isEmpty()) {
            throw new IllegalStateException("displayLines is empty");
        }
        int clamped = Math.max(0, Math.min(lineIndex, lines.size() - 1));
        return lines.get(clamped);
    }

    private int getLineBeginIndex(Object lineView) {
        return ((MultilineTextFieldStringViewAccessor) lineView).bettercommandblock$beginIndex();
    }

    private int getLineEndIndex(Object lineView) {
        return ((MultilineTextFieldStringViewAccessor) lineView).bettercommandblock$endIndex();
    }

    private MultilineTextFieldAccessor fieldAccessor() {
        return (MultilineTextFieldAccessor) (Object) this.textField;
    }

    private record StyledRange(int start, int end, Style style) {}
}

