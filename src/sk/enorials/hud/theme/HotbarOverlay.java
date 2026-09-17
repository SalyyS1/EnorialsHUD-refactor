/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.MainHand
 */
package sk.enorials.hud.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import javax.imageio.ImageIO;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.model.position.OffsetPosition;
import sk.enorials.hud.model.position.PositionContainer;
import sk.enorials.hud.model.position.ScreenPosition;
import sk.enorials.hud.model.position.ShaderInfo;
import sk.enorials.hud.theme.HudTheme;

public final class HotbarOverlay {
    public static final String PREFERENCE = "themed-hotbar";
    private static final int TILE = 64;
    private static final int SCALE = 3;
    private static final int COLUMNS = 4;
    private static final int HEIGHT = 48;

    private HotbarOverlay() {
    }

    public static BufferedImage frame(HudTheme hudTheme) throws IOException {
        return HotbarOverlay.artwork(hudTheme, "bar");
    }

    public static BufferedImage offhand(HudTheme hudTheme, boolean bl) throws IOException {
        return HotbarOverlay.artwork(hudTheme, bl ? "left" : "right");
    }

    public static BufferedImage selection(HudTheme hudTheme) throws IOException {
        return HotbarOverlay.artwork(hudTheme, "selected");
    }

    private static BufferedImage artwork(HudTheme hudTheme, String string) throws IOException {
        String string2 = "/hotbar-art/" + hudTheme.id() + "_" + string + ".png";
        try (InputStream inputStream = HotbarOverlay.class.getResourceAsStream(string2);){
            if (inputStream == null) {
                throw new IOException("Missing authored hotbar sprite: " + string2);
            }
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IOException("Invalid hotbar sprite: " + string2);
            }
            BufferedImage bufferedImage2 = bufferedImage;
            return bufferedImage2;
        }
    }

    public static void build(Path path, PositionContainer positionContainer) throws IOException {
        Path path2 = path.resolve("assets/enorials/textures/hotbar");
        Path path3 = path.resolve("assets/enorials/font/hotbar");
        Files.createDirectories(path2, new FileAttribute[0]);
        Files.createDirectories(path3, new FileAttribute[0]);
        for (HudTheme hudTheme : HudTheme.values()) {
            int n;
            BufferedImage bufferedImage = HotbarOverlay.frame(hudTheme);
            for (n = 0; n < 4; ++n) {
                BufferedImage bufferedImage2 = new BufferedImage(192, 144, 2);
                Graphics2D graphics2D = bufferedImage2.createGraphics();
                graphics2D.drawImage((Image)bufferedImage, -n * 64 * 3, 0, null);
                graphics2D.dispose();
                HotbarOverlay.glyph(path2, path3, positionContainer, hudTheme.id() + "_bar_" + n, bufferedImage2, 48, -103 + 64 * n, -48);
            }
            for (n = 0; n < 9; ++n) {
                HotbarOverlay.glyph(path2, path3, positionContainer, hudTheme.id() + "_selected_" + n, HotbarOverlay.selection(hudTheme), 23, -92 + 20 * n, -23);
            }
            HotbarOverlay.glyph(path2, path3, positionContainer, hudTheme.id() + "_left", HotbarOverlay.offhand(hudTheme, true), 23, -121, -23);
            HotbarOverlay.glyph(path2, path3, positionContainer, hudTheme.id() + "_right", HotbarOverlay.offhand(hudTheme, false), 23, 97, -23);
        }
    }

    private static void glyph(Path path, Path path2, PositionContainer positionContainer, String string, BufferedImage bufferedImage, int n, int n2, int n3) throws IOException {
        ShaderInfo shaderInfo = ShaderInfo.position(new ScreenPosition(50.0f, 100.0f, new OffsetPosition(n2, n3, 4.0))).nativeCenter().build();
        int n4 = positionContainer.addThenGetAscent(shaderInfo);
        ImageIO.write((RenderedImage)bufferedImage, "png", path.resolve(string + ".png").toFile());
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "bitmap");
        jsonObject.addProperty("file", "enorials:hotbar/" + string + ".png");
        jsonObject.addProperty("height", (Number)n);
        jsonObject.addProperty("ascent", (Number)n4);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("\uefd0");
        jsonObject.add("chars", (JsonElement)jsonArray);
        JsonArray jsonArray2 = new JsonArray();
        jsonArray2.add((JsonElement)jsonObject);
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.add("providers", (JsonElement)jsonArray2);
        Files.writeString(path2.resolve(string + ".json"), (CharSequence)jsonObject2.toString(), new OpenOption[0]);
    }

    public static Component render(EnorialsHUD enorialsHUD, Player player) {
        return HotbarOverlay.render(enorialsHUD, player, -1);
    }

    public static boolean enabled(EnorialsHUD enorialsHUD, Player player) {
        return player.getGameMode() != GameMode.SPECTATOR && HudAccess.hud(player) && enorialsHUD.storage().preference(player.getUniqueId(), PREFERENCE, true);
    }

    public static Component render(EnorialsHUD enorialsHUD, Player player, int n) {
        boolean bl;
        if (!HotbarOverlay.enabled(enorialsHUD, player)) {
            return Component.empty();
        }
        String string = enorialsHUD.storage().theme(player.getUniqueId()).id();
        TextComponent textComponent = Component.empty();
        boolean bl2 = bl = !player.getInventory().getItemInOffHand().getType().isAir();
        String string2 = bl ? (player.getMainHand() == MainHand.RIGHT ? "_left" : "_right") : "";
        for (int i = 0; i < 4; ++i) {
            textComponent = textComponent.append(HotbarOverlay.glyph(enorialsHUD, string + "_bar_" + i));
        }
        textComponent = textComponent.append(HotbarOverlay.glyph(enorialsHUD, string + "_selected_" + (n < 0 ? player.getInventory().getHeldItemSlot() : n)));
        if (bl) {
            textComponent = textComponent.append(HotbarOverlay.glyph(enorialsHUD, string + string2));
        }
        return (Component)TextCompat.shadow(textComponent, 0);
    }

    private static Component glyph(EnorialsHUD enorialsHUD, String string) {
        return enorialsHUD.packs().registry().metrics().neutral(((TextComponent)LegacyText.font((Component)Component.text((char)'\uefd0'), Key.key((String)("enorials:hotbar/" + string)))).color((TextColor)NamedTextColor.WHITE));
    }
}

