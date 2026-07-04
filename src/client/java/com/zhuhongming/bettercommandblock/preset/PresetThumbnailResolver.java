package com.zhuhongming.bettercommandblock.preset;

import com.zhuhongming.bettercommandblock.BetterCommandBlockMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves preview thumbnails for built-in and custom presets.
 *
 * <p>Rules:
 * <ul>
 *     <li>Built-in: loaded from mod resources under textures/gui/pvic_thumb/.</li>
 *     <li>Custom: loaded from config/viccolor/custom, next to .pvic files.</li>
 *     <li>Invalid/missing/broken images return {@code null}, caller draws placeholder.</li>
 * </ul>
 */
public final class PresetThumbnailResolver {

    private static final String BUILTIN_THUMB_ROOT = "textures/gui/pvic_thumb/";

    private final Set<ResourceLocation> dynamicCustomTextures = new HashSet<>();

    public ResourceLocation loadThumbnail(Minecraft minecraft, CommandPreset preset) {
        if (minecraft == null || preset == null) {
            return null;
        }
        if (preset.isBuiltin()) {
            return loadBuiltinThumbnail(minecraft, preset.getFileStem());
        }
        return loadCustomThumbnail(minecraft, preset.getFileStem());
    }

    public void releaseCustomTextures(Minecraft minecraft) {
        if (minecraft == null || dynamicCustomTextures.isEmpty()) {
            return;
        }
        for (ResourceLocation id : dynamicCustomTextures) {
            minecraft.getTextureManager().release(id);
        }
        dynamicCustomTextures.clear();
    }

    private ResourceLocation loadBuiltinThumbnail(Minecraft minecraft, String fileStem) {
        if (!isLegalFileStem(fileStem)) {
            return null;
        }
        ResourceLocation resource = buildId(BUILTIN_THUMB_ROOT + fileStem + ".png");
        if (resource == null) {
            return null;
        }
        if (minecraft.getResourceManager().getResource(resource).isEmpty()) {
            return null;
        }
        minecraft.getTextureManager().getTexture(resource);
        return resource;
    }

    private ResourceLocation loadCustomThumbnail(Minecraft minecraft, String fileStem) {
        Path customBasePath = resolveCustomBasePath(fileStem);
        if (customBasePath == null) {
            return null;
        }
        Path imagePath = resolveCustomPreviewPath(customBasePath);
        if (!Files.isRegularFile(imagePath)) {
            return null;
        }
        if (!isReadablePng(imagePath)) {
            return null;
        }
        try (InputStream input = Files.newInputStream(imagePath)) {
            NativeImage image = NativeImage.read(input);
            ResourceLocation id = dynamicTextureId(fileStem);
            minecraft.getTextureManager().register(id, new DynamicTexture(image));
            dynamicCustomTextures.add(id);
            return id;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static Path resolveCustomPreviewPath(Path customBasePath) {
        Path sameName = Path.of(customBasePath.toString() + ".png");
        if (Files.isRegularFile(sameName)) {
            return sameName;
        }

        Path folder = customBasePath.getParent();
        if (folder == null || !Files.isDirectory(folder)) {
            return sameName;
        }

        Path folderNamed = folder.resolve(folder.getFileName().toString() + ".png");
        if (Files.isRegularFile(folderNamed)) {
            return folderNamed;
        }

        try (var stream = Files.list(folder)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .findFirst()
                    .orElse(sameName);
        } catch (IOException ignored) {
            return sameName;
        }
    }

    private static boolean isReadablePng(Path imagePath) {
        if (!imagePath.getFileName().toString().toLowerCase().endsWith(".png")) {
            return false;
        }
        try (InputStream input = Files.newInputStream(imagePath)) {
            return ImageIO.read(input) != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isLegalFileStem(String fileStem) {
        if (fileStem == null) {
            return false;
        }
        String normalized = fileStem.replace('\\', '/').trim();
        return !normalized.isBlank() && !normalized.contains("..");
    }

    private static ResourceLocation dynamicTextureId(String fileStem) {
        String normalized = fileStem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        String suffix = Integer.toHexString(fileStem.hashCode());
        return buildId("dynamic/pvic_thumb/" + normalized + "_" + suffix);
    }

    private static ResourceLocation buildId(String path) {
        return ResourceLocation.tryParse(BetterCommandBlockMod.MOD_ID + ":" + path);
    }

    private static Path resolveCustomBasePath(String fileStem) {
        if (!isLegalFileStem(fileStem)) {
            return null;
        }
        Path customRoot = CommandPresetManager.getCustomPresetDir().toAbsolutePath().normalize();
        String normalizedStem = fileStem.replace('\\', '/');
        if (normalizedStem.startsWith("/")) {
            return null;
        }
        Path basePath = customRoot.resolve(normalizedStem).normalize();
        if (!basePath.startsWith(customRoot)) {
            return null;
        }
        return basePath;
    }
}

