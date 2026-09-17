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
package sk.enorials.hud.betonquest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sk.enorials.hud.betonquest.DialoguePages;
import sk.enorials.hud.betonquest.DialogueWindow;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.Numbers;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.util.SharedUiSprites;
import sk.enorials.hud.util.SpaceUtil;

public final class QuestLogWindow {
    private static final SharedUiSprites SPRITES = new SharedUiSprites("quest_log", "quest-log-sprites.properties");
    private final DialogueWindow metrics;
    private final DialogueWindow titleMetrics;
    private final Map<Integer, Component> backgrounds = new LinkedHashMap<Integer, Component>(32, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Component> entry) {
            return this.size() > 64;
        }
    };
    private int layout;
    private int panelHeight;
    private int scaleIndex;

    public QuestLogWindow(InputStream inputStream) throws IOException {
        this.metrics = new DialogueWindow(inputStream);
        this.titleMetrics = new DialogueWindow(QuestLogWindow.class.getClassLoader().getResourceAsStream("quest-log-title-widths.properties"));
    }

    private int titleWidth(String string) {
        return this.titleMetrics.width(string);
    }

    private Component at(String string, String string2, int n, int n2, int n3, int n4) {
        int n5 = this.layout << 21 | this.panelHeight / 4 << 14 | n2 << 5 | this.scaleIndex << 3 | n3;
        return SpaceUtil.space(n).append(TextCompat.shadow((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)string), Key.key((String)("enorials:quest_log/" + string2)))).color(TextColor.color((int)n5)), 0).decoration(TextDecoration.ITALIC, false)).append(SpaceUtil.space(-n - n4));
    }

    private Component sprite(int n2, int n3, int n4, int n5) {
        return SPRITES.at(n2, n3, n4, n -> TextColor.color((int)(this.layout << 21 | this.panelHeight / 4 << 14 | n << 5 | this.scaleIndex << 3)));
    }

    private Component text(String string, int n, int n2, int n3, boolean bl) {
        String string2 = bl ? "title" : "text";
        int n4 = bl ? this.titleWidth(string) : this.metrics.width(string);
        return this.at(string, string2, n + 1, n2 + 1, 1, n4).append(this.at(string, string2, n, n2, n3, n4));
    }

    private List<String> lines(String string, boolean bl) {
        ToIntFunction<String> toIntFunction = bl ? this::titleWidth : this.metrics::width;
        int n = bl ? 156 : 148;
        List<String> list = DialoguePages.wrap(Objects.requireNonNullElse(string, ""), n, toIntFunction);
        if (list.size() <= 2) {
            return list;
        }
        ArrayList<String> arrayList = new ArrayList<String>(list.subList(0, 2));
        String string2 = arrayList.get(arrayList.size() - 1);
        while (!string2.isEmpty() && toIntFunction.applyAsInt(string2 + "...") > n) {
            string2 = string2.substring(0, string2.offsetByCodePoints(string2.length(), -1));
        }
        arrayList.set(1, string2 + "...");
        return arrayList;
    }

    private Component frame() {
        return this.backgrounds.computeIfAbsent(this.panelHeight | this.layout << 10 | this.scaleIndex << 13, n -> {
            TextComponent.Builder builder = (TextComponent.Builder)Component.text().append(this.sprite(0, -184, 0, 184));
            for (int i = 20; i < this.panelHeight - 8; i += 4) {
                builder.append(this.sprite(1, -184, i, 184));
            }
            return ((TextComponent.Builder)builder.append(this.sprite(2, -184, this.panelHeight - 8, 184))).asComponent();
        });
    }

    public Component render(List<Entry> list) {
        return this.render(list, 0);
    }

    public Component render(List<Entry> list, int n) {
        return this.render(list, n, 100);
    }

    public Component render(List<Entry> list, int n, int n2) {
        int n3;
        this.scaleIndex = switch (n2) {
            case 75 -> 1;
            case 125 -> 2;
            case 50 -> 3;
            default -> 0;
        };
        this.layout = Numbers.clamp(n, 0, 6);
        ArrayList<Row> arrayList = new ArrayList<Row>();
        int n4 = 0;
        for (Entry entry : list.subList(0, Math.min(5, list.size()))) {
            List<String> list2 = this.lines(entry.title(), true);
            List<String> object = this.lines(entry.objective(), false);
            n3 = 28 + (list2.size() - 1) * 10 + (object.size() - 1) * 9;
            arrayList.add(new Row(entry, list2, object, n3));
            n4 += n3;
        }
        this.panelHeight = 28 + (Math.max(16, n4 += Math.max(0, arrayList.size() - 1) * 3) + 4 + 3) / 4 * 4;
        TextComponent.Builder builder = (TextComponent.Builder)Component.text().append(this.frame());
        if (arrayList.isEmpty()) {
            return ((TextComponent.Builder)builder.append(this.text("No active quests", -163, 25, 3, false))).asComponent();
        }
        int n5 = 22;
        for (Row row : arrayList) {
            if (row.entry().tracked()) {
                for (n3 = 0; n3 < row.height(); ++n3) {
                    builder.append(this.sprite(3, -177, n5 + n3, 170));
                }
                builder.append(this.sprite(5, -174, n5 + 3, 7));
            }
            n3 = n5 + 3;
            for (String string : row.title()) {
                builder.append(this.text(string, -163, n3, row.entry().tracked() ? 2 : 4, true));
                n3 += 10;
            }
            builder.append(this.sprite(4, -163, n3 + 1, 74));
            builder.append(this.sprite(6, -163, (n3 += 5) + 1, 5));
            for (String string : row.objective()) {
                builder.append(this.text(string, -155, n3, 3, false));
                n3 += 9;
            }
            n5 += row.height() + 3;
        }
        return builder.asComponent();
    }

    public record Entry(String key, String title, String objective, long accepted, boolean tracked) {
    }

    private record Row(Entry entry, List<String> title, List<String> objective, int height) {
    }
}

