package com.zhuhongming.bettercommandblock.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.zhuhongming.bettercommandblock.platform.PvicPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PresetExportHistory {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS_PATH = PvicPaths.configDir().resolve("export_history.json");

    private PresetExportHistory() {}

    public static Path loadLastDirectory() {
        if (!Files.isRegularFile(SETTINGS_PATH)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(SETTINGS_PATH, StandardCharsets.UTF_8)) {
            ExportHistoryData data = GSON.fromJson(reader, ExportHistoryData.class);
            if (data == null || data.lastDirectory == null || data.lastDirectory.isBlank()) {
                return null;
            }
            Path path = Path.of(data.lastDirectory);
            return Files.isDirectory(path) ? path : null;
        } catch (IOException | JsonParseException ignored) {
            return null;
        }
    }

    public static void saveLastDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(SETTINGS_PATH.getParent());
            ExportHistoryData data = new ExportHistoryData();
            data.lastDirectory = directory.toAbsolutePath().normalize().toString();
            try (Writer writer = Files.newBufferedWriter(SETTINGS_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class ExportHistoryData {
        String lastDirectory;
    }
}
