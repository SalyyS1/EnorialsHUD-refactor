/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 */
package sk.enorials.hud.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.imageio.ImageIO;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.pack.PackRegistry;

public final class AvatarFont {
    public static final String SUBJECT = "enorials:avatar:";
    public static final char FIRST = '\uee00';
    public static final char BACK = '\uef00';
    public static final char ADVANCE = '\uef40';
    public static final char SHIFT = '\uef41';
    public static final char UNSHIFT = '\uef49';

    private AvatarFont() {
    }

    public static int cellWidth(BufferedImage bufferedImage) {
        return bufferedImage.getWidth() % 8 == 0 ? bufferedImage.getWidth() / 8 : bufferedImage.getWidth();
    }

    public static int cellOffset(BufferedImage bufferedImage, int n) {
        return bufferedImage.getWidth() % 8 == 0 ? n * AvatarFont.cellWidth(bufferedImage) : 0;
    }

    public static BufferedImage atlas(BufferedImage bufferedImage) {
        int n = bufferedImage.getWidth();
        int n2 = bufferedImage.getHeight();
        int n3 = AvatarFont.cellWidth(bufferedImage);
        BufferedImage bufferedImage2 = new BufferedImage(n3 * 8, n2 * 8, 2);
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                int n4 = j * 8 / n;
                int n5 = i * 8 / n2;
                bufferedImage2.setRGB(n4 * n3 + j - AvatarFont.cellOffset(bufferedImage, n4), n5 * n2 + i, bufferedImage.getRGB(j, i) & 0xFF000000 | 0xFFFFFF);
            }
        }
        return bufferedImage2;
    }

    public static int advance(BufferedImage bufferedImage, int n, int n2, int n3, int n4, int n5) {
        int n6 = 0;
        for (int i = 0; i < n4; ++i) {
            for (int j = 0; j < n3; ++j) {
                if (bufferedImage.getRGB(n + j, n2 + i) >>> 24 == 0) continue;
                n6 = Math.max(n6, j + 1);
            }
        }
        return (int)((float)n6 * ((float)n5 / (float)n4) + 0.5f) + 1;
    }

    public static void build(Path path2, PackRegistry packRegistry) throws IOException {
        List<Path> list;
        Path path3 = path2.resolve("assets");
        try (Object object = Files.walk(path3, new FileVisitOption[0]);){
            list = object.filter(path -> path.toString().endsWith(".json") && path.toString().replace('\\', '/').contains("/font/")).sorted().toList();
        }
        object = new HashMap();
        HashMap<String, String[]> hashMap = new HashMap<String, String[]>();
        HashMap<String, BufferedImage> hashMap2 = new HashMap<String, BufferedImage>();
        block5: for (Path path4 : list) {
            JsonObject jsonObject;
            String string = path3.relativize(path4).toString().replace('\\', '/');
            if (!string.startsWith("enorialshud/font/") || !(jsonObject = JsonParser.parseString((String)Files.readString(path4)).getAsJsonObject()).has("providers")) continue;
            for (JsonElement jsonElement : jsonObject.getAsJsonArray("providers")) {
                int n;
                Object object;
                String string2;
                JsonArray jsonArray;
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                if (!jsonObject2.has("file") || !jsonObject2.has("chars") || (jsonArray = jsonObject2.getAsJsonArray("chars")).size() != 1 || !jsonArray.get(0).getAsString().equals("\ue100") || !(string2 = jsonObject2.get("file").getAsString()).endsWith("/avatar.png") && !string2.endsWith("/portrait.png") && !string2.endsWith("/player_portrait.png")) continue;
                BufferedImage bufferedImage = (BufferedImage)object.get(string2);
                if (bufferedImage == null) {
                    object = string2.split(":", 2);
                    bufferedImage = ImageIO.read(path3.resolve(object[0] + "/textures/" + object[1]).toFile());
                    if (bufferedImage == null) {
                        throw new IOException("Invalid avatar mask: " + string2);
                    }
                    object.put(string2, bufferedImage);
                }
                if ((object = (String)hashMap.get(string2)) == null) {
                    object = "avatars/cells_" + hashMap.size() + ".png";
                    hashMap.put(string2, (String[])object);
                    BufferedImage bufferedImage2 = AvatarFont.atlas(bufferedImage);
                    hashMap2.put(string2, bufferedImage2);
                    Path path5 = path3.resolve("enorials/textures/" + (String)object);
                    Files.createDirectories(path5.getParent(), new FileAttribute[0]);
                    ImageIO.write((RenderedImage)bufferedImage2, "png", path5.toFile());
                }
                int n2 = bufferedImage.getWidth();
                int n3 = bufferedImage.getHeight();
                int n4 = jsonObject2.get("height").getAsInt();
                JsonObject jsonObject3 = jsonObject2.deepCopy();
                jsonObject3.addProperty("file", "enorials:" + (String)object);
                JsonArray jsonArray2 = new JsonArray();
                for (int i = 0; i < 8; ++i) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int j = 0; j < 8; ++j) {
                        stringBuilder.append((char)(60928 + i * 8 + j));
                    }
                    jsonArray2.add(stringBuilder.toString());
                }
                jsonObject3.add("chars", (JsonElement)jsonArray2);
                BufferedImage bufferedImage3 = (BufferedImage)hashMap2.get(string2);
                int n5 = AvatarFont.cellWidth(bufferedImage);
                JsonObject jsonObject4 = new JsonObject();
                for (n = 0; n < 64; ++n) {
                    jsonObject4.addProperty(String.valueOf((char)(61184 + n)), (Number)(-AvatarFont.advance(bufferedImage3, n % 8 * n5, n / 8 * n3, n5, n3, n4)));
                }
                for (n = 0; n < 8; ++n) {
                    float f = (float)AvatarFont.cellOffset(bufferedImage, n) * ((float)n4 / (float)n3);
                    jsonObject4.addProperty(String.valueOf((char)(61249 + n)), (Number)Float.valueOf(f));
                    jsonObject4.addProperty(String.valueOf((char)(61257 + n)), (Number)Float.valueOf(-f));
                }
                jsonObject4.addProperty(String.valueOf('\uef40'), (Number)AvatarFont.advance(bufferedImage, 0, 0, n2, n3, n4));
                JsonObject jsonObject5 = new JsonObject();
                jsonObject5.addProperty("type", "space");
                jsonObject5.add("advances", (JsonElement)jsonObject4);
                JsonArray jsonArray3 = new JsonArray();
                jsonArray3.add((JsonElement)jsonObject3);
                jsonArray3.add((JsonElement)jsonObject5);
                JsonObject jsonObject6 = new JsonObject();
                jsonObject6.add("providers", (JsonElement)jsonArray3);
                String string3 = "enorialshud:" + string.substring("enorialshud/font/".length(), string.length() - 5);
                String string4 = "avatars/" + string3.replace(':', '/');
                Path path6 = path3.resolve("enorials/font/" + string4 + ".json");
                Files.createDirectories(path6.getParent(), new FileAttribute[0]);
                Files.writeString(path6, (CharSequence)jsonObject6.toString(), new OpenOption[0]);
                packRegistry.avatarFonts().put(Key.key((String)string3), new Definition(Key.key((String)("enorials:" + string4))));
                continue block5;
            }
        }
    }

    public static Component paint(Component component, Key key, Map<Key, Definition> map, Function<Definition, Component> function) {
        return AvatarFont.paint(component, key, map, null, (uUID, definition) -> (Component)function.apply((Definition)definition));
    }

    public static Component paint(Component component, Key key, Map<Key, Definition> map, UUID uUID, BiFunction<UUID, Definition, Component> biFunction) {
        Component component2;
        Object object;
        String string = component.insertion();
        if (string != null && string.startsWith(SUBJECT)) {
            try {
                uUID = UUID.fromString(string.substring(SUBJECT.length()));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        Key key2 = LegacyText.font(component) == null ? key : LegacyText.font(component);
        List list = component.children();
        ArrayList<Component> arrayList = null;
        for (int i = 0; i < list.size(); ++i) {
            object = (Component)list.get(i);
            if (object == (component2 = AvatarFont.paint(object, key2, map, uUID, biFunction))) continue;
            if (arrayList == null) {
                arrayList = new ArrayList<Component>(list);
            }
            arrayList.set(i, component2);
        }
        Component component3 = arrayList == null ? component : component.children(arrayList);
        object = map.get(key2);
        if (object != null && component instanceof TextComponent && (component2 = (TextComponent)component).content().indexOf(57600) >= 0) {
            TextComponent.Builder builder = (TextComponent.Builder)Component.text().style(component.style());
            String string2 = component2.content();
            int n = 0;
            for (int i = 0; i < string2.length(); ++i) {
                if (string2.charAt(i) != '\ue100') continue;
                if (i > n) {
                    builder.append((Component)Component.text((String)string2.substring(n, i)));
                }
                builder.append(biFunction.apply(uUID, (Definition)object));
                n = i + 1;
            }
            if (n < string2.length()) {
                builder.append((Component)Component.text((String)string2.substring(n)));
            }
            builder.append((Iterable)component3.children());
            component3 = builder.asComponent();
        }
        return component3;
    }

    public record Definition(Key font) {
        public Component component(int[] nArray) {
            TextComponent.Builder builder = (TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().font(this.font)).decoration(TextDecoration.BOLD, false)).decoration(TextDecoration.ITALIC, false);
            int n = 0;
            while (n < 64) {
                int n2;
                for (n2 = n + 1; n2 < 64 && nArray[n2] == nArray[n]; ++n2) {
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = n; i < n2; ++i) {
                    stringBuilder.append((char)(61249 + i % 8)).append((char)(60928 + i)).append((char)(61184 + i)).append((char)(61257 + i % 8));
                }
                builder.append(Component.text((String)stringBuilder.toString()).color(TextColor.color((int)(nArray[n] & 0xFFFFFF))));
                n = n2;
            }
            return TextCompat.shadow(((TextComponent.Builder)builder.append((Component)Component.text((char)'\uef40'))).asComponent(), 0);
        }
    }
}

