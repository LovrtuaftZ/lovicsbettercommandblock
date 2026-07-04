package com.zhuhongming.bettercommandblock.preset;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** PVIC preset model (compatible with VicColor {@code .pvic} files). */
public final class CommandPreset {

    private static final Set<String> ALLOWED_BLOCK_TYPES = Set.of("pulse", "repeat", "chain");
    public static final String TAG_PATTERN = "pattern";
    public static final String TAG_ANIMATION = "animation";
    public static final String TAG_TEXT = "text";
    /** BCB-only PVIC tag; VicColor ignores unknown tags when loading. */
    public static final String TAG_COMMAND = "command";

    private final String name;
    private final String command;
    private final String blockType;
    private final String icon;
    private final boolean builtin;
    private final String fileStem;
    private final String categoryTag;
    private final boolean conditional;
    private final boolean alwaysActive;

    public CommandPreset(
            String name,
            String command,
            String blockType,
            String icon,
            boolean builtin,
            String fileStem,
            String categoryTag,
            boolean conditional,
            boolean alwaysActive) {
        this.name = sanitize(name);
        this.command = sanitize(command);
        this.blockType = sanitize(blockType).toLowerCase(Locale.ROOT);
        this.icon = sanitize(icon);
        this.builtin = builtin;
        this.fileStem = sanitizeFileStem(fileStem);
        this.categoryTag = normalizeTag(categoryTag);
        this.conditional = conditional;
        this.alwaysActive = alwaysActive;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    public String getBlockType() {
        return blockType;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getFileStem() {
        return fileStem;
    }

    public String getCategoryTag() {
        return categoryTag;
    }

    public boolean isAnimationTag() {
        return TAG_ANIMATION.equals(categoryTag);
    }

    public boolean isPatternTag() {
        return TAG_PATTERN.equals(categoryTag);
    }

    public boolean isTextTag() {
        return TAG_TEXT.equals(categoryTag);
    }

    public boolean isCommandTag() {
        return TAG_COMMAND.equals(categoryTag);
    }

    /** VicColor-compatible tag cycle (pattern / animation / text). */
    public static String cycleTag(String current) {
        if (TAG_PATTERN.equals(current)) {
            return TAG_ANIMATION;
        }
        if (TAG_ANIMATION.equals(current)) {
            return TAG_TEXT;
        }
        return TAG_PATTERN;
    }

    /** Full tag cycle for BCB export UI, includes {@link #TAG_COMMAND}. */
    public static String cycleExportTag(String current) {
        if (TAG_PATTERN.equals(current)) {
            return TAG_ANIMATION;
        }
        if (TAG_ANIMATION.equals(current)) {
            return TAG_TEXT;
        }
        if (TAG_TEXT.equals(current)) {
            return TAG_COMMAND;
        }
        if (TAG_COMMAND.equals(current)) {
            return TAG_PATTERN;
        }
        return TAG_PATTERN;
    }

    public static String tagDisplayLabel(String tag) {
        if (TAG_ANIMATION.equals(tag)) {
            return "动画";
        }
        if (TAG_TEXT.equals(tag)) {
            return "文本";
        }
        if (TAG_COMMAND.equals(tag)) {
            return "指令";
        }
        return "图案";
    }

    public boolean isConditional() {
        return conditional;
    }

    public boolean isAlwaysActive() {
        return alwaysActive;
    }

    public boolean isValid() {
        return !name.isEmpty()
                && !command.isEmpty()
                && !icon.isEmpty()
                && ALLOWED_BLOCK_TYPES.contains(blockType);
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sanitizeFileStem(String value) {
        String sanitized = sanitize(value);
        if (sanitized.endsWith(".pvic")) {
            sanitized = sanitized.substring(0, sanitized.length() - ".pvic".length());
        }
        if (sanitized.endsWith(".png")) {
            sanitized = sanitized.substring(0, sanitized.length() - ".png".length());
        }
        return sanitized;
    }

    private static String normalizeTag(String value) {
        String sanitized = sanitize(value);
        if (sanitized.isEmpty()) {
            return TAG_PATTERN;
        }
        String lower = sanitized.toLowerCase(Locale.ROOT);
        if (TAG_ANIMATION.equals(lower) || "animated".equals(lower) || "动画".equals(sanitized)) {
            return TAG_ANIMATION;
        }
        if (TAG_TEXT.equals(lower) || "文本".equals(sanitized)) {
            return TAG_TEXT;
        }
        if (TAG_COMMAND.equals(lower) || "指令".equals(sanitized)) {
            return TAG_COMMAND;
        }
        return TAG_PATTERN;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CommandPreset that)) {
            return false;
        }
        return builtin == that.builtin
                && conditional == that.conditional
                && alwaysActive == that.alwaysActive
                && Objects.equals(name, that.name)
                && Objects.equals(command, that.command)
                && Objects.equals(blockType, that.blockType)
                && Objects.equals(icon, that.icon)
                && Objects.equals(fileStem, that.fileStem)
                && Objects.equals(categoryTag, that.categoryTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name, command, blockType, icon, builtin, fileStem, categoryTag, conditional, alwaysActive);
    }
}
