/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sk.enorials.gps.input;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.input.TargetInputType;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.util.Color;
import sk.enorials.hud.access.HudAccess;

public final class ChatInputService {
    private final EnorialsGpsPlugin plugin;
    private final NavigationManager navigationManager;
    private final IntegrationManager integrationManager;
    private final Map<UUID, TargetInputType> pending = new ConcurrentHashMap<UUID, TargetInputType>();
    private final Map<UUID, PendingCustomInput> pendingCustom = new ConcurrentHashMap<UUID, PendingCustomInput>();

    public ChatInputService(EnorialsGpsPlugin enorialsGpsPlugin, NavigationManager navigationManager, IntegrationManager integrationManager) {
        this.plugin = enorialsGpsPlugin;
        this.navigationManager = navigationManager;
        this.integrationManager = integrationManager;
    }

    public void request(Player player, TargetInputType targetInputType) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.use")) {
            return;
        }
        String string = this.permissionFor(targetInputType);
        if (string != null && !HudAccess.has((CommandSender)player, string)) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &f" + string);
            return;
        }
        this.pendingCustom.remove(player.getUniqueId());
        this.pending.put(player.getUniqueId(), targetInputType);
        String string2 = switch (targetInputType) {
            default -> throw new IncompatibleClassChangeError();
            case TargetInputType.PLAYER -> "prompts.player";
            case TargetInputType.MOB -> "prompts.mob";
            case TargetInputType.LOCATION -> "prompts.location";
            case TargetInputType.NPC -> "prompts.npc";
            case TargetInputType.BLOCK -> "prompts.block";
        };
        this.plugin.msg().send((CommandSender)player, this.plugin.config().getString(string2, "&eType input in chat. Type cancel."));
        this.sendQuickSuggestions(player, targetInputType);
    }

    public void requestCustom(Player player, String string, List<String> list, Consumer<String> consumer) {
        this.pending.remove(player.getUniqueId());
        this.pendingCustom.put(player.getUniqueId(), new PendingCustomInput(string, list, consumer));
        this.plugin.msg().send((CommandSender)player, string);
        this.sendCustomSuggestions(player, list);
    }

    public boolean hasPending(UUID uUID) {
        return this.pending.containsKey(uUID) || this.pendingCustom.containsKey(uUID);
    }

    public void handleChat(Player player, String string) {
        PendingCustomInput pendingCustomInput = this.pendingCustom.remove(player.getUniqueId());
        if (pendingCustomInput != null) {
            if (string.equalsIgnoreCase("cancel")) {
                this.plugin.msg().send((CommandSender)player, "&cNavigation input cancelled.");
                this.plugin.gui().openTarget(player);
                return;
            }
            pendingCustomInput.consumer().accept(string);
            return;
        }
        TargetInputType targetInputType = this.pending.remove(player.getUniqueId());
        if (targetInputType == null) {
            return;
        }
        if (string.equalsIgnoreCase("cancel")) {
            this.plugin.msg().send((CommandSender)player, "&cNavigation input cancelled.");
            this.plugin.gui().openTarget(player);
            return;
        }
        switch (targetInputType) {
            case PLAYER: {
                this.handlePlayer(player, string);
                break;
            }
            case MOB: {
                this.handleMob(player, string);
                break;
            }
            case LOCATION: {
                this.handleLocation(player, string);
                break;
            }
            case NPC: {
                this.handleNpc(player, string);
                break;
            }
            case BLOCK: {
                this.handleBlock(player, string);
            }
        }
    }

    private void handlePlayer(Player player, String string) {
        if (!HudAccess.has((CommandSender)player, "enorials.gps.track.player")) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.player");
            return;
        }
        if (!this.integrationManager.isPlayerTargetsEnabled()) {
            this.plugin.msg().send((CommandSender)player, "&cPlayer targets are disabled.");
            return;
        }
        Player player2 = Bukkit.getPlayerExact((String)string);
        if (player2 == null || !player.canSee(player2)) {
            this.plugin.msg().send((CommandSender)player, "&cPlayer not found: &f" + string);
            return;
        }
        this.navigationManager.startNavigation(player, new PlayerTarget(player2.getUniqueId(), player2.getName()), this.navigationManager.getCurrentOrDefaultMode(player), "chat-player", RouteDescriptor.player(player2.getUniqueId().toString()));
    }

    private void handleMob(Player player, String string) {
        if (!HudAccess.has((CommandSender)player, "enorials.gps.track.mob")) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.mob");
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolveMobInput(string);
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&cCould not resolve mob input: &f" + string);
            return;
        }
        this.navigationManager.startNavigation(player, optional.get(), this.navigationManager.getCurrentOrDefaultMode(player), "chat-mob", RouteDescriptor.mob(string));
    }

    private void handleNpc(Player player, String string) {
        if (!HudAccess.has((CommandSender)player, "enorials.gps.track.npc")) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.npc");
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolveNpcInput(string);
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&cCould not resolve NPC input: &f" + string);
            return;
        }
        this.navigationManager.startNavigation(player, optional.get(), this.navigationManager.getCurrentOrDefaultMode(player), "chat-npc", RouteDescriptor.npc(string));
    }

    private void handleLocation(Player player, String string) {
        if (!this.integrationManager.isLocationTargetsEnabled()) {
            this.plugin.msg().send((CommandSender)player, "&cLocation targets are disabled.");
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolveLocationInput(player, string);
        if (optional.isPresent()) {
            Location location = optional.get().resolve(player);
            if (location == null) {
                this.plugin.msg().send((CommandSender)player, "&cCould not resolve location input: &f" + string);
                return;
            }
            this.navigationManager.startNavigation(player, optional.get(), this.navigationManager.getCurrentOrDefaultMode(player), "chat-loc", RouteDescriptor.location(location));
            return;
        }
        String[] stringArray = string.trim().split("\\s+");
        if (stringArray.length < 3) {
            this.plugin.msg().send((CommandSender)player, "&cInvalid location format. Use: X Y Z [world], warp:spawn, cmi:spawn, wg:region, town:MyTown, or dh:spawn_info.");
            return;
        }
        try {
            double d = Double.parseDouble(stringArray[0]);
            double d2 = Double.parseDouble(stringArray[1]);
            double d3 = Double.parseDouble(stringArray[2]);
            if (!Double.isFinite(d) || !Double.isFinite(d2) || !Double.isFinite(d3)) {
                this.plugin.msg().send((CommandSender)player, "&cInvalid coordinates.");
                return;
            }
            String string2 = stringArray.length >= 4 ? stringArray[3] : player.getWorld().getName();
            String string3 = string2;
            if (Bukkit.getWorld((String)string2) == null) {
                this.plugin.msg().send((CommandSender)player, "&cUnknown world: &f" + string2);
                return;
            }
            Location location = new Location(Bukkit.getWorld((String)string2), d, d2, d3);
            StaticLocationTarget staticLocationTarget = new StaticLocationTarget("Location " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), location);
            this.navigationManager.startNavigation(player, staticLocationTarget, this.navigationManager.getCurrentOrDefaultMode(player), "chat-loc", RouteDescriptor.location(location));
        }
        catch (NumberFormatException numberFormatException) {
            this.plugin.msg().send((CommandSender)player, "&cInvalid coordinates.");
        }
    }

    private void handleBlock(Player player, String string) {
        if (!HudAccess.has((CommandSender)player, "enorials.gps.track.block")) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.block");
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolveBlockInput(player, string);
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&cThat block was not found near the player: &f" + string);
            return;
        }
        Location location = optional.get().resolve(player);
        if (location == null) {
            this.plugin.msg().send((CommandSender)player, "&cBlock target is unavailable right now.");
            return;
        }
        this.navigationManager.startNavigation(player, optional.get(), this.navigationManager.getCurrentOrDefaultMode(player), "chat-block", RouteDescriptor.location(location));
    }

    private void sendQuickSuggestions(Player player, TargetInputType targetInputType) {
        String string2;
        List list;
        if (!this.plugin.config().getBoolean("chat_suggestions.enabled", true)) {
            return;
        }
        if (targetInputType == TargetInputType.PLAYER) {
            list = Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(string -> !string.equalsIgnoreCase(player.getName())).limit(this.plugin.config().getInt("chat_suggestions.max_player_suggestions", 6)).toList();
            if (list.isEmpty()) {
                list = this.plugin.config().getStringList("chat_suggestions.player_fallback");
            }
        } else {
            string2 = switch (targetInputType) {
                case TargetInputType.MOB -> "chat_suggestions.mob";
                case TargetInputType.LOCATION -> "chat_suggestions.location";
                case TargetInputType.NPC -> "chat_suggestions.npc";
                case TargetInputType.BLOCK -> "chat_suggestions.block";
                default -> "chat_suggestions.player_fallback";
            };
            list = this.plugin.config().getStringList(string2);
        }
        if (list.isEmpty()) {
            return;
        }
        string2 = Color.component("&6[GPS] &eQuick input examples: ");
        player.sendMessage((Component)string2);
        for (String string3 : list) {
            Component component = Color.component(" &7- ").append(Color.component("&b" + string3).clickEvent(ClickEvent.suggestCommand((String)string3)).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Click to insert into chat"))));
            player.sendMessage(component);
        }
        Component component = Color.component(" &7- ").append(Color.component("&c cancel").clickEvent(ClickEvent.suggestCommand((String)"cancel")).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Cancel pending GPS input"))));
        player.sendMessage(component);
    }

    private void sendCustomSuggestions(Player player, List<String> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Component component = Color.component("&6[GPS] &eQuick input examples: ");
        player.sendMessage(component);
        for (String string : list) {
            Component component2 = Color.component(" &7- ").append(Color.component("&b" + string).clickEvent(ClickEvent.suggestCommand((String)string)).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Click to insert into chat"))));
            player.sendMessage(component2);
        }
    }

    private String permissionFor(TargetInputType targetInputType) {
        return switch (targetInputType) {
            default -> throw new IncompatibleClassChangeError();
            case TargetInputType.PLAYER -> "enorials.gps.track.player";
            case TargetInputType.MOB -> "enorials.gps.track.mob";
            case TargetInputType.NPC -> "enorials.gps.track.npc";
            case TargetInputType.BLOCK -> "enorials.gps.track.block";
            case TargetInputType.LOCATION -> "enorials.gps.coordinates";
        };
    }

    private record PendingCustomInput(String prompt, List<String> suggestions, Consumer<String> consumer) {
    }
}

