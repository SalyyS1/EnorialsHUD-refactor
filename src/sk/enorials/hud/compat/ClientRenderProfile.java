/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 */
package sk.enorials.hud.compat;

import java.util.Map;
import org.bukkit.Bukkit;
import sk.enorials.hud.compat.PackProfile;

public record ClientRenderProfile(int protocol, String minecraft, int resourceMajor, int resourceMinor) {
    private static final Map<Integer, String> VERSIONS = Map.ofEntries(Map.entry(755, "1.17"), Map.entry(756, "1.17.1"), Map.entry(757, "1.18"), Map.entry(758, "1.18.2"), Map.entry(759, "1.19"), Map.entry(760, "1.19.2"), Map.entry(761, "1.19.3"), Map.entry(762, "1.19.4"), Map.entry(763, "1.20.1"), Map.entry(764, "1.20.2"), Map.entry(765, "1.20.4"), Map.entry(766, "1.20.6"), Map.entry(767, "1.21.1"), Map.entry(768, "1.21.3"), Map.entry(769, "1.21.4"), Map.entry(770, "1.21.5"), Map.entry(771, "1.21.6"), Map.entry(772, "1.21.8"), Map.entry(773, "1.21.10"), Map.entry(774, "1.21.11"), Map.entry(775, "26.1.2"), Map.entry(776, "26.2"), Map.entry(777, "26.3"));

    public static ClientRenderProfile protocol(int n) {
        String string = VERSIONS.get(n);
        return string == null ? new ClientRenderProfile(n, "unknown", -1, 0) : new ClientRenderProfile(n, string, PackProfile.formatFor(string), PackProfile.minorFor(string));
    }

    public static ClientRenderProfile nativeProfile() {
        String string = Bukkit.getServer() == null ? "26.2" : Bukkit.getMinecraftVersion();
        return ClientRenderProfile.minecraft(string);
    }

    public static ClientRenderProfile minecraft(String string) {
        int n = PackProfile.formatFor(string);
        int n2 = PackProfile.minorFor(string);
        int n3 = VERSIONS.entrySet().stream().filter(entry -> ((String)entry.getValue()).equals(string)).mapToInt(Map.Entry::getKey).findFirst().orElseGet(() -> VERSIONS.entrySet().stream().filter(entry -> PackProfile.formatFor((String)entry.getValue()) == n && PackProfile.minorFor((String)entry.getValue()) == n2).mapToInt(Map.Entry::getKey).min().orElse(-1));
        return new ClientRenderProfile(n3, string, n, n2);
    }

    public boolean supported() {
        return this.resourceMajor >= 7;
    }

    public boolean legacyTerrain() {
        return this.resourceMajor < 46;
    }

    public boolean cameraTerrain() {
        return this.resourceMajor >= 63;
    }

    public boolean stackPacks() {
        return this.protocol >= 765 && this.supported();
    }

    public boolean itemModel() {
        return this.resourceMajor >= 46;
    }

    public String key() {
        return "rp-" + this.resourceMajor + "-" + this.resourceMinor;
    }

    public boolean sameResources(ClientRenderProfile clientRenderProfile) {
        return this.key().equals(clientRenderProfile.key());
    }
}

