/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.pack.BuildablePack;
import sk.enorials.hud.pack.GeneratedPackBridge;
import sk.enorials.hud.pack.HudPackComposition;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.pack.PreparedPack;
import sk.enorials.hud.pack.VanillaBarPack;
import sk.enorials.hud.util.GitHubUploader;
import sk.enorials.hud.util.PackHostingSetup;

public class PackManager {
    private final EnorialsHUD plugin;
    private final AtomicReference<PackRegistry> registry = new AtomicReference<PackRegistry>(new PackRegistry());
    private final Object buildLock = new Object();
    private volatile boolean building = false;
    private volatile boolean pending = false;
    private volatile boolean closed = false;
    private BukkitTask scheduled;
    private final List<Runnable> completionCallbacks = new ArrayList<Runnable>();
    private final AtomicLong generation = new AtomicLong();
    private volatile Template template;
    private PreparedPack rawTemplate;
    private final Map<String, Template> clientTemplates = new ConcurrentHashMap<String, Template>();
    private final Map<String, String> clientFailures = new HashMap<String, String>();
    private volatile long failedGeneration = -1L;
    private final GeneratedPackBridge generated;

    public String activeProfile(ClientRenderProfile clientRenderProfile) {
        if (clientRenderProfile.sameResources(ClientRenderProfile.nativeProfile())) {
            return this.activeProfile();
        }
        Template template = this.clientTemplates.get(clientRenderProfile.key());
        return template == null ? "unavailable" : template.pack().profile();
    }

    public GeneratedPackBridge generated() {
        return this.generated;
    }

    public boolean isReady() {
        Template template = this.template;
        return this.generated.ready() && template != null && !this.closed && template.profile().equals(this.plugin.packComposition().snapshot().key()) && (template.generation() == this.generation.get() || this.failedGeneration == this.generation.get());
    }

    public String activeProfile() {
        Template template = this.template;
        return template == null ? "unavailable" : template.pack().profile();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PreparedPack template(HudPackComposition.Snapshot snapshot) throws IOException {
        Object object = this.plugin.stateLock();
        synchronized (object) {
            if (this.closed) {
                throw new IOException("Pack manager is closed");
            }
            if (!this.generated.canBuild()) {
                throw new IOException("Waiting for generated server resources");
            }
            long l = this.generation.get();
            if (this.template != null && this.template.generation() == l && this.template.profile().equals(snapshot.key())) {
                return this.template.pack();
            }
            PackRegistry packRegistry = new PackRegistry();
            long l2 = System.nanoTime();
            BuildablePack buildablePack = new BuildablePack(this.plugin, packRegistry, snapshot);
            buildablePack.buildRaw();
            long l3 = System.nanoTime();
            PreparedPack preparedPack = PreparedPack.read(buildablePack.getPackDirectory().toPath());
            ClientRenderProfile clientRenderProfile = ClientRenderProfile.nativeProfile();
            PreparedPack preparedPack2 = preparedPack.forClient(this.plugin.getDataFolder().toPath().resolve("built-pack-clients").resolve(clientRenderProfile.key()), clientRenderProfile).withQuestPack(snapshot.quests()).withServerPacks(this.plugin.getDataFolder().toPath(), snapshot.key(), VanillaBarPack.defaults(this.plugin, false), this.plugin.cfg().getBoolean("server-resource-packs.enabled", true), Math.max(1L, this.plugin.cfg().getLong("server-resource-packs.max-uncompressed-mb", 256L)) * 1024L * 1024L, Math.max(1, this.plugin.cfg().getInt("server-resource-packs.max-files", 20000)), this.generated.snapshot());
            this.plugin.getLogger().info(String.format(Locale.ROOT, "Resource pack prepared in %.2fs (assets %.2fs, composition %.2fs).", (double)(System.nanoTime() - l2) / 1.0E9, (double)(l3 - l2) / 1.0E9, (double)(System.nanoTime() - l3) / 1.0E9));
            if (this.plugin.vanillaBars() != null) {
                this.plugin.vanillaBars().prepareVisible(preparedPack2.visibleBars());
            }
            this.registry.set(packRegistry);
            this.template = new Template(l, snapshot.key(), preparedPack2);
            this.rawTemplate = preparedPack;
            this.clientTemplates.clear();
            this.clientFailures.clear();
            return preparedPack2;
        }
    }

    public PreparedPack forPlayer(HudPackComposition.Snapshot snapshot) throws IOException {
        Template template = this.template;
        if (template == null && this.failedGeneration == this.generation.get()) {
            throw new IOException("Resource pack build previously failed. Fix the input reported in pack-merge-report.json and run /enohud hud pack.");
        }
        if (template != null && this.failedGeneration == this.generation.get() && template.profile().equals(snapshot.key())) {
            return template.pack();
        }
        return this.template(snapshot);
    }

    public PreparedPack forPlayer(HudPackComposition.Snapshot snapshot, ClientRenderProfile clientRenderProfile) throws IOException {
        Object object = this.plugin.stateLock();
        synchronized (object) {
            PreparedPack preparedPack = this.forPlayer(snapshot);
            if (clientRenderProfile.sameResources(ClientRenderProfile.nativeProfile())) {
                return preparedPack;
            }
            if (!clientRenderProfile.supported()) {
                throw new IOException("Unsupported client protocol " + clientRenderProfile.protocol());
            }
            Template template = this.clientTemplates.get(clientRenderProfile.key());
            if (template != null) {
                return template.pack();
            }
            if (this.clientFailures.containsKey(clientRenderProfile.key())) {
                throw new IOException(this.clientFailures.get(clientRenderProfile.key()));
            }
            try {
                PreparedPack preparedPack2 = this.rawTemplate.forClient(this.plugin.getDataFolder().toPath().resolve("built-pack-clients").resolve(clientRenderProfile.key()), clientRenderProfile).withQuestPack(snapshot.quests()).withServerPacks(this.plugin.getDataFolder().toPath(), snapshot.key() + "-" + clientRenderProfile.key(), VanillaBarPack.defaults(this.plugin, false, clientRenderProfile), this.plugin.cfg().getBoolean("server-resource-packs.enabled", true), Math.max(1L, this.plugin.cfg().getLong("server-resource-packs.max-uncompressed-mb", 256L)) * 1024L * 1024L, Math.max(1, this.plugin.cfg().getInt("server-resource-packs.max-files", 20000)), this.generated.snapshot()).shareWith(preparedPack);
                if (this.plugin.vanillaBars() != null) {
                    this.plugin.vanillaBars().prepareVisible(preparedPack2.visibleBars(), clientRenderProfile);
                }
                this.clientTemplates.put(clientRenderProfile.key(), new Template(this.template.generation(), snapshot.key(), preparedPack2));
                this.plugin.getLogger().info("Prepared shared client pack for Minecraft " + clientRenderProfile.minecraft() + " (" + clientRenderProfile.key() + ").");
                return preparedPack2;
            }
            catch (IOException iOException) {
                this.clientFailures.put(clientRenderProfile.key(), iOException.getMessage());
                throw iOException;
            }
        }
    }

    public PackManager(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.generated = new GeneratedPackBridge(enorialsHUD);
    }

    public void requestBuild() {
        int n = this.plugin.cfg().getInt("pack-build.debounce-ticks", 40);
        this.scheduleBuild(n);
    }

    public void requestBuildImmediate() {
        this.scheduleBuild(0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void requestBuildWithCallback(Runnable runnable, boolean bl) {
        if (this.closed || !PackHostingSetup.canBuild()) {
            return;
        }
        if (runnable != null) {
            List<Runnable> list = this.completionCallbacks;
            synchronized (list) {
                this.completionCallbacks.add(runnable);
            }
        }
        if (bl) {
            this.requestBuildImmediate();
        } else {
            this.requestBuild();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void scheduleBuild(int n) {
        if (!PackHostingSetup.canBuild()) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.scheduleBuild(n));
            return;
        }
        this.plugin.packComposition().refresh();
        Object object = this.buildLock;
        synchronized (object) {
            if (this.closed) {
                return;
            }
            this.generation.incrementAndGet();
            if (this.scheduled != null) {
                this.scheduled.cancel();
            }
            this.scheduled = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::startBuild, (long)Math.max(0, n));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void startBuild() {
        Object object = this.buildLock;
        synchronized (object) {
            if (this.closed) {
                return;
            }
            if (!this.generated.canBuild()) {
                this.scheduled = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::startBuild, 20L);
                return;
            }
            if (this.building) {
                this.pending = true;
                return;
            }
            this.building = true;
        }
        object = this::lambda$startBuild$0;
        if (!this.plugin.packBuildQueue().submit(this, null, (Runnable)object)) {
            Object object2 = this.buildLock;
            synchronized (object2) {
                this.building = false;
            }
        }
    }

    public PackRegistry registry() {
        return this.registry.get();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void close() {
        this.generated.close();
        Object object = this.buildLock;
        synchronized (object) {
            this.closed = true;
            this.pending = false;
            if (this.scheduled != null) {
                this.scheduled.cancel();
            }
        }
        object = this.completionCallbacks;
        synchronized (object) {
            this.completionCallbacks.clear();
        }
        object = this.plugin.stateLock();
        synchronized (object) {
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void runCallbacks() {
        ArrayList<Runnable> arrayList;
        if (this.closed || !this.plugin.isEnabled()) {
            return;
        }
        List<Runnable> list = this.completionCallbacks;
        synchronized (list) {
            if (this.completionCallbacks.isEmpty()) {
                return;
            }
            arrayList = new ArrayList<Runnable>(this.completionCallbacks);
            this.completionCallbacks.clear();
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (this.closed) {
                return;
            }
            for (Runnable runnable : arrayList) {
                try {
                    runnable.run();
                }
                catch (Exception exception) {
                    this.plugin.getLogger().log(Level.WARNING, "Resource pack completion callback failed", exception);
                }
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    private /* synthetic */ void lambda$startBuild$0() {
        try {
            if (this.closed) {
                return;
            }
            PreparedPack preparedPack = this.template(this.plugin.packComposition().snapshot());
            if (this.closed) {
                return;
            }
            if (this.plugin.singlePack() != null) {
                this.plugin.singlePack().prepare(preparedPack);
            }
            try {
                this.plugin.avatars().prepareStaticPacks(preparedPack);
            }
            catch (Exception exception) {
                throw new IOException("Cannot export static shared HUD packs", exception);
            }
            if (this.plugin.cfg().getBoolean("copy-resource-pack.enabled", true)) {
                String version = this.plugin.getDescription().getVersion();
                String zipPath = this.plugin.cfg().getString("copy-resource-pack.zip-path");
                if (zipPath == null || zipPath.isBlank()) {
                    zipPath = "EnorialsHUD/EnorialsHUD-pack-" + version + ".zip";
                }
                zipPath = zipPath.replace("{version}", version);
                File destination = new File(this.plugin.getDataFolder().getParentFile(), zipPath);
                Files.createDirectories(destination.getParentFile().toPath(), new FileAttribute[0]);
                preparedPack.write(destination.toPath(), Map.of());
                if (!GitHubUploader.uploadIfEnabled(this.plugin, destination)) {
                    this.plugin.getLogger().warning("Resource pack GitHub upload failed; keeping local zip only.");
                }
            }
            this.runCallbacks();
        }
        catch (IOException | RuntimeException exception) {
            this.failedGeneration = this.generation.get();
            synchronized (this.completionCallbacks) {
                this.completionCallbacks.clear();
            }
            if (!this.closed) {
                this.plugin.getLogger().log(Level.SEVERE, "Resource pack build failed", exception);
            }
            this.generated.buildFailed();
        }
        finally {
            synchronized (this.buildLock) {
                this.building = false;
                if (this.pending && !this.closed && this.plugin.isEnabled()) {
                    this.pending = false;
                    this.scheduleBuild(0);
                }
            }
        }
    }

    private record Template(long generation, String profile, PreparedPack pack) {
    }
}

