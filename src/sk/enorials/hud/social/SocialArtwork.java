/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.social;

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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import javax.imageio.ImageIO;
import sk.enorials.hud.theme.HudTheme;

public final class SocialArtwork {
    public static final int SCALE = 3;
    public static final int WIDTH = 252;
    public static final int LEFT = -38;
    public static final int TOP = -28;
    public static final int TILE = 63;
    public static final int COLUMNS = 4;

    private SocialArtwork() {
    }

    public static int top(boolean bl) {
        return bl ? -36 : -28;
    }

    public static int height(boolean bl) {
        return bl ? 294 : 180;
    }

    public static int rows(boolean bl) {
        return (SocialArtwork.height(bl) + 63 - 1) / 63;
    }

    public static int accent(HudTheme hudTheme) {
        return switch (hudTheme) {
            default -> throw new IncompatibleClassChangeError();
            case HudTheme.ORIGINAL -> 16426285;
            case HudTheme.FROSTBOUND -> 9034994;
            case HudTheme.WILDWOOD -> 11323769;
            case HudTheme.INFERNO -> 16347191;
            case HudTheme.ASTRAL -> 12952045;
            case HudTheme.CORSAIR -> 14267751;
        };
    }

    private static int base(HudTheme hudTheme) {
        return switch (hudTheme) {
            default -> throw new IncompatibleClassChangeError();
            case HudTheme.ORIGINAL -> 2109237;
            case HudTheme.FROSTBOUND -> 1518652;
            case HudTheme.WILDWOOD -> 2108964;
            case HudTheme.INFERNO -> 3153958;
            case HudTheme.ASTRAL -> 2432829;
            case HudTheme.CORSAIR -> 2698285;
        };
    }

    private static BufferedImage read(String string) {
        BufferedImage bufferedImage;
        block9: {
            InputStream inputStream = SocialArtwork.class.getResourceAsStream(string);
            try {
                if (inputStream == null) {
                    throw new IOException("Missing authored social artwork: " + string);
                }
                bufferedImage = ImageIO.read(inputStream);
                if (inputStream == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException iOException) {
                    throw new UncheckedIOException(iOException);
                }
            }
            inputStream.close();
        }
        return bufferedImage;
    }

    public static BufferedImage render(HudTheme hudTheme, boolean bl) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6 = bl ? 222 : 168;
        int n7 = SocialArtwork.height(bl);
        BufferedImage bufferedImage = new BufferedImage(756, n7 * 3, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.scale(3.0, 3.0);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = new Color(SocialArtwork.base(hudTheme));
        Color color2 = new Color(SocialArtwork.accent(hudTheme));
        BufferedImage bufferedImage2 = SocialArtwork.read((String)(hudTheme == HudTheme.ORIGINAL ? "/default/textures/assets/enorialshud/player_frame.png" : "/theme-art/" + hudTheme.id() + "/frames/player_frame_foreground.png"));
        BufferedImage bufferedImage3 = SocialArtwork.read("/social-frames/" + hudTheme.id() + (bl ? "-trade.png" : "-invite.png"));
        int n8 = n5 = bl ? 0 : 14;
        if (bl) {
            n4 = 0;
        } else {
            switch (hudTheme) {
                case FROSTBOUND: 
                case INFERNO: {
                    n4 = 19;
                    break;
                }
                case WILDWOOD: {
                    n4 = 20;
                    break;
                }
                case ASTRAL: {
                    n4 = 18;
                    break;
                }
                default: {
                    n4 = 17;
                }
            }
        }
        int n9 = n4;
        int n10 = bl ? 252 : 224;
        int n11 = n3 = bl ? n7 : 150;
        if (bufferedImage3.getWidth() != n10 * 3 || bufferedImage3.getHeight() != n3 * 3) {
            throw new IllegalStateException("Invalid social frame dimensions: " + String.valueOf((Object)hudTheme));
        }
        graphics2D.drawImage(bufferedImage3, n5, n9, n10, n3, null);
        graphics2D.translate(38, -SocialArtwork.top(bl));
        for (n2 = bl ? 1 : 2; n2 < (bl ? n6 - 1 : 130); n2 += 3) {
            for (n = 1; n < 175; n += 3) {
                if ((n * 13 + n2 * 7) % 19 >= 5) continue;
                graphics2D.setColor(new Color(255, 255, 255, 3));
                graphics2D.fillRect(n, n2, 3, 3);
            }
        }
        if (bl) {
            for (n2 = 1; n2 <= 3; ++n2) {
                for (n = 0; n < 9; ++n) {
                    if (n == 4) continue;
                    SocialArtwork.slot(graphics2D, 8 + 18 * n, 18 + 18 * n2, color, color2);
                }
            }
            SocialArtwork.plate(graphics2D, bufferedImage2, hudTheme, 5, 89, 76, 19, color);
            SocialArtwork.plate(graphics2D, bufferedImage2, hudTheme, 95, 89, 76, 19, color);
            SocialArtwork.plate(graphics2D, bufferedImage2, hudTheme, 5, 107, 76, 20, color);
            SocialArtwork.plate(graphics2D, bufferedImage2, hudTheme, 95, 107, 76, 20, color);
        } else {
            SocialArtwork.plate(graphics2D, bufferedImage2, hudTheme, 5, 85, 76, 20, color);
            SocialArtwork.plate(graphics2D, bufferedImage2, hudTheme, 95, 85, 76, 20, color);
            graphics2D.setColor(new Color(SocialArtwork.accent(hudTheme), false));
            graphics2D.setComposite(AlphaComposite.SrcOver.derive(0.3f));
            graphics2D.drawLine(12, 77, 163, 77);
            graphics2D.setComposite(AlphaComposite.SrcOver);
        }
        int n12 = n2 = bl ? 139 : 85;
        if (bl) {
            for (n = 0; n < 3; ++n) {
                for (int i = 0; i < 9; ++i) {
                    SocialArtwork.slot(graphics2D, 8 + 18 * i, n2 + 18 * n, color, color2);
                }
            }
            for (n = 0; n < 9; ++n) {
                SocialArtwork.slot(graphics2D, 8 + 18 * n, n2 + 58, color, color2);
            }
        }
        graphics2D.dispose();
        return bufferedImage;
    }

    public static BufferedImage heading(HudTheme hudTheme, String string) {
        return SocialArtwork.read("/social-headings/" + hudTheme.id() + "-" + string + ".png");
    }

    private static void slot(Graphics2D graphics2D, int n, int n2, Color color, Color color2) {
        graphics2D.setColor(color.darker());
        graphics2D.fillRect(n - 1, n2 - 1, 18, 18);
        graphics2D.setColor(color2.darker().darker());
        graphics2D.drawRect(n - 1, n2 - 1, 17, 17);
        graphics2D.setColor(new Color(593428));
        graphics2D.fillRect(n, n2, 16, 16);
        graphics2D.setColor(new Color(255, 255, 255, 16));
        graphics2D.drawLine(n, n2 + 16, n + 16, n2 + 16);
    }

    public static BufferedImage lock(HudTheme hudTheme, boolean bl) {
        BufferedImage bufferedImage = new BufferedImage(96, 96, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.scale(4.0, 4.0);
        graphics2D.translate(12, 12);
        graphics2D.scale(0.93, 0.93);
        graphics2D.translate(-12, -12);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = new Color(SocialArtwork.accent(hudTheme));
        Color color2 = new Color(SocialArtwork.base(hudTheme)).darker();
        Path2D.Float float_ = new Path2D.Float();
        if (bl) {
            float_.moveTo(7.0f, 10.0f);
            float_.lineTo(7.0f, 7.0f);
            float_.curveTo(7.0f, 1.0f, 17.0f, 1.0f, 17.0f, 7.0f);
            float_.lineTo(17.0f, 10.0f);
        } else {
            float_.moveTo(10.0f, 10.0f);
            float_.lineTo(10.0f, 7.0f);
            float_.curveTo(10.0f, 1.0f, 20.0f, 1.0f, 20.0f, 7.0f);
        }
        graphics2D.setColor(color2);
        graphics2D.setStroke(new BasicStroke(3.6f, 1, 1));
        graphics2D.draw(float_);
        graphics2D.setPaint(new GradientPaint(5.0f, 2.0f, color.brighter(), 19.0f, 10.0f, color));
        graphics2D.setStroke(new BasicStroke(2.1f, 1, 1));
        graphics2D.draw(float_);
        RoundRectangle2D.Double double_ = new RoundRectangle2D.Double(3.5, 10.0, 17.0, 12.0, 3.0, 3.0);
        graphics2D.setColor(color2);
        graphics2D.setStroke(new BasicStroke(1.5f));
        graphics2D.draw(double_);
        graphics2D.setPaint(new GradientPaint(0.0f, 10.0f, color.brighter(), 0.0f, 22.0f, color.darker()));
        graphics2D.fill(double_);
        graphics2D.setColor(color.brighter());
        graphics2D.setStroke(new BasicStroke(0.6f));
        graphics2D.drawLine(6, 11, 18, 11);
        graphics2D.setColor(color2);
        graphics2D.fillOval(10, 13, 4, 4);
        graphics2D.fillRect(11, 16, 2, 3);
        graphics2D.setColor(new Color(255, 255, 255, 140));
        for (int n : new int[]{6, 18}) {
            for (int n2 : new int[]{14, 19}) {
                graphics2D.fill(new Ellipse2D.Double((double)n - 0.45, (double)n2 - 0.45, 0.9, 0.9));
            }
        }
        graphics2D.dispose();
        return bufferedImage;
    }

    private static void plate(Graphics2D graphics2D, BufferedImage bufferedImage, HudTheme hudTheme, int n, int n2, int n3, int n4, Color color) {
        Graphics2D graphics2D2 = (Graphics2D)graphics2D.create();
        graphics2D2.clipRect(n, n2, n3, n4);
        SocialArtwork.paintPlate(graphics2D2, bufferedImage, hudTheme, n, n2, n3, n4, "normal");
        graphics2D2.dispose();
    }

    public static BufferedImage button(HudTheme hudTheme, int n, int n2) {
        return SocialArtwork.button(hudTheme, n, n2, "normal");
    }

    public static BufferedImage button(HudTheme hudTheme, int n, int n2, String string) {
        BufferedImage bufferedImage = new BufferedImage(n * 3, n2 * 3, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.scale(3.0, 3.0);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BufferedImage bufferedImage2 = SocialArtwork.read((String)(hudTheme == HudTheme.ORIGINAL ? "/default/textures/assets/enorialshud/player_frame.png" : "/theme-art/" + hudTheme.id() + "/frames/player_frame_foreground.png"));
        SocialArtwork.paintPlate(graphics2D, bufferedImage2, hudTheme, 0, 0, n, n2, string);
        graphics2D.dispose();
        return bufferedImage;
    }

    private static void paintPlate(Graphics2D graphics2D, BufferedImage bufferedImage, HudTheme hudTheme, int n, int n2, int n3, int n4, String string) {
        Color color = new Color(switch (hudTheme) {
            default -> throw new IncompatibleClassChangeError();
            case HudTheme.ORIGINAL -> 4411480;
            case HudTheme.FROSTBOUND -> 3234675;
            case HudTheme.WILDWOOD -> 4929831;
            case HudTheme.INFERNO -> 5122344;
            case HudTheme.ASTRAL -> 4207195;
            case HudTheme.CORSAIR -> 5718058;
        });
        if (!string.equals("normal")) {
            color = new Color(switch (string) {
                case "open" -> 7887400;
                case "locked" -> 2382714;
                case "confirmed" -> 4356914;
                default -> 3300658;
            });
        }
        Object object = SocialArtwork.read((String)(hudTheme == HudTheme.ORIGINAL ? "/default/textures/assets/enorialshud/player_health.png" : "/theme-art/" + hudTheme.id() + "/frames/player_health_mask.png"));
        BufferedImage bufferedImage2 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
        Color color2 = color.brighter();
        Color color3 = color.darker();
        for (int i = 0; i < ((BufferedImage)object).getHeight(); ++i) {
            for (int j = 0; j < ((BufferedImage)object).getWidth(); ++j) {
                float f = (float)i / (float)Math.max(1, ((BufferedImage)object).getHeight() - 1);
                int n5 = (int)((float)color2.getRed() * (1.0f - f) + (float)color3.getRed() * f) << 16 | (int)((float)color2.getGreen() * (1.0f - f) + (float)color3.getGreen() * f) << 8 | (int)((float)color2.getBlue() * (1.0f - f) + (float)color3.getBlue() * f);
                bufferedImage2.setRGB(368 + j, 52 + i, ((BufferedImage)object).getRGB(j, i) & 0xFF000000 | n5);
            }
        }
        BufferedImage bufferedImage3 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
        Graphics2D graphics2D2 = bufferedImage3.createGraphics();
        graphics2D2.drawImage((Image)bufferedImage, 0, 0, null);
        graphics2D2.setComposite(AlphaComposite.DstOut);
        graphics2D2.drawImage((Image)bufferedImage2, 0, 0, null);
        graphics2D2.dispose();
        for (BufferedImage bufferedImage4 : new BufferedImage[]{bufferedImage2, bufferedImage3}) {
            int n6 = 365;
            int n7 = 36;
            int n8 = 627;
            int n9 = 80;
            int n10 = 38;
            int n11 = 8;
            if (hudTheme == HudTheme.ORIGINAL) {
                graphics2D.drawImage(bufferedImage4, n, n2, n + n11, n2 + n4, n6 + n8, n7, n6 + n8 - n10, n7 + n9, null);
            } else {
                graphics2D.drawImage(bufferedImage4, n, n2, n + n11, n2 + n4, n6, n7, n6 + n10, n7 + n9, null);
            }
            graphics2D.drawImage(bufferedImage4, n + n11, n2, n + n3 - n11, n2 + n4, n6 + n10, n7, n6 + n8 - n10, n7 + n9, null);
            graphics2D.drawImage(bufferedImage4, n + n3 - n11, n2, n + n3, n2 + n4, n6 + n8 - n10, n7, n6 + n8, n7 + n9, null);
        }
    }

    public static void build(Path path) throws IOException {
        JsonObject jsonObject;
        JsonObject jsonObject2;
        BufferedImage bufferedImage;
        String string;
        JsonArray jsonArray;
        BufferedImage bufferedImage2;
        String string2;
        Path path2 = path.resolve("assets/enorials/font/social");
        Path path3 = path.resolve("assets/enorials/textures/social");
        Files.createDirectories(path2, new FileAttribute[0]);
        Files.createDirectories(path3, new FileAttribute[0]);
        for (HudTheme hudTheme : HudTheme.values()) {
            for (boolean bl : new boolean[]{false, true}) {
                string2 = (bl ? "trade_" : "invite_") + hudTheme.id();
                bufferedImage2 = SocialArtwork.render(hudTheme, bl);
                jsonArray = new JsonArray();
                for (int i = 0; i < SocialArtwork.rows(bl); ++i) {
                    for (int j = 0; j < 4; ++j) {
                        string = string2 + "_" + i + "_" + j + ".png";
                        bufferedImage = new BufferedImage(189, 189, 2);
                        Graphics2D graphics2D = bufferedImage.createGraphics();
                        graphics2D.drawImage((Image)bufferedImage2, -j * 63 * 3, -i * 63 * 3, null);
                        graphics2D.dispose();
                        ImageIO.write((RenderedImage)bufferedImage, "png", path3.resolve(string).toFile());
                        JsonObject jsonObject3 = new JsonObject();
                        jsonObject3.addProperty("type", "bitmap");
                        jsonObject3.addProperty("file", "enorials:social/" + string);
                        jsonObject3.addProperty("height", (Number)63);
                        jsonObject3.addProperty("ascent", (Number)(13 - SocialArtwork.top(bl) - i * 63));
                        JsonArray jsonArray2 = new JsonArray();
                        jsonArray2.add(String.valueOf((char)(61296 + i * 4 + j)));
                        jsonObject3.add("chars", (JsonElement)jsonArray2);
                        jsonArray.add((JsonElement)jsonObject3);
                    }
                }
                jsonObject2 = new JsonObject();
                jsonObject2.add("providers", (JsonElement)jsonArray);
                Files.writeString(path2.resolve(string2 + ".json"), (CharSequence)jsonObject2.toString(), new OpenOption[0]);
            }
        }
        for (HudTheme hudTheme : HudTheme.values()) {
            for (String string3 : new String[]{"open", "locked", "confirm", "confirmed"}) {
                string2 = "state_" + hudTheme.id() + "_" + string3;
                ImageIO.write((RenderedImage)SocialArtwork.button(hudTheme, 76, 20, string3), "png", path3.resolve(string2 + ".png").toFile());
                bufferedImage2 = new JsonObject();
                bufferedImage2.addProperty("type", "bitmap");
                bufferedImage2.addProperty("file", "enorials:social/" + string2 + ".png");
                bufferedImage2.addProperty("height", 20);
                bufferedImage2.addProperty("ascent", -94);
                jsonArray = new JsonArray();
                jsonArray.add("\uefb0");
                bufferedImage2.add("chars", (JsonElement)jsonArray);
                jsonObject2 = new JsonArray();
                jsonObject2.add((JsonElement)bufferedImage2);
                JsonObject jsonObject4 = new JsonObject();
                jsonObject4.add("providers", (JsonElement)jsonObject2);
                Files.writeString(path2.resolve(string2 + ".json"), (CharSequence)jsonObject4.toString(), new OpenOption[0]);
            }
        }
        for (HudTheme hudTheme : HudTheme.values()) {
            String string4 = "locks_" + hudTheme.id();
            BufferedImage bufferedImage3 = new BufferedImage(192, 96, 2);
            Graphics2D graphics2D = bufferedImage3.createGraphics();
            graphics2D.drawImage((Image)SocialArtwork.lock(hudTheme, true), 0, 0, null);
            graphics2D.drawImage((Image)SocialArtwork.lock(hudTheme, false), 96, 0, null);
            graphics2D.dispose();
            ImageIO.write((RenderedImage)bufferedImage3, "png", path3.resolve(string4 + ".png").toFile());
            for (int n : new int[]{27, 111}) {
                jsonObject2 = new JsonObject();
                jsonObject2.addProperty("type", "bitmap");
                jsonObject2.addProperty("file", "enorials:social/" + string4 + ".png");
                jsonObject2.addProperty("height", (Number)(n == 27 ? 8 : 12));
                jsonObject2.addProperty("ascent", (Number)(13 - n));
                JsonArray jsonArray3 = new JsonArray();
                jsonArray3.add("\uefb1\uefb2");
                jsonObject2.add("chars", (JsonElement)jsonArray3);
                string = new JsonArray();
                string.add((JsonElement)jsonObject2);
                bufferedImage = new JsonObject();
                bufferedImage.add("providers", (JsonElement)string);
                Files.writeString(path2.resolve(string4 + "_" + n + ".json"), (CharSequence)bufferedImage.toString(), new OpenOption[0]);
            }
        }
        for (HudTheme hudTheme : HudTheme.values()) {
            for (String string5 : new String[]{"trade", "party", "request"}) {
                String string6 = "heading_" + hudTheme.id() + "_" + string5;
                BufferedImage bufferedImage4 = SocialArtwork.heading(hudTheme, string5);
                JsonArray jsonArray4 = new JsonArray();
                for (int i = 0; i < 4; ++i) {
                    String string7 = string6 + "_" + i + ".png";
                    ImageIO.write((RenderedImage)bufferedImage4.getSubimage(i * 40 * 3, 0, 120, 42), "png", path3.resolve(string7).toFile());
                    string = new JsonObject();
                    string.addProperty("type", "bitmap");
                    string.addProperty("file", "enorials:social/" + string7);
                    string.addProperty("height", (Number)14);
                    string.addProperty("ascent", (Number)(13 - (string5.equals("trade") ? 0 : 24)));
                    bufferedImage = new JsonArray();
                    bufferedImage.add(String.valueOf((char)(61376 + i)));
                    string.add("chars", (JsonElement)bufferedImage);
                    jsonArray4.add((JsonElement)string);
                }
                jsonObject2 = new JsonObject();
                jsonObject2.add("providers", (JsonElement)jsonArray4);
                Files.writeString(path2.resolve(string6 + ".json"), (CharSequence)jsonObject2.toString(), new OpenOption[0]);
            }
        }
        try (InputStream inputStream = SocialArtwork.class.getResourceAsStream("/default/pack/assets/enorials/font/menu/ref/native6_82.json");){
            if (inputStream == null) {
                throw new IOException("Missing native menu font");
            }
            jsonObject = JsonParser.parseString((String)new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (Object object : (InputStream)new int[]{17, 44}) {
            JsonObject jsonObject5 = jsonObject.deepCopy();
            for (JsonElement jsonElement : jsonObject5.getAsJsonArray("providers")) {
                JsonObject jsonObject6 = jsonElement.getAsJsonObject();
                if (jsonObject6.has("height")) {
                    jsonObject6.addProperty("height", (Number)8);
                }
                if (!jsonObject6.has("ascent")) continue;
                jsonObject6.addProperty("ascent", (Number)(12 - object));
            }
            Files.writeString(path2.resolve("name_" + (int)object + ".json"), (CharSequence)jsonObject5.toString(), new OpenOption[0]);
        }
        for (Object object : (InputStream)new int[]{5, 17, 18, 22, 28, 29, 43, 45, 52, 58, 61, 66, 91, 95, 113, 130}) {
            Object object22;
            JsonObject jsonObject7 = jsonObject.deepCopy();
            for (Object object22 : jsonObject7.getAsJsonArray("providers")) {
                if (!object22.getAsJsonObject().has("ascent")) continue;
                object22.getAsJsonObject().addProperty("ascent", (Number)Math.min(6, 12 - object));
            }
            Files.writeString(path2.resolve("text_" + (int)object + ".json"), (CharSequence)jsonObject7.toString(), new OpenOption[0]);
            if (object != 28 && object != 95) continue;
            Iterator iterator = jsonObject7.deepCopy();
            object22 = iterator.getAsJsonArray("providers").iterator();
            while (object22.hasNext()) {
                JsonElement jsonElement = (JsonElement)object22.next();
                if (!jsonElement.getAsJsonObject().has("height")) continue;
                jsonElement.getAsJsonObject().addProperty("height", (Number)5);
            }
            Files.writeString(path2.resolve(object == 95 ? "money.json" : "status.json"), (CharSequence)iterator.toString(), new OpenOption[0]);
        }
    }
}

