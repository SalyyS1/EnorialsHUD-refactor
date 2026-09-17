/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.bukkit.Bukkit
 */
package sk.enorials.hud.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import sk.enorials.hud.avatar.MinimapLayoutFonts;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.LegacyFonts;
import sk.enorials.hud.compat.LegacySpacing;
import sk.enorials.hud.compat.MenuHead;
import sk.enorials.hud.compat.ModernShaders;
import sk.enorials.minimap.map.icon.MarkerIcons;

public final class PackProfile {
    private static final Map<String, Format> FORMATS = PackProfile.loadFormats();

    private PackProfile() {
    }

    public static boolean modern() {
        return PackProfile.format() >= 88;
    }

    public static int format() {
        return PackProfile.formatFor(Bukkit.getServer() == null ? "26.2" : Bukkit.getMinecraftVersion());
    }

    private static Map<String, Format> loadFormats() {
        Object object;
        Properties properties = new Properties();
        try {
            object = PackProfile.class.getResourceAsStream("/compat/pack-formats.properties");
            try {
                if (object == null) {
                    throw new IllegalStateException("Missing resource profiles");
                }
                properties.load((InputStream)object);
            }
            finally {
                if (object != null) {
                    ((InputStream)object).close();
                }
            }
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
        object = new HashMap();
        for (String string : properties.stringPropertyNames()) {
            String[] stringArray = properties.getProperty(string).split("\\.", 2);
            ((HashMap)object).put(string, new Format(Integer.parseInt(stringArray[0]), stringArray.length == 2 ? Integer.parseInt(stringArray[1]) : 0));
        }
        return Map.copyOf(object);
    }

    private static Format profileFor(String string) {
        Format format = FORMATS.get(string);
        if (format == null) {
            throw new IllegalStateException("Unknown Minecraft resource profile: " + string);
        }
        return format;
    }

    public static int formatFor(String string) {
        return PackProfile.profileFor(string).major();
    }

    public static int minorFor(String string) {
        return PackProfile.profileFor(string).minor();
    }

    public static void apply(Path path) throws IOException {
        PackProfile.apply(path, ClientRenderProfile.nativeProfile());
    }

    public static void apply(Path path2, ClientRenderProfile clientRenderProfile) throws IOException {
        Object object;
        int n = clientRenderProfile.resourceMajor();
        MinimapLayoutFonts.build(path2, clientRenderProfile.legacyTerrain());
        if (n > 8) {
            LegacySpacing.aliases(path2);
        }
        if (n == 88) {
            Files.write(path2.resolve("pack.mcmeta"), PackProfile.metadata(clientRenderProfile), new OpenOption[0]);
            return;
        }
        Files.write(path2.resolve("pack.mcmeta"), PackProfile.metadata(clientRenderProfile), new OpenOption[0]);
        if (n == 97) {
            ModernShaders.apply(path2);
            return;
        }
        if (n < 46) {
            for (String string : new String[]{"pose_raster", "pose_radar", "hq_marker", "pose_icon", "player", "skin_head", "frame_0", "frame_1", "frame_2", "frame_3", "frame_4", "frame_5"}) {
                object = path2.resolve("assets/enorials_gui/font/" + string);
                if (!Files.isDirectory((Path)object, new LinkOption[0])) continue;
                try (Stream<Path> stream = Files.walk((Path)object, new FileVisitOption[0]);){
                    for (Path path3 : stream.filter(path -> Files.isRegularFile(path, new LinkOption[0]) && path.getFileName().toString().endsWith(".json")).toList()) {
                        Files.writeString(path3, (CharSequence)"{\"providers\":[]}", new OpenOption[0]);
                    }
                }
            }
        }
        if (n < 15) {
            LegacyFonts.flatten(path2);
        }
        if (n <= 8) {
            LegacySpacing.convert(path2);
        }
        if (n < 46) {
            PackProfile.writeLegacyHeadModels(path2);
        }
        if (n <= 8) {
            PackProfile.writeLegacyItemModels(path2);
        }
        PackProfile.writeShaders(path2, n);
        if (n < 42) {
            try (String[] stringArray = Files.walk(path2.resolve("assets/minecraft/shaders"), new FileVisitOption[0]);){
                for (Path path4 : stringArray.filter(path -> Files.isRegularFile(path, new LinkOption[0])).toList()) {
                    String string;
                    string = path4.getFileName().toString();
                    if (!string.endsWith(".vsh") && !string.endsWith(".fsh") && !string.endsWith(".glsl")) continue;
                    object = Files.readString(path4);
                    Files.writeString(path4, (CharSequence)((String)object).replace("#moj_import <minecraft:", "#moj_import <"), new OpenOption[0]);
                }
            }
        }
    }

    public static byte[] metadata() {
        return PackProfile.metadata(ClientRenderProfile.nativeProfile());
    }

    public static byte[] metadata(ClientRenderProfile clientRenderProfile) {
        Format format = new Format(clientRenderProfile.resourceMajor(), clientRenderProfile.resourceMinor());
        JsonObject jsonObject = new JsonObject();
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("description", "Enorials HUD");
        JsonArray jsonArray = new JsonArray();
        jsonArray.add((Number)format.major());
        jsonArray.add((Number)format.minor());
        if (format.major() >= 69) {
            jsonObject2.add("min_format", (JsonElement)jsonArray);
            jsonObject2.add("max_format", (JsonElement)jsonArray.deepCopy());
        } else {
            jsonObject2.addProperty("pack_format", (Number)format.major());
        }
        jsonObject.add("pack", (JsonElement)jsonObject2);
        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void writeLegacyItemModels(Path path) throws IOException {
        Object object;
        Object object2;
        String string2;
        JsonArray jsonArray = new JsonArray();
        for (String string2 : MarkerIcons.BUILTIN) {
            object2 = new JsonObject();
            object = new JsonObject();
            object.addProperty("custom_model_data", (Number)(950000 + MarkerIcons.BUILTIN.indexOf(string2)));
            object2.add("predicate", (JsonElement)object);
            object2.addProperty("model", "enorials:marker/" + MarkerIcons.textureKey(string2));
            jsonArray.add((JsonElement)object2);
        }
        JsonObject jsonObject = new JsonObject();
        string2 = new JsonObject();
        jsonObject.addProperty("parent", "minecraft:item/generated");
        string2.addProperty("layer0", "minecraft:item/paper");
        jsonObject.add("textures", (JsonElement)string2);
        jsonObject.add("overrides", (JsonElement)jsonArray);
        object2 = path.resolve("assets/minecraft/models/item");
        Files.createDirectories((Path)object2, new FileAttribute[0]);
        Files.writeString(object2.resolve("paper.json"), (CharSequence)jsonObject.toString(), new OpenOption[0]);
        object = object2.resolve("echo_shard.json");
        if (Files.isRegularFile((Path)object, new LinkOption[0])) {
            Files.writeString(object2.resolve("amethyst_shard.json"), (CharSequence)Files.readString((Path)object).replace("minecraft:item/echo_shard", "minecraft:item/amethyst_shard"), new OpenOption[0]);
        }
    }

    private static void writeLegacyHeadModels(Path path2) throws IOException {
        Object object;
        Path path32;
        Path path4 = path2.resolve("assets/enorials/models/item/menu");
        if (!Files.isDirectory(path4, new LinkOption[0])) {
            return;
        }
        JsonArray jsonArray = new JsonArray();
        try (Object object2 = Files.list(path4);){
            for (Path path32 : object2.filter(path -> path.getFileName().toString().startsWith("head_")).sorted().toList()) {
                object = path32.getFileName().toString().replace(".json", "");
                JsonObject jsonObject = new JsonObject();
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("custom_model_data", (Number)MenuHead.modelId("enorials:menu/" + (String)object));
                jsonObject.add("predicate", (JsonElement)jsonObject2);
                jsonObject.addProperty("model", "enorials:item/menu/" + (String)object);
                jsonArray.add((JsonElement)jsonObject);
            }
        }
        object2 = new ArrayList();
        jsonArray.forEach(((List)object2)::add);
        object2.sort(Comparator.comparingInt(jsonElement -> jsonElement.getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt()));
        JsonArray jsonArray2 = new JsonArray();
        object2.forEach(arg_0 -> ((JsonArray)jsonArray2).add(arg_0));
        path32 = new JsonObject();
        path32.addProperty("parent", "minecraft:item/template_skull");
        path32.add("overrides", (JsonElement)jsonArray2);
        object = path2.resolve("assets/minecraft/models/item/player_head.json");
        Files.createDirectories(object.getParent(), new FileAttribute[0]);
        Files.writeString((Path)object, (CharSequence)path32.toString(), new OpenOption[0]);
    }

    private static void writeShaders(Path path, int n) throws IOException {
        Object object;
        Object object2;
        String[] stringArray;
        Object object3;
        String string;
        Path path2 = path.resolve("assets/minecraft/shaders/core");
        if (n < 84) {
            Files.writeString(path.resolve("assets/minecraft/shaders/include/sample_lightmap.glsl"), (CharSequence)"vec4 sample_lightmap(sampler2D lightmap,ivec2 uv){return texelFetch(lightmap,uv/16,0);}\n", new OpenOption[0]);
        }
        String string2 = Files.readString(path2.resolve("text.vsh"));
        String string3 = Files.readString(path2.resolve("text.fsh"));
        if (n <= 8) {
            string2 = string2.replace("void main() {", "void main() {\n if(abs(Position.y)>900000000.0){gl_Position=vec4(2.0,2.0,2.0,1.0);return;}\n");
        }
        if (n < 46) {
            string = "float id = get_id((round(EH_OFFSET - Position.y)) * -1.0);";
            if (!string2.contains(string)) {
                throw new IOException("Missing shared UI shader ID decoder");
            }
            string2 = string2.replace(string, string + "\n    if(id>=90.0 && id<=94.0 && fract(Position.z+0.001)<0.02){gl_Position=vec4(2.0,2.0,2.0,1.0);return;}\n");
            object3 = path.resolve("assets/minecraft/shaders/include/gps3d/legacy.glsl");
            Files.createDirectories(object3.getParent(), new FileAttribute[0]);
            stringArray = PackProfile.class.getResourceAsStream("/default/pack/assets/minecraft/shaders/include/gps3d/vertex_main.glsl");
            try {
                if (stringArray == null) {
                    throw new IOException("Missing GPS transport decoder");
                }
                Files.copy((InputStream)stringArray, object3.resolveSibling("vertex_main.glsl"), StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                if (stringArray != null) {
                    stringArray.close();
                }
            }
            stringArray = PackProfile.class.getResourceAsStream("/compat/shaders/gps_legacy.glsl");
            try {
                if (stringArray == null) {
                    throw new IOException("Missing legacy GPS shader");
                }
                Files.copy((InputStream)stringArray, (Path)object3, StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                if (stringArray != null) {
                    stringArray.close();
                }
            }
            string2 = string2.replace("#version 330", "#version 330\n#define ENO_LEGACY_GPS");
            string2 = string2.replace("void main() {", Files.readString((Path)object3) + "\nvoid main() {");
            string2 = string2.replace("vec3 pos = Position;", "vec3 pos = Position;\n    pos.x -= enoLegacyGpsCursor(Position,UV0,Color);");
        }
        string = "\n#define IS_GUI\nin ivec2 UV2;\nuniform sampler2D Sampler2;\nflat out int enoWorldText;\nout float enoSphere;\nout float enoCylinder;\n#moj_import <minecraft:sample_lightmap.glsl>\n";
        object3 = "\n#define IS_GUI\nflat in int enoWorldText;\nin float enoSphere;\nin float enoCylinder;\n";
        string2 = string2.replace("#version 330", "#version 330" + string);
        string3 = string3.replace("#version 330", "#version 330" + (String)object3);
        string2 = string2.replace("void main() {", "void main() {\n enoWorldText=ProjMat[3][3]==0.0?1:0;\n if(enoWorldText==1){gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);vertexColor=Color*sample_lightmap(Sampler2,UV2);texCoord0=UV0;enoSphere=fog_spherical_distance(Position);enoCylinder=fog_cylindrical_distance(Position);return;}\n");
        string3 = string3.replace("void main() {", "void main() {\n if(enoWorldText==1){vec4 c=texture(Sampler0,texCoord0)*vertexColor*ColorModulator;if(c.a<0.1)discard;fragColor=apply_fog(c,enoSphere,enoCylinder,FogEnvironmentalStart,FogEnvironmentalEnd,FogRenderDistanceStart,FogRenderDistanceEnd,FogColor);return;}\n");
        if (n < 46) {
            stringArray = "\nflat out int enoTerrain;\nout vec2 enoTerrainUv;\nflat out vec4 enoTerrainCanvas;\nflat out int enoTerrainShape;\nflat out int enoTerrainProfile;\n";
            object2 = stringArray.replace("out ", "in ");
            string2 = string2.replace("#define IS_GUI", "#define IS_GUI" + (String)stringArray);
            string3 = string3.replace("#define IS_GUI", "#define IS_GUI" + (String)object2);
            string2 = string2.replace("void main() {", PackProfile.shaderResource("terrain_legacy.vsh") + "\nvoid main() {\n enoTerrain=0;\n if(enoLegacyTerrainVertex()){enoWorldText=0;return;}\n");
            string3 = string3.replace("void main() {", "void main() {\n" + PackProfile.shaderResource("terrain_legacy.fsh"));
            object = "\nflat out int enoMarker;\nout vec2 enoMarkerLocal;\nflat out vec4 enoMarkerCanvas;\nflat out ivec4 enoMarkerInfo;\nflat out ivec2 enoMarkerSprite;\nflat out ivec2 enoMarkerSlice;\nflat out vec4 enoMarkerMotion;\n";
            string2 = string2.replace("#define IS_GUI", "#define IS_GUI" + (String)object);
            string3 = string3.replace("#define IS_GUI", "#define IS_GUI" + ((String)object).replace("out ", "in "));
            string2 = string2.replace("void main() {", PackProfile.shaderResource("markers_legacy.vsh") + "\nvoid main() {\n enoMarker=0;\n if(enoLegacyMarkerVertex()){enoWorldText=0;enoTerrain=0;return;}\n");
            string3 = string3.replace("void main() {", PackProfile.shaderResource("rgba_legacy.glsl") + "\nvoid main() {\n" + PackProfile.shaderResource("markers_legacy.fsh"));
            String string4 = "\nflat out int enoFullscreen;\nout vec2 enoFullCell;\nout vec2 enoFullLocal;\nflat out ivec4 enoFullData;\nflat out float enoFullFade;\n";
            string2 = string2.replace("#define IS_GUI", "#define IS_GUI" + string4);
            string3 = string3.replace("#define IS_GUI", "#define IS_GUI" + string4.replace("out ", "in "));
            string2 = string2.replace("void main() {", PackProfile.shaderResource("fullscreen_legacy.vsh") + "\nvoid main() {\n enoFullscreen=0;\n if(enoLegacyFullscreenVertex()){enoWorldText=0;enoTerrain=0;enoMarker=0;return;}\n");
            string3 = string3.replace("void main() {", "void main() {\n" + PackProfile.shaderResource("fullscreen_legacy.fsh"));
        }
        if (n < 63) {
            stringArray = path.resolve("assets/minecraft/shaders/include/guiminimap/data_util.glsl");
            Files.writeString((Path)stringArray, (CharSequence)Files.readString((Path)stringArray).replace("#define ENO_CAMERA_TERRAIN", ""), new OpenOption[0]);
            string2 = PackProfile.legacyUniforms(string2);
            string3 = PackProfile.legacyUniforms(string3);
            object2 = path.resolve("assets/minecraft/shaders/include/guiminimap/vertex_main.glsl");
            if (Files.exists((Path)object2, new LinkOption[0])) {
                object = Files.readString((Path)object2);
                object = ((String)object).replaceAll("(?s)#ifdef ENO_CAMERA_TERRAIN\\s.*?#else\\s(.*?)#endif", "$1");
                object = ((String)object).replace("if (egFamily != 0) {", "if (egFamily == 4 && Color.r < 0.75) { gl_Position=vec4(2.0,2.0,2.0,1.0);return; }\nif (egFamily != 0) {");
                int n2 = ((String)object).indexOf("} else if (egFamily == 1) {");
                int n3 = ((String)object).indexOf("} else if (egFamily == 2) {", n2);
                if (n2 < 0 || n3 < 0) {
                    throw new IOException("Unknown legacy GUI shader structure");
                }
                object = ((String)object).substring(0, n2) + ((String)object).substring(n3);
                object = ((String)object).replace("offset = egFamily == 12 ? anchor - 128.0 : mod(anchor - vec2(CameraBlockPos.xz & ivec2(255)) + 128.0, 256.0) - 128.0 + CameraOffset.xz;", "offset = anchor - 128.0;");
                object = ((String)object).replace("} else offset = fixedToMap ? anchor - 2048.0 : mod(anchor - vec2(CameraBlockPos.xz & ivec2(4095)) + 2048.0,4096.0)-2048.0+CameraOffset.xz;", "}");
                if (((String)(object = ((String)object).replace("if (egFamily != 0) {", "if (egFamily == 1 || egFamily == 6 || egFamily == 7) { gl_Position=vec4(2.0,2.0,2.0,1.0);return; }\nif (egFamily != 0) {"))).contains("CameraBlockPos") || ((String)object).contains("CameraOffset")) {
                    throw new IOException("Unexpected camera-dependent GUI branch");
                }
                Files.writeString((Path)object2, (CharSequence)object, new OpenOption[0]);
            }
            string2 = string2.replace("#moj_import <minecraft:minimap/vertex_main.glsl>", "");
            string3 = string3.replace("#moj_import <minecraft:minimap/fragment_main.glsl>", "");
            if (n < 55) {
                Files.writeString(path2.resolve("rendertype_text.json"), (CharSequence)PackProfile.legacyProgram(n), new OpenOption[0]);
            }
        }
        Files.writeString(path2.resolve("rendertype_text.vsh"), (CharSequence)string2, new OpenOption[0]);
        Files.writeString(path2.resolve("rendertype_text.fsh"), (CharSequence)string3, new OpenOption[0]);
        for (String string4 : new String[]{"text.vsh", "text.fsh", "entity.vsh", "entity.fsh"}) {
            Files.deleteIfExists(path2.resolve(string4));
        }
    }

    private static String legacyUniforms(String string) {
        return string.replace("#version 330", "#version 150").replace("#moj_import <minecraft:dynamictransforms.glsl>", "uniform mat4 ModelViewMat;\nuniform vec4 ColorModulator;").replace("#moj_import <minecraft:projection.glsl>", "uniform mat4 ProjMat;").replace("#moj_import <minecraft:globals.glsl>", "uniform vec2 ScreenSize;\nuniform float GameTime;").replace("#moj_import <minecraft:sample_lightmap.glsl>", "#ifndef ENO_LEGACY_LIGHTMAP\n#define ENO_LEGACY_LIGHTMAP\nvec4 sample_lightmap(sampler2D s,ivec2 uv){return texelFetch(s,uv/16,0);}\n#endif").replace("#moj_import <minecraft:fog.glsl>", "uniform float FogStart;\nuniform float FogEnd;\nuniform vec4 FogColor;\nuniform int FogShape;\n#define FogEnvironmentalStart FogStart\n#define FogEnvironmentalEnd FogEnd\n#define FogRenderDistanceStart FogStart\n#define FogRenderDistanceEnd FogEnd\nfloat fog_spherical_distance(vec3 p){return length(p);}\nfloat fog_cylindrical_distance(vec3 p){return max(length(p.xz),abs(p.y));}\nvec4 apply_fog(vec4 c,float sphere,float cylinder,float a,float b,float d,float e,vec4 fog){float distance=FogShape==0?sphere:cylinder;float mixAmount=distance<=FogStart?0.0:distance>=FogEnd?1.0:smoothstep(FogStart,FogEnd,distance);return vec4(mix(c.rgb,fog.rgb,mixAmount*fog.a),c.a);}");
    }

    private static String shaderResource(String string) throws IOException {
        try (InputStream inputStream = PackProfile.class.getResourceAsStream("/compat/shaders/" + string);){
            if (inputStream == null) {
                throw new IOException("Missing legacy shader: " + string);
            }
            String string2 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return string2;
        }
    }

    private static String legacyProgram(int n) {
        String string = n < 42 ? "rendertype_text" : "minecraft:core/rendertype_text";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("vertex", string);
        jsonObject.addProperty("fragment", string);
        JsonArray jsonArray = new JsonArray();
        for (String string2 : new JsonObject[]{"Position", "Color", "UV0", "UV2"}) {
            jsonArray.add(string2);
        }
        jsonObject.add("attributes", (JsonElement)jsonArray);
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("func", "add");
        jsonObject2.addProperty("srcrgb", "srcalpha");
        jsonObject2.addProperty("dstrgb", "1-srcalpha");
        jsonObject.add("blend", (JsonElement)jsonObject2);
        JsonArray jsonArray2 = new JsonArray();
        for (String string3 : new JsonArray[]{"Sampler0", "Sampler2"}) {
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("name", string3);
            jsonArray2.add((JsonElement)jsonObject3);
        }
        jsonObject.add("samplers", (JsonElement)jsonArray2);
        JsonArray jsonArray3 = new JsonArray();
        PackProfile.uniform(jsonArray3, "ModelViewMat", "matrix4x4", 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
        PackProfile.uniform(jsonArray3, "ProjMat", "matrix4x4", 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
        PackProfile.uniform(jsonArray3, "ColorModulator", "float", 1, 1, 1, 1);
        PackProfile.uniform(jsonArray3, "ScreenSize", "float", 1, 1);
        PackProfile.uniform(jsonArray3, "GameTime", "float", 0);
        PackProfile.uniform(jsonArray3, "FogStart", "float", 0);
        PackProfile.uniform(jsonArray3, "FogEnd", "float", 1);
        PackProfile.uniform(jsonArray3, "FogColor", "float", 0, 0, 0, 0);
        PackProfile.uniform(jsonArray3, "FogShape", "int", 0);
        jsonObject.add("uniforms", (JsonElement)jsonArray3);
        return jsonObject.toString();
    }

    private static void uniform(JsonArray jsonArray, String string, String string2, int ... nArray) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", string);
        jsonObject.addProperty("type", string2);
        jsonObject.addProperty("count", (Number)nArray.length);
        JsonArray jsonArray2 = new JsonArray();
        for (int n : nArray) {
            jsonArray2.add((Number)n);
        }
        jsonObject.add("values", (JsonElement)jsonArray2);
        jsonArray.add((JsonElement)jsonObject);
    }

    private record Format(int major, int minor) {
    }
}

