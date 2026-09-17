/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.RegisteredServiceProvider
 */
package sk.enorials.hud.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.api.HudNpcSocialProvider;

final class NpcSocialMenus {
    NpcSocialMenus() {
    }

    static boolean valid(EnorialsHUD enorialsHUD, Player player, Target target) {
        Player player3 = target.npc();
        return target.registration().getPlugin().isEnabled() && player.isOnline() && player3.isValid() && enorialsHUD.api().resolveMember(player3.getUniqueId()) == player3 && enorialsHUD.api().member(player3.getUniqueId()).npc() && player.getWorld() == player3.getWorld() && player.canSee(player3) && player.getLocation().distanceSquared(player3.getLocation()) <= 100.0 && ((HudNpcSocialProvider)target.registration().getProvider()).targets(player).stream().anyMatch(player2 -> player2 == player3);
    }

    static List<Target> targets(EnorialsHUD enorialsHUD, Player player) {
        ArrayList<Target> arrayList = new ArrayList<Target>();
        HashSet<UUID> hashSet = new HashSet<UUID>();
        for (RegisteredServiceProvider registeredServiceProvider : Bukkit.getServicesManager().getRegistrations(HudNpcSocialProvider.class)) {
            if (!registeredServiceProvider.getPlugin().isEnabled()) continue;
            for (Player player2 : ((HudNpcSocialProvider)registeredServiceProvider.getProvider()).targets(player)) {
                Target target;
                if (player2 == null || !NpcSocialMenus.valid(enorialsHUD, player, target = new Target((RegisteredServiceProvider<HudNpcSocialProvider>)registeredServiceProvider, player2)) || !hashSet.add(player2.getUniqueId())) continue;
                arrayList.add(target);
            }
        }
        return arrayList;
    }

    static void request(EnorialsHUD enorialsHUD, Player player, Target target, boolean bl) {
        if (!NpcSocialMenus.valid(enorialsHUD, player, target)) {
            player.sendMessage("That NPC is no longer nearby. Open the list again.");
            return;
        }
        player.closeInventory();
        if (bl) {
            ((HudNpcSocialProvider)target.registration().getProvider()).requestTrade(player, target.npc().getUniqueId());
        } else {
            ((HudNpcSocialProvider)target.registration().getProvider()).requestFriend(player, target.npc().getUniqueId());
        }
    }

    record Target(RegisteredServiceProvider<HudNpcSocialProvider> registration, Player npc) {
    }
}

