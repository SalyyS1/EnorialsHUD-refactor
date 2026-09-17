/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Server
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.manager;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.PackTransport;
import sk.enorials.hud.pack.PreparedPack;
import sk.enorials.hud.pack.VanillaBarPack;
import sk.enorials.hud.protocol.VanillaBars;
import sk.enorials.hud.util.PackDeliverySettings;
import sk.enorials.hud.util.PackDownloadCheck;
import sk.enorials.hud.util.PackHostingSetup;

public final class SinglePackDelivery
implements Listener,
AutoCloseable {
    private final EnorialsHUD hud;
    private final PackDeliverySettings settings;
    private final boolean hidden;
    private final UUID packId;
    private final Set<UUID> loaded = new HashSet<UUID>();
    private final Set<UUID> reconnect = new HashSet<UUID>();
    private final Map<UUID, String> observed = new HashMap<UUID, String>();
    private final AtomicBoolean checking = new AtomicBoolean();
    private volatile PackDownloadCheck.Identity expected;
    private volatile boolean valid;
    private volatile boolean closed;
    private volatile String problem = "Waiting for the combined pack export.";
    private final String selectionError;

    public SinglePackDelivery(EnorialsHUD enorialsHUD) {
        PackDeliverySettings packDeliverySettings;
        this.hud = enorialsHUD;
        String string = null;
        try {
            packDeliverySettings = this.readSettings();
        }
        catch (IOException iOException) {
            packDeliverySettings = new PackDeliverySettings(PackDeliverySettings.Source.MANAGED, "", "");
            string = iOException.getMessage();
        }
        this.settings = packDeliverySettings;
        this.hidden = enorialsHUD.cfg().getBoolean("resource-pack.single-pack.hide-vanilla-bars", false);
        UUID uUID = VanillaBars.MAIN_PACK_ID;
        if (this.settings.source() == PackDeliverySettings.Source.SERVER_PROPERTIES && PackTransport.STACKED && !this.settings.url().isBlank()) {
            try {
                Object object = Server.class.getMethod("getServerResourcePack", new Class[0]).invoke((Object)enorialsHUD.getServer(), new Object[0]);
                uUID = (UUID)Class.forName("org.bukkit.packs.ResourcePack").getMethod("getId", new Class[0]).invoke(object, new Object[0]);
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                string = "Cannot read the running server's resource-pack ID. Restart with a valid server.properties resource-pack URL.";
            }
        }
        this.packId = uUID;
        this.selectionError = string;
        if (string != null) {
            this.problem = string;
        }
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
    }

    private PackDeliverySettings readSettings() throws IOException {
        return PackDeliverySettings.select(this.hud.cfg().getString("resource-pack.delivery", "auto"), this.hud.getServer().getResourcePack(), this.hud.getServer().getResourcePackHash(), this.hud.cfg().getString("resource-pack.url", ""), this.hud.cfg().getString("resource-pack.sha1", ""));
    }

    public boolean single() {
        return this.settings.single();
    }

    public boolean serverProperties() {
        return this.settings.source() == PackDeliverySettings.Source.SERVER_PROPERTIES;
    }

    public boolean ready() {
        return this.valid && !this.closed;
    }

    public boolean loaded(Player player) {
        return !this.single() || this.ready() && this.loaded.contains(player.getUniqueId());
    }

    public String selectionError() {
        return this.selectionError;
    }

    public void announce() {
        if (this.single()) {
            this.hud.getLogger().info("Resource-pack sender: " + (this.serverProperties() ? "server.properties (Minecraft sends one combined ZIP; HUD will not send additional packs)." : "config.yml resource-pack.url (HUD sends one combined ZIP)."));
        }
    }

    public void prepare(PreparedPack preparedPack) throws IOException {
        if (!this.single() || this.closed) {
            return;
        }
        Path path = this.hud.getDataFolder().toPath().resolve("server-pack");
        Files.createDirectories(path, new FileAttribute[0]);
        Path path2 = path.resolve("EnorialsHUD-pack.zip");
        preparedPack.write(path2, VanillaBarPack.replacements(this.hud, this.hidden, preparedPack));
        PackDownloadCheck.Identity identity = PackDownloadCheck.identity(path2);
        PackDownloadCheck.Identity identity2 = this.expected;
        this.expected = identity;
        Files.writeString(path.resolve("SHA1SUMS.txt"), (CharSequence)(identity.sha1() + "  EnorialsHUD-pack.zip\n"), new OpenOption[0]);
        Files.writeString(path.resolve("SHA256SUMS.txt"), (CharSequence)(identity.sha256() + "  EnorialsHUD-pack.zip\n"), new OpenOption[0]);
        Files.writeString(path.resolve("server.properties.example"), (CharSequence)("# Copy these values into your existing server.properties, then restart.\n# Replace the example URL with the direct link to this exact combined ZIP.\nresource-pack=" + (this.serverProperties() && !this.settings.url().isBlank() ? this.settings.url() : "https://packs.example.com/EnorialsHUD-pack.zip") + "\nresource-pack-sha1=" + identity.sha1() + "\n"), new OpenOption[0]);
        Files.writeString(path.resolve("README.txt"), (CharSequence)("ONE COMBINED SERVER PACK\nUpload EnorialsHUD-pack.zip unchanged. It already includes compatible packs from ../server-resource-packs/.\nUse its direct ZIP URL in server.properties resource-pack (and SHA-1 from SHA1SUMS.txt), or config.yml resource-pack.url. Restart after changing delivery settings.\nNever put the same URL in both senders. In auto mode server.properties wins; a broken selected URL does not fall back silently.\nRun /enohud hud packcheck after fixing the hosted file. New players, skins and themes require no upload.\nAfter changing plugin/content/merged packs, rebuild, upload this new ZIP, update the hash and restart. Existing players must reconnect after a changed pack.\nOne fixed pack keeps native hearts/food/armor " + (this.hidden ? "hidden" : "visible") + "; toggling HUD panels does not download a second overlay. Change resource-pack.single-pack.hide-vanilla-bars and rebuild/restart to choose the other appearance.\n"), new OpenOption[0]);
        this.hud.getLogger().info("Single HUD pack ready: plugins/EnorialsHUD/server-pack/EnorialsHUD-pack.zip; SHA-1 " + identity.sha1());
        if (identity2 != null && !identity2.equals(identity)) {
            this.valid = false;
            this.sync(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.reconnect.add(player.getUniqueId());
                    this.loaded.remove(player.getUniqueId());
                    this.hud.renderer().detach(player);
                    player.sendMessage("The server's combined resource pack changed. Reconnect after the administrator uploads the new ZIP and restarts the server.");
                }
            });
        }
        this.verify(identity);
    }

    private void verify(PackDownloadCheck.Identity identity) {
        try {
            if (this.selectionError != null) {
                throw new IOException(this.selectionError);
            }
            if (!this.settings.equals(this.readSettings()) || this.hidden != this.hud.cfg().getBoolean("resource-pack.single-pack.hide-vanilla-bars", false)) {
                throw new IOException("Pack delivery settings changed while the server is running. Restart so Minecraft and EnorialsHUD use the same settings.");
            }
            URI uRI = this.settings.validate();
            if (!this.settings.sha1().isEmpty() && !this.settings.sha1().equals(identity.sha1())) {
                throw new IOException((this.serverProperties() ? "server.properties resource-pack-sha1" : "config.yml resource-pack.sha1") + " does not match the current combined pack. Upload server-pack/EnorialsHUD-pack.zip, copy SHA1SUMS.txt's hash and restart.");
            }
            PackDownloadCheck.verify(uRI, identity);
            if (this.closed || !identity.equals(this.expected)) {
                return;
            }
            this.valid = true;
            this.problem = "Combined pack URL, content and hash verified.";
            this.hud.getLogger().info("EnorialsHUD: " + this.problem);
            if (this.serverProperties() && this.settings.sha1().isEmpty()) {
                this.hud.getLogger().warning("server.properties resource-pack-sha1 is empty. Set it to " + identity.sha1() + " and restart to make client caching reliable.");
            }
            this.sync(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (this.serverProperties()) {
                        this.activate(player);
                        continue;
                    }
                    this.request(player);
                }
            });
        }
        catch (Exception exception) {
            if (this.closed || !identity.equals(this.expected)) {
                return;
            }
            this.valid = false;
            this.problem = exception instanceof IOException ? exception.getMessage() : "Pack verification failed (" + exception.getClass().getSimpleName() + "). Check the direct ZIP URL and try /enohud hud packcheck.";
            this.hud.getLogger().warning("EnorialsHUD pack setup: " + this.problem);
            this.sync(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.hud.renderer().detach(player);
                    if (!this.admin((CommandSender)player)) continue;
                    this.explain((CommandSender)player);
                }
            });
        }
    }

    public void check(CommandSender commandSender) {
        if (!this.single()) {
            PackHostingSetup.explain(commandSender);
            return;
        }
        PackDownloadCheck.Identity identity = this.expected;
        if (identity == null) {
            commandSender.sendMessage("The combined pack is not ready. Run /enohud hud pack and check pack-merge-report.json for conflicts.");
            return;
        }
        if (!this.checking.compareAndSet(false, true)) {
            commandSender.sendMessage("A resource-pack check is already running.");
            return;
        }
        commandSender.sendMessage("Checking the selected pack URL, download and combined content...");
        if (!this.hud.packBuildQueue().submit(this, null, () -> {
            try {
                this.verify(identity);
            }
            finally {
                this.checking.set(false);
                this.sync(() -> this.explain(commandSender));
            }
        })) {
            this.checking.set(false);
            commandSender.sendMessage("Pack worker is busy; try again shortly.");
        }
    }

    private boolean compatible(Player player) {
        if (!this.hud.clientProfiles().usable(player)) {
            return false;
        }
        if (ClientProfiles.of(player).sameResources(ClientRenderProfile.nativeProfile())) {
            return true;
        }
        player.sendMessage("This server uses one fixed resource-pack URL for its native Minecraft version. Ask the administrator to enable managed per-client HUD packs before joining with another version.");
        return false;
    }

    public void join(Player player) {
        if (!this.compatible(player)) {
            return;
        }
        UUID uUID = player.getUniqueId();
        this.reconnect.remove(uUID);
        if (this.serverProperties() && !this.observed.containsKey(uUID) && player.getResourcePackStatus() != null) {
            this.observed.put(uUID, player.getResourcePackStatus().name());
        }
        if (!this.ready()) {
            this.explain((CommandSender)player);
            return;
        }
        if (this.serverProperties()) {
            this.activate(player);
            if (!this.loaded(player)) {
                player.sendMessage("EnorialsHUD is waiting for the server.properties pack to finish loading. Accept the server resource pack.");
            }
        } else {
            this.request(player);
        }
    }

    public boolean request(Player player) {
        if (!(this.single() && player != null && player.isOnline() && this.compatible(player))) {
            return false;
        }
        if (!this.ready()) {
            this.explain((CommandSender)player);
            return false;
        }
        if (this.serverProperties()) {
            this.activate(player);
            return true;
        }
        if (this.loaded(player) || this.hud.packLoadGuard().isWaiting(player)) {
            return true;
        }
        this.hud.packLoadGuard().preparing(player);
        this.hud.packLoadGuard().expect(player, this.packId);
        this.hud.storage().setPackUrl(player.getUniqueId(), this.settings.url());
        PackTransport.main(player, this.packId, this.settings.url(), HexFormat.of().parseHex(this.expected.sha1()), null, this.hud.cfg().getBoolean("resource-pack.force", false));
        return true;
    }

    private void activate(Player player) {
        UUID uUID = player.getUniqueId();
        if (this.ready() && player.isOnline() && ClientProfiles.of(player).sameResources(ClientRenderProfile.nativeProfile()) && !this.reconnect.contains(uUID) && "SUCCESSFULLY_LOADED".equals(this.observed.get(uUID)) && this.loaded.add(uUID)) {
            this.hud.renderer().attach(player);
            if (this.hud.minimap() != null && this.hud.getPlayerMinimap(player) == null) {
                try {
                    this.hud.playerDataStorage().restore(this.hud.minimap(), player);
                }
                catch (Exception exception) {
                    this.hud.getLogger().warning("Could not restore minimap after the combined pack loaded: " + exception.getClass().getSimpleName());
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void status(PlayerResourcePackStatusEvent playerResourcePackStatusEvent) {
        if (!this.single() || !PackTransport.id(playerResourcePackStatusEvent, this.packId).equals(this.packId)) {
            return;
        }
        Player player = playerResourcePackStatusEvent.getPlayer();
        String string = playerResourcePackStatusEvent.getStatus().name();
        this.observed.put(player.getUniqueId(), string);
        if (string.equals("SUCCESSFULLY_LOADED")) {
            this.activate(player);
        } else if (Set.of("DECLINED", "FAILED_DOWNLOAD", "FAILED_RELOAD", "INVALID_URL", "DISCARDED").contains(string)) {
            this.loaded.remove(player.getUniqueId());
            if (player.isOnline()) {
                this.hud.renderer().detach(player);
                player.sendMessage("EnorialsHUD could not load the selected server resource pack (" + string + "). Enable server resource packs and reconnect; ask the administrator to run /enohud hud packcheck.");
            }
            this.hud.getLogger().warning("A player could not load the selected combined resource pack: " + string + ". Use /enohud hud packcheck to verify the host.");
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        UUID uUID = playerQuitEvent.getPlayer().getUniqueId();
        this.loaded.remove(uUID);
        this.observed.remove(uUID);
        this.reconnect.remove(uUID);
    }

    public void explain(CommandSender commandSender) {
        if (this.admin(commandSender)) {
            commandSender.sendMessage("EnorialsHUD pack delivery: " + String.valueOf((Object)this.settings.source()) + ". " + this.problem);
            if (!this.ready()) {
                commandSender.sendMessage("Check plugins/EnorialsHUD/server-pack/README.txt. Delivery/config changes require a restart; fixing an uploaded ZIP can be checked with /enohud hud packcheck.");
            }
        } else {
            commandSender.sendMessage(this.ready() ? "Accept the server resource pack and reconnect to use EnorialsHUD." : "EnorialsHUD graphics are waiting for the administrator to fix the server resource-pack setup. You can continue playing.");
        }
    }

    private boolean admin(CommandSender commandSender) {
        return !(commandSender instanceof Player) || commandSender.isOp() || commandSender.hasPermission("enorials.hud.admin") || commandSender.hasPermission("enorialshud.admin");
    }

    private void sync(Runnable runnable) {
        if (!this.closed && this.hud.isEnabled()) {
            Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
                if (!this.closed) {
                    runnable.run();
                }
            });
        }
    }

    @Override
    public void close() {
        this.closed = true;
        this.valid = false;
        HandlerList.unregisterAll((Listener)this);
        this.loaded.clear();
        this.observed.clear();
        this.reconnect.clear();
    }
}

