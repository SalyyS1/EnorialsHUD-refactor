/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import javax.imageio.ImageIO;

public final class MainMenuArtwork {
    private static final String BASE = "/default/pack/assets/enorials/";

    private MainMenuArtwork() {
    }

    private static BufferedImage source(String string) throws IOException {
        try (InputStream inputStream = MainMenuArtwork.class.getResourceAsStream("/default/pack/assets/enorials/textures/menu/ref/" + string + ".png");){
            if (inputStream == null) {
                throw new IOException("Missing menu plate " + string);
            }
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            return bufferedImage;
        }
    }

    private static BufferedImage root(boolean bl) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(912, 912, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                graphics2D.drawImage((Image)MainMenuArtwork.source("root40_" + (bl ? 1 : 0) + "_1_" + i + "_" + j), j * 228, i * 228, null);
            }
        }
        graphics2D.dispose();
        return bufferedImage;
    }

    public static void build(Path path2) throws IOException {
        InputStream inputStream;
        Object object;
        Object object2;
        Object object4;
        Path path3 = path2.resolve("assets/enorials/items/menu");
        if (Files.isDirectory(path3, new LinkOption[0])) {
            object4 = Files.list(path3);
            try {
                for (Path object32 : object4.filter(path -> path.getFileName().toString().startsWith("head_") && path.toString().endsWith(".json")).toList()) {
                    object2 = JsonParser.parseString((String)Files.readString(object32)).getAsJsonObject();
                    object2.addProperty("oversized_in_gui", Boolean.valueOf(true));
                    Files.writeString(object32, (CharSequence)object2.toString(), new OpenOption[0]);
                }
            }
            finally {
                if (object4 != null) {
                    object4.close();
                }
            }
        }
        object4 = path2.resolve("assets/enorials/textures/menu/ref");
        Path path4 = path2.resolve("assets/enorials/font/menu/ref");
        Files.createDirectories((Path)object4, new FileAttribute[0]);
        Files.createDirectories(path4.resolve("symbol"), new FileAttribute[0]);
        BufferedImage bufferedImage = MainMenuArtwork.root(false);
        object2 = MainMenuArtwork.root(true);
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
                BufferedImage bufferedImage2 = new BufferedImage(912, 912, 2);
                Graphics2D n = bufferedImage2.createGraphics();
                n.drawImage((Image)(j == 1 ? object2 : bufferedImage), 0, 0, null);
                n.setComposite(AlphaComposite.Src);
                n.drawImage(bufferedImage, 48, 600, 864, 736, 48, 736, 864, 752, null);
                object = i == 1 ? object2 : bufferedImage;
                if (i == 1) {
                    n.drawImage((Image)object, 48, 608, 440, 720, 48, 760, 440, 872, null);
                    n.drawImage((Image)object, 472, 608, 864, 720, 472, 760, 864, 872, null);
                } else {
                    n.drawImage((Image)object, 48, 608, 864, 720, 48, 760, 864, 872, null);
                }
                n.dispose();
                for (int k = 0; k < 4; ++k) {
                    for (int i2 = 0; i2 < 4; ++i2) {
                        ImageIO.write((RenderedImage)bufferedImage2.getSubimage(i2 * 228, k * 228, 228, 228), "png", object4.resolve("root40_" + i + "_" + j + "_" + k + "_" + i2 + ".png").toFile());
                    }
                }
            }
        }
        ImageIO.write((RenderedImage)MainMenuArtwork.tradingIcon(), "png", object4.resolve("trading.png").toFile());
        for (int n : new int[]{5, 6}) {
            for (int jsonElement2 : new int[]{101, 109}) {
                inputStream = MainMenuArtwork.class.getResourceAsStream("/default/pack/assets/enorials/font/menu/ref/native6_82.json");
                try {
                    if (inputStream == null) {
                        throw new IOException("Missing menu text font");
                    }
                    JsonObject jsonObject = JsonParser.parseReader((Reader)new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (JsonElement jsonElement : jsonObject.getAsJsonArray("providers")) {
                        JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                        if (jsonObject2.has("height")) {
                            jsonObject2.addProperty("height", (Number)n);
                        }
                        if (!jsonObject2.has("ascent")) continue;
                        jsonObject2.addProperty("ascent", (Number)(12 - jsonElement2));
                    }
                    Files.writeString(path4.resolve("native" + n + "_" + jsonElement2 + ".json"), (CharSequence)jsonObject.toString(), new OpenOption[0]);
                }
                finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }
        }
        for (int n : new int[]{151, 189}) {
            JsonElement jsonElement2;
            JsonObject jsonObject;
            object = MainMenuArtwork.class.getResourceAsStream("/default/pack/assets/enorials/font/menu/ref/atlas41_22_148.json");
            try {
                if (object == null) {
                    throw new IOException("Missing menu icon atlas font");
                }
                jsonObject = JsonParser.parseReader((Reader)new InputStreamReader((InputStream)object, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement jsonElement2 : jsonObject.getAsJsonArray("providers")) {
                    inputStream = jsonElement2.getAsJsonObject();
                    if (!"bitmap".equals(inputStream.get("type").getAsString())) continue;
                    inputStream.addProperty("height", 18);
                    inputStream.addProperty("ascent", 13 - n);
                }
                Files.writeString(path4.resolve("atlas41_18_" + n + ".json"), (CharSequence)jsonObject.toString(), new OpenOption[0]);
            }
            finally {
                if (object != null) {
                    ((InputStream)object).close();
                }
            }
            object = new JsonObject();
            object.addProperty("type", "bitmap");
            object.addProperty("file", "enorials:menu/ref/trading.png");
            object.addProperty("height", (Number)18);
            object.addProperty("ascent", (Number)(13 - n));
            jsonObject = new JsonArray();
            jsonObject.add("\ued00");
            object.add("chars", (JsonElement)jsonObject);
            Iterator iterator = new JsonArray();
            iterator.add((JsonElement)object);
            jsonElement2 = new JsonObject();
            jsonElement2.add("providers", (JsonElement)iterator);
            Files.writeString(path4.resolve("symbol/trading_18_" + n + ".json"), (CharSequence)jsonElement2.toString(), new OpenOption[0]);
        }
    }

    private static BufferedImage tradingIcon() {
        BufferedImage bufferedImage = new BufferedImage(96, 96, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.scale(4.0, 4.0);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 2; ++i) {
            Path2D.Double double_ = new Path2D.Double();
            double_.moveTo(3.0, 5.0);
            double_.lineTo(15.0, 5.0);
            double_.lineTo(15.0, 2.0);
            double_.lineTo(22.0, 8.0);
            double_.lineTo(15.0, 14.0);
            double_.lineTo(15.0, 11.0);
            double_.lineTo(3.0, 11.0);
            double_.closePath();
            graphics2D.setColor(new Color(1516330));
            graphics2D.setStroke(new BasicStroke(1.4f, 1, 1));
            graphics2D.draw(double_);
            graphics2D.setPaint(new GradientPaint(0.0f, 3.0f, new Color(16768420), 0.0f, 13.0f, new Color(13990440)));
            graphics2D.fill(double_);
            if (i != 0) continue;
            graphics2D.translate(24, 24);
            graphics2D.rotate(Math.PI);
        }
        graphics2D.dispose();
        return bufferedImage;
    }
}

