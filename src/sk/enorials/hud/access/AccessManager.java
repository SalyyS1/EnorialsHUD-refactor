package sk.enorials.hud.access;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.map.Minimap;

public final class AccessManager implements Listener, AutoCloseable {
   private final EnorialsHUD hud;
   private final File file;
   private YamlConfiguration config;
   private final Map<UUID, Map<String, Optional<Boolean>>> resolved = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> signatures = new HashMap<>();
   private BukkitTask task;

   public AccessManager(EnorialsHUD var1) {
      this.hud = var1;
      this.file = new File(var1.getDataFolder(), "access.yml");
      if (!this.file.exists()) {
         var1.saveResource("access.yml", false);
      }

      this.reload();
      HudAccess.install(this);
      Bukkit.getPluginManager().registerEvents(this, var1);
      this.task = Bukkit.getScheduler().runTaskTimer(var1, this::refresh, 20L, 20L);
   }

   public synchronized void reload() {
      this.config = YamlConfiguration.loadConfiguration(this.file);
      this.resolved.clear();
   }

   private void save() {
      try {
         this.config.save(this.file);
         this.resolved.clear();
      } catch (IOException var2) {
         throw new IllegalStateException("Cannot save EnorialsHUD access.yml", var2);
      }
   }

   Boolean local(UUID var1, String var2) {
      Map var3 = this.resolved.computeIfAbsent(var1, var0 -> new ConcurrentHashMap<>());
      Optional var4 = (Optional)var3.get(var2);
      if (var4 == null) {
         var4 = Optional.ofNullable(this.resolve(var1, var2));
         var3.put(var2, var4);
      }

      return (Boolean)var4.orElse(null);
   }

   private synchronized Boolean resolve(UUID var1, String var2) {
      Boolean var3 = this.rule("players." + var1, var2);
      return var3 != null ? var3 : this.rule("defaults", var2);
   }

   private Boolean rule(String var1, String var2) {
      List var3 = this.config.getStringList(var1 + ".deny");
      List var4 = this.config.getStringList(var1 + ".grant");
      if (var3.contains(var2)) {
         return false;
      }

      if (var4.contains(var2)) {
         return true;
      }

      for (String var6 : var3) {
         if (HudAccess.includes(var6, var2, new HashSet<>())) {
            return false;
         }
      }

      for (String var8 : var4) {
         if (HudAccess.includes(var8, var2, new HashSet<>())) {
            return true;
         }
      }

      return null;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public synchronized void join(PlayerJoinEvent var1) {
      Player var2 = var1.getPlayer();
      String var3 = "players." + var2.getUniqueId();
      if (!this.config.getBoolean(var3 + ".first-join-processed", false)) {
         this.config.set(var3 + ".name", var2.getName());
         this.config.set(var3 + ".first-join-processed", true);
         if (!var2.hasPlayedBefore()) {
            LinkedHashSet var4 = new LinkedHashSet(this.config.getStringList(var3 + ".grant"));

            for (String var6 : this.config.getStringList("first-join.grant")) {
               if (this.known(var6)) {
                  var4.add(var6);
               } else {
                  this.hud.getLogger().warning("Unknown first-join permission: " + var6);
               }
            }

            this.config.set(var3 + ".grant", new ArrayList(var4));
         }

         this.save();
      }
   }

   private boolean known(String var1) {
      return (var1.startsWith("enorials.") || var1.startsWith("enorialshud.")) && Bukkit.getPluginManager().getPermission(var1) != null;
   }

   public synchronized boolean command(CommandSender var1, String[] var2) {
      if (!HudAccess.check(var1, "enorials.hud.admin")) {
         return true;
      }

      try {
         if (var2.length == 1 && var2[0].equalsIgnoreCase("reload")) {
            this.reload();
            var1.sendMessage("EnorialsHUD: access.yml reloaded.");
            return true;
         }

         if (var2.length == 1 && var2[0].equalsIgnoreCase("list")) {
            this.hud.getDescription().getPermissions().stream().map(Permission::getName).sorted().forEach(var1::sendMessage);
            return true;
         }

         String var3;
         byte var4;
         if (var2.length == 3 && var2[0].equalsIgnoreCase("default")) {
            var3 = "defaults";
            var4 = 1;
         } else {
            if (var2.length != 4 || !var2[0].equalsIgnoreCase("player")) {
               throw new IllegalArgumentException(
                  "Usage: /enohud access list | reload | default <allow|deny|unset> <node> | player <name|UUID> <allow|deny|unset> <node>"
               );
            }

            UUID var5;
            try {
               var5 = UUID.fromString(var2[1]);
            } catch (IllegalArgumentException var9) {
               OfflinePlayer var7 = Bukkit.getOfflinePlayerIfCached(var2[1]);
               if (var7 == null) {
                  throw new IllegalArgumentException("Unknown player; use an online/cached player name or UUID.");
               }

               var5 = var7.getUniqueId();
            }

            var3 = "players." + var5;
            var4 = 2;
         }

         String var11 = var2[var4].toLowerCase(Locale.ROOT);
         String var6 = var2[var4 + 1].toLowerCase(Locale.ROOT);
         if (!Set.of("allow", "deny", "unset").contains(var11) || !this.known(var6)) {
            throw new IllegalArgumentException("Use allow, deny or unset and a known EnorialsHUD permission (access list).");
         }

         LinkedHashSet var12 = new LinkedHashSet(this.config.getStringList(var3 + ".grant"));
         LinkedHashSet var8 = new LinkedHashSet(this.config.getStringList(var3 + ".deny"));
         var12.remove(var6);
         var8.remove(var6);
         if (var11.equals("allow")) {
            var12.add(var6);
         }

         if (var11.equals("deny")) {
            var8.add(var6);
         }

         this.config.set(var3 + ".grant", new ArrayList(var12));
         this.config.set(var3 + ".deny", new ArrayList(var8));
         this.save();
         var1.sendMessage("EnorialsHUD: " + var3 + " " + var11 + " " + var6 + ". Explicit Bukkit/LuckPerms rules take precedence.");
      } catch (IllegalArgumentException var10) {
         var1.sendMessage("EnorialsHUD: " + var10.getMessage());
      }

      return true;
   }

   private void refresh() {
      if (this.hud.minimap() != null) {
         for (Player var2 : Bukkit.getOnlinePlayers()) {
            int var3 = 1;

            for (Permission var5 : this.hud.getDescription().getPermissions()) {
               var3 = 31 * var3 + (HudAccess.has(var2, var5.getName()) ? 1 : 0);
            }

            Integer var9 = this.signatures.put(var2.getUniqueId(), var3);
            Minimap var10 = this.hud.getPlayerMinimap(var2);
            if (var10 != null && !HudAccess.map(var2)) {
               this.hud.queueMinimapSave(var10);
               this.hud.minimap().minimapListener().disableMinimap(var2);
            } else if (var10 != null && !Objects.equals(var9, var3)) {
               var10.update(this.hud.minimap());
               if (this.hud.minimap().packetSender() instanceof GuiMinimapPacketSender var6) {
                  var6.refreshPermissions(var2);
               }
            } else if (var10 == null && var9 != null && !Objects.equals(var9, var3) && HudAccess.map(var2)) {
               try {
                  this.hud.minimap().playerDataStorage().restore(this.hud.minimap(), var2);
               } catch (Exception var8) {
                  this.hud.getLogger().log(Level.WARNING, "Could not restore minimap after access change", var8);
               }
            }

            if (this.hud.getFullscreenMinimap(var2) != null
               && (!HudAccess.map(var2) || !HudAccess.has(var2, "enorials.minimap.fullscreen") || var9 != null && !Objects.equals(var9, var3))) {
               this.hud.minimap().minimapListener().closeFullscreen(var2);
            }
         }
      }
   }

   @EventHandler
   public void quit(PlayerQuitEvent var1) {
      this.resolved.remove(var1.getPlayer().getUniqueId());
      this.signatures.remove(var1.getPlayer().getUniqueId());
   }

   @Override
   public void close() {
      if (this.task != null) {
         this.task.cancel();
      }

      HandlerList.unregisterAll(this);
      this.resolved.clear();
      HudAccess.install(null);
   }
}
