/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.avatar;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.avatar.AvatarFont;
import sk.enorials.hud.avatar.NameSkinLookup;
import sk.enorials.hud.avatar.SkinHead;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.partyhud.event.PartyCreateEvent;
import sk.enorials.partyhud.event.PartyJoinEvent;
import sk.enorials.partyhud.party.Party;

public final class SkinAvatars
implements AutoCloseable,
Listener {
    private final EnorialsHUD hud;
    private final Face fallback;
    private final Map<UUID, State> players = new HashMap<UUID, State>();
    private final Map<UUID, State> own = new HashMap<UUID, State>();
    private final Map<UUID, Painted> painted = new HashMap<UUID, Painted>();
    private final LinkedHashMap<UUID, Face> recentFaces = new LinkedHashMap(32, 0.75f, true);
    private final LinkedHashMap<String, CompletableFuture<Face>> sources = new LinkedHashMap();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(128), runnable -> {
        Thread thread = new Thread(runnable, "Enorials-skin-cache");
        thread.setDaemon(true);
        return thread;
    });
    private final NameSkinLookup nameSkins = new NameSkinLookup(this.workers);
    private volatile boolean closed;
    private boolean registered;

    public SkinAvatars(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        try (InputStream inputStream = enorialsHUD.getResource("default/textures/assets/enorialshud/party_steve.png");){
            if (inputStream == null) {
                throw new IOException("Missing default face");
            }
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            int[] nArray = new int[64];
            for (int i = 0; i < 8; ++i) {
                for (int j = 0; j < 8; ++j) {
                    nArray[i * 8 + j] = bufferedImage.getRGB((2 * j + 1) * bufferedImage.getWidth() / 16, (2 * i + 1) * bufferedImage.getHeight() / 16) & 0xFFFFFF;
                }
            }
            this.fallback = new Face(nArray);
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
    }

    public void watch(Player player, boolean bl) {
        if (this.closed) {
            return;
        }
        if (!this.registered) {
            Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.hud);
            this.registered = true;
        }
        this.watch(player, bl, this.own);
        if (bl && this.players.containsKey(player.getUniqueId())) {
            this.players.get((Object)player.getUniqueId()).resolved = false;
        }
        this.watch(player, false, this.players);
    }

    private void watch(Player player, boolean bl, Map<UUID, State> map) {
        boolean bl2;
        if (this.closed || !this.active(player, map)) {
            return;
        }
        URL uRL = player.getPlayerProfile().getTextures().getSkin();
        Object object = uRL == null ? "" : uRL.toExternalForm();
        boolean bl3 = bl2 = ((String)object).isEmpty() && this.hud.cfg().getBoolean("avatars.name-lookup.enabled", false) && (!this.hud.cfg().getBoolean("avatars.name-lookup.offline-only", true) || !Bukkit.getOnlineMode());
        if (bl2) {
            object = "name:" + player.getName().toLowerCase(Locale.ROOT);
        }
        UUID uUID = player.getUniqueId();
        State state = map.get(uUID);
        if (!bl && state != null && state.requestKey.equals(object) && (state.resolved || ((String)object).isEmpty())) {
            return;
        }
        State state2 = new State((String)object, state == null ? this.recentFaces.getOrDefault(uUID, this.fallback) : state.face);
        map.put(uUID, state2);
        this.painted.clear();
        if (((String)object).isEmpty()) {
            state2.face = this.fallback;
            state2.resolved = true;
            return;
        }
        if (bl2) {
            this.nameSkins.skinAsync(player.getName(), bl, this.hud.cfg().getInt("avatars.name-lookup.cache-minutes", 1440), this.hud.cfg().getInt("avatars.name-lookup.retry-minutes", 15)).thenAccept(string -> {
                if (this.closed || !this.hud.isEnabled()) {
                    return;
                }
                Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
                    if (this.closed || map.get(uUID) != state2 || !this.active(player, map) || string.isEmpty()) {
                        return;
                    }
                    this.loadForState(player, bl, map, uUID, state2, (String)string);
                });
            });
            return;
        }
        this.loadForState(player, bl, map, uUID, state2, (String)object);
    }

    private void loadForState(Player player, boolean bl, Map<UUID, State> map, UUID uUID, State state, String string) {
        state.source = string;
        CompletableFuture<Face> completableFuture = this.sources.get(string);
        if (bl && completableFuture != null && completableFuture.isDone()) {
            this.sources.remove(string);
            completableFuture = null;
        }
        if (completableFuture == null || completableFuture.isDone() && (completableFuture.getNow(null) == null || ((Face)completableFuture.getNow(null)).head() == null)) {
            if (this.sources.size() >= 256) {
                Iterator<Map.Entry<String, CompletableFuture<Face>>> iterator = this.sources.entrySet().iterator();
                while (iterator.hasNext()) {
                    if (!iterator.next().getValue().isDone()) continue;
                    iterator.remove();
                    break;
                }
                if (this.sources.size() >= 256) {
                    return;
                }
            }
            try {
                completableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return this.load(string, bl);
                    }
                    catch (Exception exception) {
                        this.hud.getLogger().fine("Skin unavailable; keeping cached/default avatar: " + exception.getClass().getSimpleName());
                        return null;
                    }
                }, this.workers);
                this.sources.put(string, completableFuture);
            }
            catch (RejectedExecutionException rejectedExecutionException) {
                return;
            }
        }
        completableFuture.thenAccept(face -> {
            if (face == null || this.closed || !this.hud.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
                if (this.closed || map.get(uUID) != state || !this.active(player, map)) {
                    return;
                }
                if (face.head() == null && state.face.head() != null) {
                    return;
                }
                state.face = face;
                state.resolved = face.head() != null;
                this.painted.clear();
                this.recentFaces.put(uUID, (Face)face);
                while (this.recentFaces.size() > 256) {
                    this.recentFaces.remove(this.recentFaces.keySet().iterator().next());
                }
                for (Player player2 : Bukkit.getOnlinePlayers()) {
                    Party party = this.hud.getPartyService().getPartyOf(player2.getUniqueId()).orElse(null);
                    if (!player2.equals((Object)player) && (party == null || !party.getMembers().contains(uUID))) continue;
                    this.hud.renderer().refreshTheme(player2);
                }
            });
        });
    }

    private boolean active(Player player, Map<UUID, State> map) {
        return player.isOnline() || map == this.players && this.hud.api() != null && this.hud.api().resolveMember(player.getUniqueId()) == player;
    }

    public void party(Player player) {
        Party party = this.hud.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        if (party == null) {
            return;
        }
        for (UUID uUID : party.getMembers()) {
            Player player2 = this.hud.api().resolveMember(uUID);
            if (player2 == null) continue;
            this.watch(player2, false, this.players);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void created(PartyCreateEvent partyCreateEvent) {
        this.party(partyCreateEvent.getLeader());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void joined(PartyJoinEvent partyJoinEvent) {
        this.party(partyJoinEvent.getPlayer());
    }

    private Face load(String string, boolean bl) throws Exception {
        URI uRI = URI.create(string);
        if (!(List.of("http", "https").contains(uRI.getScheme()) && "textures.minecraft.net".equalsIgnoreCase(uRI.getHost()) && uRI.getPath().matches("/texture/[a-zA-Z0-9]+") && uRI.getRawUserInfo() == null && uRI.getRawQuery() == null && uRI.getPort() == -1)) {
            throw new IOException("Unsupported skin origin");
        }
        String string2 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(uRI.getPath().getBytes(StandardCharsets.UTF_8)));
        Path path = this.hud.getDataFolder().toPath().resolve("cache/player-skins-v1/" + string2 + ".png");
        if (!bl && Files.isRegularFile(path, new LinkOption[0])) {
            try {
                BufferedImage bufferedImage = ImageIO.read(path.toFile());
                if (bufferedImage != null && bufferedImage.getWidth() == 64 && (bufferedImage.getHeight() == 32 || bufferedImage.getHeight() == 64)) {
                    return SkinAvatars.decoded(bufferedImage);
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        try {
            return this.download(uRI, path);
        }
        catch (IOException iOException) {
            Path path2 = this.hud.getDataFolder().toPath().resolve("cache/skin-faces-rgb/" + string2 + ".png");
            if (Files.isRegularFile(path2, new LinkOption[0])) {
                try {
                    BufferedImage bufferedImage = ImageIO.read(path2.toFile());
                    if (bufferedImage != null && bufferedImage.getWidth() == 8 && bufferedImage.getHeight() == 8) {
                        return new Face(bufferedImage.getRGB(0, 0, 8, 8, null, 0, 8));
                    }
                }
                catch (IOException iOException2) {
                    // empty catch block
                }
            }
            throw iOException;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Face download(URI uRI, Path path) throws IOException {
        Object object;
        byte[] byArray;
        Object object2;
        HttpURLConnection httpURLConnection = (HttpURLConnection)URI.create("https://textures.minecraft.net" + uRI.getPath()).toURL().openConnection();
        httpURLConnection.setInstanceFollowRedirects(false);
        httpURLConnection.setConnectTimeout(4000);
        httpURLConnection.setReadTimeout(5000);
        try {
            if (httpURLConnection.getResponseCode() != 200) {
                throw new IOException("Skin download failed");
            }
            object2 = httpURLConnection.getInputStream();
            try {
                byArray = ((InputStream)object2).readNBytes(0x100001);
            }
            finally {
                if (object2 != null) {
                    ((InputStream)object2).close();
                }
            }
        }
        finally {
            httpURLConnection.disconnect();
        }
        if (byArray.length > 0x100000) {
            throw new IOException("Skin exceeds limit");
        }
        try (Object object3 = ImageIO.createImageInputStream(new ByteArrayInputStream(byArray));){
            object = ImageIO.getImageReaders(object3);
            if (!object.hasNext()) {
                throw new IOException("Invalid skin image");
            }
            ImageReader imageReader = object.next();
            try {
                imageReader.setInput(object3);
                if (imageReader.getWidth(0) != 64 || imageReader.getHeight(0) != 32 && imageReader.getHeight(0) != 64) {
                    throw new IOException("Invalid skin dimensions");
                }
                object2 = imageReader.read(0);
            }
            finally {
                imageReader.dispose();
            }
        }
        object3 = SkinAvatars.decoded((BufferedImage)object2);
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            object = Files.createTempFile(path.getParent(), "skin-", ".tmp", new FileAttribute[0]);
            try {
                ImageIO.write((RenderedImage)object2, "png", object.toFile());
                Files.move((Path)object, path, StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                Files.deleteIfExists((Path)object);
            }
        }
        catch (IOException iOException) {
            this.hud.getLogger().fine("Skin disk cache unavailable; using memory cache");
        }
        return object3;
    }

    private static Face decoded(BufferedImage bufferedImage) {
        BufferedImage bufferedImage2 = SkinAvatars.face(bufferedImage);
        return new Face(bufferedImage2.getRGB(0, 0, 8, 8, null, 0, 8), SkinHead.render(bufferedImage));
    }

    private Face forPlayer(UUID uUID) {
        State state = this.players.get(uUID);
        return state == null ? this.recentFaces.getOrDefault(uUID, this.fallback) : state.face;
    }

    public SkinHead head(UUID uUID) {
        return this.forPlayer(uUID).head();
    }

    public static BufferedImage face(BufferedImage bufferedImage) {
        if (bufferedImage.getWidth() != 64 || bufferedImage.getHeight() != 32 && bufferedImage.getHeight() != 64) {
            throw new IllegalArgumentException("Expected vanilla skin");
        }
        BufferedImage bufferedImage2 = new BufferedImage(8, 8, 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.drawImage((Image)bufferedImage.getSubimage(8, 8, 8, 8), 0, 0, null);
        graphics2D.drawImage((Image)bufferedImage.getSubimage(40, 8, 8, 8), 0, 0, null);
        graphics2D.dispose();
        return bufferedImage2;
    }

    public Component paint(Player player, Component component) {
        State state = this.own.get(player.getUniqueId());
        Face face = state == null ? this.fallback : state.face;
        PackRegistry packRegistry = this.hud.packs().registry();
        Painted painted = this.painted.get(player.getUniqueId());
        if (painted != null && painted.input == component && painted.face == face && painted.registry == packRegistry) {
            return painted.output;
        }
        Component component2 = AvatarFont.paint(component, null, packRegistry.avatarFonts(), null, (uUID, definition) -> (uUID == null ? face : this.forPlayer((UUID)uUID)).component((AvatarFont.Definition)definition, packRegistry));
        this.painted.put(player.getUniqueId(), new Painted(component, face, packRegistry, component2));
        return component2;
    }

    public void releaseMember(UUID uUID) {
        this.players.remove(uUID);
        this.painted.clear();
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.releaseMember(playerQuitEvent.getPlayer().getUniqueId());
        this.own.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    @Override
    public void close() {
        this.closed = true;
        HandlerList.unregisterAll((Listener)this);
        this.nameSkins.close();
        this.workers.shutdownNow();
        try {
            this.workers.awaitTermination(10L, TimeUnit.SECONDS);
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        this.players.clear();
        this.own.clear();
        this.painted.clear();
        this.sources.clear();
        this.recentFaces.clear();
    }

    public static final class Face {
        private final int[] colors;
        private final SkinHead head;
        private final Map<AvatarFont.Definition, Component> components = new HashMap<AvatarFont.Definition, Component>();
        private PackRegistry registry;

        public Face(int[] nArray) {
            this(nArray, null);
        }

        public Face(int[] nArray, SkinHead skinHead) {
            if (nArray.length != 64) {
                throw new IllegalArgumentException("Expected 8x8 face");
            }
            this.colors = (int[])nArray.clone();
            this.head = skinHead;
        }

        public int[] colors() {
            return (int[])this.colors.clone();
        }

        public SkinHead head() {
            return this.head;
        }

        Component component(AvatarFont.Definition definition2, PackRegistry packRegistry) {
            if (this.registry != packRegistry) {
                this.components.clear();
                this.registry = packRegistry;
            }
            return this.components.computeIfAbsent(definition2, definition -> definition.component(this.colors));
        }
    }

    private static final class State {
        final String requestKey;
        String source;
        Face face;
        boolean resolved;

        State(String string, Face face) {
            this.requestKey = string;
            this.source = string;
            this.face = face;
        }
    }

    private record Painted(Component input, Face face, PackRegistry registry, Component output) {
    }
}

