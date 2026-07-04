package com.zhuhongming.bettercommandblock.client.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class BetterSuggestionPane extends AbstractWidget {
    private static final int BG_COLOR = 0xCC1F1F1F;
    private static final int TRACK_COLOR = 0x553D3D3D;
    private static final int THUMB_COLOR = 0xAA8A8A8A;
    private static final int TEXT_COLOR = 0xEDEDED;
    private static final int SCROLLBAR_SIZE = 6;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int SELECT_BG_COLOR = 0xA07A6400;
    private static final int SELECT_TEXT_COLOR = 0xFFFF55;
    private static final int HOVER_BG_COLOR = 0x66444444;
    private static final int HOVER_TEXT_COLOR = 0xF0F0F0;
    private static final long HAND_CURSOR =
            GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);

    private final Font font;
    private final List<String> lines = new ArrayList<>();
    private int scrollX;
    private int scrollY;
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private boolean draggingV;
    private boolean draggingH;
    private int dragOffset;
    private IntConsumer clickSuggestionListener = index -> {};

    public BetterSuggestionPane(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.font = font;
    }

    public void setSuggestions(List<String> suggestions) {
        this.lines.clear();
        this.lines.addAll(suggestions);
        PaneLayout layout = this.layout();
        this.scrollX = Mth.clamp(this.scrollX, 0, this.maxScrollX(layout));
        this.scrollY = Mth.clamp(this.scrollY, 0, this.maxScrollY(layout));
    }

    public void clear() {
        this.lines.clear();
        this.scrollX = 0;
        this.scrollY = 0;
        this.selectedIndex = -1;
        this.hoveredIndex = -1;
    }

    public boolean isHoveringClickableLine(int mouseX, int mouseY) {
        return this.visible && this.hoveredIndex >= 0;
    }

    public void applyCursorStyle(int mouseX, int mouseY) {
        if (!this.visible || this.hoveredIndex < 0) {
            return;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetCursor(window, HAND_CURSOR);
    }

    public void resetInteractionState() {
        this.draggingV = false;
        this.draggingH = false;
        this.hoveredIndex = -1;
    }

    public void setClickSuggestionListener(IntConsumer listener) {
        this.clickSuggestionListener = listener == null ? index -> {} : listener;
    }

    public void setSelectedIndex(int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= this.lines.size()) {
            this.selectedIndex = -1;
            return;
        }
        if (this.selectedIndex == selectedIndex) {
            return;
        }
        this.selectedIndex = selectedIndex;
        this.ensureSelectedVisible(this.layout());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Suggestions"));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        PaneLayout layout = this.layout();
        this.updateDragFromMouse(mouseX, mouseY, layout);
        this.hoveredIndex = this.resolveHoveredLineIndex(mouseX, mouseY, layout);

        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BG_COLOR);
        int contentRight = this.getX() + layout.contentWidth;
        int contentBottom = this.getY() + layout.contentHeight;
        guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, contentRight - 1, contentBottom - 1);

        if (this.hoveredIndex >= 0
                && this.hoveredIndex < this.lines.size()
                && this.hoveredIndex != this.selectedIndex) {
            int hoverY = this.getY() + PADDING - this.scrollY + this.hoveredIndex * LINE_HEIGHT;
            guiGraphics.fill(this.getX() + 1, hoverY - 1, contentRight - 1, hoverY + LINE_HEIGHT, HOVER_BG_COLOR);
        }

        int lineY = this.getY() + PADDING - this.scrollY;
        if (this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
            int selectedY = lineY + this.selectedIndex * LINE_HEIGHT;
            guiGraphics.fill(this.getX() + 1, selectedY - 1, contentRight - 1, selectedY + LINE_HEIGHT, SELECT_BG_COLOR);
        }

        int drawY = this.getY() + PADDING - this.scrollY;
        int index = 0;
        for (String line : this.lines) {
            if (drawY > this.getY() - LINE_HEIGHT && drawY < contentBottom + LINE_HEIGHT) {
                int color = TEXT_COLOR;
                if (index == this.selectedIndex) {
                    color = SELECT_TEXT_COLOR;
                } else if (index == this.hoveredIndex) {
                    color = HOVER_TEXT_COLOR;
                }
                guiGraphics.drawString(this.font, line, this.getX() + PADDING - this.scrollX, drawY, color);
            }
            drawY += LINE_HEIGHT;
            index++;
        }
        guiGraphics.disableScissor();

        if (layout.showVertical) {
            int trackX0 = this.getX() + layout.contentWidth;
            int trackX1 = this.getX() + this.width;
            guiGraphics.fill(trackX0, this.getY(), trackX1, this.getY() + layout.contentHeight, TRACK_COLOR);
            int thumbY = this.verticalThumbY(layout);
            guiGraphics.fill(trackX0, thumbY, trackX1, thumbY + this.verticalThumbHeight(layout), THUMB_COLOR);
        }

        if (layout.showHorizontal) {
            int trackY0 = this.getY() + layout.contentHeight;
            int trackY1 = this.getY() + this.height;
            guiGraphics.fill(this.getX(), trackY0, this.getX() + layout.contentWidth, trackY1, TRACK_COLOR);
            int thumbX = this.horizontalThumbX(layout);
            guiGraphics.fill(thumbX, trackY0, thumbX + this.horizontalThumbWidth(layout), trackY1, THUMB_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || button != 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        PaneLayout layout = this.layout();
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (layout.showVertical
                && mx >= this.getX() + layout.contentWidth
                && my >= this.verticalThumbY(layout)
                && my <= this.verticalThumbY(layout) + this.verticalThumbHeight(layout)) {
            this.draggingV = true;
            this.dragOffset = my - this.verticalThumbY(layout);
            return true;
        } else if (layout.showVertical && mx >= this.getX() + layout.contentWidth) {
            int thumbH = this.verticalThumbHeight(layout);
            int centered = my - thumbH / 2;
            this.applyVerticalThumbPosition(centered, layout);
            this.draggingV = true;
            this.dragOffset = thumbH / 2;
            return true;
        }
        if (layout.showHorizontal
                && my >= this.getY() + layout.contentHeight
                && mx >= this.horizontalThumbX(layout)
                && mx <= this.horizontalThumbX(layout) + this.horizontalThumbWidth(layout)) {
            this.draggingH = true;
            this.dragOffset = mx - this.horizontalThumbX(layout);
            return true;
        } else if (layout.showHorizontal && my >= this.getY() + layout.contentHeight) {
            int thumbW = this.horizontalThumbWidth(layout);
            int centered = mx - thumbW / 2;
            this.applyHorizontalThumbPosition(centered, layout);
            this.draggingH = true;
            this.dragOffset = thumbW / 2;
            return true;
        }
        int contentX0 = this.getX();
        int contentX1 = this.getX() + layout.contentWidth;
        int contentY0 = this.getY();
        int contentY1 = this.getY() + layout.contentHeight;
        if (mx >= contentX0 && mx < contentX1 && my >= contentY0 && my < contentY1) {
            int lineIndex = (my - this.getY() - PADDING + this.scrollY) / LINE_HEIGHT;
            if (lineIndex >= 0 && lineIndex < this.lines.size()) {
                this.setSelectedIndex(lineIndex);
                this.clickSuggestionListener.accept(lineIndex);
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible || button != 0) {
            return false;
        }
        PaneLayout layout = this.layout();
        if (this.draggingV && layout.showVertical) {
            this.applyVerticalThumbPosition((int) mouseY - this.dragOffset, layout);
            return true;
        }
        if (this.draggingH && layout.showHorizontal) {
            this.applyHorizontalThumbPosition((int) mouseX - this.dragOffset, layout);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingV = false;
        this.draggingH = false;
        return button == 0 && this.visible;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!this.visible || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        PaneLayout layout = this.layout();
        int step = 10;
        if (Screen.hasControlDown()) {
            this.scrollX = Mth.clamp(this.scrollX - (int) (scrollDelta * step), 0, this.maxScrollX(layout));
        } else {
            this.scrollY = Mth.clamp(this.scrollY - (int) (scrollDelta * step), 0, this.maxScrollY(layout));
        }
        return true;
    }

    private int totalContentHeight() {
        return PADDING * 2 + this.lines.size() * LINE_HEIGHT;
    }

    private int maxLineWidth() {
        int max = 0;
        for (String line : this.lines) {
            max = Math.max(max, this.font.width(line));
        }
        return PADDING * 2 + max;
    }

    private int maxScrollY(PaneLayout layout) {
        return Math.max(0, this.totalContentHeight() - layout.contentHeight);
    }

    private int maxScrollX(PaneLayout layout) {
        return Math.max(0, this.maxLineWidth() - layout.contentWidth);
    }

    private void ensureSelectedVisible(PaneLayout layout) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        int selectedTop = this.selectedIndex * LINE_HEIGHT;
        int selectedBottom = selectedTop + LINE_HEIGHT;
        if (selectedTop < this.scrollY) {
            this.scrollY = selectedTop;
        } else if (selectedBottom > this.scrollY + layout.contentHeight - PADDING) {
            this.scrollY = Math.max(0, selectedBottom - layout.contentHeight + PADDING);
        }
        this.scrollY = Mth.clamp(this.scrollY, 0, this.maxScrollY(layout));
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

    private int horizontalThumbWidth(PaneLayout layout) {
        int total = this.maxLineWidth();
        int view = layout.contentWidth;
        if (total <= 0) {
            return view;
        }
        return Math.max(12, view * view / total);
    }

    private int horizontalThumbX(PaneLayout layout) {
        int max = this.maxScrollX(layout);
        if (max <= 0) {
            return this.getX();
        }
        int trackLen = layout.contentWidth - this.horizontalThumbWidth(layout);
        return this.getX() + this.scrollX * trackLen / max;
    }

    private PaneLayout layout() {
        int totalHeight = this.totalContentHeight();
        int maxWidth = this.maxLineWidth();
        boolean showV = totalHeight > this.height;
        int contentWidth = this.width - (showV ? SCROLLBAR_SIZE : 0);
        boolean showH = maxWidth > contentWidth;
        int contentHeight = this.height - (showH ? SCROLLBAR_SIZE : 0);
        if (!showV && totalHeight > contentHeight) {
            showV = true;
            contentWidth = this.width - SCROLLBAR_SIZE;
            showH = maxWidth > contentWidth;
            contentHeight = this.height - (showH ? SCROLLBAR_SIZE : 0);
        }
        contentWidth = Math.max(1, contentWidth);
        contentHeight = Math.max(1, contentHeight);
        return new PaneLayout(contentWidth, contentHeight, showV, showH);
    }

    private void updateDragFromMouse(int mouseX, int mouseY, PaneLayout layout) {
        if (!this.draggingV && !this.draggingH) {
            return;
        }
        if (!this.isLeftMousePressed()) {
            this.draggingV = false;
            this.draggingH = false;
            return;
        }
        if (this.draggingV && layout.showVertical) {
            this.applyVerticalThumbPosition(mouseY - this.dragOffset, layout);
        }
        if (this.draggingH && layout.showHorizontal) {
            this.applyHorizontalThumbPosition(mouseX - this.dragOffset, layout);
        }
    }

    private void applyVerticalThumbPosition(int thumbTop, PaneLayout layout) {
        int trackStart = this.getY();
        int trackLen = layout.contentHeight - this.verticalThumbHeight(layout);
        float pct = trackLen <= 0 ? 0.0F : (float) (thumbTop - trackStart) / (float) trackLen;
        this.scrollY = Mth.clamp((int) (pct * this.maxScrollY(layout)), 0, this.maxScrollY(layout));
    }

    private void applyHorizontalThumbPosition(int thumbLeft, PaneLayout layout) {
        int trackStart = this.getX();
        int trackLen = layout.contentWidth - this.horizontalThumbWidth(layout);
        float pct = trackLen <= 0 ? 0.0F : (float) (thumbLeft - trackStart) / (float) trackLen;
        this.scrollX = Mth.clamp((int) (pct * this.maxScrollX(layout)), 0, this.maxScrollX(layout));
    }

    private boolean isLeftMousePressed() {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private int resolveHoveredLineIndex(int mouseX, int mouseY, PaneLayout layout) {
        if (mouseX < this.getX() || mouseX >= this.getX() + layout.contentWidth) {
            return -1;
        }
        if (mouseY < this.getY() || mouseY >= this.getY() + layout.contentHeight) {
            return -1;
        }
        int lineIndex = (mouseY - this.getY() - PADDING + this.scrollY) / LINE_HEIGHT;
        if (lineIndex >= 0 && lineIndex < this.lines.size()) {
            return lineIndex;
        }
        return -1;
    }

    private record PaneLayout(int contentWidth, int contentHeight, boolean showVertical, boolean showHorizontal) {}
}
