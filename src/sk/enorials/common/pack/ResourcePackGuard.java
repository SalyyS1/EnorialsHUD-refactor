package sk.enorials.common.pack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.compat.PackTransport;

public class ResourcePackGuard implements Listener, AutoCloseable {
   private static final boolean DAMAGE_SOURCE = Arrays.stream(EntityDamageEvent.class.getMethods()).anyMatch(var0 -> var0.getName().equals("getDamageSource"));
   private static final Component WAIT = Component.text("Please wait while your resource pack is prepared and loaded...", NamedTextColor.YELLOW);
   private final UUID mainPackId;
   private final JavaPlugin plugin;
   private final Map<UUID, ResourcePackGuard.Pending> pending = new HashMap<>();
   private final BukkitTask task;

   public static boolean isLoading(Player var0) {
      return var0.hasMetadata("enorials.pack-loading");
   }

   public ResourcePackGuard(JavaPlugin var1, UUID var2) {
      this.mainPackId = var2;
      this.plugin = var1;
      Bukkit.getPluginManager().registerEvents(this, var1);
      this.task = Bukkit.getScheduler().runTaskTimer(var1, () -> {
         for (UUID var2x : List.copyOf(this.pending.keySet())) {
            Player var3 = Bukkit.getPlayer(var2x);
            ResourcePackGuard.Pending var4 = this.pending.get(var2x);
            if (var3 == null) {
               this.pending.remove(var2x);
            } else if (var4.queuePosition > 0) {
               var3.sendActionBar(queueMessage(var4.queuePosition));
            } else if (System.currentTimeMillis() - var4.started > 300000L) {
               this.fail(var3, "Resource pack loading timed out. Please reconnect and try again.");
            } else {
               var3.sendActionBar(WAIT);
            }
         }
      }, 1L, 20L);
   }

   public boolean isWaiting(Player var1) {
      return this.pending.containsKey(var1.getUniqueId());
   }

   private ResourcePackGuard.Pending state(Player var1) {
      return this.pending.computeIfAbsent(var1.getUniqueId(), var2 -> {
         var1.sendMessage(WAIT);
         var1.setMetadata("enorials.pack-loading", new FixedMetadataValue(this.plugin, true));
         return new ResourcePackGuard.Pending();
      });
   }

   public void preparing(Player var1) {
      this.state(var1).preparing = true;
   }

   private static Component queueMessage(int var0) {
      return Component.text("Resource pack queue: #" + var0 + ". Please wait for your turn...", NamedTextColor.YELLOW);
   }

   public void queuePosition(Player var1, int var2) {
      ResourcePackGuard.Pending var3 = this.pending.get(var1.getUniqueId());
      if (var3 != null) {
         if (var2 >= 0 && var2 != var3.queuePosition) {
            var3.queuePosition = var2;
            var3.started = System.currentTimeMillis();
            if (var2 > 0) {
               var1.sendMessage(queueMessage(var2));
            } else {
               var1.sendMessage(Component.text("It is your turn. Preparing your resource pack...", NamedTextColor.AQUA));
            }
         }
      }
   }

   public void expect(Player var1, UUID var2) {
      ResourcePackGuard.Pending var3 = this.state(var1);
      var3.packs.add(var2);
      if (var2.equals(this.mainPackId)) {
         var3.preparing = false;
         var3.queuePosition = -1;
         var3.started = System.currentTimeMillis();
      }
   }

   public void complete(Player var1, UUID var2) {
      ResourcePackGuard.Pending var3 = this.pending.get(var1.getUniqueId());
      if (var3 != null) {
         var3.packs.remove(var2);
         if (!var3.preparing && var3.packs.isEmpty()) {
            this.pending.remove(var1.getUniqueId());
            var1.removeMetadata("enorials.pack-loading", this.plugin);
            var1.setFallDistance(0.0F);
            var1.sendActionBar(Component.text("Resource pack ready.", NamedTextColor.GREEN));
         }
      }
   }

   public void fail(Player var1, String var2) {
      if (this.isWaiting(var1)) {
         var1.kick(Component.text(var2, NamedTextColor.YELLOW));
         this.pending.remove(var1.getUniqueId());
         var1.removeMetadata("enorials.pack-loading", this.plugin);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void status(PlayerResourcePackStatusEvent var1) {
      ResourcePackGuard.Pending var2 = this.pending.get(var1.getPlayer().getUniqueId());
      UUID var3 = PackTransport.id(var1, this.mainPackId);
      if (var2 != null && var2.packs.contains(var3)) {
         switch (var1.getStatus()) {
            case DECLINED:
            case FAILED_DOWNLOAD:
            case FAILED_RELOAD:
            case INVALID_URL:
            case DISCARDED:
               this.plugin.getLogger().warning("Resource pack status=" + var1.getStatus().name() + " player=" + var1.getPlayer().getName() + " pack=" + var3);

               String var4 = switch (var1.getStatus()) {
                  case DECLINED -> "This server requires its resource pack. Enable server resource packs for this server and reconnect.";
                  case FAILED_DOWNLOAD -> "The server resource pack could not be downloaded. Please reconnect; if it repeats, contact the server administrator.";
                  case FAILED_RELOAD -> "Minecraft could not apply the server resource pack. Restart Minecraft and reconnect; the client log contains details if it repeats.";
                  case INVALID_URL -> "The server resource pack address is invalid. Please contact the server administrator.";
                  default -> "Minecraft discarded the server resource pack before loading it. Please reconnect; if it repeats, contact the server administrator.";
               };
               this.fail(var1.getPlayer(), var4);
               break;
            case SUCCESSFULLY_LOADED:
               this.complete(var1.getPlayer(), var3);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void move(PlayerMoveEvent var1) {
      if (!(var1 instanceof PlayerTeleportEvent) && this.isWaiting(var1.getPlayer()) && var1.hasChangedPosition()) {
         Location var2 = var1.getFrom().clone();
         var2.setYaw(var1.getTo().getYaw());
         var2.setPitch(var1.getTo().getPitch());
         var1.setTo(var2);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void velocity(PlayerVelocityEvent var1) {
      if (this.isWaiting(var1.getPlayer())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void earlyDamage(EntityDamageEvent var1) {
      this.damage(var1);
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void damage(EntityDamageEvent var1) {
      if (var1.getEntity() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      } else if (!DAMAGE_SOURCE
         || !this.blockedSource(var1.getDamageSource().getCausingEntity()) && !this.blockedSource(var1.getDamageSource().getDirectEntity())) {
         if (var1 instanceof EntityDamageByEntityEvent var7) {
            Entity var8 = var7.getDamager();
            if (var8 instanceof Projectile var4 && var4.getShooter() instanceof Entity var5) {
               var8 = var5;
            }

            if (var8 instanceof Player var9 && this.isWaiting(var9)) {
               var1.setCancelled(true);
            }
         }
      } else {
         var1.setCancelled(true);
      }
   }

   private boolean blockedSource(Entity var1) {
      if (var1 instanceof Player var7) {
         return this.isWaiting(var7);
      } else if (var1 instanceof Projectile var2 && var2.getShooter() instanceof Player var9) {
         return this.isWaiting(var9);
      } else if (var1 instanceof TNTPrimed var5 && var5.getSource() instanceof Player var8) {
         return this.isWaiting(var8);
      } else {
         return var1 instanceof AreaEffectCloud var6 && var6.getSource() instanceof Player var3 ? this.isWaiting(var3) : false;
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void food(FoodLevelChangeEvent var1) {
      if (var1.getEntity() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void air(EntityAirChangeEvent var1) {
      if (var1.getEntity() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void interact(PlayerInteractEvent var1) {
      if (this.isWaiting(var1.getPlayer())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void interactEntity(PlayerInteractEntityEvent var1) {
      if (this.isWaiting(var1.getPlayer())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void breakBlock(BlockBreakEvent var1) {
      if (this.isWaiting(var1.getPlayer())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void placeBlock(BlockPlaceEvent var1) {
      if (this.isWaiting(var1.getPlayer())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void launch(ProjectileLaunchEvent var1) {
      if (var1.getEntity().getShooter() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void bow(EntityShootBowEvent var1) {
      if (var1.getEntity() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void potion(PotionSplashEvent var1) {
      if (var1.getPotion().getShooter() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void cloud(AreaEffectCloudApplyEvent var1) {
      if (var1.getEntity().getSource() instanceof Player var2 && this.isWaiting(var2)) {
         var1.setCancelled(true);
      }
   }

   @EventHandler
   public void quit(PlayerQuitEvent var1) {
      this.pending.remove(var1.getPlayer().getUniqueId());
      var1.getPlayer().removeMetadata("enorials.pack-loading", this.plugin);
   }

   @Override
   public void close() {
      this.task.cancel();
      HandlerList.unregisterAll(this);

      for (UUID var2 : this.pending.keySet()) {
         Player var3 = Bukkit.getPlayer(var2);
         if (var3 != null) {
            var3.removeMetadata("enorials.pack-loading", this.plugin);
         }
      }

      this.pending.clear();
   }

   private static final class Pending {
      long started = System.currentTimeMillis();
      int queuePosition = -1;
      final Set<UUID> packs = new HashSet<>();
      boolean preparing;
   }
}
