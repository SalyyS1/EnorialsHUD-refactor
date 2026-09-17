/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.avatar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class MinimapLayoutFonts {
    public static final int BANK_HEIGHT = 5000000;
    private static final Set<String> FAMILIES = Set.of("coords", "custom", "pose_raster", "pose_radar", "pose_icon", "player", "skin_head", "skin_head_motion", "hq_marker", "frame_1", "frame_2", "frame_3", "frame_4", "frame_5");

    private MinimapLayoutFonts() {
    }

    public static void build(Path path3, boolean bl) throws IOException {
        List<Path> list;
        Path path4 = path3.resolve("assets/enorials_gui/font");
        if (!Files.isDirectory(path4, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path4, new FileVisitOption[0]);){
            list = stream.filter(path -> Files.isRegularFile(path, new LinkOption[0])).filter(path -> path.getFileName().toString().matches("v\\d\\d\\.json")).filter(path2 -> !path4.relativize((Path)path2).getName(0).toString().matches("t[1-5]")).filter(path2 -> FAMILIES.contains(path4.relativize((Path)path2).getName(0).toString())).toList();
        }
        for (Path path5 : list) {
            Path path6 = path4.relativize(path5);
            String string = path6.getName(0).toString();
            JsonObject jsonObject2 = JsonParser.parseString((String)Files.readString(path5)).getAsJsonObject();
            IdentityHashMap<JsonObject, Integer> identityHashMap = new IdentityHashMap<JsonObject, Integer>();
            for (Object object : jsonObject2.getAsJsonArray("providers")) {
                JsonObject jsonObject3 = object.getAsJsonObject();
                if (!jsonObject3.has("ascent")) continue;
                identityHashMap.put(jsonObject3, jsonObject3.get("ascent").getAsInt());
            }
            for (int i = 1; i < 6; ++i) {
                Object object;
                object = path4.resolve("t" + i).resolve(path6);
                if (bl && !string.equals("coords") || string.startsWith("frame_") && !string.equals("frame_" + i)) {
                    if (!Files.exists((Path)object, new LinkOption[0])) continue;
                    Files.writeString((Path)object, (CharSequence)"{\"providers\":[]}", new OpenOption[0]);
                    continue;
                }
                Files.createDirectories(object.getParent(), new FileAttribute[0]);
                int n = 5000000 * i;
                identityHashMap.forEach((jsonObject, n2) -> jsonObject.addProperty("ascent", (Number)(n2 - n)));
                Files.writeString((Path)object, (CharSequence)jsonObject2.toString(), new OpenOption[0]);
            }
        }
    }
}

