/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Argument
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.objective.Objective
 *  org.betonquest.betonquest.api.quest.objective.service.ObjectiveService
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.betonquest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.objective.Objective;
import org.betonquest.betonquest.api.quest.objective.service.ObjectiveService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import sk.enorials.gps.nav.NavigationSession;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.hud.betonquest.HudBetonQuest;

final class QuestMarkerObjective
implements Objective {
    private final HudBetonQuest hook;
    private final ObjectiveService service;
    private final Argument<Location> location;
    private final Argument<String> icon;
    private final Argument<String> label;
    private final Argument<Number> range;
    private final Argument<Boolean> gps;
    private final String id;
    private final Map<UUID, Target> rendered = new HashMap<UUID, Target>();
    private long lastWarning;

    QuestMarkerObjective(HudBetonQuest hudBetonQuest, Instruction instruction, ObjectiveService objectiveService) throws QuestException {
        this.hook = hudBetonQuest;
        this.service = objectiveService;
        this.location = instruction.location().get();
        this.icon = instruction.string().get();
        this.label = instruction.string().get("label", (Object)"Quest");
        this.range = instruction.number().inRange((Number)0, (Number)100000).get("range", (Object)0);
        this.gps = instruction.bool().get("gps", (Object)false);
        this.id = HudBetonQuest.markerId(objectiveService.getObjectiveID().getFull());
        objectiveService.setDefaultData(profile -> "");
        hudBetonQuest.objectives.add(this);
    }

    public ObjectiveService getService() {
        return this.service;
    }

    void tick() {
        Optional optional;
        HashSet<UUID> hashSet = new HashSet<UUID>();
        for (Profile object : List.copyOf(this.service.getData().keySet())) {
            optional = object.getOnlineProfile();
            if (optional.isEmpty()) continue;
            Player player = ((OnlineProfile)optional.get()).getPlayer();
            try {
                boolean bl;
                if (!this.service.containsProfile(object) || !this.service.checkConditions(object)) continue;
                Location location = ((Location)this.location.getValue(object)).clone();
                location.setYaw(0.0f);
                location.setPitch(0.0f);
                double d = ((Number)this.range.getValue(object)).doubleValue();
                if (!Double.isFinite(d)) {
                    throw new QuestException("Marker range must be finite");
                }
                if (d > 0.0 && HudBetonQuest.near(player, location, d)) {
                    this.remove(player);
                    this.service.complete(object);
                    continue;
                }
                boolean bl2 = (Boolean)this.gps.getValue(object);
                Target target = new Target(location, (String)this.icon.getValue(object), (String)this.label.getValue(object));
                Target target2 = this.rendered.get(player.getUniqueId());
                boolean bl3 = bl = !this.hook.hud.api().hasMarker(this.hook.owner(), player, this.id);
                if (!target.equals(target2) || bl) {
                    this.hook.hud.api().putMarker(this.hook.owner(), player, this.id, location, target.icon(), target.label());
                    this.rendered.put(player.getUniqueId(), target);
                    NavigationSession navigationSession = this.hook.hud.gps().navigation().session(player);
                    if (bl2 && (target2 == null || bl && navigationSession == null || navigationSession != null && this.id.equals(navigationSession.source()))) {
                        this.hook.hud.gps().api().startNavigation(player, RouteDescriptor.location(location), this.hook.hud.gps().api().getPlayerMode(player), this.id);
                    }
                }
                hashSet.add(player.getUniqueId());
            }
            catch (RuntimeException | QuestException throwable) {
                if (System.currentTimeMillis() - this.lastWarning <= 60000L) continue;
                this.lastWarning = System.currentTimeMillis();
                this.hook.hud.getLogger().log(Level.WARNING, "Quest marker unavailable: " + this.getObjectiveID().getFull(), throwable);
            }
        }
        for (UUID uUID : List.copyOf(this.rendered.keySet())) {
            if (hashSet.contains(uUID)) continue;
            optional = Bukkit.getPlayer((UUID)uUID);
            if (optional != null) {
                this.remove((Player)optional);
                continue;
            }
            this.rendered.remove(uUID);
        }
    }

    private void remove(Player player) {
        if (this.hook.hud.api() != null) {
            this.hook.hud.api().removeMarker(this.hook.owner(), player, this.id);
        }
        this.rendered.remove(player.getUniqueId());
        NavigationSession navigationSession = this.hook.hud.gps().navigation().session(player);
        if (navigationSession != null && this.id.equals(navigationSession.source())) {
            this.hook.hud.gps().api().stopNavigation(player, "quest-finished");
        }
    }

    public void close() {
        for (UUID uUID : List.copyOf(this.rendered.keySet())) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            this.remove(player);
        }
        this.rendered.clear();
        this.hook.objectives.remove(this);
    }

    private record Target(Location at, String icon, String label) {
    }
}

