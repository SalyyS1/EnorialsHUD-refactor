/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.party;

import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.manager.ListenerManager;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.theme.ThemeForeground;
import sk.enorials.hud.util.HudStatus;
import sk.enorials.hud.util.SpaceUtil;

public final class PlayerPanel {
    private final EnorialsHUD plugin;

    public PlayerPanel(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
    }

    public Component render(Player player, PackRegistry packRegistry) {
        ListenerManager.ResourceState resourceState = this.plugin.listeners().getResourceState(player);
        boolean bl = resourceState.available();
        String string = bl ? "player" : "player-compact";
        Component component = this.text(packRegistry, string, "head", "\ue100", false).append(this.texture(packRegistry, string, "frame", "player_frame" + (bl ? "" : "_compact"), 1.0, 0xFFFFFF));
        component = component.append(this.bar(player, packRegistry, string, "health", "health", HudStatus.snapshot(player).tone().rgb()));
        if (bl) {
            component = component.append(this.bar(player, packRegistry, string, "mana", "resource", 0xFFFFFF));
        }
        component = component.append(this.bar(player, packRegistry, string, "food", "food", 0xFFFFFF));
        component = component.append(ThemeForeground.render(this.plugin, player, packRegistry, "player-panel", string, "frame", "assets/enorialshud/player_frame" + (bl ? "" : "_compact") + ".png"));
        component = component.append(this.texture(packRegistry, string, "shield", "player_shield", 1.0, 0xFFFFFF));
        component = component.append(this.text(packRegistry, string, "armor", String.format(Locale.ROOT, "%02d", Math.round(this.plugin.listeners().getCurrent(player, "armor"))), true));
        for (int i = 0; i < 4; ++i) {
            for (boolean bl2 : new boolean[]{false, true}) {
                String string2 = (bl2 ? "timer" : "effect") + i;
                String string3 = packRegistry.getTextFont("layout", "player-panel", string, string2);
                component = component.append(this.plugin.renderer().renderEffects(player, string3, bl2, i, packRegistry));
            }
        }
        return TextCompat.shadow(packRegistry.metrics().neutral(component), 0);
    }

    private Component bar(Player player, PackRegistry packRegistry, String string, String string2, String string3, int n) {
        double d = this.plugin.listeners().getCurrent(player, string3);
        double d2 = this.plugin.listeners().getMax(player, string3);
        Component component = this.texture(packRegistry, string, string2, "player_" + string2, d2 <= 0.0 ? 0.0 : d / d2, n);
        return string2.equals("food") ? component : component.append(this.text(packRegistry, string, string2 + "-number", Math.round(d) + "/" + Math.round(d2), true));
    }

    private Component texture(PackRegistry packRegistry, String string, String string2, String string3, double d, int n) {
        PackRegistry.GlyphInfo glyphInfo;
        String string4 = "assets/enorialshud/" + string3 + ".png";
        PackRegistry.TextureSteps textureSteps = packRegistry.getTextureSteps("layout", "player-panel", string, string2, string4);
        if (textureSteps != null) {
            int n2 = (int)Math.ceil(Math.max(0.0, Math.min(1.0, d)) * (double)textureSteps.steps().size()) - 1;
            if (n2 < 0) {
                return Component.empty();
            }
            glyphInfo = textureSteps.steps().get(n2);
        } else {
            glyphInfo = packRegistry.getTexture("layout", "player-panel", string, string2, string4);
        }
        return glyphInfo == null ? Component.empty() : packRegistry.metrics().neutral(glyphInfo.component(packRegistry).color(TextColor.color((int)n)));
    }

    private Component text(PackRegistry packRegistry, String string, String string2, String string3, boolean bl) {
        String string4 = packRegistry.getTextFont("layout", "player-panel", string, string2);
        if (string4 == null) {
            return Component.empty();
        }
        Component component = ((TextComponent)LegacyText.font((Component)Component.text((String)string3), Key.key((String)string4))).color(TextColor.color((int)0xFFFFFF));
        if (bl) {
            float f = string3.contains("/") ? packRegistry.metrics().firstCenter(component, 47) : packRegistry.metrics().measure(component).center();
            component = string2.equals("armor") && f - (float)((int)f) > 0.25f ? SpaceUtil.space(-((int)f)).append(LegacyText.font((Component)Component.text((char)'\ue931'), Key.key((String)string4))).append(component).append(LegacyText.font((Component)Component.text((char)'\ue921'), Key.key((String)string4))) : SpaceUtil.space(-Math.round(f)).append(component);
        }
        return packRegistry.metrics().neutral(component);
    }
}

