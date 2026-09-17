/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.avatar.SkinAvatars;
import sk.enorials.hud.avatar.SkinHead;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.PackTransport;
import sk.enorials.hud.manager.SharedAvatarPack;
import sk.enorials.hud.pack.PreparedPack;
import sk.enorials.hud.protocol.VanillaBars;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.hud.util.StaticPackHosting;

public final class AvatarManager
implements AutoCloseable {
    private final EnorialsHUD plugin;
    private final SharedAvatarPack packs;
    private final SkinAvatars skins;

    public AvatarManager(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.packs = new SharedAvatarPack(enorialsHUD);
        this.skins = new SkinAvatars(enorialsHUD);
        this.reload();
    }

    public void reload() {
        try {
            Object object;
            Object object2;
            BufferedImage bufferedImage;
            Path path = this.plugin.getDataFolder().toPath();
            Path path2 = path.resolve("textures/assets/enorialshud/avatars/heads");
            Files.createDirectories(path2, new FileAttribute[0]);
            try (Object object3 = this.plugin.getResource("default/textures/assets/enorialshud/party_steve.png");){
                if (object3 == null) {
                    throw new IOException("Missing default avatar");
                }
                bufferedImage = ImageIO.read((InputStream)object3);
            }
            object3 = new BufferedImage(40, 40, 2);
            Graphics2D graphics2D = ((BufferedImage)object3).createGraphics();
            graphics2D.drawImage(bufferedImage, 0, 0, 40, 40, null);
            graphics2D.dispose();
            ImageIO.write((RenderedImage)object3, "png", path2.resolve("avatar.png").toFile());
            Path path3 = path.resolve("textures/assets/enorialshud/player_avatar_mask.png");
            if (Files.isRegularFile(path3, new LinkOption[0])) {
                object2 = ImageIO.read(path3.toFile());
                object = new BufferedImage(((BufferedImage)object2).getWidth(), ((BufferedImage)object2).getHeight(), 2);
                for (int i = 0; i < ((BufferedImage)object2).getHeight(); ++i) {
                    for (int j = 0; j < ((BufferedImage)object2).getWidth(); ++j) {
                        ((BufferedImage)object).setRGB(j, i, ((BufferedImage)object2).getRGB(j, i) & 0xFF000000 | ((BufferedImage)object3).getRGB(j * 40 / ((BufferedImage)object2).getWidth(), i * 40 / ((BufferedImage)object2).getHeight()) & 0xFFFFFF);
                    }
                }
                ImageIO.write((RenderedImage)object, "png", path2.resolve("portrait.png").toFile());
            }
            object2 = path.resolve("assets/font/enorialsavatar.yml").toFile();
            object = YamlConfiguration.loadConfiguration((File)object2);
            String string = "enorialsavatar.providers.avatar";
            object.set(string + ".type", (Object)"bitmap");
            object.set(string + ".texture", (Object)"assets/enorialshud/avatars/heads/avatar.png");
            object.set(string + ".height", (Object)40);
            object.set(string + ".ascent", (Object)5);
            object.set(string + ".chars", List.of("\ue100"));
            Files.createDirectories(((File)object2).toPath().getParent(), new FileAttribute[0]);
            object.save((File)object2);
            this.packs.invalidate();
        }
        catch (IOException iOException) {
            throw new UncheckedIOException("Cannot initialize shared avatar fonts", iOException);
        }
    }

    public void handleJoin(Player player) {
        if (!this.plugin.clientProfiles().usable(player)) {
            return;
        }
        this.skins.watch(player, this.plugin.cfg().getBoolean("avatars.refresh-on-join", false));
        if (this.plugin.singlePack() != null && this.plugin.singlePack().single()) {
            this.plugin.singlePack().join(player);
            return;
        }
        if (!PackHostingSetup.ready()) {
            PackHostingSetup.explain((CommandSender)player);
            return;
        }
        if (!this.plugin.cfg().getBoolean("avatars.send-on-join", this.plugin.cfg().getBoolean("avatars.rebuild-on-join", true))) {
            return;
        }
        this.plugin.packLoadGuard().preparing(player);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.requestPack(player, false), 20L);
    }

    public boolean refreshAvatar(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        this.skins.watch(player, true);
        return true;
    }

    public String getAvatarGlyph(Player player) {
        return "\ue100";
    }

    public Component paint(Player player, Component component) {
        return this.skins.paint(player, component);
    }

    public SkinHead head(UUID uUID) {
        return this.skins.head(uUID);
    }

    public void releaseNpc(UUID uUID) {
        this.skins.releaseMember(uUID);
    }

    public void invalidatePacks() {
        this.packs.invalidate();
    }

    public void prepareStaticPacks(PreparedPack preparedPack) throws Exception {
        if (!StaticPackHosting.enabled(this.plugin)) {
            return;
        }
        Path path = this.plugin.getDataFolder().toPath().resolve("hosting-export");
        JsonObject jsonObject = new JsonObject();
        JsonObject jsonObject2 = new JsonObject();
        for (ClientRenderProfile clientRenderProfile : this.plugin.clientProfiles().exportProfiles()) {
            PreparedPack preparedPack2 = this.plugin.packs().forPlayer(this.plugin.packComposition().snapshot(), clientRenderProfile);
            SharedAvatarPack.Entry entry = this.packs.get(preparedPack2, true);
            SharedAvatarPack.Entry entry2 = this.packs.get(preparedPack2, false);
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("minecraft", clientRenderProfile.minecraft());
            jsonObject3.addProperty("profile", preparedPack2.profile());
            jsonObject3.addProperty("hidden", entry.url());
            jsonObject3.addProperty("visible", entry2.url());
            jsonObject2.add(clientRenderProfile.key(), (JsonElement)jsonObject3);
            if (!clientRenderProfile.sameResources(preparedPack.client())) continue;
            jsonObject.addProperty("profile", preparedPack2.profile());
            jsonObject.addProperty("hidden", entry.url());
            jsonObject.addProperty("visible", entry2.url());
        }
        jsonObject.add("clients", (JsonElement)jsonObject2);
        Files.writeString(path.resolve("pack-index.json"), (CharSequence)jsonObject.toString(), new OpenOption[0]);
        Files.writeString(path.resolve("UPLOAD.txt"), (CharSequence)("Upload the generated folder to " + StaticPackHosting.base(this.plugin) + "/generated/\nKeep filenames unchanged. New players and skins never require another upload.\nclients in pack-index.json lists the exported resource profiles. Add wanted Minecraft versions to resource-pack.clients.prebuild, restart and upload again before those players join.\nUpload again after changing the HUD/plugin/resource-pack assets.\n"), new OpenOption[0]);
        this.plugin.getLogger().info("Static HUD packs ready in plugins/EnorialsHUD/hosting-export. Upload its generated folder to " + StaticPackHosting.base(this.plugin) + "/generated/");
    }

    public boolean requestPack(Player player, boolean bl) {
        if (this.plugin.singlePack() != null && this.plugin.singlePack().single()) {
            if (player == null || !player.isOnline()) {
                return false;
            }
            if (bl) {
                this.skins.watch(player, true);
            }
            return this.plugin.singlePack().request(player);
        }
        if (!PackHostingSetup.ready() || player == null || !player.isOnline() || this.plugin.packBuildQueue().isClosed()) {
            return false;
        }
        if (!this.plugin.clientProfiles().usable(player)) {
            return false;
        }
        ClientRenderProfile clientRenderProfile = ClientProfiles.of(player);
        if (bl) {
            this.skins.watch(player, true);
        }
        this.plugin.packLoadGuard().preparing(player);
        if (this.plugin.packs().generated().defer(player, bl)) {
            return true;
        }
        return this.plugin.packBuildQueue().submit(player, player, () -> {
            try {
                PreparedPack preparedPack = this.plugin.packs().forPlayer(this.plugin.packComposition().snapshot(), clientRenderProfile);
                Boolean bl = (Boolean)Bukkit.getScheduler().callSyncMethod((Plugin)this.plugin, () -> player.isOnline() ? Boolean.valueOf(HudAccess.hud(player) && this.plugin.storage().preference(player.getUniqueId(), "hud", true)) : null).get(10L, TimeUnit.SECONDS);
                if (bl == null) {
                    return;
                }
                SharedAvatarPack.Entry entry = this.packs.get(preparedPack, bl);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.send(player, entry, preparedPack.profile(), clientRenderProfile));
            }
            catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (this.plugin.packBuildQueue().isClosed()) {
                    return;
                }
                this.plugin.getLogger().log(Level.WARNING, "Cannot prepare shared resource pack", exception);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    if (player.isOnline()) {
                        this.plugin.packLoadGuard().fail(player, "Resource pack preparation failed. Please contact the server administrator.");
                    }
                });
            }
        });
    }

    private void send(Player player, SharedAvatarPack.Entry entry, String string, ClientRenderProfile clientRenderProfile) {
        boolean bl;
        if (!player.isOnline() || this.plugin.packBuildQueue().isClosed() || !clientRenderProfile.equals(ClientProfiles.of(player))) {
            return;
        }
        if (this.plugin.packs().generated().defer(player, false)) {
            return;
        }
        if (!this.plugin.packs().isReady() || !string.equals(this.plugin.packs().activeProfile(clientRenderProfile))) {
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.requestPack(player, false), 20L);
            return;
        }
        boolean bl2 = bl = HudAccess.hud(player) && this.plugin.storage().preference(player.getUniqueId(), "hud", true);
        if (bl != entry.hidden()) {
            this.requestPack(player, false);
            return;
        }
        if (this.plugin.vanillaBars() != null) {
            this.plugin.vanillaBars().basePackSent(player, entry.hidden());
        }
        String string2 = this.plugin.cfg().getString("resource-pack.prompt", "").trim();
        this.plugin.storage().setPackUrl(player.getUniqueId(), entry.url());
        this.plugin.packLoadGuard().expect(player, VanillaBars.MAIN_PACK_ID);
        PackTransport.main(player, VanillaBars.MAIN_PACK_ID, entry.url(), entry.hash(), (Component)(string2.isEmpty() ? null : Component.text((String)string2)), this.plugin.cfg().getBoolean("resource-pack.force", true));
    }

    @Override
    public void close() {
        this.skins.close();
    }
}

