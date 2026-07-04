package com.zhuhongming.bettercommandblock.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.zhuhongming.bettercommandblock.platform.PvicPaths;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

public final class CommandPresetManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CommandPresetManager INSTANCE = new CommandPresetManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<CommandPreset> customPresets = new ArrayList<>();

    private CommandPresetManager() {}

    public static CommandPresetManager getInstance() {
        return INSTANCE;
    }

    public synchronized void reloadAllPresets() {
        loadCustomPresets();
    }

    public synchronized void loadCustomPresets() {
        customPresets.clear();
        Path customDir = getCustomPresetDir();
        try {
            Files.createDirectories(customDir);
        } catch (IOException exception) {
            LOGGER.error("Failed to create PVIC custom preset directory: {}", customDir, exception);
            return;
        }

        try (var pathStream = Files.walk(customDir)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pvic"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> readCustomPreset(path).ifPresent(customPresets::add));
        } catch (IOException exception) {
            LOGGER.error("Failed to read PVIC presets from {}", customDir, exception);
        }
    }

    public synchronized List<CommandPreset> getAllPresets() {
        return Collections.unmodifiableList(new ArrayList<>(customPresets));
    }

    public synchronized Optional<CommandPreset> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return customPresets.stream().filter(preset -> preset.getName().equals(name)).findFirst();
    }

    public synchronized int getCustomCount() {
        return customPresets.size();
    }

    private Optional<CommandPreset> readCustomPreset(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            PresetFileData data = GSON.fromJson(reader, PresetFileData.class);
            if (data == null) {
                LOGGER.warn("PVIC preset file is empty: {}", path);
                return Optional.empty();
            }
            CommandPreset preset = new CommandPreset(
                    data.name,
                    data.command,
                    data.blockType,
                    data.icon,
                    false,
                    getCustomFileStem(path),
                    data.tag,
                    data.conditional,
                    data.alwaysActive);
            if (!preset.isValid()) {
                LOGGER.warn("PVIC preset missing required fields: {}", path);
                return Optional.empty();
            }
            return Optional.of(preset);
        } catch (IOException | JsonParseException exception) {
            LOGGER.error("Failed to load PVIC preset: {}", path, exception);
            return Optional.empty();
        }
    }

    /** Same directory as VicColor: {@code config/viccolor/custom/}. */
    public static Path getCustomPresetDir() {
        return PvicPaths.configDir().resolve("custom");
    }

    private static String getCustomFileStem(Path presetPath) {
        Path customRoot = getCustomPresetDir().toAbsolutePath().normalize();
        Path absolutePreset = presetPath.toAbsolutePath().normalize();
        if (!absolutePreset.startsWith(customRoot)) {
            return getFileStem(presetPath.getFileName().toString());
        }
        Path relative = customRoot.relativize(absolutePreset);
        String relativeText = relative.toString().replace('\\', '/');
        int dot = relativeText.lastIndexOf('.');
        if (dot <= 0) {
            return relativeText;
        }
        return relativeText.substring(0, dot);
    }

    private static String getFileStem(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName.trim();
        }
        return fileName.substring(0, dot).trim();
    }

    static final class PresetFileData {
        String name;
        String command;
        String blockType;
        String icon;
        String tag;
        boolean conditional;
        boolean alwaysActive;
    }
}
