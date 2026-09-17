/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.theme;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.theme.HudTheme;

public final class ThemeForeground {
    private ThemeForeground() {
    }

    public static Component render(EnorialsHUD enorialsHUD, Player player, PackRegistry packRegistry, String string, String string2, String string3, String string4) {
        if (enorialsHUD.storage().theme(player.getUniqueId()) == HudTheme.ORIGINAL) {
            return Component.empty();
        }
        PackRegistry.GlyphInfo glyphInfo = packRegistry.getTexture("layout", string, string2, string3 + "-foreground", string4.replace(".png", "_foreground.png"));
        return glyphInfo == null ? Component.empty() : packRegistry.metrics().neutral(glyphInfo.component(packRegistry));
    }
}

