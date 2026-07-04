package com.zhuhongming.bettercommandblock.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class BetterReadonlyWrapPane extends AbstractWidget {
    private static final int BG_COLOR = 0xCC1F1F1F;
    private static final int TRACK_COLOR = 0x553D3D3D;
    private static final int THUMB_COLOR = 0xAA8A8A8A;
    private static final int TEXT_COLOR = 0xEDEDED;
    private static final int HIGHLIGHT_BG_COLOR = 0xA07A6400;
    private static final int HIGHLIGHT_TEXT_COLOR = 0xFFFF55;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int SCROLLBAR_SIZE = 6;

    private final Font font;
    private final List<String> sourceLines = new ArrayList<>();
    private final List<String> wrappedLines = new ArrayList<>();
    private final List<Integer> wrappedSourceLineIndexes = new ArrayList<>();
    private int scrollY;
    private int highlightedSourceLine = -1;
    private boolean draggingV;
    private int dragOffset;

    public BetterReadonlyWrapPane(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.font = font;
    }

    public void setLines(List<String> lines) {
        this.sourceLines.clear();
        for (String line : lines) {
            this.sourceLines.add(line == null ? "" : line);
        }
        this.rebuildWrappedLines();
    }

    public void clear() {
        this.sourceLines.clear();
        this.wrappedLines.clear();
        this.wrappedSourceLineIndexes.clear();
        this.wrappedLines.add("");
        this.wrappedSourceLineIndexes.add(-1);
        this.scrollY = 0;
        this.highlightedSourceLine = -1;
    }

    public void setHighlightedLine(int sourceLineIndex) {
        int normalized = sourceLineIndex;
        if (normalized < 0 || normalized >= this.sourceLines.size()) {
            normalized = -1;
        }
        if (this.highlightedSourceLine == normalized) {
            return;
        }
        this.highlightedSourceLine = normalized;
        if (normalized >= 0) {
            this.ensureHighlightedVisible();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        PaneLayout layout = this.layout();
        this.updateDragFromMouse(mouseX, mouseY, layout);

        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BG_COLOR);
        int contentRight = this.getX() + layout.contentWidth;
        int contentBottom = this.getY() + layout.contentHeight;
        guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, contentRight - 1, contentBottom - 1);

        int y = this.getY() + PADDING - this.scrollY;
        for (int i = 0; i < this.wrappedLines.size(); i++) {
            String line = this.wrappedLines.get(i);
            boolean highlighted = this.wrappedSourceLineIndexes.get(i) == this.highlightedSourceLine;
            if (y > this.getY() - LINE_HEIGHT && y < contentBottom + LINE_HEIGHT) {
                if (highlighted) {
                    guiGraphics.fill(this.getX() + 1, y - 1, contentRight - 1, y + LINE_HEIGHT, HIGHLIGHT_BG_COLOR);
                }
                guiGraphics.drawString(this.font, line, this.getX() + PADDING, y, highlighted ? HIGHLIGHT_TEXT_COLOR : TEXT_COLOR);
            }
            y += LINE_HEIGHT;
        }
        guiGraphics.disableScissor();

        if (layout.showVertical) {
            int trackX0 = this.getX() + layout.contentWidth;
            int trackX1 = this.getX() + this.width;
            guiGraphics.fill(trackX0, this.getY(), trackX1, this.getY() + layout.contentHeight, TRACK_COLOR);
            int thumbY = this.verticalThumbY(layout);
            guiGraphics.fill(trackX0, thumbY, trackX1, thumbY + this.verticalThumbHeight(layout), THUMB_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || button != 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        PaneLayout layout = this.layout();
        if (!layout.showVertical) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int trackX0 = this.getX() + layout.contentWidth;
        int thumbY = this.verticalThumbY(layout);
        int thumbH = this.verticalThumbHeight(layout);
        if (mx >= trackX0 && my >= thumbY && my <= thumbY + thumbH) {
            this.draggingV = true;
            this.dragOffset = my - thumbY;
        } else if (mx >= trackX0) {
            int centered = my - thumbH / 2;
            this.applyThumbPosition(centered, layout);
            this.draggingV = true;
            this.dragOffset = thumbH / 2;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible || button != 0 || !this.draggingV) {
            return false;
        }
        this.updateDragFromMouse((int) mouseX, (int) mouseY, this.layout());
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingV = false;
            return this.visible;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!this.visible || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        PaneLayout layout = this.layout();
        this.scrollY = Mth.clamp(this.scrollY - (int) (scrollDelta * LINE_HEIGHT), 0, this.maxScrollY(layout));
        return true;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Suggestion details"));
    }

    private void wrapLine(String source, int maxWidth, int sourceLineIndex, List<String> output, List<Integer> sourceIndexes) {
        if (source.isEmpty()) {
            output.add("");
            sourceIndexes.add(sourceLineIndex);
            return;
        }
        String remain = source;
        while (!remain.isEmpty()) {
            String piece = this.font.plainSubstrByWidth(remain, maxWidth);
            if (piece.isEmpty()) {
                break;
            }
            output.add(piece);
            sourceIndexes.add(sourceLineIndex);
            remain = remain.substring(piece.length());
        }
    }

    private void rebuildWrappedLines() {
        PaneLayout layout = this.layout();
        int maxWidth = Math.max(8, layout.contentWidth - PADDING * 2);
        int previousScroll = this.scrollY;
        this.wrappedLines.clear();
        this.wrappedSourceLineIndexes.clear();
        for (int i = 0; i < this.sourceLines.size(); i++) {
            this.wrapLine(this.sourceLines.get(i), maxWidth, i, this.wrappedLines, this.wrappedSourceLineIndexes);
        }
        if (this.wrappedLines.isEmpty()) {
            this.wrappedLines.add("");
            this.wrappedSourceLineIndexes.add(-1);
        }
        this.scrollY = Mth.clamp(previousScroll, 0, this.maxScrollY(layout));
        this.highlightedSourceLine =
                this.highlightedSourceLine >= 0 && this.highlightedSourceLine < this.sourceLines.size()
                        ? this.highlightedSourceLine
                        : -1;
    }

    private int totalContentHeight() {
        return PADDING * 2 + this.wrappedLines.size() * LINE_HEIGHT;
    }

    private int maxScrollY(PaneLayout layout) {
        return Math.max(0, this.totalContentHeight() - layout.contentHeight);
    }

    private int verticalThumbHeight(PaneLayout layout) {
        int total = this.totalContentHeight();
        int view = layout.contentHeight;
        if (total <= 0) {
            return view;
        }
        return Math.max(12, view * view / total);
    }

    private int verticalThumbY(PaneLayout layout) {
        int max = this.maxScrollY(layout);
        if (max <= 0) {
            return this.getY();
        }
        int trackLen = layout.contentHeight - this.verticalThumbHeight(layout);
        return this.getY() + this.scrollY * trackLen / max;
    }

    private void applyThumbPosition(int thumbTop, PaneLayout layout) {
        int trackStart = this.getY();
        int trackLen = layout.contentHeight - this.verticalThumbHeight(layout);
        float pct = trackLen <= 0 ? 0.0F : (float) (thumbTop - trackStart) / (float) trackLen;
        this.scrollY = Mth.clamp((int) (pct * this.maxScrollY(layout)), 0, this.maxScrollY(layout));
    }

    private void updateDragFromMouse(int mouseX, int mouseY, PaneLayout layout) {
        if (!this.draggingV) {
            return;
        }
        if (!this.isLeftMousePressed()) {
            this.draggingV = false;
            return;
        }
        this.applyThumbPosition(mouseY - this.dragOffset, layout);
    }

    private boolean isLeftMousePressed() {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private void ensureHighlightedVisible() {
        int firstWrappedIndex = -1;
        for (int i = 0; i < this.wrappedSourceLineIndexes.size(); i++) {
            if (this.wrappedSourceLineIndexes.get(i) == this.highlightedSourceLine) {
                firstWrappedIndex = i;
                break;
            }
        }
        if (firstWrappedIndex < 0) {
            return;
        }
        PaneLayout layout = this.layout();
        int lineTop = firstWrappedIndex * LINE_HEIGHT;
        int lineBottom = lineTop + LINE_HEIGHT;
        if (lineTop < this.scrollY) {
            this.scrollY = lineTop;
        } else if (lineBottom > this.scrollY + layout.contentHeight - PADDING) {
            this.scrollY = Math.max(0, lineBottom - layout.contentHeight + PADDING);
        }
        this.scrollY = Mth.clamp(this.scrollY, 0, this.maxScrollY(layout));
    }

    private PaneLayout layout() {
        boolean showVertical = this.totalContentHeight() > this.height;
        int contentWidth = this.width - (showVertical ? SCROLLBAR_SIZE : 0);
        int contentHeight = this.height;
        contentWidth = Math.max(1, contentWidth);
        contentHeight = Math.max(1, contentHeight);
        return new PaneLayout(contentWidth, contentHeight, showVertical);
    }

    private record PaneLayout(int contentWidth, int contentHeight, boolean showVertical) {}
}
