/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.party;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.avatar.SkinHead;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.partyhud.party.Party;

public final class PartyMapTracker
implements Runnable {
    public static final String PREFIX = "__party::";
    private final EnorialsHUD plugin;
    private final MinimapIcon[] icons = new MinimapIcon[8];

    public PartyMapTracker(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        for (int i = 0; i < this.icons.length; ++i) {
            try (InputStream inputStream = enorialsHUD.getResource("party-map/party_" + (i + 1) + ".png");){
                if (inputStream == null) {
                    throw new IOException("Missing party marker " + (i + 1));
                }
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                BufferedImage bufferedImage2 = new BufferedImage(10, 10, 2);
                Graphics2D graphics2D = bufferedImage2.createGraphics();
                graphics2D.drawImage(bufferedImage, 0, 0, 10, 10, null);
                graphics2D.dispose();
                this.icons[i] = MinimapIcon.fromBufferedImage("party_" + (i + 1), bufferedImage2);
                continue;
            }
            catch (IOException iOException) {
                throw new IllegalStateException("Cannot load bundled party marker", iOException);
            }
        }
    }

    public static List<UUID> members(Party party) {
        ArrayList<UUID> arrayList = new ArrayList<UUID>(party.getMembers());
        arrayList.sort(Comparator.comparingInt(uUID -> uUID.equals(party.getLeader()) ? 0 : 1).thenComparing(UUID::toString));
        return arrayList;
    }

    /*
     * Could not resolve type clashes
     * Unable to fully structure code
     */
    @Override
    public void run() {
        for (Player var2_2 : Bukkit.getOnlinePlayers()) {
            var3_3 = this.plugin.minimap().getPlayerMinimap(var2_2);
            if (var3_3 == null) continue;
            var4_4 = this.plugin.getPartyService().getPartyOf(var2_2.getUniqueId()).orElse(null);
            var5_5 = var4_4 == null || HudAccess.has((CommandSender)var2_2, "enorials.party.map") == false ? List.of() : PartyMapTracker.members(var4_4);
            var6_6 = new HashSet<CallSite>();
            for (var7_8 = 0; var7_8 < var5_5.size() && var7_8 < 8; ++var7_8) {
                var8_9 = (UUID)var5_5.get(var7_8);
                var9_10 = this.plugin.api().resolveMember((UUID)var8_9);
                if (var9_10 == null || var9_10.equals((Object)var2_2) || !var9_10.getWorld().equals((Object)var2_2.getWorld())) continue;
                var10_11 = "__party::" + String.valueOf(var8_9);
                var6_6.add((CallSite)var10_11);
                var11_12 = var3_3.secondaryLayers().get(var10_11);
                if (var11_12 instanceof Layer existing
                    && existing.index == var7_8 + 1
                    && existing.getWorld().equals(var9_10.getWorld())) {
                    var12_13 = existing;
                } else {
                    if (var11_12 != null) {
                        this.plugin.minimap().packetSender().despawnLayer(var2_2, var11_12.getBaseLayer());
                    }
                    var14_15 = this.plugin.minimap().clientsideMinimapFactory().createMinimapLayer(var2_2.getWorld(), null);
                    var12_13 = new Layer(var14_15, var9_10.getLocation(), this.icons[var7_8], var7_8 + 1);
                    var3_3.secondaryLayers().put(var10_11, var12_13);
                    this.plugin.minimap().packetSender().spawnLayer(var2_2, var14_15);
                }
                var13_14 = this.plugin.avatars().head((UUID)var8_9);
                if (var13_14 != var12_13.head) {
                    var12_13.head = var13_14;
                    var12_13.setRenderer(new MinimapIconRenderer(var13_14 == null ? this.icons[var7_8] : var13_14.icon("party_" + (var7_8 + 1))));
                }
                var12_13.sample(var9_10.getLocation(), var2_2.getWorld().getGameTime());
                var3_3.updateSecondaryLayer((MinimapProvider)this.plugin.minimap(), var10_11);
            }
            for (Object var8_9 : List.copyOf(var3_3.secondaryLayers().keySet())) {
                if (!var8_9.startsWith("__party::") || var6_6.contains(var8_9) || (var9_10 = (SecondaryMinimapLayer)var3_3.secondaryLayers().remove(var8_9)) == null) continue;
                this.plugin.minimap().packetSender().despawnLayer(var2_2, var9_10.getBaseLayer());
            }
        }
    }

    public static final class Layer
    extends SecondaryMinimapLayer {
        public final int index;
        public double x;
        public double z;
        public double previousX;
        public double previousZ;
        private long tick = Long.MIN_VALUE;
        public SkinHead head;

        Layer(MinimapLayer minimapLayer, Location location, MinimapIcon minimapIcon, int n) {
            super(minimapLayer, new MinimapIconRenderer(minimapIcon), true, true, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.08f);
            this.index = n;
            this.x = this.previousX = location.getX();
            this.z = this.previousZ = location.getZ();
        }

        void sample(Location location, long l) {
            if (this.tick != l) {
                this.previousX = this.x;
                this.previousZ = this.z;
            }
            this.x = location.getX();
            this.z = location.getZ();
            if (l - this.tick != 1L || Math.hypot(this.x - this.previousX, this.z - this.previousZ) > 8.0) {
                this.previousX = this.x;
                this.previousZ = this.z;
            }
            this.tick = l;
            this.setPositionX(location.getBlockX());
            this.setPositionZ(location.getBlockZ());
        }
    }
}

