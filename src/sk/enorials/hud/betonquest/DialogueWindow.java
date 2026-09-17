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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sk.enorials.hud.betonquest.DialoguePages;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.util.SharedUiSprites;
import sk.enorials.hud.util.SpaceUtil;

public final class DialogueWindow {
    private static final SharedUiSprites SPRITES = new SharedUiSprites("conversation", "conversation-sprites.properties");
    private final Properties widths = new Properties();
    private final Properties titleWidths = new Properties();
    private final Map<Frame, Component> frames = new LinkedHashMap<Frame, Component>(128, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<Frame, Component> entry) {
            return this.size() > 128;
        }
    };
    private static final int[] WIDTH = new int[]{338, 338, 338, 78, 240, 240, 240, 220, 220, 220, 220, 220, 220, 16, 40, 36, 338, 338, 7};

    public DialogueWindow(InputStream inputStream) throws IOException {
        InputStream inputStream2;
        if (inputStream != null) {
            inputStream2 = inputStream;
            try {
                this.widths.load(inputStream);
            }
            finally {
                if (inputStream2 != null) {
                    inputStream2.close();
                }
            }
        }
        inputStream2 = DialogueWindow.class.getResourceAsStream("/dialogue-title-widths.properties");
        try {
            if (inputStream2 != null) {
                this.titleWidths.load(inputStream2);
            }
        }
        finally {
            if (inputStream2 != null) {
                inputStream2.close();
            }
        }
    }

    public int width(String string) {
        return DialogueWindow.measure(string, this.widths);
    }

    private int titleWidth(String string) {
        return DialogueWindow.measure(string, this.titleWidths);
    }

    private static int measure(String string, Properties properties) {
        return string.codePoints().map(n -> Integer.parseInt(properties.getProperty(Integer.toHexString(n), "6"))).sum();
    }

    private static int ceil4(int n) {
        return (n + 3) / 4 * 4;
    }

    private List<String> names(String string) {
        return DialoguePages.wrap(string.toUpperCase(Locale.ROOT), 282, this::titleWidth);
    }

    public Layout layout(String string, DialoguePages dialoguePages) {
        int n = DialogueWindow.ceil4(Math.max(24, this.names(string).size() * 12 + 12));
        int n2 = Math.max(80, DialogueWindow.ceil4(n + 14 + dialoguePages.body().size() * 12));
        List<DialoguePages.Card> list = dialoguePages.cards();
        int n3 = list.isEmpty() ? 0 : 34 + list.stream().mapToInt(card -> card.lines().size() * 12 + 10).sum() + (list.size() - 1) * 4;
        int n4 = -n2;
        int n5 = n4 - 8 - n3;
        int n6 = DialogueWindow.ceil4(-Math.min(n4 - 4, list.isEmpty() ? n4 - 4 : n5) + 24);
        return new Layout(n4, n, n2, n5, n3, n6);
    }

    private Component at(String string, String string2, int n, int n2, int n3, int n4, int n5) {
        int n6 = n5 / 4 << 16 | (n2 + 512 & 0x3FF) << 4 | n3;
        return SpaceUtil.space(n).append(TextCompat.shadow((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)string), Key.key((String)("enorials:conversation/" + string2)))).color(TextColor.color((int)n6)), 0).decoration(TextDecoration.ITALIC, false)).append(SpaceUtil.space(-n - n4));
    }

    private Component text(String string, int n, int n2, int n3, int n4) {
        return this.at(string, "text", n, n2, n3, this.width(string), n4);
    }

    private Component title(String string, int n, int n2, int n3, int n4) {
        return this.at(string, "title", n + 1, n2 + 1, 4, this.titleWidth(string), n4).append(this.at(string, "title", n, n2, n3, this.titleWidth(string), n4));
    }

    private Component sprite(int n, int n3, int n4, int n5) {
        return SPRITES.at(n, n3, n4, n2 -> TextColor.color((int)(n5 / 4 << 16 | (n2 + 512 & 0x3FF) << 4)));
    }

    private Component panel(int n, int n2, int n3, int n4, int n5) {
        return this.frames.computeIfAbsent(new Frame(n, n2, n3, n4, n5), frame -> {
            int n6;
            int n7 = n == 0 ? 0 : (n == 1 ? 4 : (n6 = n == 2 ? 7 : 10));
            int n8 = n < 2 ? (n == 0 ? 24 : 26) : 5;
            int n9 = n < 2 ? 8 : 5;
            TextComponent.Builder builder = (TextComponent.Builder)Component.text().append(this.sprite(n6, n2, n3, n5));
            for (int i = n8; i < n4 - n9; i += 2) {
                builder.append(this.sprite(n6 + 1, n2, n3 + i, n5));
            }
            return ((TextComponent.Builder)builder.append(this.sprite(n6 + 2, n2, n3 + n4 - n9, n5))).asComponent();
        });
    }

    public Component render(String string, DialoguePages dialoguePages, Component component, boolean bl) {
        Object object;
        int n;
        Layout layout = this.layout(string, dialoguePages);
        int n2 = layout.extent();
        int n3 = layout.top();
        TextComponent.Builder builder = (TextComponent.Builder)Component.text().append(this.panel(0, -130, n3, layout.bodyHeight(), n2));
        if (layout.header() > 24) {
            for (n = n3 + 22; n < n3 + layout.header() - 2; n += 2) {
                builder.append(this.sprite(16, -130, n, n2));
            }
            builder.append(this.sprite(17, -130, n3 + layout.header() - 2, n2));
        }
        builder.append(this.sprite(3, -208, n3 - 4, n2));
        if (component == null) {
            builder.append(this.title("?", -173, n3 + 33, 2, n2));
        } else {
            builder.append(component);
        }
        n = n3 + 9;
        for (String object2 : this.names(string)) {
            builder.append(this.title(object2, -116, n, 2, n2));
            n += 12;
        }
        if (dialoguePages.pages() > 1) {
            object = dialoguePages.page() + 1 + " / " + dialoguePages.pages();
            builder.append(this.text((String)object, 180 - this.width((String)object), n3 + layout.header() + 1, 3, n2));
        }
        n = n3 + layout.header() + 11;
        for (String string2 : dialoguePages.body()) {
            builder.append(this.text(string2, -116, n, 1, n2));
            n += 12;
        }
        if (!dialoguePages.cards().isEmpty()) {
            builder.append(this.panel(1, -36, layout.replyTop(), layout.replyHeight(), n2));
            builder.append(this.title("YOUR REPLY", -16, layout.replyTop() + 10, 3, n2));
            object = dialoguePages.selected() + 1 + " / " + dialoguePages.answerCount();
            builder.append(this.text((String)object, 188 - this.width((String)object), layout.replyTop() + 11, 1, n2));
            n = layout.replyTop() + 26;
            for (DialoguePages.Card card : dialoguePages.cards()) {
                int n4 = card.lines().size() * 12 + 10;
                builder.append(this.panel(card.selected() ? 3 : 2, -26, n, n4, n2));
                if (card.selected()) {
                    builder.append(this.sprite(18, -20, n + (n4 - 10) / 2, n2));
                }
                for (int i = 0; i < card.lines().size(); ++i) {
                    String string3 = (String)(i == 0 ? card.index() + 1 + ". " : "   ") + card.lines().get(i);
                    builder.append(this.text(string3, -5, n + 7 + i * 12, card.selected() ? 2 : 1, n2));
                }
                n += n4 + 4;
            }
        }
        return ((TextComponent.Builder)builder.append(this.controls(dialoguePages, bl, n2))).asComponent();
    }

    private Component controls(DialoguePages dialoguePages, boolean bl, int n) {
        String string = dialoguePages.reading() ? "or Wheel: Read" : "or Wheel: Select";
        String string2 = dialoguePages.reading() || dialoguePages.answerPages() > dialoguePages.answerPage() + 1 || dialoguePages.answerCount() == 0 ? "or RMB: Continue" : "or RMB: Confirm";
        int n2 = 38 + this.width(string);
        int n3 = 46 + this.width(string2);
        int n4 = bl ? 42 + this.width(": Exit") : 0;
        int n5 = -(n2 + 16 + n3 + (bl ? 16 + n4 : 0)) / 2;
        TextComponent.Builder builder = (TextComponent.Builder)((TextComponent.Builder)Component.text().append(this.key(13, "W", n5, n))).append(this.key(13, "S", n5 + 18, n));
        builder.append(this.text(string, n5 + 38, 12, 3, n));
        builder.append(this.text("|", n5 += n2 + 7, 12, 2, n));
        ((TextComponent.Builder)builder.append(this.key(14, "Space", n5 += 9, n))).append(this.text(string2, n5 + 46, 12, 3, n));
        n5 += n3;
        if (bl) {
            builder.append(this.text("|", n5 + 7, 12, 2, n));
            ((TextComponent.Builder)builder.append(this.key(15, "Shift", n5 += 16, n))).append(this.text(": Exit", n5 + 42, 12, 3, n));
        }
        return builder.asComponent();
    }

    private Component key(int n, String string, int n2, int n3) {
        return this.sprite(n, n2, 7, n3).append(this.text(string, n2 + (WIDTH[n] - this.width(string)) / 2, 12, 1, n3));
    }

    public int portraitTop(String string, DialoguePages dialoguePages) {
        return this.layout(string, dialoguePages).top() + 3;
    }

    public record Layout(int top, int header, int bodyHeight, int replyTop, int replyHeight, int extent) {
    }

    private record Frame(int kind, int x, int y, int height, int extent) {
    }
}

