package sk.enorials.gps.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;

public final class GpsApiCommand implements CommandExecutor, TabCompleter {
   private final NavigationManager navigationManager;
   private final IntegrationManager integrationManager;
   private static final String DYNAMIC_ROUTE_SOURCE = "bq-dynamic";

   public GpsApiCommand(NavigationManager var1, IntegrationManager var2) {
      this.navigationManager = var1;
      this.integrationManager = var2;
   }

   public boolean onCommand(@NotNull CommandSender var1, @NotNull Command var2, @NotNull String var3, @NotNull String[] var4) {
      if (!HudAccess.check(var1, "enorials.gps.api")) {
         return true;
      }

      if (var4.length < 1) {
         var1.sendMessage("Usage: /gpsapi <start|stop|mode|perf|route> ...");
         return true;
      }

      String var5 = var4[0].toLowerCase();
      if (var5.equals("waystone")) {
         return EnorialsHUD.getInstance().waystoneTravel().command(var1, var4);
      }

      if (var5.equals("perf")) {
         return this.handlePerf(var1, var4);
      }

      if (var5.equals("route")) {
         return this.handleRoute(var1, var4);
      }

      if (var4.length < 2) {
         var1.sendMessage("Usage: /gpsapi <start|stop|mode> <player> ...");
         return true;
      }

      Player var6 = Bukkit.getPlayerExact(var4[1]);
      if (var6 == null) {
         var1.sendMessage("Player not found: " + var4[1]);
         return true;
      }

      return switch (var5) {
         case "stop" -> {
            this.navigationManager.stopNavigation(var6, "api");
            var1.sendMessage("Stopped navigation for " + var6.getName());
            yield true;
         }
         case "mode" -> {
            if (var4.length < 3) {
               var1.sendMessage("Usage: /gpsapi mode <player> <actionbar|bossbar|compass>");
               yield true;
            } else {
               NavigationMode var9 = NavigationMode.fromConfig(var4[2]);
               this.navigationManager.setMode(var6, var9);
               var1.sendMessage("Mode set to " + var9.display() + " for " + var6.getName());
               yield true;
            }
         }
         case "start" -> this.handleStart(var1, var6, var4);
         default -> {
            var1.sendMessage("Unknown subcommand.");
            yield true;
         }
      };
   }

   private boolean handlePerf(CommandSender var1, String[] var2) {
      if (var2.length >= 2 && var2[1].equalsIgnoreCase("reset")) {
         this.navigationManager.resetProfilingStats();
         var1.sendMessage("GPS profiling stats reset.");
         return true;
      } else {
         var1.sendMessage(this.navigationManager.getProfilingSummary());
         return true;
      }
   }

   private boolean handleRoute(CommandSender var1, String[] var2) {
      if (var2.length < 3) {
         var1.sendMessage("Usage: /gpsapi route <add|remove|clear|list> <player> ...");
         return true;
      }

      String var3 = var2[1].toLowerCase();
      Player var4 = Bukkit.getPlayerExact(var2[2]);
      if (var4 == null) {
         var1.sendMessage("Player not found: " + var2[2]);
         return true;
      }

      return switch (var3) {
         case "add" -> this.handleRouteAdd(var1, var4, var2);
         case "remove" -> this.handleRouteRemove(var1, var4, var2);
         case "clear" -> this.handleRouteClear(var1, var4);
         case "list" -> this.handleRouteList(var1, var4);
         default -> {
            var1.sendMessage("Unknown route action. Use add/remove/clear/list.");
            yield true;
         }
      };
   }

   private boolean handleRouteAdd(CommandSender var1, Player var2, String[] var3) {
      if (var3.length < 4) {
         var1.sendMessage("Usage:");
         var1.sendMessage(
            "/gpsapi route add <player> <routeId> <display_with_underscores> <PLAYER|MOB|NPC|POI|LOCATION> <actionbar|bossbar|compass> <value...>"
         );
         var1.sendMessage("/gpsapi route add <player> <PLAYER|MOB|NPC|POI|LOCATION> [actionbar|bossbar|compass] <value...>");
         var1.sendMessage("/gpsapi route add <player> <value...>   # type/mode/id/display auto");
         return true;
      }

      RouteDescriptor.Type var4;
      if (var3.length >= 8 && (var4 = this.parseRouteType(var3[5])) != null) {
         return this.addRoute(var1, var2, var3[3].trim(), var3[4].replace('_', ' '), var4, NavigationMode.fromConfig(var3[6]), this.join(var3, 7));
      }

      RouteDescriptor.Type var5 = this.parseRouteType(var3[3]);
      if (var5 != null) {
         byte var9 = 4;
         NavigationMode var10 = this.navigationManager.getCurrentOrDefaultMode(var2);
         if (var3.length >= 5 && this.isModeToken(var3[4])) {
            var10 = NavigationMode.fromConfig(var3[4]);
            var9 = 5;
         }

         if (var3.length <= var9 - 1) {
            var1.sendMessage("Missing route value.");
            return true;
         } else {
            String var8 = this.join(var3, var9);
            return this.addAutoRoute(var1, var2, var5, var8, var10);
         }
      } else {
         String var6 = this.join(var3, 3);
         GpsApiCommand.InferredInput var7 = this.inferRouteTypeAndValue(var2, var6);
         if (var7 == null) {
            var1.sendMessage("Could not infer route type. Use explicit type: PLAYER|MOB|NPC|POI|LOCATION.");
            return true;
         } else {
            return this.addAutoRoute(var1, var2, var7.type(), var7.value(), this.navigationManager.getCurrentOrDefaultMode(var2));
         }
      }
   }

   private boolean addAutoRoute(CommandSender var1, Player var2, RouteDescriptor.Type var3, String var4, NavigationMode var5) {
      String var6 = this.nextAutoRouteId(var2, var3, var4);
      String var7 = this.autoDisplay(var3, var4);
      return this.addRoute(var1, var2, var6, var7, var3, var5, var4);
   }

   private boolean addRoute(CommandSender var1, Player var2, String var3, String var4, RouteDescriptor.Type var5, NavigationMode var6, String var7) {
      if (var3 == null || var3.isBlank()) {
         var1.sendMessage("Route id is empty.");
         return true;
      }

      if (var4 != null && !var4.isBlank()) {
         RouteDescriptor var8 = new RouteDescriptor(var5, var7);
         if (this.integrationManager.resolveDescriptor(var2, var8).isEmpty()) {
            var1.sendMessage("Could not resolve route target right now for value: " + var7);
            return true;
         } else {
            this.integrationManager.putDynamicQuestRoute(var2.getUniqueId(), var3, var4, var8, var6, "bq-dynamic");
            var1.sendMessage("Added quest route button '" + var3 + "' for " + var2.getName() + ".");
            return true;
         }
      } else {
         var1.sendMessage("Display is empty.");
         return true;
      }
   }

   private RouteDescriptor.Type parseRouteType(String var1) {
      if (var1 != null && !var1.isBlank()) {
         try {
            return RouteDescriptor.Type.valueOf(var1.trim().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private boolean isModeToken(String var1) {
      if (var1 == null) {
         return false;
      }

      String var2 = var1.toLowerCase(Locale.ROOT);
      return var2.equals("actionbar") || var2.equals("bossbar") || var2.equals("compass");
   }

   private String nextAutoRouteId(Player var1, RouteDescriptor.Type var2, String var3) {
      String var4 = "q_" + var2.name().toLowerCase(Locale.ROOT) + "_" + this.sanitizeId(var3);
      if (var4.length() > 42) {
         var4 = var4.substring(0, 42);
      }

      String var5 = var4;
      int var6 = 2;
      Set var7 = this.integrationManager.getDynamicQuestRouteKeys(var1.getUniqueId());

      while (this.containsIgnoreCase(var7, var5)) {
         var5 = var4 + "_" + var6++;
      }

      return var5;
   }

   private String autoDisplay(RouteDescriptor.Type var1, String var2) {
      String var3 = var2 == null ? "" : var2.trim();
      if (var1 == RouteDescriptor.Type.POI) {
         var3 = this.integrationManager.getPoiDisplayName(var3);
      } else {
         int var5 = var3.indexOf(58);
         if (var5 > 0 && var5 + 1 < var3.length()) {
            var3 = var3.substring(var5 + 1);
         }

         var3 = var3.replace('_', ' ');
      }

      if (var3.isBlank()) {
         var3 = var1.name();
      }

      return var3;
   }

   private String sanitizeId(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
         return var2.isBlank() ? "route" : var2;
      } else {
         return "route";
      }
   }

   private boolean containsIgnoreCase(Iterable<String> var1, String var2) {
      if (var2 == null) {
         return false;
      }

      for (String var4 : var1) {
         if (var4 != null && var4.equalsIgnoreCase(var2)) {
            return true;
         }
      }

      return false;
   }

   private GpsApiCommand.InferredInput inferRouteTypeAndValue(Player var1, String var2) {
      String var3 = var2 == null ? "" : var2.trim();
      if (var3.isBlank()) {
         return null;
      } else {
         Player var5 = Bukkit.getPlayerExact(var3);
         if (var5 != null && this.integrationManager.isPlayerTargetsEnabled()) {
            return new GpsApiCommand.InferredInput(RouteDescriptor.Type.PLAYER, var5.getUniqueId().toString());
         } else {
            String var6 = this.findPoiKeyIgnoreCase(var3);
            if (var6 != null) {
               return new GpsApiCommand.InferredInput(RouteDescriptor.Type.POI, var6);
            } else {
               String var7 = var3.toLowerCase(Locale.ROOT);
               if (this.looksLikeLocationInput(var3, var7)) {
                  return new GpsApiCommand.InferredInput(RouteDescriptor.Type.LOCATION, var3);
               } else if (this.looksLikeNpcInput(var7)) {
                  return new GpsApiCommand.InferredInput(RouteDescriptor.Type.NPC, var3);
               } else {
                  return this.looksLikeMobInput(var3, var7) ? new GpsApiCommand.InferredInput(RouteDescriptor.Type.MOB, var3) : null;
               }
            }
         }
      }
   }

   private String findPoiKeyIgnoreCase(String var1) {
      for (String var3 : this.integrationManager.getAvailablePoiKeys()) {
         if (var3.equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   private boolean looksLikeLocationInput(String var1, String var2) {
      if (!var2.startsWith("warp:")
         && !var2.startsWith("esswarp:")
         && !var2.startsWith("ess:")
         && !var2.startsWith("essentials:")
         && !var2.startsWith("cmiwarp:")
         && !var2.startsWith("cmi:")
         && !var2.startsWith("wg:")
         && !var2.startsWith("worldguard:")
         && !var2.startsWith("region:")
         && !var2.startsWith("town:")
         && !var2.startsWith("towny:")
         && !var2.startsWith("dh:")
         && !var2.startsWith("holo:")
         && !var2.startsWith("decentholograms:")) {
         String[] var3 = var1.split("[,;\\s]+");
         return var3.length >= 3 && this.isDouble(var3[0]) && this.isDouble(var3[1]) && this.isDouble(var3[2])
            ? true
            : var3.length >= 4 && this.isDouble(var3[1]) && this.isDouble(var3[2]) && this.isDouble(var3[3]);
      } else {
         return true;
      }
   }

   private boolean looksLikeNpcInput(String var1) {
      return var1.startsWith("citizens:")
         || var1.startsWith("denizen:")
         || var1.startsWith("znpcs:")
         || var1.startsWith("znpcsplus:")
         || var1.startsWith("fancynpc:")
         || var1.startsWith("fancy:")
         || var1.startsWith("shopkeeper:")
         || var1.startsWith("shopkeepers:");
   }

   private boolean looksLikeMobInput(String var1, String var2) {
      if (!var2.startsWith("mythic:")
         && !var2.startsWith("elite:")
         && !var2.startsWith("elitemobs:")
         && !var2.startsWith("modelengine:")
         && !var2.startsWith("model:")
         && !var2.startsWith("denizen:")
         && !var2.startsWith("mmocore:")
         && !var2.startsWith("mmoitems:")
         && !var2.startsWith("ecoboss:")
         && !var2.startsWith("ecobosses:")
         && !var2.startsWith("tag:")
         && !var2.startsWith("meta:")
         && !var2.startsWith("pdc:")) {
         try {
            EntityType.valueOf(var1.toUpperCase(Locale.ROOT));
            return true;
         } catch (IllegalArgumentException var4) {
            return false;
         }
      } else {
         return true;
      }
   }

   private boolean isDouble(String var1) {
      try {
         Double.parseDouble(var1);
         return true;
      } catch (NumberFormatException var3) {
         return false;
      }
   }

   private boolean handleRouteRemove(CommandSender var1, Player var2, String[] var3) {
      if (var3.length < 4) {
         var1.sendMessage("Usage: /gpsapi route remove <player> <routeId>");
         return true;
      } else {
         String var4 = var3[3];
         boolean var5 = this.integrationManager.removeDynamicQuestRoute(var2.getUniqueId(), var4);
         if (!var5) {
            var1.sendMessage("Route not found: " + var4);
            return true;
         } else {
            var1.sendMessage("Removed quest route button '" + var4 + "' for " + var2.getName() + ".");
            return true;
         }
      }
   }

   private boolean handleRouteClear(CommandSender var1, Player var2) {
      int var3 = this.integrationManager.clearDynamicQuestRoutes(var2.getUniqueId());
      var1.sendMessage("Cleared " + var3 + " dynamic quest route(s) for " + var2.getName() + ".");
      return true;
   }

   private boolean handleRouteList(CommandSender var1, Player var2) {
      ArrayList var3 = new ArrayList<>(this.integrationManager.getDynamicQuestRouteKeys(var2.getUniqueId()));
      if (var3.isEmpty()) {
         var1.sendMessage("No dynamic quest routes for " + var2.getName() + ".");
         return true;
      } else {
         Collections.sort(var3);
         var1.sendMessage("Dynamic quest routes for " + var2.getName() + ": " + String.join(", ", var3));
         return true;
      }
   }

   private boolean handleStart(CommandSender var1, Player var2, String[] var3) {
      if (var3.length < 4) {
         var1.sendMessage("Usage: /gpsapi start <player> <player|mob|npc|poi|loc|warp|region|town|hologram|block> <value...>");
         return true;
      }

      String var4 = var3[2].toLowerCase();
      NavigationMode var5 = this.navigationManager.getCurrentOrDefaultMode(var2);
      switch (var4) {
         case "player":
            if (!this.integrationManager.isPlayerTargetsEnabled()) {
               var1.sendMessage("Player targets are disabled.");
               return true;
            } else {
               Player var28 = Bukkit.getPlayerExact(var3[3]);
               if (var28 == null) {
                  var1.sendMessage("Target player not found.");
                  return true;
               }

               this.navigationManager
                  .startNavigation(
                     var2, new PlayerTarget(var28.getUniqueId(), var28.getName()), var5, "api-player", RouteDescriptor.player(var28.getUniqueId().toString())
                  );
               var1.sendMessage("Navigation started.");
               return true;
            }
         case "mob":
            String var27 = this.join(var3, 3);
            Optional var36 = this.integrationManager.resolveMobInput(var27);
            if (var36.isEmpty()) {
               var1.sendMessage("Could not resolve mob target: " + var27);
               return true;
            }

            this.navigationManager.startNavigation(var2, (TargetLocator)var36.get(), var5, "api-mob", RouteDescriptor.mob(var27));
            var1.sendMessage("Navigation started.");
            return true;
         case "npc":
            String var26 = this.join(var3, 3);
            Optional var35 = this.integrationManager.resolveNpcInput(var26);
            if (var35.isEmpty()) {
               var1.sendMessage("Could not resolve NPC target: " + var26);
               return true;
            }

            this.navigationManager.startNavigation(var2, (TargetLocator)var35.get(), var5, "api-npc", RouteDescriptor.npc(var26));
            var1.sendMessage("Navigation started.");
            return true;
         case "poi":
            String var25 = var3[3];
            Optional var34 = this.integrationManager.resolvePoi(var2, var25);
            if (var34.isEmpty()) {
               var1.sendMessage("POI not available: " + var25);
               return true;
            }

            this.navigationManager.startNavigation(var2, (TargetLocator)var34.get(), var5, "api-poi", RouteDescriptor.poi(var25));
            var1.sendMessage("Navigation started.");
            return true;
         case "loc":
            if (!this.integrationManager.isLocationTargetsEnabled()) {
               var1.sendMessage("Location targets are disabled.");
               return true;
            } else {
               String var24 = this.join(var3, 3);
               Optional var33 = this.integrationManager.resolveLocationInput(var2, var24);
               if (var33.isPresent()) {
                  Location var42 = ((TargetLocator)var33.get()).resolve(var2);
                  if (var42 == null) {
                     var1.sendMessage("Could not resolve location target: " + var24);
                     return true;
                  }

                  this.navigationManager.startNavigation(var2, (TargetLocator)var33.get(), var5, "api-loc", RouteDescriptor.location(var42));
                  var1.sendMessage("Navigation started.");
                  return true;
               } else if (var3.length < 6) {
                  var1.sendMessage(
                     "Usage: /gpsapi start <player> loc <x> <y> <z> [world] OR /gpsapi start <player> loc warp:spawn OR /gpsapi start <player> loc wg:region OR /gpsapi start <player> loc town:MyTown OR /gpsapi start <player> loc dh:spawn_holo"
                  );
                  return true;
               } else {
                  try {
                     double var43 = Double.parseDouble(var3[3]);
                     double var13 = Double.parseDouble(var3[4]);
                     double var15 = Double.parseDouble(var3[5]);
                     String var41 = var3.length >= 7 ? var3[6] : var2.getWorld().getName();
                     if (Bukkit.getWorld(var41) == null) {
                        var1.sendMessage("World not found: " + var41);
                        return true;
                     }

                     Location var18 = new Location(Bukkit.getWorld(var41), var43, var13, var15);
                     this.navigationManager
                        .startNavigation(
                           var2,
                           new StaticLocationTarget("Location " + var18.getBlockX() + " " + var18.getBlockY() + " " + var18.getBlockZ(), var18),
                           var5,
                           "api-loc",
                           RouteDescriptor.location(var18)
                        );
                     var1.sendMessage("Navigation started.");
                     return true;
                  } catch (NumberFormatException var19) {
                     var1.sendMessage("Invalid coordinates.");
                     return true;
                  }
               }
            }
         case "warp":
            if (!this.integrationManager.isLocationTargetsEnabled()) {
               var1.sendMessage("Location targets are disabled.");
               return true;
            } else {
               String var23 = this.join(var3, 3);
               Optional var32 = this.integrationManager.resolveLocationInput(var2, "warp:" + var23);
               if (var32.isEmpty()) {
                  var1.sendMessage("Could not resolve warp target: " + var23);
                  return true;
               } else {
                  Location var40 = ((TargetLocator)var32.get()).resolve(var2);
                  if (var40 == null) {
                     var1.sendMessage("Warp target is unavailable.");
                     return true;
                  }

                  this.navigationManager.startNavigation(var2, (TargetLocator)var32.get(), var5, "api-warp", RouteDescriptor.location(var40));
                  var1.sendMessage("Navigation started.");
                  return true;
               }
            }
         case "region":
            if (!this.integrationManager.isLocationTargetsEnabled()) {
               var1.sendMessage("Location targets are disabled.");
               return true;
            } else if (var3.length < 4) {
               var1.sendMessage("Usage: /gpsapi start <player> region <region> [world]");
               return true;
            } else {
               String var22 = var3[3];
               String var31 = var3.length >= 5 ? var3[4] : "";
               String var39 = var31.isBlank() ? "wg:" + var22 : "wg:" + var31 + ":" + var22;
               Optional var11 = this.integrationManager.resolveLocationInput(var2, var39);
               if (var11.isEmpty()) {
                  var1.sendMessage("Could not resolve region target: " + var39);
                  return true;
               } else {
                  Location var12 = ((TargetLocator)var11.get()).resolve(var2);
                  if (var12 == null) {
                     var1.sendMessage("Region target is unavailable.");
                     return true;
                  }

                  this.navigationManager.startNavigation(var2, (TargetLocator)var11.get(), var5, "api-region", RouteDescriptor.location(var12));
                  var1.sendMessage("Navigation started.");
                  return true;
               }
            }
         case "town":
            if (!this.integrationManager.isLocationTargetsEnabled()) {
               var1.sendMessage("Location targets are disabled.");
               return true;
            } else {
               String var21 = this.join(var3, 3);
               Optional var30 = this.integrationManager.resolveLocationInput(var2, "town:" + var21);
               if (var30.isEmpty()) {
                  var1.sendMessage("Could not resolve town target: " + var21);
                  return true;
               } else {
                  Location var38 = ((TargetLocator)var30.get()).resolve(var2);
                  if (var38 == null) {
                     var1.sendMessage("Town target is unavailable.");
                     return true;
                  }

                  this.navigationManager.startNavigation(var2, (TargetLocator)var30.get(), var5, "api-town", RouteDescriptor.location(var38));
                  var1.sendMessage("Navigation started.");
                  return true;
               }
            }
         case "hologram":
            if (!this.integrationManager.isLocationTargetsEnabled()) {
               var1.sendMessage("Location targets are disabled.");
               return true;
            } else {
               String var20 = this.join(var3, 3);
               Optional var29 = this.integrationManager.resolveLocationInput(var2, "dh:" + var20);
               if (var29.isEmpty()) {
                  var1.sendMessage("Could not resolve hologram target: " + var20);
                  return true;
               } else {
                  Location var37 = ((TargetLocator)var29.get()).resolve(var2);
                  if (var37 == null) {
                     var1.sendMessage("Hologram target is unavailable.");
                     return true;
                  }

                  this.navigationManager.startNavigation(var2, (TargetLocator)var29.get(), var5, "api-hologram", RouteDescriptor.location(var37));
                  var1.sendMessage("Navigation started.");
                  return true;
               }
            }
         case "block":
            String var8 = this.join(var3, 3);
            Optional var9 = this.integrationManager.resolveBlockInput(var2, var8);
            if (var9.isEmpty()) {
               var1.sendMessage("Block target was not found near the player: " + var8);
               return true;
            } else {
               Location var10 = ((TargetLocator)var9.get()).resolve(var2);
               if (var10 == null) {
                  var1.sendMessage("Block target is unavailable.");
                  return true;
               }

               this.navigationManager.startNavigation(var2, (TargetLocator)var9.get(), var5, "api-block", RouteDescriptor.location(var10));
               var1.sendMessage("Navigation started.");
               return true;
            }
         default:
            var1.sendMessage("Unknown start type: " + var4);
            return true;
      }
   }

   private String join(String[] var1, int var2) {
      StringBuilder var3 = new StringBuilder();

      for (int var4 = var2; var4 < var1.length; var4++) {
         if (var4 > var2) {
            var3.append(' ');
         }

         var3.append(var1[var4]);
      }

      return var3.toString();
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender var1, @NotNull Command var2, @NotNull String var3, @NotNull String[] var4) {
      if (var4.length == 1) {
         return List.of("start", "stop", "mode", "perf", "route");
      }

      if (var4.length == 2 && var4[0].equalsIgnoreCase("perf")) {
         return List.of("reset");
      }

      if (var4.length == 2 && var4[0].equalsIgnoreCase("route")) {
         return List.of("add", "remove", "clear", "list");
      }

      if (var4.length == 3 && var4[0].equalsIgnoreCase("route")) {
         return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).toList();
      }

      if (var4.length == 4 && var4[0].equalsIgnoreCase("route") && var4[1].equalsIgnoreCase("add")) {
         return List.of("POI", "NPC", "MOB", "PLAYER", "LOCATION", "quest_route_1");
      }

      if (var4.length == 4 && var4[0].equalsIgnoreCase("route") && var4[1].equalsIgnoreCase("remove")) {
         Player var12 = Bukkit.getPlayerExact(var4[2]);
         return var12 == null ? Collections.emptyList() : new ArrayList<>(this.integrationManager.getDynamicQuestRouteKeys(var12.getUniqueId()));
      }

      if (var4.length == 5 && var4[0].equalsIgnoreCase("route") && var4[1].equalsIgnoreCase("add")) {
         return this.parseRouteType(var4[3]) != null ? List.of("actionbar", "bossbar", "compass") : List.of("Quest_Objective_1");
      }

      if (var4.length == 6 && var4[0].equalsIgnoreCase("route") && var4[1].equalsIgnoreCase("add")) {
         if (this.parseRouteType(var4[3]) != null) {
            return switch (var4[3].toUpperCase(Locale.ROOT)) {
               case "PLAYER" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
               case "MOB" -> List.of("ZOMBIE", "mythic:BossSkeleton", "tag:event_boss");
               case "NPC" -> List.of("citizens:1", "fancynpc:guard_1", "znpcs:15");
               case "POI" -> new ArrayList(this.integrationManager.getAvailablePoiKeys());
               case "LOCATION" -> List.of("warp:spawn", "wg:spawn_region", "0 64 0 world");
               default -> Collections.emptyList();
            };
         } else {
            return List.of("PLAYER", "MOB", "NPC", "POI", "LOCATION");
         }
      } else {
         if (var4.length == 7 && var4[0].equalsIgnoreCase("route") && var4[1].equalsIgnoreCase("add")) {
            return this.parseRouteType(var4[5]) != null ? List.of("actionbar", "bossbar", "compass") : Collections.emptyList();
         }

         if (var4.length == 8 && var4[0].equalsIgnoreCase("route") && var4[1].equalsIgnoreCase("add")) {
            return switch (var4[5].toUpperCase(Locale.ROOT)) {
               case "PLAYER" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
               case "MOB" -> List.of("ZOMBIE", "mythic:BossSkeleton", "tag:event_boss");
               case "NPC" -> List.of("citizens:1", "fancynpc:guard_1", "znpcs:15");
               case "POI" -> new ArrayList(this.integrationManager.getAvailablePoiKeys());
               case "LOCATION" -> List.of("warp:spawn", "wg:spawn_region", "0 64 0 world");
               default -> Collections.emptyList();
            };
         } else {
            if (var4.length == 2) {
               return Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).toList();
            }

            if (var4.length == 3 && var4[0].equalsIgnoreCase("start")) {
               return List.of("player", "mob", "npc", "poi", "loc", "warp", "region", "town", "hologram", "block");
            }

            if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("mob")) {
               return List.of("ZOMBIE", "mythic:BossSkeleton", "elite:miniboss", "ecoboss:worldboss", "modelengine:guardian", "tag:event_boss");
            }

            if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("npc")) {
               return List.of("1", "citizens:1", "citizens:Blacksmith", "znpcs:15", "fancynpc:guard_1", "shopkeeper:*", "denizen:QuestGiver");
            }

            if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("loc")) {
               return List.of("0", "warp:spawn", "cmi:spawn", "wg:spawn_region", "town:SpawnTown", "dh:spawn_info");
            }

            if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("warp")) {
               List var11 = this.integrationManager.getWarpNames();
               return !var11.isEmpty() ? var11 : List.of("spawn", "market");
            }

            if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("region")) {
               Player var6 = var4.length >= 2 ? Bukkit.getPlayerExact(var4[1]) : null;
               if (var6 == null && var1 instanceof Player) {
                  Player var8;
                  var6 = var8 = (Player)var1;
               }

               List var10;
               return !(var10 = this.integrationManager.getWorldGuardRegionNames(var6)).isEmpty() ? var10 : List.of("spawn_region", "market_region");
            } else if (var4.length == 5 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("region")) {
               return Bukkit.getWorlds().stream().<String>map(WorldInfo::getName).toList();
            } else if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("town")) {
               List var9 = this.integrationManager.getTownyTownNames();
               return !var9.isEmpty() ? var9 : List.of("SpawnTown", "MarketTown");
            } else if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("hologram")) {
               List var5 = this.integrationManager.getDecentHologramNames();
               return !var5.isEmpty() ? var5 : List.of("spawn_info", "quests_board");
            } else if (var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("block")) {
               return List.of("STONE", "itemsadder:myplugin:jade_ore", "nexo:ancient_rock", "oraxen:my_custom_block");
            } else if (var4.length == 3 && var4[0].equalsIgnoreCase("mode")) {
               return List.of("actionbar", "bossbar", "compass");
            } else {
               return var4.length == 4 && var4[0].equalsIgnoreCase("start") && var4[2].equalsIgnoreCase("poi")
                  ? new ArrayList<>(this.integrationManager.getAvailablePoiKeys())
                  : Collections.emptyList();
            }
         }
      }
   }

   private record InferredInput(RouteDescriptor.Type type, String value) {
   }
}
