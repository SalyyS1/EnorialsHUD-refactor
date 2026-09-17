/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.resourcepack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.config.Config;

public final class MinimapResourcePackBuilder {
    private static final String GENERATED_PACK_NAME = "EnorialsMinimap-pack-26.2-generated.zip";
    private static final int PACK_FORMAT_262 = 88;
    private static final List<String> SHADER_RESOURCES = List.of("shaders/core/entity.fsh", "shaders/core/entity.vsh", "shaders/core/text.fsh", "shaders/core/text.vsh", "shaders/include/minimap/fragment_main.glsl", "shaders/include/minimap/fragment_util.glsl", "shaders/include/minimap/vertex_main.glsl", "shaders/include/minimap/vertex_util.glsl");
    private final EnorialsMinimap plugin;

    public MinimapResourcePackBuilder(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }

    public Path build() throws IOException {
        Path path = this.plugin.getDataFolder().toPath().resolve("generated-resourcepack");
        Files.createDirectories(path, new FileAttribute[0]);
        Path path2 = path.resolve(GENERATED_PACK_NAME);
        Path path3 = path.resolve("EnorialsMinimap-pack-26.2-generated.zip.tmp");
        Files.deleteIfExists(path3);
        boolean bl = Config.instance().resourcePack.guiMode();
        List<Object> list = bl ? this.guiResources() : List.of();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(path3, new OpenOption[0]), StandardCharsets.UTF_8);){
            boolean bl2 = bl || Config.instance().resourcePack.shouldIncludeShaders();
            this.writeText(zipOutputStream, "pack.mcmeta", "{\n  \"pack\": {\n    \"min_format\": [%d, 0],\n    \"max_format\": [88, 0],\n    \"description\": \"%s\"\n  }\n}\n".formatted(88, bl ? "EnorialsMinimap generated resource pack (26.2, GUI mode)" : (bl2 ? "EnorialsMinimap generated resource pack (26.2, shader mode)" : "EnorialsMinimap generated resource pack (26.2, fallback mode)")));
            if (bl2) {
                for (String string : SHADER_RESOURCES) {
                    String string2 = "assets/minecraft/" + string;
                    if (list.contains(string2)) continue;
                    this.writeResource(zipOutputStream, string, string2);
                }
            }
            for (String string : list) {
                this.writeResource(zipOutputStream, "gui-pack/" + string, string);
            }
        }
        Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return path2;
    }

    private List<String> guiResources() throws IOException {
        try (InputStream inputStream = this.plugin.getResource("gui-pack/manifest.txt");){
            if (inputStream == null) {
                throw new IOException("Missing embedded GUI resource-pack manifest");
            }
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
            for (String string2 : new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                String string3 = string2.strip();
                if (string3.isEmpty() || string3.startsWith("#")) continue;
                if (!string3.startsWith("assets/") || string3.contains("\\") || string3.contains(":") || string3.endsWith("/") || Arrays.stream(string3.split("/", -1)).anyMatch(string -> string.isEmpty() || string.equals(".") || string.equals(".."))) {
                    throw new IOException("Invalid embedded GUI resource path: " + string3);
                }
                if (linkedHashSet.add(string3)) continue;
                throw new IOException("Duplicate embedded GUI resource: " + string3);
            }
            if (linkedHashSet.isEmpty()) {
                throw new IOException("Empty embedded GUI resource-pack manifest");
            }
            List<String> list = List.copyOf(linkedHashSet);
            return list;
        }
    }

    private void writeText(ZipOutputStream zipOutputStream, String string, String string2) throws IOException {
        ZipEntry zipEntry = new ZipEntry(string);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(string2.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private void writeResource(ZipOutputStream zipOutputStream, String string, String string2) throws IOException {
        try (InputStream inputStream = this.plugin.getResource(string);){
            if (inputStream == null) {
                throw new IOException("Missing embedded resource: " + string);
            }
            ZipEntry zipEntry = new ZipEntry(string2);
            zipOutputStream.putNextEntry(zipEntry);
            inputStream.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }
}

