/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.ServicePriority
 */
package sk.enorials.hud.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import sk.enorials.hud.api.HudStatProvider;
import sk.enorials.hud.api.HudTypes;
import sk.enorials.hud.api.HudValue;

public interface EnorialsHudApi {
    public String apiVersion();

    public Set<String> capabilities();

    public Map<String, HudTypes.Setting> settings();

    public Set<String> menus();

    public boolean hasPermission(Player var1, String var2);

    public HudTypes.Status status(Player var1);

    public Set<String> markerIcons();

    public void putMarker(Plugin var1, Player var2, String var3, Location var4, String var5, String var6);

    public boolean removeMarker(Plugin var1, Player var2, String var3);

    public int clearMarkers(Plugin var1, Player var2);

    public boolean hasMarker(Plugin var1, Player var2, String var3);

    public String getSetting(Player var1, String var2);

    public void setSetting(Player var1, String var2, String var3);

    public void openMenu(Player var1, String var2);

    public void putMarker(Plugin var1, Player var2, HudTypes.Marker var3);

    public List<HudTypes.Marker> markers(Plugin var1, Player var2);

    public HudTypes.Zoom zoom(Player var1);

    public void setZoom(Player var1, HudTypes.Zoom var2);

    public boolean radar(Player var1, HudTypes.Radar var2);

    public void setRadar(Player var1, HudTypes.Radar var2, boolean var3);

    public Optional<HudTypes.Navigation> navigation(Player var1);

    public boolean go(Plugin var1, Player var2, String var3, Location var4, HudTypes.NavigationMode var5);

    public boolean go(Plugin var1, Player var2, HudTypes.TargetType var3, String var4, HudTypes.NavigationMode var5);

    public boolean stop(Plugin var1, Player var2);

    public boolean stop(Player var1);

    public List<HudTypes.QuestRoute> questRoutes(Player var1);

    public String routeId(Plugin var1, String var2);

    public String putQuestRoute(Plugin var1, Player var2, String var3, String var4, Location var5, HudTypes.NavigationMode var6);

    public String putQuestRoute(Plugin var1, Player var2, String var3, String var4, Location var5, HudTypes.NavigationMode var6, boolean var7);

    public boolean removeQuestRoute(Plugin var1, Player var2, String var3);

    public boolean track(Player var1, String var2);

    public Optional<HudTypes.Party> party(UUID var1);

    public List<HudTypes.Party> openParties();

    public HudTypes.Member member(UUID var1);

    public HudTypes.Party createParty(Player var1, boolean var2);

    public boolean joinOpenParty(Player var1, UUID var2);

    public boolean leaveParty(Player var1);

    public boolean kick(Player var1, UUID var2);

    public boolean promote(Player var1, UUID var2);

    public void openParty(Player var1, boolean var2);

    public void disbandParty(Player var1);

    public HudTypes.Registration partyLimit(Plugin var1, Player var2, int var3);

    public HudTypes.Registration registerNpc(Plugin var1, Player var2);

    public boolean joinNpc(Plugin var1, Player var2, UUID var3);

    public boolean leaveNpc(Plugin var1, UUID var2);

    public Optional<HudTypes.Target> currentTarget(Player var1);

    public HudValue stat(Player var1, String var2);

    public HudTypes.Registration registerStats(Plugin var1, HudStatProvider var2, ServicePriority var3);

    public boolean showPopup(Player var1, String var2, List<String> var3, long var4);

    public List<HudTypes.Quest> quests(Player var1);

    public void refreshQuests(Player var1);

    public boolean startConversation(Player var1, String var2, Location var3);

    public boolean cancelConversation(Player var1);

    public void release(Plugin var1);
}

