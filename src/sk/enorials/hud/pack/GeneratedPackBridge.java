/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.pack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.zip.ZipFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.pack.GeneratedPackCompiler;
import sk.enorials.hud.pack.ServerResourcePacks;

public final class GeneratedPackBridge
implements Listener {
    private final EnorialsHUD hud;
    private Plugin crucible;
    private Plugin engine;
    private Path source;
    private Path cache;
    private volatile Map<String, byte[]> active = Map.of();
    private Map<String, byte[]> previous = Map.of();
    private String activeHash = "";
    private String previousHash = "";
    private final Map<UUID, Boolean> waiting = new LinkedHashMap<UUID, Boolean>();
    private volatile boolean canBuild = true;
    private volatile boolean ready = true;
    private volatile boolean closed;
    private boolean startup = true;
    private boolean importing;
    private boolean publishing;
    private boolean dirty;
    private boolean modelBusy;
    private boolean regenerate;
    private boolean generatedDuringModels;
    private boolean terminalFailure;
    private long serial;
    private BukkitTask timeout;
    private BukkitTask scheduled;
    private long maxBytes;
    private int maxFiles;

    public GeneratedPackBridge(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
    }

    public boolean canBuild() {
        return this.canBuild;
    }

    public boolean ready() {
        return this.ready;
    }

    public Map<String, byte[]> snapshot() {
        return this.active;
    }

    private void sync(Runnable runnable) {
        if (this.closed) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
                if (!this.closed) {
                    runnable.run();
                }
            });
        }
    }

    public void start() {
        if (!this.hud.cfg().getBoolean("generated-resource-packs.enabled", true)) {
            return;
        }
        this.crucible = Bukkit.getPluginManager().getPlugin("MythicCrucible");
        this.engine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        if (this.crucible != null && !this.crucible.isEnabled()) {
            this.crucible = null;
        }
        if (this.engine != null && !this.engine.isEnabled()) {
            this.engine = null;
        }
        if (this.crucible == null && this.engine == null) {
            return;
        }
        this.cache = this.hud.getDataFolder().toPath().resolve("generated-resource-packs/current.zip");
        this.maxBytes = Math.max(1L, this.hud.cfg().getLong("server-resource-packs.max-uncompressed-mb", 256L)) * 1024L * 1024L;
        this.maxFiles = Math.max(1, this.hud.cfg().getInt("server-resource-packs.max-files", 20000));
        this.ready = false;
        this.canBuild = false;
        this.armTimeout();
        try {
            Object object2;
            Object object3;
            if (this.crucible != null) {
                object3 = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MythicMobs"), "MythicMobs");
                this.source = object3.getDataFolder().toPath().resolve("generation/resource_pack.zip").toAbsolutePath().normalize();
                this.hook(this.crucible, "io.lumine.mythiccrucible.events.MythicCrucibleGeneratePackEvent", event -> {
                    try {
                        Path path = ((File)event.getClass().getMethod("getZippedPack", new Class[0]).invoke(event, new Object[0])).toPath().toAbsolutePath().normalize();
                        if (!this.source.equals(path)) {
                            throw new IOException("Unexpected generator output: " + String.valueOf(path));
                        }
                        this.sync(() -> {
                            this.regenerate = false;
                            if (this.modelBusy) {
                                this.generatedDuringModels = true;
                            } else {
                                this.requestImport();
                            }
                        });
                    }
                    catch (Exception exception) {
                        this.sync(() -> this.failure(exception));
                    }
                });
                object2 = YamlConfiguration.loadConfiguration((File)object3.getDataFolder().toPath().resolve("config/config-generation.yml").toFile());
                if (!object2.getBoolean("Generation.GenerateOnStartup", true)) {
                    this.requestImport();
                }
            } else {
                this.source = this.engine.getDataFolder().toPath().resolve("resource pack.zip").toAbsolutePath().normalize();
            }
            if (this.engine != null) {
                this.hook(this.engine, "com.ticxo.modelengine.api.events.ModelRegistrationEvent", event -> {
                    try {
                        String string = event.getClass().getMethod("getPhase", new Class[0]).invoke(event, new Object[0]).toString();
                        this.sync(() -> {
                            if (string.equals("PRE_IMPORT") || string.equals("PRE_ASSETS")) {
                                this.modelBusy = true;
                            }
                            if (string.equals("FINISHED")) {
                                this.modelFinished();
                            }
                        });
                    }
                    catch (Exception exception) {
                        this.sync(() -> this.failure(exception));
                    }
                });
                object3 = this.engine.getClass().getMethod("getModelGenerator", new Class[0]).invoke((Object)this.engine, new Object[0]);
                object2 = Class.forName("com.ticxo.modelengine.api.generator.ModelGenerator$Phase", true, this.engine.getClass().getClassLoader());
                Object object4 = Arrays.stream(((Class)object2).getEnumConstants()).filter(object -> object.toString().equals("FINISHED")).findFirst().orElseThrow();
                object3.getClass().getMethod("queueTask", new Class[]{object2, Runnable.class}).invoke(object3, object4, () -> this.sync(this::modelFinished));
            }
            this.hud.getLogger().info("Generated resource packs: waiting for " + (this.crucible != null ? "MythicCrucible" : "ModelEngine") + "; players will receive one combined pack.");
        }
        catch (Exception exception) {
            this.failure(exception);
        }
    }

    private void hook(Plugin plugin, String string, Consumer<Event> consumer) throws Exception {
        Class<Event> clazz = Class.forName(string, true, plugin.getClass().getClassLoader()).asSubclass(Event.class);
        Bukkit.getPluginManager().registerEvent(clazz, (Listener)this, EventPriority.MONITOR, (listener, event) -> consumer.accept(event), (Plugin)this.hud);
    }

    private void modelFinished() {
        boolean bl = this.modelBusy;
        this.modelBusy = false;
        if (this.crucible == null) {
            this.requestImport();
            return;
        }
        if (!this.startup || this.generatedDuringModels) {
            this.generatedDuringModels = false;
            this.generateCrucible();
        }
    }

    private void generateCrucible() {
        if (this.regenerate) {
            return;
        }
        this.regenerate = true;
        Bukkit.getScheduler().runTaskLater((Plugin)this.hud, () -> {
            if (this.closed) {
                return;
            }
            try {
                Object object = this.crucible.getClass().getMethod("getPackGenerationManager", new Class[0]).invoke((Object)this.crucible, new Object[0]);
                Method method = object.getClass().getMethod("generatePack", CommandSender.class);
                ConsoleCommandSender consoleCommandSender = Bukkit.getConsoleSender();
                this.hud.packBuildQueue().submit(this.toString() + "-generator", null, () -> {
                    try {
                        method.invoke(object, consoleCommandSender);
                    }
                    catch (Exception exception) {
                        this.sync(() -> this.failure(exception));
                    }
                });
            }
            catch (Exception exception) {
                this.failure(exception);
            }
        }, 20L);
    }

    private void armTimeout() {
        if (this.timeout != null) {
            this.timeout.cancel();
        }
        this.timeout = Bukkit.getScheduler().runTaskLater((Plugin)this.hud, () -> this.failure(new IOException("Generator did not produce a complete pack before timeout")), (long)Math.max(20, this.hud.cfg().getInt("generated-resource-packs.startup-timeout-seconds", 120)) * 20L);
    }

    private void requestImport() {
        if (this.closed) {
            return;
        }
        if (this.importing || this.publishing) {
            this.dirty = true;
            return;
        }
        if (this.scheduled != null) {
            this.scheduled.cancel();
        }
        this.scheduled = Bukkit.getScheduler().runTaskLater((Plugin)this.hud, () -> this.readAttempt(0), (long)Math.max(3, this.hud.cfg().getInt("generated-resource-packs.debounce-ticks", 20)));
    }

    private void readAttempt(int n) {
        if (this.closed) {
            return;
        }
        this.importing = true;
        long l = this.serial;
        if (!this.hud.packBuildQueue().submit(this, null, () -> {
            try {
                Map<String, byte[]> map = this.readComplete(this.source);
                GeneratedPackCompiler generatedPackCompiler = new GeneratedPackCompiler(this.hud.getResource("pack-profiles/vanilla-resources.txt"), this.hud.getResource("pack-profiles/vanilla-items.json"));
                Set<String> set = ServerResourcePacks.externalResources(this.hud.getDataFolder().toPath(), this.maxBytes, this.maxFiles);
                GeneratedPackCompiler.Output output = generatedPackCompiler.compile(map, set);
                this.sync(() -> {
                    if (l != this.serial) {
                        return;
                    }
                    this.importing = false;
                    this.accept(output);
                });
            }
            catch (Exception exception) {
                this.sync(() -> {
                    if (l != this.serial) {
                        return;
                    }
                    this.importing = false;
                    if (n < 9) {
                        this.scheduled = Bukkit.getScheduler().runTaskLater((Plugin)this.hud, () -> this.readAttempt(n + 1), 10L);
                    } else {
                        this.failure(exception);
                    }
                });
            }
        })) {
            this.importing = false;
            this.failure(new IOException("Pack build queue is closed"));
        }
    }

    private Map<String, byte[]> readComplete(Path path) throws IOException {
        int n;
        long l = Files.size(path);
        if (l > this.maxBytes || l < 22L) {
            throw new IOException("Invalid generated ZIP size: " + l);
        }
        FileTime fileTime = Files.getLastModifiedTime(path, new LinkOption[0]);
        try (Object object = new ZipFile(path.toFile());){
            n = (int)((ZipFile)object).stream().filter(zipEntry -> !zipEntry.isDirectory()).count();
        }
        object = Files.readAllBytes(path);
        if ((long)((Object)object).length != l || Files.size(path) != l || !fileTime.equals(Files.getLastModifiedTime(path, new LinkOption[0]))) {
            throw new IOException("Generator is still writing ZIP");
        }
        Map<String, byte[]> map = GeneratedPackCompiler.readZip((byte[])object, this.maxBytes, this.maxFiles);
        if (map.size() != n) {
            throw new IOException("Incomplete generated ZIP directory");
        }
        return map;
    }

    private void accept(GeneratedPackCompiler.Output output) {
        this.terminalFailure = false;
        if (this.timeout != null) {
            this.timeout.cancel();
            this.timeout = null;
        }
        if (output.hash().equals(this.activeHash)) {
            this.hud.getLogger().info("Generated resource pack unchanged; no rebuild or player download.");
            if (this.startup) {
                this.publish(false);
            } else {
                this.finish(false);
            }
            return;
        }
        this.previous = this.active;
        this.previousHash = this.activeHash;
        this.active = output.files();
        this.activeHash = output.hash();
        this.report(output, "validated", null);
        this.publish(!this.startup);
    }

    private void publish(boolean bl) {
        this.publishing = true;
        this.ready = false;
        this.canBuild = true;
        this.hud.packs().requestBuildWithCallback(() -> {
            if (this.closed || !this.publishing) {
                return;
            }
            Map<String, byte[]> map = this.active;
            String string = this.activeHash;
            this.hud.packBuildQueue().submit(this.toString() + "-commit", null, () -> {
                try {
                    GeneratedPackCompiler.writeZip(this.cache, map);
                    this.sync(() -> {
                        this.hud.getLogger().info("Generated resource pack integrated: " + map.size() + " files, SHA-256 " + string + ".");
                        this.finish(bl && this.hud.cfg().getBoolean("generated-resource-packs.send-on-change", true));
                    });
                }
                catch (Exception exception) {
                    this.sync(() -> this.failure(exception));
                }
            });
        }, true);
    }

    public void buildFailed() {
        this.sync(() -> {
            if (this.publishing) {
                this.active = this.previous;
                this.activeHash = this.previousHash;
                this.publishing = false;
                this.finish(false);
            }
        });
    }

    private void failure(Exception exception) {
        if (this.closed) {
            return;
        }
        ++this.serial;
        this.importing = false;
        this.regenerate = false;
        if (this.scheduled != null) {
            this.scheduled.cancel();
        }
        if (this.timeout != null) {
            this.timeout.cancel();
            this.timeout = null;
        }
        this.hud.getLogger().log(Level.SEVERE, "Generated resource pack import failed; retaining last successful pack", exception);
        this.report(null, "failed", exception.getMessage());
        if (!this.startup) {
            if (this.publishing) {
                this.active = this.previous;
                this.activeHash = this.previousHash;
            }
            this.finish(false);
            return;
        }
        this.hud.packBuildQueue().submit(this.toString() + "-fallback", null, () -> {
            Map<Object, Object> map = Map.of();
            try {
                if (Files.isRegularFile(this.cache, new LinkOption[0])) {
                    map = this.readComplete(this.cache);
                }
            }
            catch (Exception exception) {
                this.hud.getLogger().warning("Last successful generated pack unavailable: " + exception.getMessage());
            }
            Map map2 = map;
            this.sync(() -> {
                if (map2.isEmpty()) {
                    this.canBuild = true;
                    this.ready = true;
                    this.startup = false;
                    for (UUID uUID : new ArrayList<UUID>(this.waiting.keySet())) {
                        Player player = Bukkit.getPlayer((UUID)uUID);
                        if (player == null) continue;
                        this.hud.packLoadGuard().fail(player, "Server resource pack preparation failed. Please reconnect later.");
                    }
                    this.waiting.clear();
                    this.canBuild = false;
                    this.ready = false;
                    this.terminalFailure = true;
                } else {
                    this.active = map2;
                    this.activeHash = GeneratedPackCompiler.hash(map2);
                    this.publish(false);
                }
            });
        });
    }

    public boolean defer(Player player, boolean bl) {
        if (this.terminalFailure) {
            this.hud.packLoadGuard().fail(player, "Server resource pack preparation failed. Please reconnect later.");
            return true;
        }
        if (this.ready) {
            return false;
        }
        if (!this.waiting.containsKey(player.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Please wait: preparing the server's combined resource pack.");
        }
        this.waiting.merge(player.getUniqueId(), bl, Boolean::logicalOr);
        return true;
    }

    private void finish(boolean bl2) {
        this.publishing = false;
        this.ready = true;
        this.canBuild = true;
        this.startup = false;
        if (bl2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.waiting.putIfAbsent(player.getUniqueId(), false);
            }
        }
        LinkedHashMap<UUID, Boolean> linkedHashMap = new LinkedHashMap<UUID, Boolean>(this.waiting);
        this.waiting.clear();
        linkedHashMap.forEach((uUID, bl) -> {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player != null && player.isOnline()) {
                this.hud.avatars().requestPack(player, (boolean)bl);
            }
        });
        if (this.dirty) {
            this.dirty = false;
            this.requestImport();
        }
    }

    private void report(GeneratedPackCompiler.Output output, String string, String string2) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", string);
        jsonObject.addProperty("source", this.source == null ? "" : this.hud.getDataFolder().getParentFile().toPath().toAbsolutePath().relativize(this.source).toString());
        if (output != null) {
            jsonObject.addProperty("sha256", output.hash());
            jsonObject.addProperty("inputFiles", (Number)output.sourceFiles());
            jsonObject.addProperty("outputFiles", (Number)output.files().size());
            jsonObject.addProperty("convertedItemTypes", (Number)output.convertedItems());
            jsonObject.add("removed", new Gson().toJsonTree(output.removed()));
        }
        if (string2 != null) {
            jsonObject.addProperty("error", string2);
        }
        try {
            Files.createDirectories(this.cache.getParent(), new FileAttribute[0]);
            Files.write(this.cache.getParent().resolve("report.json"), GeneratedPackCompiler.encode((JsonElement)jsonObject), new OpenOption[0]);
        }
        catch (IOException iOException) {
            this.hud.getLogger().warning("Cannot write generated pack report: " + iOException.getMessage());
        }
    }

    public void close() {
        this.closed = true;
        ++this.serial;
        HandlerList.unregisterAll((Listener)this);
        if (this.timeout != null) {
            this.timeout.cancel();
        }
        if (this.scheduled != null) {
            this.scheduled.cancel();
        }
        this.waiting.clear();
    }
}

