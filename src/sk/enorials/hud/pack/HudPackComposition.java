/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.event.server.PluginEnableEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.common.pack.BetonQuestPack;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.pack.GpsEqualFields;
import sk.enorials.hud.pack.MarkerAssets;
import sk.enorials.hud.party.TargetPortraits;

public final class HudPackComposition
implements Listener {
    private final EnorialsHUD plugin;
    private volatile Snapshot snapshot = new Snapshot("hud", null);
    private Plugin spellsOwner;

    public HudPackComposition(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
    }

    public Snapshot snapshot() {
        return this.snapshot;
    }

    public void start() {
        MarkerAssets.installIcons(this.plugin);
        TargetPortraits.install(this.plugin);
        GpsEqualFields.install(this.plugin);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.plugin);
        this.refresh();
        this.plugin.packs().generated().start();
        Bukkit.getScheduler().runTask((Plugin)this.plugin, this::refresh);
    }

    public void refresh() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EnorialsSpells");
        try {
            Object object;
            byte[] byArray = null;
            Object object2 = "hud";
            if (plugin != null && plugin.isEnabled()) {
                if (plugin == this.spellsOwner) {
                    byArray = this.snapshot.spells();
                } else {
                    object = plugin.getResource("resource-pack.zip");
                    try {
                        if (object == null) {
                            throw new IOException("EnorialsSpells must contain its own resource-pack.zip");
                        }
                        byArray = ((InputStream)object).readAllBytes();
                    }
                    finally {
                        if (object != null) {
                            ((InputStream)object).close();
                        }
                    }
                }
                object2 = (String)object2 + "-spells-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(byArray));
            } else {
                plugin = null;
            }
            object = BetonQuestPack.load((Plugin)this.plugin);
            object2 = (String)object2 + ((BetonQuestPack.Snapshot)object).key();
            boolean bl = !((String)object2).equals(this.snapshot.key());
            this.snapshot = new Snapshot((String)object2, byArray, (BetonQuestPack.Snapshot)object);
            this.spellsOwner = plugin;
            if (bl) {
                this.plugin.getLogger().info("Resource pack profile: HUD" + (byArray == null ? "" : " + Spells") + (((BetonQuestPack.Snapshot)object).present() ? " + BetonQuest" : ""));
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException("Cannot select the combined Enorials pack", exception);
        }
    }

    @EventHandler
    public void enabled(PluginEnableEvent pluginEnableEvent) {
        if (Set.of("EnorialsSpells", "BetonQuest").contains(pluginEnableEvent.getPlugin().getName())) {
            this.refresh();
        }
    }

    @EventHandler
    public void disabled(PluginDisableEvent pluginDisableEvent) {
        if (pluginDisableEvent.getPlugin() == this.spellsOwner) {
            this.spellsOwner = null;
            BetonQuestPack.Snapshot snapshot = this.snapshot.quests();
            this.snapshot = new Snapshot("hud" + snapshot.key(), null, snapshot);
        }
        if (pluginDisableEvent.getPlugin().getName().equals("BetonQuest")) {
            this.snapshot = new Snapshot(this.snapshot.key().replace(this.snapshot.quests().key(), ""), this.snapshot.spells());
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void apply(Path path, Snapshot snapshot) throws IOException {
        MarkerAssets.installPack(this.plugin, path);
        try (Object object = this.plugin.getResource("pack-profiles/obsolete-assets.json");){
            if (object != null) {
                JsonObject jsonObject = JsonParser.parseReader((Reader)new InputStreamReader((InputStream)object, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry entry : jsonObject.entrySet()) {
                    Path path2 = HudPackComposition.target(path, (String)entry.getKey());
                    if (!Files.isRegularFile(path2, new LinkOption[0])) continue;
                    try {
                        String string = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path2)));
                        boolean bl = ((JsonElement)entry.getValue()).isJsonArray() ? StreamSupport.stream(((JsonElement)entry.getValue()).getAsJsonArray().spliterator(), false).anyMatch(jsonElement -> string.equals(jsonElement.getAsString())) : string.equals(((JsonElement)entry.getValue()).getAsString());
                        if (!bl) continue;
                        Files.delete(path2);
                    }
                    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                        throw new IOException(noSuchAlgorithmException);
                    }
                }
            }
        }
        object = this.plugin.getResource("pack-profiles/spell-files.txt");
        try {
            if (object == null) {
                throw new IOException("Missing spell asset manifest");
            }
            for (String string : new String(((InputStream)object).readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (string.isBlank()) continue;
                Files.deleteIfExists(HudPackComposition.target(path, string));
            }
        }
        finally {
            if (object != null) {
                ((InputStream)object).close();
            }
        }
        if (snapshot.spells() != null) {
            HudPackComposition.extract(path, new ByteArrayInputStream(snapshot.spells()), false);
            object = this.plugin.getResource("pack-profiles/combined-shaders.zip");
            try {
                if (object == null) {
                    throw new IOException("Missing combined shaders");
                }
                HudPackComposition.extract(path, (InputStream)object, true);
                return;
            }
            finally {
                if (object != null) {
                    ((InputStream)object).close();
                }
            }
        }
        object = List.of("vsh", "fsh").iterator();
        while (object.hasNext()) {
            String string = (String)object.next();
            String string2 = "assets/minecraft/shaders/core/text." + string;
            InputStream inputStream = this.plugin.getResource("default/pack/" + string2);
            try {
                if (inputStream == null) {
                    throw new IOException("Missing HUD shader");
                }
                Path path3 = HudPackComposition.target(path, string2);
                Files.createDirectories(path3.getParent(), new FileAttribute[0]);
                Files.copy(inputStream, path3, StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                if (inputStream == null) continue;
                inputStream.close();
            }
        }
    }

    private static Path target(Path path, String string) throws IOException {
        Path path2 = path.resolve(string).normalize();
        if (!string.startsWith("assets/") || !path2.startsWith(path.normalize())) {
            throw new IOException("Invalid pack entry " + string);
        }
        return path2;
    }

    private static void extract(Path path, InputStream inputStream, boolean bl) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream);){
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String string = zipEntry.getName();
                if (zipEntry.isDirectory() || !string.startsWith("assets/") || !bl && string.startsWith("assets/minecraft/shaders/core/")) continue;
                Path path2 = HudPackComposition.target(path, string);
                Files.createDirectories(path2.getParent(), new FileAttribute[0]);
                Files.copy(zipInputStream, path2, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public record Snapshot(String key, byte[] spells, BetonQuestPack.Snapshot quests) {
        public Snapshot(String string, byte[] byArray) {
            this(string, byArray, BetonQuestPack.empty());
        }
    }
}

