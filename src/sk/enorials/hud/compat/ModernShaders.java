/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class ModernShaders {
    private static final List<String> MAP = List.of("vec4 texCoord1:xy texCoord2:zw", "vec3 minimapCarrierPosition:xyz", "vec4 minimap:x minimapShape:y keepEdges:z frameStyle:w", "vec4 frameThickness:x colorProfile:y transition:z fullscreenMinimap:w", "vec2 sx:x sy:y");
    private static final List<String> GUI = List.of("flat ivec4 egKind:x egVariant:y egAlias:z egBundleLength:w", "flat vec4 egColor:xyzw", "flat vec4 egCellMin:xy egCellMax:zw", "flat vec4 egCanvas:xyzw", "flat ivec4 egSettings:xyzw", "flat vec2 egFade:x gpsClock:y", "flat ivec4 egSprite:xyzw", "flat vec4 egMotion:xyzw", "flat vec4 egColor2:xyzw", "flat vec4 egColor3:xyzw", "flat int gpsActive:", "flat vec4 gpsAngles:xyzw", "vec2 gpsLocal:xy");

    private ModernShaders() {
    }

    static void apply(Path path2) throws IOException {
        String string;
        String string2;
        String string3;
        Path path3 = path2.resolve("assets/minecraft/shaders");
        Path path4 = path3.resolve("core");
        for (String object : List.of("vsh", "fsh")) {
            String string4 = Files.readString(path4.resolve("text." + object));
            string3 = ModernShaders.guiBranch(string4);
            string3 = ModernShaders.locations(string3, object.equals("vsh") ? "out" : "in", MAP, 4);
            string3 = ModernShaders.locations(string3, object.equals("vsh") ? "out" : "in", GUI, 9);
            string3 = ModernShaders.nativeLocations(string3, object);
            string3 = ModernShaders.entryPoint(string3, object.equals("vsh"), "ehTransfer4(); ehTransfer9();");
            string2 = "#version 330\n#extension GL_ARB_separate_shader_objects : require\n";
            string = ModernShaders.nativeSource("text." + object);
            string = string.substring(string.indexOf(10, string.indexOf("#extension")) + 1);
            String string5 = string2 + "#ifndef IS_GUI\n" + string + "\n#else\n#include <minecraft:dynamictransforms.glsl>\n" + (object.equals("vsh") ? "#include <minecraft:projection.glsl>\n" : "") + string3 + "\n#endif\n";
            Files.writeString(path4.resolve("text." + object), (CharSequence)ModernShaders.imports(string5), new OpenOption[0]);
        }
        ModernShaders.entity(path4);
        try (Stream<Path> stream = Files.walk(path3, new FileVisitOption[0]);){
            for (Path path5 : stream.filter(path -> Files.isRegularFile(path, new LinkOption[0])).toList()) {
                string3 = path5.getFileName().toString();
                if (!string3.endsWith(".vsh") && !string3.endsWith(".fsh") && !string3.endsWith(".glsl") || (string2 = Files.readString(path5)).equals(string = ModernShaders.imports(string2))) continue;
                Files.writeString(path5, (CharSequence)string, new OpenOption[0]);
            }
        }
    }

    private static void entity(Path path) throws IOException {
        String string = Files.readString(path.resolve("entity.vsh"));
        String string2 = string.substring(string.indexOf("#moj_import <minecraft:globals.glsl>"), string.indexOf("void main()"));
        string2 = ModernShaders.locations(string2, "out", MAP, 8);
        Object object = ModernShaders.nativeSource("entity.vsh").replace("void main()", ModernShaders.imports(string2) + "\nvoid main()");
        int n = ((String)object).lastIndexOf(125);
        object = ((String)object).substring(0, n) + "#ifndef PER_FACE_LIGHTING\n    vec4 vcolor = vertexColor;\n    #include <minecraft:minimap/vertex_main.glsl>\n    minimapCarrierPosition = Position;\n#else\n    minimap = 0.0; fullscreenMinimap = 0.0;\n#endif\n" + ((String)object).substring(n);
        Files.writeString(path.resolve("entity.vsh"), (CharSequence)ModernShaders.entryPoint((String)object, true, "ehTransfer8();"), new OpenOption[0]);
        String string3 = Files.readString(path.resolve("entity.fsh"));
        string2 = string3.substring(string3.indexOf("in vec2 texCoord1;"), string3.indexOf("void main()"));
        string2 = ModernShaders.locations(string2, "in", MAP, 8);
        String string4 = ModernShaders.nativeSource("entity.fsh");
        String string5 = "#ifndef PER_FACE_LIGHTING\nvec4 ehMapColor;\n#define fragColor ehMapColor\nvoid ehDrawMap() {\n    #include <minecraft:minimap/fragment_main.glsl>\n}\n#undef fragColor\n#endif\n";
        string4 = string4.replace("void main() {", ModernShaders.imports(string2) + string5 + "void main() {\n#ifndef PER_FACE_LIGHTING\n    if(minimap>0.0 || fullscreenMinimap>0.0) {\n        ehDrawMap();\n        #ifdef OIT_ALPHA_ONLY\n        executeAlphaOnlyPhase(gl_FragCoord.z,ehMapColor.a);\n        #else\n            #ifdef OIT_ACCUMULATE\n            ehMapColor=sampleColorForAccumulation(ehMapColor);\n            #endif\n        fragColor=ehMapColor;\n        #endif\n        return;\n    }\n#endif\n");
        Files.writeString(path.resolve("entity.fsh"), (CharSequence)ModernShaders.entryPoint(string4, false, "ehTransfer8();"), new OpenOption[0]);
    }

    private static String nativeLocations(String string, String string2) {
        int n;
        String[] stringArray;
        if (string2.equals("vsh")) {
            stringArray = new String[]{"Position", "Color", "UV0", "UV2"};
            for (n = 0; n < stringArray.length; ++n) {
                string = ModernShaders.location(string, "in", stringArray[n], n);
            }
        } else {
            string = ModernShaders.location(string, "out", "fragColor", 0);
        }
        stringArray = new String[]{"sphericalVertexDistance", "cylindricalVertexDistance", "vertexColor", "texCoord0"};
        for (n = 0; n < stringArray.length; ++n) {
            string = ModernShaders.location(string, string2.equals("vsh") ? "out" : "in", stringArray[n], n);
        }
        return string;
    }

    private static String location(String string, String string2, String string3, int n) {
        return string.replaceAll("(?m)^(" + string2 + " \\w+ " + string3 + ";)$", "layout(location = " + n + ") $1");
    }

    private static String locations(String string, String string2, List<String> list, int n) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder("\nvoid ehTransfer" + n + "() {\n");
        for (int i = 0; i < list.size(); ++i) {
            String[] stringArray = list.get(i).split(" ");
            boolean bl = stringArray[0].equals("flat");
            int n2 = bl ? 1 : 0;
            String string3 = "ehVarying" + (n + i);
            stringBuilder.append("layout(location = ").append(n + i).append(") ").append(bl ? "flat " : "").append(string2).append(' ').append(stringArray[n2]).append(' ').append(string3).append(";\n");
            for (int j = n2 + 1; j < stringArray.length; ++j) {
                String[] stringArray2 = stringArray[j].split(":", -1);
                String string4 = stringArray2[0];
                Pattern pattern = Pattern.compile("(?m)^(?:flat )?" + string2 + " (\\w+ " + string4 + ";)\\r?\\n");
                Matcher matcher = pattern.matcher(string);
                if (!matcher.find()) {
                    throw new IOException("Missing 26.3 shader varying: " + string4);
                }
                string = matcher.replaceAll("$1\n");
                String string5 = string3 + (String)(stringArray2[1].isEmpty() ? "" : "." + stringArray2[1]);
                stringBuilder2.append("    ").append((String)(string2.equals("out") ? string5 : string4)).append(" = ").append((String)(string2.equals("out") ? string4 : string5)).append(";\n");
            }
        }
        return String.valueOf(stringBuilder) + string + String.valueOf(stringBuilder2.append("}\n"));
    }

    private static String entryPoint(String string, boolean bl, String string2) {
        string = string.replace("void main()", "void ehMain()");
        return string + "\nvoid main() { " + (bl ? "ehMain(); " + string2 : string2 + " ehMain();") + " }\n";
    }

    private static String guiBranch(String string) throws IOException {
        int n = (string = string.replace("\r\n", "\n")).indexOf("#ifndef IS_GUI\n");
        if (n < 0) {
            throw new IOException("Missing native/GUI shader split");
        }
        int n2 = 0;
        int n3 = -1;
        int n4 = -1;
        int n5 = n;
        for (String string2 : string.substring(n).split("(?<=\\n)")) {
            String string3 = string2.strip();
            if (string3.startsWith("#if")) {
                ++n2;
            } else if (string3.equals("#else") && n2 == 1) {
                n3 = n5 + string2.length();
            } else if (string3.equals("#endif") && --n2 == 0) {
                n4 = n5;
                break;
            }
            n5 += string2.length();
        }
        if (n3 < 0 || n4 < n3) {
            throw new IOException("Unbalanced native/GUI shader split");
        }
        return string.substring(n3, n4);
    }

    private static String imports(String string) {
        return string.replace("#moj_import ", "#include ").replace("gl_VertexID", "gl_VertexIndex").replace("\\\r\n", " ").replace("\\\n", " ");
    }

    private static String nativeSource(String string) throws IOException {
        try (InputStream inputStream = ModernShaders.class.getResourceAsStream("/compat/26.3/" + string);){
            if (inputStream == null) {
                throw new IOException("Missing 26.3 shader: " + string);
            }
            String string2 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            return string2;
        }
    }
}

