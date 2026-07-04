package com.zhuhongming.bettercommandblock.client.widget;

import java.util.ArrayDeque;

/** 命令编辑框文本撤销 / 重做栈（快照含光标与选区）。 */
public final class CommandTextUndoHistory {

    private static final int MAX_ENTRIES = 64;

    public record Snapshot(String text, int cursor, int selectCursor) {}

    private final ArrayDeque<Snapshot> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Snapshot> redoStack = new ArrayDeque<>();
    private boolean recording = true;

    public void resetBaseline() {
        this.undoStack.clear();
        this.redoStack.clear();
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    /** 在即将修改文本前调用，将当前状态压入撤销栈。 */
    public void recordBeforeEdit(Snapshot current) {
        if (!this.recording || current == null) {
            return;
        }
        Snapshot top = this.undoStack.peekLast();
        if (current.equals(top)) {
            return;
        }
        this.undoStack.addLast(current);
        while (this.undoStack.size() > MAX_ENTRIES) {
            this.undoStack.removeFirst();
        }
        this.redoStack.clear();
    }

    public Snapshot undo(Snapshot present) {
        if (this.undoStack.isEmpty()) {
            return null;
        }
        this.redoStack.addLast(present);
        return this.undoStack.removeLast();
    }

    public Snapshot redo(Snapshot present) {
        if (this.redoStack.isEmpty()) {
            return null;
        }
        this.undoStack.addLast(present);
        return this.redoStack.removeLast();
    }
}
