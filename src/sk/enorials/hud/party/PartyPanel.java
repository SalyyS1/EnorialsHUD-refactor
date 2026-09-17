/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.party;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.api.HudTypes;
import sk.enorials.hud.api.HudValue;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.manager.ListenerManager;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.party.PartyMapTracker;
import sk.enorials.hud.theme.ThemeForeground;
import sk.enorials.hud.util.SpaceUtil;
import sk.enorials.partyhud.party.Party;

public final class PartyPanel {
    private final EnorialsHUD plugin;

    public PartyPanel(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
    }

    public Component render(Player player, PackRegistry packRegistry) {
        if (player.hasMetadata("enorials.conversation")) {
            return Component.empty();
        }
        if (!HudAccess.has((CommandSender)player, "enorials.party.hud") || !this.plugin.storage().preference(player.getUniqueId(), "party", true)) {
            return Component.empty();
        }
        if (this.plugin.party() == null || !this.plugin.party().getConfig().getBoolean("hud.enabled", true)) {
            return Component.empty();
        }
        Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        if (party == null) {
            return Component.empty();
        }
        List<UUID> list = PartyMapTracker.members(party);
        TextComponent textComponent = Component.empty();
        int n = 0;
        for (UUID uUID : list) {
            HudValue hudValue;
            Player player2 = this.plugin.api().resolveMember(uUID);
            if (n >= 8) break;
            String string = "party-row-" + this.plugin.storage().partySize(player.getUniqueId()) + "-" + n++;
            if (player2 == null) {
                textComponent = textComponent.append(this.portrait(packRegistry, string, uUID)).append(this.texture(packRegistry, string, "frame-compact", null, 1.0)).append(this.foreground(player, packRegistry, string, "frame-compact")).append(this.text(packRegistry, string, "name", Objects.toString(Bukkit.getOfflinePlayer((UUID)uUID).getName(), "Player") + " (offline)", 10070200));
                continue;
            }
            HudTypes.Member member = this.plugin.api().member(uUID);
            HudValue hudValue2 = hudValue = member.npc() ? this.plugin.api().read(player2, "mana") : null;
            ListenerManager.ResourceState resourceState = member.npc() ? new ListenerManager.ResourceState(hudValue != null, "mana", hudValue == null ? 0.0 : hudValue.current(), hudValue == null ? 1.0 : hudValue.maximum()) : this.plugin.listeners().getResourceState(player2);
            String string2 = resourceState.available() ? "frame" : "frame-compact";
            double d = this.plugin.listeners().getCurrent(player2, "health");
            double d2 = this.plugin.listeners().getMax(player2, "health");
            textComponent = textComponent.append(this.portrait(packRegistry, string, uUID)).append(this.texture(packRegistry, string, string2, null, 1.0)).append(this.texture(packRegistry, string, "health", null, d2 <= 0.0 ? 0.0 : d / d2));
            if (resourceState.available()) {
                textComponent = textComponent.append(this.texture(packRegistry, string, "mana", null, resourceState.current() / resourceState.max()));
            }
            textComponent = textComponent.append(this.foreground(player, packRegistry, string, string2)).append(this.text(packRegistry, string, "name", player2.getName(), uUID.equals(party.getLeader()) ? 15914380 : 15199730)).append(this.text(packRegistry, string, "health-number", PartyPanel.number(d) + "/" + PartyPanel.number(d2), 0xFFFFFF));
            if (!resourceState.available()) continue;
            textComponent = textComponent.append(this.text(packRegistry, string, "mana-number", PartyPanel.number(resourceState.current()) + "/" + PartyPanel.number(resourceState.max()), 0xFFFFFF));
        }
        return TextCompat.shadow(packRegistry.metrics().neutral((Component)textComponent), 0);
    }

    private Component foreground(Player player, PackRegistry packRegistry, String string, String string2) {
        return ThemeForeground.render(this.plugin, player, packRegistry, "party-panel", string, string2, "assets/enorialshud/party_" + string.split("-")[2] + "_" + string2.replace('-', '_') + ".png");
    }

    private Component portrait(PackRegistry packRegistry, String string, UUID uUID) {
        return this.text(packRegistry, string, "head", "\ue100", 0xFFFFFF).insertion("enorials:avatar:" + String.valueOf(uUID));
    }

    private Component texture(PackRegistry packRegistry, String string, String string2, String object, double d) {
        PackRegistry.GlyphInfo glyphInfo;
        object = "assets/enorialshud/party_" + string.split("-")[2] + "_" + string2.replace('-', '_') + ".png";
        PackRegistry.TextureSteps textureSteps = packRegistry.getTextureSteps("layout", "party-panel", string, string2, (String)object);
        if (textureSteps != null) {
            int n = (int)Math.ceil(Math.max(0.0, Math.min(1.0, d)) * (double)textureSteps.steps().size()) - 1;
            if (n < 0) {
                return Component.empty();
            }
            glyphInfo = textureSteps.steps().get(n);
        } else {
            glyphInfo = packRegistry.getTexture("layout", "party-panel", string, string2, (String)object);
        }
        if (glyphInfo == null) {
            return Component.empty();
        }
        return packRegistry.metrics().neutral(glyphInfo.component(packRegistry));
    }

    private Component text(PackRegistry packRegistry, String string, String string2, String string3, int n) {
        String string4 = packRegistry.getTextFont("layout", "party-panel", string, string2);
        if (string4 == null) {
            return Component.empty();
        }
        Component component = ((TextComponent)LegacyText.font((Component)Component.text((String)string3), Key.key((String)string4))).color(TextColor.color((int)n));
        if (string2.equals("name")) {
            component = TextCompat.shadow(component, -586675440);
        }
        if (string2.endsWith("number")) {
            component = SpaceUtil.space(-Math.round(packRegistry.metrics().measure(component).center())).append(component);
        }
        return packRegistry.metrics().neutral(component);
    }

    private static String number(double d) {
        return Long.toString(Math.round(Math.max(0.0, d)));
    }
}

