/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.module.PartyConfig;

public class PartyFX {
    private final Plugin plugin;

    public PartyFX(Plugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return PartyConfig.get(this.plugin).getBoolean("effects.enabled", true);
    }

    private boolean sounds() {
        return this.enabled() && PartyConfig.get(this.plugin).getBoolean("effects.sounds", true);
    }

    private boolean particles() {
        return this.enabled() && PartyConfig.get(this.plugin).getBoolean("effects.particles", true);
    }

    private int count(int n) {
        return Math.max(0, PartyConfig.get(this.plugin).getInt("effects.particle-count", n));
    }

    private void burst(Player player, Particle particle, int n, double d, double d2, double d3) {
        if (this.particles()) {
            World world = player.getWorld();
            world.spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0), n, d, d2, d3, 0.0);
        }
    }

    private void ring(Player player, Particle particle, int n, double d, double d2) {
        if (this.particles()) {
            World world = player.getWorld();
            Location location = player.getLocation().add(0.0, d2, 0.0);
            for (int i = 0; i < n; ++i) {
                double d3 = Math.PI * 2 * ((double)i / (double)n);
                double d4 = Math.cos(d3) * d;
                double d5 = Math.sin(d3) * d;
                world.spawnParticle(particle, location.clone().add(d4, 0.0, d5), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    public void partyCreated(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.75f, 1.55f);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.45f, 1.2f);
        }
        this.burst(player, Particle.HAPPY_VILLAGER, this.count(22), 0.6, 0.9, 0.6);
        this.ring(player, Particle.END_ROD, 18, 0.8, 0.2);
    }

    public void inviteSent(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.65f, 1.85f);
        }
        this.burst(player, Particle.END_ROD, this.count(10), 0.25, 0.55, 0.25);
        this.burst(player, Particle.WITCH, this.count(6), 0.2, 0.3, 0.2);
    }

    public void inviteReceived(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.65f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
        }
        this.burst(player, Particle.END_ROD, this.count(14), 0.4, 0.7, 0.4);
        this.burst(player, Particle.NOTE, 2, 0.35, 0.5, 0.35);
    }

    public void inviteAccepted(Player player) {
        this.memberJoined(player);
    }

    public void memberJoined(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.25f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
        }
        this.burst(player, Particle.TOTEM_OF_UNDYING, this.count(14), 0.45, 0.8, 0.45);
        this.burst(player, Particle.FIREWORK, this.count(18), 0.55, 0.9, 0.55);
    }

    public void memberLeft(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.75f, 0.85f);
        }
        this.burst(player, Particle.CLOUD, this.count(10), 0.35, 0.25, 0.35);
    }

    public void kicked(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.65f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        }
        this.burst(player, Particle.LARGE_SMOKE, this.count(12), 0.45, 0.5, 0.45);
        this.burst(player, Particle.ANGRY_VILLAGER, 6, 0.4, 0.6, 0.4);
    }

    public void promoted(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.65f, 1.4f);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.55f, 1.3f);
        }
        this.burst(player, Particle.FIREWORK, this.count(22), 0.55, 0.9, 0.55);
        this.ring(player, Particle.END_ROD, 22, 0.95, 0.25);
    }

    public void trackingOn(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.65f, 1.4f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.4f, 1.9f);
        }
        this.burst(player, Particle.ENCHANTED_HIT, this.count(10), 0.25, 0.45, 0.25);
    }

    public void voteCast(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.55f, 1.75f);
        }
        this.burst(player, Particle.NOTE, 1, 0.25, 0.35, 0.25);
    }

    public void votePassed(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.8f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.6f, 1.3f);
        }
        this.burst(player, Particle.FIREWORK, this.count(16), 0.45, 0.7, 0.45);
    }

    public void trackingOff(Player player) {
        if (this.sounds()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.45f, 0.85f);
        }
        this.burst(player, Particle.INSTANT_EFFECT, this.count(8), 0.25, 0.45, 0.25);
    }
}

