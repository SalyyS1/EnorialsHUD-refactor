/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.avatar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import sk.enorials.hud.compat.ClientRenderProfile;

public final class HeadMapFont {
    public static final int ASCENT = 4250000;

    private HeadMapFont() {
    }

    public static void build(Path path) throws IOException {
        HeadMapFont.build(path, ClientRenderProfile.nativeProfile());
    }

    public static void build(Path path, ClientRenderProfile clientRenderProfile) throws IOException {
        Object object;
        InputStream inputStream2;
        String[] stringArray = new String[]{"data_util.glsl", "vertex_main.glsl", "fragment_main.glsl", "frame.glsl"};
        int n = stringArray.length;
        for (int i = 0; i < n; ++i) {
            String string = stringArray[i];
            String object22 = "assets/minecraft/shaders/include/guiminimap/" + string;
            inputStream2 = HeadMapFont.class.getResourceAsStream("/default/pack/" + object22);
            try {
                if (inputStream2 == null) {
                    throw new IOException("Missing head transport decoder: " + object22);
                }
                object = path.resolve(object22);
                Files.createDirectories(object.getParent(), new FileAttribute[0]);
                Files.copy(inputStream2, (Path)object, StandardCopyOption.REPLACE_EXISTING);
                continue;
            }
            finally {
                if (inputStream2 != null) {
                    inputStream2.close();
                }
            }
        }
        for (int i = 0; i < 6; ++i) {
            for (String string : new String[]{"circle", "square"}) {
                HeadMapFont.copy(path, "assets/enorials_gui/textures/font/frame_" + i + "_" + string + ".png");
            }
            for (n = 0; n < 36; ++n) {
                HeadMapFont.copy(path, String.format(Locale.ROOT, "assets/enorials_gui/font/frame_%d/v%02d.json", i, n));
            }
        }
        if (clientRenderProfile.legacyTerrain()) {
            return;
        }
        Path path2 = path.resolve("assets/enorials_gui/font");
        for (n = 0; n < 36; ++n) {
            String string = String.format(Locale.ROOT, "v%02d.json", n);
            JsonObject jsonObject = JsonParser.parseString((String)Files.readString(path2.resolve((clientRenderProfile.cameraTerrain() ? "pose_raster/" : "pose_radar/") + string))).getAsJsonObject();
            for (InputStream inputStream2 : jsonObject.getAsJsonArray("providers")) {
                object = inputStream2.getAsJsonObject();
                if (!object.has("type") || !object.get("type").getAsString().equals("bitmap")) continue;
                object.addProperty("ascent", (Number)(-4250000 - n * 4096));
            }
            Path path3 = path2.resolve("skin_head/" + string);
            Files.createDirectories(path3.getParent(), new FileAttribute[0]);
            Files.writeString(path3, (CharSequence)jsonObject.toString(), new OpenOption[0]);
            if (!clientRenderProfile.cameraTerrain()) continue;
            inputStream2 = JsonParser.parseString((String)Files.readString(path2.resolve("pose_radar/" + string))).getAsJsonObject();
            for (JsonElement jsonElement : inputStream2.getAsJsonArray("providers")) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                if (!jsonObject2.has("type") || !jsonObject2.get("type").getAsString().equals("bitmap")) continue;
                jsonObject2.addProperty("ascent", (Number)(-4500000 - n * 4096));
            }
            object = path2.resolve("skin_head_motion/" + string);
            Files.createDirectories(object.getParent(), new FileAttribute[0]);
            Files.writeString((Path)object, (CharSequence)inputStream2.toString(), new OpenOption[0]);
        }
    }

    private static void copy(Path path, String string) throws IOException {
        try (InputStream inputStream = HeadMapFont.class.getResourceAsStream("/default/pack/" + string);){
            if (inputStream == null) {
                throw new IOException("Missing minimap frame: " + string);
            }
            Path path2 = path.resolve(string);
            Files.createDirectories(path2.getParent(), new FileAttribute[0]);
            Files.copy(inputStream, path2, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

