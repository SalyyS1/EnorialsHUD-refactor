/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParser
 *  net.citizensnpcs.api.CitizensAPI
 *  net.citizensnpcs.api.npc.NPC
 *  net.citizensnpcs.trait.SkinTrait
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.betonquest;

import com.google.gson.JsonParser;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.party.TargetPanel;
import sk.enorials.hud.util.SpaceUtil;

public final class DialoguePortraits
implements AutoCloseable {
    private final EnorialsHUD hud;
    private final TargetPanel target;
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(128), runnable -> {
        Thread thread = new Thread(runnable, "Enorials-dialogue-portraits");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, CompletableFuture<BufferedImage>> cache = new LinkedHashMap<String, CompletableFuture<BufferedImage>>();
    private final Map<String, BufferedImage> atlases = new HashMap<String, BufferedImage>();
    private final YamlConfiguration definitions;
    private volatile boolean closed;

    public DialoguePortraits(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        this.target = new TargetPanel(enorialsHUD);
        this.definitions = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(Objects.requireNonNull(enorialsHUD.getResource("default/assets/font/target-heads.yml")), StandardCharsets.UTF_8));
    }

    public void resolve(Location location, Player player, Consumer<BufferedImage> consumer) {
        URL uRL;
        Player player2;
        if (location == null || location.getWorld() == null) {
            return;
        }
        LivingEntity livingEntity2 = location.getWorld().getNearbyLivingEntities(location, 1.5).stream().filter(livingEntity -> livingEntity != player && !(livingEntity instanceof ArmorStand)).min(Comparator.comparingDouble(livingEntity -> livingEntity.getLocation().distanceSquared(location))).orElse(null);
        if (livingEntity2 == null) {
            return;
        }
        Object object = null;
        if (livingEntity2 instanceof Player) {
            player2 = (Player)livingEntity2;
            uRL = player2.getPlayerProfile().getTextures().getSkin();
            if (uRL != null) {
                object = uRL.toExternalForm();
            }
            if (object == null && Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
                object = CitizensSkin.url((Entity)livingEntity2);
            }
            if (object == null) {
                return;
            }
        }
        player2 = object == null ? this.target.portraitGlyph(livingEntity2) : null;
        uRL = object;
        String string = object == null ? "mob/" + (String)player2 : "skin/" + (String)object;
        CompletableFuture<BufferedImage> completableFuture = this.cache.get(string);
        if (completableFuture == null) {
            if (this.cache.size() >= 128) {
                this.cache.remove(this.cache.keySet().iterator().next());
            }
            try {
                completableFuture = CompletableFuture.supplyAsync(() -> this.lambda$resolve$2((String)((Object)uRL), (String)player2), this.worker);
                this.cache.put(string, completableFuture);
            }
            catch (RejectedExecutionException rejectedExecutionException) {
                return;
            }
        }
        completableFuture.thenAccept(bufferedImage -> {
            if (bufferedImage != null && this.hud.isEnabled() && !this.closed) {
                Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
                    if (!this.closed) {
                        consumer.accept((BufferedImage)bufferedImage);
                    }
                });
            }
        });
    }

    private BufferedImage mob(String string) throws IOException {
        int n = Integer.parseInt(string, 16);
        ConfigurationSection configurationSection = this.definitions.getConfigurationSection("target-heads.providers");
        if (configurationSection == null) {
            return null;
        }
        for (String string2 : configurationSection.getKeys(false)) {
            ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string2);
            List list = configurationSection2.getStringList("chars");
            for (int i = 0; i < list.size(); ++i) {
                int[] nArray = ((String)list.get(i)).codePoints().toArray();
                for (int j = 0; j < nArray.length; ++j) {
                    if (nArray[j] != n) continue;
                    String string3 = configurationSection2.getString("texture");
                    BufferedImage bufferedImage = this.atlases.get(string3);
                    if (bufferedImage == null) {
                        try (InputStream inputStream = this.hud.getResource("default/textures/" + string3);){
                            if (inputStream == null) {
                                BufferedImage bufferedImage2 = null;
                                return bufferedImage2;
                            }
                            bufferedImage = ImageIO.read(inputStream);
                        }
                        this.atlases.put(string3, bufferedImage);
                    }
                    return DialoguePortraits.fit(bufferedImage.getSubimage(j * bufferedImage.getWidth() / nArray.length, i * bufferedImage.getHeight() / list.size(), bufferedImage.getWidth() / nArray.length, bufferedImage.getHeight() / list.size()));
                }
            }
        }
        return null;
    }

    private BufferedImage skin(String string) throws Exception {
        BufferedImage bufferedImage;
        URI uRI = URI.create(string);
        if (!"textures.minecraft.net".equalsIgnoreCase(uRI.getHost()) || !uRI.getPath().matches("/texture/[a-zA-Z0-9]+")) {
            return null;
        }
        String string2 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(("bust-v3-cover/" + string).getBytes(StandardCharsets.UTF_8)));
        Path path = this.hud.getDataFolder().toPath().resolve("cache/dialogue-portraits/" + string2 + ".png");
        if (Files.isRegularFile(path, new LinkOption[0])) {
            return ImageIO.read(path.toFile());
        }
        URLConnection uRLConnection = URI.create("https://textures.minecraft.net" + uRI.getPath()).toURL().openConnection();
        uRLConnection.setConnectTimeout(4000);
        uRLConnection.setReadTimeout(4000);
        try (Object object = uRLConnection.getInputStream();){
            byte[] byArray = ((InputStream)object).readNBytes(0x100001);
            if (byArray.length > 0x100000) {
                BufferedImage bufferedImage2 = null;
                return bufferedImage2;
            }
            bufferedImage = ImageIO.read(new ByteArrayInputStream(byArray));
        }
        if (bufferedImage == null || bufferedImage.getWidth() != 64 || bufferedImage.getHeight() < 32) {
            return null;
        }
        object = DialoguePortraits.renderSkin(bufferedImage);
        Files.createDirectories(path.getParent(), new FileAttribute[0]);
        ImageIO.write((RenderedImage)object, "png", path.toFile());
        return object;
    }

    static BufferedImage renderSkin(BufferedImage bufferedImage) {
        BufferedImage bufferedImage2 = new BufferedImage(128, 128, 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.scale(4.0, 4.0);
        graphics2D.drawImage(bufferedImage.getSubimage(20, 20, 8, 12), 6, 22, 21, 27, null);
        graphics2D.drawImage(bufferedImage.getSubimage(44, 20, 4, 12), 1, 23, 6, 23, null);
        graphics2D.drawImage(bufferedImage.getSubimage(bufferedImage.getHeight() >= 64 ? 36 : 44, bufferedImage.getHeight() >= 64 ? 52 : 20, 4, 12), 26, 22, 6, 23, null);
        if (bufferedImage.getHeight() >= 64) {
            graphics2D.drawImage(bufferedImage.getSubimage(20, 36, 8, 12), 6, 22, 21, 27, null);
            graphics2D.drawImage(bufferedImage.getSubimage(44, 36, 4, 12), 1, 23, 6, 23, null);
            graphics2D.drawImage(bufferedImage.getSubimage(52, 52, 4, 12), 26, 22, 6, 23, null);
        }
        DialoguePortraits.face(graphics2D, bufferedImage.getSubimage(8, 8, 8, 8), 5.0, 7.0, 22.0, 10.0, 5.0, 24.0);
        DialoguePortraits.face(graphics2D, bufferedImage.getSubimage(0, 8, 8, 8), 22.0, 10.0, 29.0, 6.0, 22.0, 27.0);
        DialoguePortraits.face(graphics2D, bufferedImage.getSubimage(8, 0, 8, 8), 5.0, 7.0, 22.0, 10.0, 12.0, 3.0);
        DialoguePortraits.face(graphics2D, bufferedImage.getSubimage(40, 8, 8, 8), 5.0, 7.0, 22.0, 10.0, 5.0, 24.0);
        DialoguePortraits.face(graphics2D, bufferedImage.getSubimage(32, 8, 8, 8), 22.0, 10.0, 29.0, 6.0, 22.0, 27.0);
        DialoguePortraits.face(graphics2D, bufferedImage.getSubimage(40, 0, 8, 8), 5.0, 7.0, 22.0, 10.0, 12.0, 3.0);
        graphics2D.dispose();
        bufferedImage2 = DialoguePortraits.fit(bufferedImage2);
        return bufferedImage2;
    }

    private static void face(Graphics2D graphics2D, BufferedImage bufferedImage, double d, double d2, double d3, double d4, double d5, double d6) {
        graphics2D.drawImage(bufferedImage, new AffineTransform((d3 - d) / 8.0, (d4 - d2) / 8.0, (d5 - d) / 8.0, (d6 - d2) / 8.0, d, d2), null);
    }

    private static BufferedImage fit(BufferedImage bufferedImage) {
        int n;
        int n2 = bufferedImage.getWidth();
        int n3 = bufferedImage.getHeight();
        int n4 = -1;
        int n5 = -1;
        int n6 = bufferedImage.getRGB(0, 0);
        int n7 = 0;
        int[] nArray = new int[]{bufferedImage.getRGB(bufferedImage.getWidth() - 1, 0), bufferedImage.getRGB(0, bufferedImage.getHeight() - 1), bufferedImage.getRGB(bufferedImage.getWidth() - 1, bufferedImage.getHeight() - 1)};
        int n8 = nArray.length;
        for (n = 0; n < n8; ++n) {
            int n9 = nArray[n];
            if (n9 >>> 24 != 255 || DialoguePortraits.distance(n6, n9) > 4) continue;
            ++n7;
        }
        boolean bl = n6 >>> 24 == 255 && n7 >= 1;
        for (n8 = 0; n8 < bufferedImage.getHeight(); ++n8) {
            for (n = 0; n < bufferedImage.getWidth(); ++n) {
                if (bufferedImage.getRGB(n, n8) >>> 24 < 80 || bl && DialoguePortraits.distance(bufferedImage.getRGB(n, n8), n6) <= 14) continue;
                n2 = Math.min(n2, n);
                n4 = Math.max(n4, n);
                n3 = Math.min(n3, n8);
                n5 = Math.max(n5, n8);
            }
        }
        BufferedImage bufferedImage2 = new BufferedImage(32, 36, 2);
        if (n4 < n2) {
            if (!bl) {
                return bufferedImage2;
            }
            n2 = 0;
            n3 = 0;
            n4 = bufferedImage.getWidth() - 1;
            n5 = bufferedImage.getHeight() - 1;
        }
        double d = Math.max(32.0 / (double)(n4 - n2 + 1), 36.0 / (double)(n5 - n3 + 1));
        int n10 = (int)Math.ceil((double)(n4 - n2 + 1) * d);
        int n11 = (int)Math.ceil((double)(n5 - n3 + 1) * d);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.drawImage(bufferedImage, (32 - n10) / 2, 0, (32 - n10) / 2 + n10, n11, n2, n3, n4 + 1, n5 + 1, null);
        graphics2D.dispose();
        return bufferedImage2;
    }

    private static int distance(int n, int n2) {
        return Math.max(Math.abs((n >> 16 & 0xFF) - (n2 >> 16 & 0xFF)), Math.max(Math.abs((n >> 8 & 0xFF) - (n2 >> 8 & 0xFF)), Math.abs((n & 0xFF) - (n2 & 0xFF))));
    }

    public static Component component(BufferedImage bufferedImage, int n, int n2, int n3) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < bufferedImage.getHeight(); ++i) {
            int n4 = 0;
            while (n4 < 32) {
                int n5 = bufferedImage.getRGB(n4, i);
                int n6 = DialoguePortraits.rgb565(n5);
                int n7 = n4++;
                while (n4 < 32 && DialoguePortraits.rgb565(bufferedImage.getRGB(n4, i)) == n6 && bufferedImage.getRGB(n4, i) >>> 24 >= 80 == n5 >>> 24 >= 80) {
                    ++n4;
                }
                if (n5 >>> 24 < 80) continue;
                char c = (char)(57600 + n2 + i * 2 + 256);
                String string = (String.valueOf(c) + "\uefff").repeat(n4 - n7);
                ((TextComponent.Builder)((TextComponent.Builder)builder.append(SpaceUtil.space(n + n7 * 2))).append(TextCompat.shadow((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)string), Key.key((String)"enorials:conversation/pixel"))).color(TextColor.color((int)(n3 / 4 << 16 | n6))), 0).decoration(TextDecoration.ITALIC, false))).append(SpaceUtil.space(-n - n4 * 2));
            }
        }
        return builder.asComponent();
    }

    private static int rgb565(int n) {
        return (n >> 19 & 0x1F) << 11 | (n >> 10 & 0x3F) << 5 | n >> 3 & 0x1F;
    }

    @Override
    public void close() {
        this.closed = true;
        this.worker.shutdownNow();
        this.cache.clear();
    }

    private /* synthetic */ BufferedImage lambda$resolve$2(String string, String string2) {
        try {
            return string == null ? this.mob(string2) : this.skin(string);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private static final class CitizensSkin {
        private CitizensSkin() {
        }

        static String url(Entity entity) {
            try {
                NPC nPC = CitizensAPI.getNPCRegistry().getNPC(entity);
                if (nPC == null || !nPC.hasTrait(SkinTrait.class)) {
                    return null;
                }
                String string = ((SkinTrait)nPC.getTraitNullable(SkinTrait.class)).getTexture();
                if (string == null) {
                    return null;
                }
                return JsonParser.parseString((String)new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            }
            catch (LinkageError | RuntimeException throwable) {
                return null;
            }
        }
    }
}

