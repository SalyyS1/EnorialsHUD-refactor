/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.AreaEffectCloud
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.TNTPrimed
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.AreaEffectCloudApplyEvent
 *  org.bukkit.event.entity.EntityAirChangeEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.entity.PotionSplashEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent$Status
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerVelocityEvent
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitTask
 */
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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.compat.PackTransport;

public class ResourcePackGuard
implements Listener,
AutoCloseable {
    private static final boolean DAMAGE_SOURCE = Arrays.stream(EntityDamageEvent.class.getMethods()).anyMatch(method -> method.getName().equals("getDamageSource"));
    private static final Component WAIT = Component.text((String)"Please wait while your resource pack is prepared and loaded...", (TextColor)NamedTextColor.YELLOW);
    private final UUID mainPackId;
    private final JavaPlugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<UUID, Pending>();
    private final BukkitTask task;

    public static boolean isLoading(Player player) {
        return player.hasMetadata("enorials.pack-loading");
    }

    public ResourcePackGuard(JavaPlugin javaPlugin, UUID uUID) {
        this.mainPackId = uUID;
        this.plugin = javaPlugin;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)javaPlugin);
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)javaPlugin, () -> {
            for (UUID uUID : List.copyOf(this.pending.keySet())) {
                Player player = Bukkit.getPlayer((UUID)uUID);
                Pending pending = this.pending.get(uUID);
                if (player == null) {
                    this.pending.remove(uUID);
                    continue;
                }
                if (pending.queuePosition > 0) {
                    player.sendActionBar(ResourcePackGuard.queueMessage(pending.queuePosition));
                    continue;
                }
                if (System.currentTimeMillis() - pending.started > 300000L) {
                    this.fail(player, "Resource pack loading timed out. Please reconnect and try again.");
                    continue;
                }
                player.sendActionBar(WAIT);
            }
        }, 1L, 20L);
    }

    public boolean isWaiting(Player player) {
        return this.pending.containsKey(player.getUniqueId());
    }

    private Pending state(Player player) {
        return this.pending.computeIfAbsent(player.getUniqueId(), uUID -> {
            player.sendMessage(WAIT);
            player.setMetadata("enorials.pack-loading", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
            return new Pending();
        });
    }

    public void preparing(Player player) {
        this.state((Player)player).preparing = true;
    }

    private static Component queueMessage(int n) {
        return Component.text((String)("Resource pack queue: #" + n + ". Please wait for your turn..."), (TextColor)NamedTextColor.YELLOW);
    }

    public void queuePosition(Player player, int n) {
        Pending pending = this.pending.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (n >= 0 && n != pending.queuePosition) {
            pending.queuePosition = n;
            pending.started = System.currentTimeMillis();
            if (n > 0) {
                player.sendMessage(ResourcePackGuard.queueMessage(n));
            } else {
                player.sendMessage((Component)Component.text((String)"It is your turn. Preparing your resource pack...", (TextColor)NamedTextColor.AQUA));
            }
        }
    }

    public void expect(Player player, UUID uUID) {
        Pending pending = this.state(player);
        pending.packs.add(uUID);
        if (uUID.equals(this.mainPackId)) {
            pending.preparing = false;
            pending.queuePosition = -1;
            pending.started = System.currentTimeMillis();
        }
    }

    public void complete(Player player, UUID uUID) {
        Pending pending = this.pending.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        pending.packs.remove(uUID);
        if (!pending.preparing && pending.packs.isEmpty()) {
            this.pending.remove(player.getUniqueId());
            player.removeMetadata("enorials.pack-loading", (Plugin)this.plugin);
            player.setFallDistance(0.0f);
            player.sendActionBar((Component)Component.text((String)"Resource pack ready.", (TextColor)NamedTextColor.GREEN));
        }
    }

    public void fail(Player player, String string) {
        if (!this.isWaiting(player)) {
            return;
        }
        player.kick((Component)Component.text((String)string, (TextColor)NamedTextColor.YELLOW));
        this.pending.remove(player.getUniqueId());
        player.removeMetadata("enorials.pack-loading", (Plugin)this.plugin);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void status(PlayerResourcePackStatusEvent playerResourcePackStatusEvent) {
        Pending pending = this.pending.get(playerResourcePackStatusEvent.getPlayer().getUniqueId());
        UUID uUID = PackTransport.id(playerResourcePackStatusEvent, this.mainPackId);
        if (pending == null || !pending.packs.contains(uUID)) {
            return;
        }
        switch (playerResourcePackStatusEvent.getStatus()) {
            case SUCCESSFULLY_LOADED: {
                this.complete(playerResourcePackStatusEvent.getPlayer(), uUID);
                break;
            }
            case DECLINED: 
            case FAILED_DOWNLOAD: 
            case FAILED_RELOAD: 
            case INVALID_URL: 
            case DISCARDED: {
                this.plugin.getLogger().warning("Resource pack status=" + playerResourcePackStatusEvent.getStatus().name() + " player=" + playerResourcePackStatusEvent.getPlayer().getName() + " pack=" + String.valueOf(uUID));
                String string = switch (playerResourcePackStatusEvent.getStatus()) {
                    case PlayerResourcePackStatusEvent.Status.DECLINED -> "This server requires its resource pack. Enable server resource packs for this server and reconnect.";
                    case PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD -> "The server resource pack could not be downloaded. Please reconnect; if it repeats, contact the server administrator.";
                    case PlayerResourcePackStatusEvent.Status.FAILED_RELOAD -> "Minecraft could not apply the server resource pack. Restart Minecraft and reconnect; the client log contains details if it repeats.";
                    case PlayerResourcePackStatusEvent.Status.INVALID_URL -> "The server resource pack address is invalid. Please contact the server administrator.";
                    default -> "Minecraft discarded the server resource pack before loading it. Please reconnect; if it repeats, contact the server administrator.";
                };
                this.fail(playerResourcePackStatusEvent.getPlayer(), string);
                break;
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void move(PlayerMoveEvent playerMoveEvent) {
        if (!(playerMoveEvent instanceof PlayerTeleportEvent) && this.isWaiting(playerMoveEvent.getPlayer()) && playerMoveEvent.hasChangedPosition()) {
            Location location = playerMoveEvent.getFrom().clone();
            location.setYaw(playerMoveEvent.getTo().getYaw());
            location.setPitch(playerMoveEvent.getTo().getPitch());
            playerMoveEvent.setTo(location);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void velocity(PlayerVelocityEvent playerVelocityEvent) {
        if (this.isWaiting(playerVelocityEvent.getPlayer())) {
            playerVelocityEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void earlyDamage(EntityDamageEvent entityDamageEvent) {
        this.damage(entityDamageEvent);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void damage(EntityDamageEvent entityDamageEvent) {
        Player player;
        Entity entity = entityDamageEvent.getEntity();
        if (entity instanceof Player && this.isWaiting(player = (Player)entity)) {
            entityDamageEvent.setCancelled(true);
            return;
        }
        if (DAMAGE_SOURCE && (this.blockedSource(entityDamageEvent.getDamageSource().getCausingEntity()) || this.blockedSource(entityDamageEvent.getDamageSource().getDirectEntity()))) {
            entityDamageEvent.setCancelled(true);
            return;
        }
        if (entityDamageEvent instanceof EntityDamageByEntityEvent) {
            Projectile projectile;
            ProjectileSource projectileSource;
            player = (EntityDamageByEntityEvent)entityDamageEvent;
            entity = player.getDamager();
            if (entity instanceof Projectile && (projectileSource = (projectile = (Projectile)entity).getShooter()) instanceof Entity) {
                Entity entity2;
                entity = entity2 = (Entity)projectileSource;
            }
            if (entity instanceof Player && this.isWaiting((Player)(projectile = (Player)entity))) {
                entityDamageEvent.setCancelled(true);
            }
        }
    }

    private boolean blockedSource(Entity entity) {
        Projectile projectile;
        ProjectileSource projectileSource;
        if (entity instanceof Player) {
            Player player = (Player)entity;
            return this.isWaiting(player);
        }
        if (entity instanceof Projectile && (projectileSource = (projectile = (Projectile)entity).getShooter()) instanceof Player) {
            Player player = (Player)projectileSource;
            return this.isWaiting(player);
        }
        if (entity instanceof TNTPrimed && (projectileSource = (projectile = (TNTPrimed)entity).getSource()) instanceof Player) {
            Player player = (Player)projectileSource;
            return this.isWaiting(player);
        }
        if (entity instanceof AreaEffectCloud && (projectileSource = (projectile = (AreaEffectCloud)entity).getSource()) instanceof Player) {
            Player player = (Player)projectileSource;
            return this.isWaiting(player);
        }
        return false;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void food(FoodLevelChangeEvent foodLevelChangeEvent) {
        Player player;
        HumanEntity humanEntity = foodLevelChangeEvent.getEntity();
        if (humanEntity instanceof Player && this.isWaiting(player = (Player)humanEntity)) {
            foodLevelChangeEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void air(EntityAirChangeEvent entityAirChangeEvent) {
        Player player;
        Entity entity = entityAirChangeEvent.getEntity();
        if (entity instanceof Player && this.isWaiting(player = (Player)entity)) {
            entityAirChangeEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void interact(PlayerInteractEvent playerInteractEvent) {
        if (this.isWaiting(playerInteractEvent.getPlayer())) {
            playerInteractEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void interactEntity(PlayerInteractEntityEvent playerInteractEntityEvent) {
        if (this.isWaiting(playerInteractEntityEvent.getPlayer())) {
            playerInteractEntityEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void breakBlock(BlockBreakEvent blockBreakEvent) {
        if (this.isWaiting(blockBreakEvent.getPlayer())) {
            blockBreakEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void placeBlock(BlockPlaceEvent blockPlaceEvent) {
        if (this.isWaiting(blockPlaceEvent.getPlayer())) {
            blockPlaceEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void launch(ProjectileLaunchEvent projectileLaunchEvent) {
        Player player;
        ProjectileSource projectileSource = projectileLaunchEvent.getEntity().getShooter();
        if (projectileSource instanceof Player && this.isWaiting(player = (Player)projectileSource)) {
            projectileLaunchEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void bow(EntityShootBowEvent entityShootBowEvent) {
        Player player;
        LivingEntity livingEntity = entityShootBowEvent.getEntity();
        if (livingEntity instanceof Player && this.isWaiting(player = (Player)livingEntity)) {
            entityShootBowEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void potion(PotionSplashEvent potionSplashEvent) {
        Player player;
        ProjectileSource projectileSource = potionSplashEvent.getPotion().getShooter();
        if (projectileSource instanceof Player && this.isWaiting(player = (Player)projectileSource)) {
            potionSplashEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void cloud(AreaEffectCloudApplyEvent areaEffectCloudApplyEvent) {
        Player player;
        ProjectileSource projectileSource = areaEffectCloudApplyEvent.getEntity().getSource();
        if (projectileSource instanceof Player && this.isWaiting(player = (Player)projectileSource)) {
            areaEffectCloudApplyEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.pending.remove(playerQuitEvent.getPlayer().getUniqueId());
        playerQuitEvent.getPlayer().removeMetadata("enorials.pack-loading", (Plugin)this.plugin);
    }

    @Override
    public void close() {
        this.task.cancel();
        HandlerList.unregisterAll((Listener)this);
        for (UUID uUID : this.pending.keySet()) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            player.removeMetadata("enorials.pack-loading", (Plugin)this.plugin);
        }
        this.pending.clear();
    }

    private static final class Pending {
        long started = System.currentTimeMillis();
        int queuePosition = -1;
        final Set<UUID> packs = new HashSet<UUID>();
        boolean preparing;

        private Pending() {
        }
    }
}

