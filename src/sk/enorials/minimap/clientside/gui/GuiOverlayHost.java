/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.bossbar.BossBar
 *  net.kyori.adventure.bossbar.BossBar$Color
 *  net.kyori.adventure.bossbar.BossBar$Overlay
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;

final class GuiOverlayHost {
    private final Map<UUID, BossBar> standalone = new HashMap<UUID, BossBar>();

    GuiOverlayHost() {
    }

    boolean set(Player player, Component component) {
        if (Bukkit.getPluginManager().isPluginEnabled("EnorialsHUD")) {
            BossBar bossBar = this.standalone.remove(player.getUniqueId());
            if (bossBar != null) {
                player.hideBossBar(bossBar);
            }
            return Anchor.set(player, component);
        }
        BossBar bossBar = this.standalone.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = BossBar.bossBar((Component)component, (float)0.0f, (BossBar.Color)BossBar.Color.YELLOW, (BossBar.Overlay)BossBar.Overlay.PROGRESS);
            this.standalone.put(player.getUniqueId(), bossBar);
            player.showBossBar(bossBar);
        } else {
            bossBar.name(component);
        }
        return true;
    }

    void clear(Player player) {
        BossBar bossBar;
        if (Bukkit.getPluginManager().isPluginEnabled("EnorialsHUD")) {
            Anchor.clear(player);
        }
        if ((bossBar = this.standalone.remove(player.getUniqueId())) != null) {
            player.hideBossBar(bossBar);
        }
    }

    boolean needsRefresh(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("EnorialsHUD")) {
            return this.standalone.containsKey(player.getUniqueId()) || !Anchor.present(player);
        }
        return !this.standalone.containsKey(player.getUniqueId());
    }

    private static final class Anchor {
        private Anchor() {
        }

        static boolean set(Player player, Component component) {
            return EnorialsHUD.getInstance().renderer().setMinimapOverlay(player, component);
        }

        static void clear(Player player) {
            EnorialsHUD.getInstance().renderer().clearMinimapOverlay(player);
        }

        static boolean present(Player player) {
            return EnorialsHUD.getInstance().renderer().hasMinimapOverlay(player);
        }
    }
}

