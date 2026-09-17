/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.IntFunction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.util.SpaceUtil;

public final class SharedUiSprites {
    private final Map<Integer, List<Tile>> sprites = new HashMap<Integer, List<Tile>>();
    private final Key font;

    public SharedUiSprites(String string, String string2) {
        this.font = Key.key((String)("enorials:" + string + "/ui"));
        Properties properties = new Properties();
        try (InputStream inputStream = SharedUiSprites.class.getResourceAsStream("/" + string2);){
            if (inputStream == null) {
                throw new IllegalStateException("Missing UI sprite manifest " + string2);
            }
            properties.load(inputStream);
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Cannot read UI sprite manifest", iOException);
        }
        for (String string3 : properties.stringPropertyNames()) {
            ArrayList<Tile> arrayList = new ArrayList<Tile>();
            for (String string4 : properties.getProperty(string3).split(";")) {
                String[] stringArray = string4.split(",");
                arrayList.add(new Tile(new String(Character.toChars(Integer.parseInt(stringArray[0], 16))), Integer.parseInt(stringArray[1]), Integer.parseInt(stringArray[2]), Integer.parseInt(stringArray[3])));
            }
            this.sprites.put(Integer.parseInt(string3), List.copyOf(arrayList));
        }
    }

    public Component at(int n, int n2, int n3, IntFunction<TextColor> intFunction) {
        TextComponent.Builder builder = Component.text();
        for (Tile tile : Objects.requireNonNull(this.sprites.get(n), "Unknown UI sprite")) {
            int n4 = n2 + tile.x();
            ((TextComponent.Builder)((TextComponent.Builder)builder.append(SpaceUtil.space(n4))).append(TextCompat.shadow((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)tile.glyph()), this.font)).color(intFunction.apply(n3 + tile.y())), 0).decoration(TextDecoration.ITALIC, false))).append(SpaceUtil.space(-n4 - tile.advance()));
        }
        return builder.asComponent();
    }

    private record Tile(String glyph, int x, int y, int advance) {
    }
}

