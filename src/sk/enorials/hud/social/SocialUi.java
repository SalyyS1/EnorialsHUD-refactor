/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.social;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.social.SocialArtwork;
import sk.enorials.hud.util.HudFontMetrics;
import sk.enorials.hud.util.SpaceUtil;

public final class SocialUi {
    private final EnorialsHUD hud;

    public SocialUi(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
    }

    public Component background(Player player, boolean bl) {
        String string = (bl ? "trade_" : "invite_") + this.hud.storage().theme(player.getUniqueId()).id();
        Component component = Component.empty().color((TextColor)NamedTextColor.WHITE);
        for (int i = 0; i < SocialArtwork.rows(bl); ++i) {
            for (int j = 0; j < 4; ++j) {
                component = component.append(this.at(LegacyText.font((Component)Component.text((char)((char)(61296 + i * 4 + j))), Key.key((String)("enorials:social/" + string))), -38 + j * 63));
            }
        }
        return component;
    }

    private Component at(Component component, int n) {
        return SpaceUtil.space(n - 8).append(this.hud.packs().registry().metrics().neutral(component)).append(SpaceUtil.space(8 - n));
    }

    public Component text(String string, int n, int n2, int n3, int n4) {
        return this.text(string, n, n3, n4, Key.key((String)("enorials:social/text_" + n2)));
    }

    public Component status(Player player, String string, int n, int n2, boolean bl) {
        return this.lockText(player, string, n, 70, n2, bl, 27, 11, Key.key((String)"enorials:social/status"));
    }

    public Component action(Player player, String string, int n, boolean bl) {
        return this.lockText(player, string, 43, 66, n, bl, 111, 15, Key.key((String)"enorials:social/text_113"));
    }

    private Component lockText(Player player, String string, int n, int n2, int n3, boolean bl, int n4, int n5, Key key) {
        Component component = this.fitted(string, n2 - n5, n3, key);
        int n6 = Math.round((float)n - this.hud.packs().registry().metrics().measure(component).center() - (float)n5 / 2.0f);
        Component component2 = ((TextComponent)LegacyText.font((Component)Component.text((char)(bl ? (char)'\uefb1' : '\uefb2')), Key.key((String)("enorials:social/locks_" + this.hud.storage().theme(player.getUniqueId()).id() + "_" + n4)))).color((TextColor)NamedTextColor.WHITE);
        return this.at(component2, n6).append(this.at(component, n6 + n5));
    }

    private Component text(String string, int n, int n2, int n3, Key key) {
        Component component = this.fitted(string, n2, n3, key);
        return this.at(component, n - Math.round(this.hud.packs().registry().metrics().measure(component).center()));
    }

    private Component fitted(String string, int n, int n2, Key key) {
        Object object;
        HudFontMetrics hudFontMetrics = this.hud.packs().registry().metrics();
        if (hudFontMetrics.measure(LegacyText.font((Component)Component.text((String)(object = string.replaceAll("[^\\x20-\\x7e]", "?"))), key)).advance() > (float)n) {
            while (!((String)object).isEmpty() && hudFontMetrics.measure(LegacyText.font((Component)Component.text((String)((String)object + "..")), key)).advance() > (float)n) {
                object = ((String)object).substring(0, ((String)object).length() - 1);
            }
            object = (String)object + "..";
        }
        return ((TextComponent)LegacyText.font((Component)Component.text((String)object), key)).color(TextColor.color((int)n2));
    }

    public Component invite(Player player, String string, String string2, String string3) {
        return TextCompat.shadow(this.background(player, false).append(this.heading(player, string.equals("PARTY INVITATION") ? "party" : "request")).append(this.playerName(player, string2, 44)).append(this.text(string3, 88, 61, 155, 12831954)).append(this.text("ACCEPT", 43, 91, 60, 12972716)).append(this.text("DECLINE", 133, 91, 60, 0xFFB4B4)), 0);
    }

    public Component money(String string, int n) {
        Component component = ((TextComponent)LegacyText.font((Component)Component.text((String)string), Key.key((String)"enorials:social/money"))).color(TextColor.color((int)16767881));
        HudFontMetrics.Bounds bounds = this.hud.packs().registry().metrics().measure(component);
        if (bounds.advance() > 68.0f) {
            throw new IllegalArgumentException("Money amount cannot fit in the trade window.");
        }
        return this.at(component, n - Math.round(bounds.center()));
    }

    public Component trade(Player player, String string) {
        return TextCompat.shadow(this.background(player, true).append(this.heading(player, "trade")).append(this.playerName(player, string, 17)), 0);
    }

    private Component playerName(Player player, String string, int n) {
        Component component = ((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)string.replaceAll("[^\\x20-\\x7e]", "?")), Key.key((String)("enorials:social/name_" + n)))).decorate(TextDecoration.BOLD)).color(TextColor.color((int)SocialArtwork.accent(this.hud.storage().theme(player.getUniqueId()))));
        return this.at(Component.empty().append(component), 88 - Math.round(this.hud.packs().registry().metrics().measure(component).center()));
    }

    private Component heading(Player player, String string) {
        Component component = Component.empty().color((TextColor)NamedTextColor.WHITE);
        Key key = Key.key((String)("enorials:social/heading_" + this.hud.storage().theme(player.getUniqueId()).id() + "_" + string));
        for (int i = 0; i < 4; ++i) {
            component = component.append(this.at(LegacyText.font((Component)Component.text((char)((char)(61376 + i))), key), 8 + 40 * i));
        }
        return component;
    }

    public Component stateButton(Player player, String string) {
        return this.at(LegacyText.font((Component)Component.text((char)'\uefb0'), Key.key((String)("enorials:social/state_" + this.hud.storage().theme(player.getUniqueId()).id() + "_" + string))), 5).color((TextColor)NamedTextColor.WHITE);
    }
}

