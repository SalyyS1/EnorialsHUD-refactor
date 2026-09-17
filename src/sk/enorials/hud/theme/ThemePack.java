/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.theme;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.theme.HudTheme;
import sk.enorials.hud.theme.PartyFrameOpening;
import sk.enorials.hud.theme.ThemeGeometry;

public final class ThemePack {
    private static final Pattern TILE = Pattern.compile("enorialshud:font/assets/enorialshud/(.+?)(?:_tile_(\\d+)_(\\d+))?\\.png");
    private static final Pattern FILL = Pattern.compile("(player_(?:health|mana|food)|combat_target_health|party_(?:small|medium|large)_(?:health|mana))(?:_\\d+)?");

    private ThemePack() {
    }

    public static void build(EnorialsHUD enorialsHUD, Path path) throws IOException {
        Gson gson = new Gson();
        HashMap<String, byte[]> hashMap = new HashMap<String, byte[]>();
        Path path2 = path.resolve("assets/enorialshud/textures/font/assets/enorialshud/avatars/heads/avatar.png");
        if (Files.isRegularFile(path2, new LinkOption[0])) {
            ThemePack.playerPortraits(enorialsHUD, ImageIO.read(path2.toFile()), hashMap);
        }
        for (HudTheme hudTheme : HudTheme.values()) {
            if (hudTheme == HudTheme.ORIGINAL) continue;
            HashMap<Object, BufferedImage> hashMap2 = new HashMap<Object, BufferedImage>();
            HashMap<String, Object> hashMap3 = new HashMap<String, Object>();
            for (String string : List.of("player-panel", "target-panel", "party-panel", "gps-panel")) {
                Object object;
                JsonElement jsonElement2;
                String string2 = "layout/" + string + "/textures";
                Path path3 = path.resolve("assets/enorialshud/font/" + string2 + ".json");
                if (!Files.isRegularFile(path3, new LinkOption[0])) continue;
                JsonArray jsonArray = JsonParser.parseString((String)Files.readString(path3)).getAsJsonObject().getAsJsonArray("providers");
                JsonArray jsonArray2 = new JsonArray();
                for (JsonElement jsonElement2 : jsonArray) {
                    Object object2;
                    String string3;
                    Matcher matcher;
                    boolean bl;
                    String string4;
                    Matcher matcher2;
                    object = jsonElement2.getAsJsonObject();
                    if (!object.has("file") || !(matcher2 = TILE.matcher(string4 = object.get("file").getAsString())).matches() || !(bl = (matcher = FILL.matcher(string3 = matcher2.group(1))).matches()) && !string3.contains("frame") && !string3.equals("player_shield")) continue;
                    Object object3 = (String)hashMap3.get(string4);
                    if (object3 == null) {
                        int n;
                        BufferedImage bufferedImage;
                        Object object4;
                        object2 = bl ? matcher.group(1) : string3;
                        BufferedImage bufferedImage2 = (BufferedImage)hashMap2.get(object2);
                        if (bufferedImage2 == null && bl) {
                            object4 = enorialsHUD.getDataFolder().toPath().resolve("textures/assets/enorialshud/" + (String)object2 + ".png");
                            if (!Files.isRegularFile((Path)object4, new LinkOption[0])) continue;
                            bufferedImage = null;
                            try (InputStream inputStream = enorialsHUD.getResource("theme-art/" + hudTheme.id() + "/frames/" + (String)object2 + "_mask.png");){
                                if (inputStream != null) {
                                    bufferedImage = ImageIO.read(inputStream);
                                }
                            }
                            bufferedImage2 = ThemePack.fill(ImageIO.read(object4.toFile()), hudTheme.id(), bufferedImage);
                            hashMap2.put(object2, bufferedImage2);
                        }
                        if (bufferedImage2 == null) {
                            object4 = enorialsHUD.getResource("theme-art/" + hudTheme.id() + "/frames/" + string3 + ".png");
                            try {
                                if (object4 == null) continue;
                                bufferedImage2 = ImageIO.read((InputStream)object4);
                                if (bufferedImage2 != null) {
                                    bufferedImage2 = PartyFrameOpening.apply(string3 + ".png", bufferedImage2);
                                }
                                if (bufferedImage2 == null) {
                                    throw new IOException("Invalid theme artwork: " + string3);
                                }
                                hashMap2.put(string3, bufferedImage2);
                            }
                            finally {
                                if (object4 == null) continue;
                                ((InputStream)object4).close();
                                continue;
                            }
                        }
                        object4 = path.resolve("assets/enorialshud/textures/" + string4.substring(string4.indexOf(58) + 1));
                        bufferedImage = ImageIO.read(object4.toFile());
                        int n2 = matcher2.group(2) == null ? 0 : Integer.parseInt(matcher2.group(2));
                        int n3 = n = matcher2.group(3) == null ? 0 : Integer.parseInt(matcher2.group(3));
                        if (n2 + bufferedImage.getWidth() > bufferedImage2.getWidth() || n + bufferedImage.getHeight() > bufferedImage2.getHeight()) continue;
                        BufferedImage bufferedImage3 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
                        bufferedImage3.setRGB(0, 0, bufferedImage3.getWidth(), bufferedImage3.getHeight(), bufferedImage2.getRGB(n2, n, bufferedImage3.getWidth(), bufferedImage3.getHeight(), null, 0, bufferedImage3.getWidth()), 0, bufferedImage3.getWidth());
                        ThemePack.preserveAdvance(bufferedImage3, bufferedImage);
                        object3 = "themes/" + hudTheme.id() + "/" + string3 + "_tile_" + n2 + "_" + n + ".png";
                        Path path4 = path.resolve("assets/enorialshud/textures/" + (String)object3);
                        Files.createDirectories(path4.getParent(), new FileAttribute[0]);
                        ImageIO.write((RenderedImage)bufferedImage3, "png", path4.toFile());
                        hashMap3.put(string4, object3);
                    }
                    object2 = object.deepCopy();
                    object2.addProperty("file", "enorialshud:" + (String)object3);
                    jsonArray2.add((JsonElement)object2);
                }
                Iterator iterator = new JsonObject();
                iterator.addProperty("type", "reference");
                iterator.addProperty("id", "enorialshud:" + string2);
                jsonArray2.add((JsonElement)iterator);
                jsonElement2 = new JsonObject();
                jsonElement2.add("providers", (JsonElement)jsonArray2);
                object = path.resolve("assets/enorialshud/font/themes/" + hudTheme.id() + "/" + string2 + ".json");
                Files.createDirectories(object.getParent(), new FileAttribute[0]);
                Files.writeString((Path)object, (CharSequence)gson.toJson(jsonElement2), new OpenOption[0]);
            }
            ThemePack.playerFont(path, hudTheme, gson, hashMap);
        }
    }

    private static BufferedImage fill(BufferedImage bufferedImage, String string) throws IOException {
        return ThemePack.fill(bufferedImage, string, null);
    }

    private static BufferedImage fill(BufferedImage bufferedImage, String string, BufferedImage bufferedImage2) throws IOException {
        if (bufferedImage == null) {
            throw new IOException("Invalid fill image");
        }
        int n = bufferedImage.getWidth();
        int n2 = bufferedImage.getHeight();
        BufferedImage bufferedImage3 = new BufferedImage(n, n2, 2);
        Shape shape = ThemeGeometry.opening(string, 0.0, 0.0, n, n2);
        if (bufferedImage2 != null && (bufferedImage2.getWidth() != n || bufferedImage2.getHeight() != n2)) {
            throw new IOException("Theme fill mask dimensions do not match " + string);
        }
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                if (!(bufferedImage2 != null ? bufferedImage2.getRGB(j, i) >>> 24 != 0 : shape.contains((double)j + 0.5, (double)i + 0.5))) continue;
                int n3 = bufferedImage.getRGB(j, i);
                if (n3 >>> 24 < 255) {
                    n3 = bufferedImage.getRGB(n / 2, i);
                }
                bufferedImage3.setRGB(j, i, 0xFF000000 | n3 & 0xFFFFFF);
            }
        }
        return bufferedImage3;
    }

    private static void playerFont(Path path, HudTheme hudTheme, Gson gson, Map<String, byte[]> map) throws IOException {
        String string = "assets/enorialshud/textures/themes/" + hudTheme.id() + "/player_portrait.png";
        if (!map.containsKey(string)) {
            return;
        }
        Path path2 = path.resolve(string);
        Files.createDirectories(path2.getParent(), new FileAttribute[0]);
        Files.write(path2, map.get(string), new OpenOption[0]);
        for (String string2 : List.of("player", "player-compact")) {
            String string3 = "layout/player-panel/fonts/" + string2 + "/head.json";
            Path path3 = path.resolve("assets/enorialshud/font/" + string3);
            if (!Files.isRegularFile(path3, new LinkOption[0])) continue;
            JsonObject jsonObject = JsonParser.parseString((String)Files.readString(path3)).getAsJsonObject();
            for (JsonElement jsonElement : jsonObject.getAsJsonArray("providers")) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                if (!jsonObject2.has("file") || !jsonObject2.get("file").getAsString().endsWith("/portrait.png")) continue;
                jsonObject2.addProperty("file", "enorialshud:themes/" + hudTheme.id() + "/player_portrait.png");
            }
            Path path4 = path.resolve("assets/enorialshud/font/themes/" + hudTheme.id() + "/" + string3);
            Files.createDirectories(path4.getParent(), new FileAttribute[0]);
            Files.writeString(path4, (CharSequence)gson.toJson((JsonElement)jsonObject), new OpenOption[0]);
        }
    }

    public static void playerPortraits(EnorialsHUD enorialsHUD, BufferedImage bufferedImage, Map<String, byte[]> map) throws IOException {
        for (HudTheme hudTheme : HudTheme.values()) {
            BufferedImage bufferedImage2;
            if (hudTheme == HudTheme.ORIGINAL) continue;
            try (Object object = enorialsHUD.getResource("theme-art/" + hudTheme.id() + "/frames/player_avatar_mask.png");){
                if (object == null) {
                    throw new IOException("Missing theme portrait mask");
                }
                bufferedImage2 = ImageIO.read((InputStream)object);
            }
            object = new BufferedImage(bufferedImage2.getWidth(), bufferedImage2.getHeight(), 2);
            for (int i = 0; i < bufferedImage2.getHeight(); ++i) {
                for (int j = 0; j < bufferedImage2.getWidth(); ++j) {
                    ((BufferedImage)object).setRGB(j, i, bufferedImage2.getRGB(j, i) & 0xFF000000 | bufferedImage.getRGB(j * bufferedImage.getWidth() / bufferedImage2.getWidth(), i * bufferedImage.getHeight() / bufferedImage2.getHeight()) & 0xFFFFFF);
                }
            }
            try (Closeable closeable = enorialsHUD.getResource("default/textures/assets/enorialshud/player_avatar_mask.png");){
                if (closeable != null) {
                    ThemePack.preserveAdvance((BufferedImage)object, ImageIO.read((InputStream)closeable));
                }
            }
            closeable = new ByteArrayOutputStream();
            ImageIO.write((RenderedImage)object, "png", (OutputStream)closeable);
            map.put("assets/enorialshud/textures/themes/" + hudTheme.id() + "/player_portrait.png", ((ByteArrayOutputStream)closeable).toByteArray());
        }
    }

    public static void preserveAdvance(BufferedImage bufferedImage, BufferedImage bufferedImage2) {
        int n;
        int n2;
        int n3 = -1;
        int n4 = 0;
        block0: for (n2 = bufferedImage2.getWidth() - 1; n2 >= 0 && n3 < 0; --n2) {
            for (n = 0; n < bufferedImage2.getHeight(); ++n) {
                if (bufferedImage2.getRGB(n2, n) >>> 24 == 0) continue;
                n3 = n2;
                n4 = n;
                continue block0;
            }
        }
        for (n2 = 0; n2 < bufferedImage.getHeight(); ++n2) {
            for (n = n3 + 1; n < bufferedImage.getWidth(); ++n) {
                bufferedImage.setRGB(n, n2, 0);
            }
        }
        if (n3 >= 0) {
            n2 = 0;
            for (n = 0; n < bufferedImage.getHeight(); ++n) {
                n2 |= bufferedImage.getRGB(n3, n) >>> 24 != 0 ? 1 : 0;
            }
            if (n2 == 0) {
                bufferedImage.setRGB(n3, n4, 0x2000000);
            }
        }
    }
}

