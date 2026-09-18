package sk.enorials.gps.input;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.util.Color;
import sk.enorials.hud.access.HudAccess;

public final class ChatInputService {
   private final EnorialsGpsPlugin plugin;
   private final NavigationManager navigationManager;
   private final IntegrationManager integrationManager;
   private final Map<UUID, TargetInputType> pending = new ConcurrentHashMap<>();
   private final Map<UUID, ChatInputService.PendingCustomInput> pendingCustom = new ConcurrentHashMap<>();

   public ChatInputService(EnorialsGpsPlugin var1, NavigationManager var2, IntegrationManager var3) {
      this.plugin = var1;
      this.navigationManager = var2;
      this.integrationManager = var3;
   }

   public void request(Player var1, TargetInputType var2) {
      if (HudAccess.check(var1, "enorials.gps.use")) {
         String var3 = this.permissionFor(var2);
         if (var3 != null && !HudAccess.has(var1, var3)) {
            this.plugin.msg().send(var1, "&cYou do not have permission: &f" + var3);
         } else {
            this.pendingCustom.remove(var1.getUniqueId());
            this.pending.put(var1.getUniqueId(), var2);

            String var4 = switch (var2) {
               case PLAYER -> "prompts.player";
               case MOB -> "prompts.mob";
               case LOCATION -> "prompts.location";
               case NPC -> "prompts.npc";
               case BLOCK -> "prompts.block";
            };
            this.plugin.msg().send(var1, this.plugin.config().getString(var4, "&eType input in chat. Type cancel."));
            this.sendQuickSuggestions(var1, var2);
         }
      }
   }

   public void requestCustom(Player var1, String var2, List<String> var3, Consumer<String> var4) {
      this.pending.remove(var1.getUniqueId());
      this.pendingCustom.put(var1.getUniqueId(), new ChatInputService.PendingCustomInput(var2, var3, var4));
      this.plugin.msg().send(var1, var2);
      this.sendCustomSuggestions(var1, var3);
   }

   public boolean hasPending(UUID var1) {
      return this.pending.containsKey(var1) || this.pendingCustom.containsKey(var1);
   }

   public void handleChat(Player var1, String var2) {
      ChatInputService.PendingCustomInput var3 = this.pendingCustom.remove(var1.getUniqueId());
      if (var3 != null) {
         if (var2.equalsIgnoreCase("cancel")) {
            this.plugin.msg().send(var1, "&cNavigation input cancelled.");
            this.plugin.gui().openTarget(var1);
         } else {
            var3.consumer().accept(var2);
         }
      } else {
         TargetInputType var4 = this.pending.remove(var1.getUniqueId());
         if (var4 != null) {
            if (var2.equalsIgnoreCase("cancel")) {
               this.plugin.msg().send(var1, "&cNavigation input cancelled.");
               this.plugin.gui().openTarget(var1);
            } else {
               switch (var4) {
                  case PLAYER:
                     this.handlePlayer(var1, var2);
                     break;
                  case MOB:
                     this.handleMob(var1, var2);
                     break;
                  case LOCATION:
                     this.handleLocation(var1, var2);
                     break;
                  case NPC:
                     this.handleNpc(var1, var2);
                     break;
                  case BLOCK:
                     this.handleBlock(var1, var2);
               }
            }
         }
      }
   }

   private void handlePlayer(Player var1, String var2) {
      if (!HudAccess.has(var1, "enorials.gps.track.player")) {
         this.plugin.msg().send(var1, "&cYou do not have permission: &fenorials.gps.track.player");
      } else if (!this.integrationManager.isPlayerTargetsEnabled()) {
         this.plugin.msg().send(var1, "&cPlayer targets are disabled.");
      } else {
         Player var3 = Bukkit.getPlayerExact(var2);
         if (var3 == null || !var1.canSee(var3)) {
            this.plugin.msg().send(var1, "&cPlayer not found: &f" + var2);
         } else {
            this.navigationManager
               .startNavigation(
                  var1,
                  new PlayerTarget(var3.getUniqueId(), var3.getName()),
                  this.navigationManager.getCurrentOrDefaultMode(var1),
                  "chat-player",
                  RouteDescriptor.player(var3.getUniqueId().toString())
               );
         }
      }
   }

   private void handleMob(Player var1, String var2) {
      if (!HudAccess.has(var1, "enorials.gps.track.mob")) {
         this.plugin.msg().send(var1, "&cYou do not have permission: &fenorials.gps.track.mob");
      } else {
         Optional var3 = this.integrationManager.resolveMobInput(var2);
         if (var3.isEmpty()) {
            this.plugin.msg().send(var1, "&cCould not resolve mob input: &f" + var2);
         } else {
            this.navigationManager
               .startNavigation(var1, (TargetLocator)var3.get(), this.navigationManager.getCurrentOrDefaultMode(var1), "chat-mob", RouteDescriptor.mob(var2));
         }
      }
   }

   private void handleNpc(Player var1, String var2) {
      if (!HudAccess.has(var1, "enorials.gps.track.npc")) {
         this.plugin.msg().send(var1, "&cYou do not have permission: &fenorials.gps.track.npc");
      } else {
         Optional var3 = this.integrationManager.resolveNpcInput(var2);
         if (var3.isEmpty()) {
            this.plugin.msg().send(var1, "&cCould not resolve NPC input: &f" + var2);
         } else {
            this.navigationManager
               .startNavigation(var1, (TargetLocator)var3.get(), this.navigationManager.getCurrentOrDefaultMode(var1), "chat-npc", RouteDescriptor.npc(var2));
         }
      }
   }

   private void handleLocation(Player var1, String var2) {
      if (!this.integrationManager.isLocationTargetsEnabled()) {
         this.plugin.msg().send(var1, "&cLocation targets are disabled.");
      } else {
         Optional var3 = this.integrationManager.resolveLocationInput(var1, var2);
         if (var3.isPresent()) {
            Location var16 = ((TargetLocator)var3.get()).resolve(var1);
            if (var16 == null) {
               this.plugin.msg().send(var1, "&cCould not resolve location input: &f" + var2);
            } else {
               this.navigationManager
                  .startNavigation(
                     var1, (TargetLocator)var3.get(), this.navigationManager.getCurrentOrDefaultMode(var1), "chat-loc", RouteDescriptor.location(var16)
                  );
            }
         } else {
            String[] var4 = var2.trim().split("\\s+");
            if (var4.length < 3) {
               this.plugin.msg().send(var1, "&cInvalid location format. Use: X Y Z [world], warp:spawn, cmi:spawn, wg:region, town:MyTown, or dh:spawn_info.");
            } else {
               try {
                  double var6 = Double.parseDouble(var4[0]);
                  double var8 = Double.parseDouble(var4[1]);
                  double var10 = Double.parseDouble(var4[2]);
                  if (!Double.isFinite(var6) || !Double.isFinite(var8) || !Double.isFinite(var10)) {
                     this.plugin.msg().send(var1, "&cInvalid coordinates.");
                     return;
                  }
                  String var5 = var4.length >= 4 ? var4[3] : var1.getWorld().getName();
                  if (Bukkit.getWorld(var5) == null) {
                     this.plugin.msg().send(var1, "&cUnknown world: &f" + var5);
                     return;
                  }

                  Location var13 = new Location(Bukkit.getWorld(var5), var6, var8, var10);
                  StaticLocationTarget var14 = new StaticLocationTarget(
                     "Location " + var13.getBlockX() + " " + var13.getBlockY() + " " + var13.getBlockZ(), var13
                  );
                  this.navigationManager
                     .startNavigation(var1, var14, this.navigationManager.getCurrentOrDefaultMode(var1), "chat-loc", RouteDescriptor.location(var13));
               } catch (NumberFormatException var15) {
                  this.plugin.msg().send(var1, "&cInvalid coordinates.");
               }
            }
         }
      }
   }

   private void handleBlock(Player var1, String var2) {
      if (!HudAccess.has(var1, "enorials.gps.track.block")) {
         this.plugin.msg().send(var1, "&cYou do not have permission: &fenorials.gps.track.block");
      } else {
         Optional var3 = this.integrationManager.resolveBlockInput(var1, var2);
         if (var3.isEmpty()) {
            this.plugin.msg().send(var1, "&cThat block was not found near the player: &f" + var2);
         } else {
            Location var4 = ((TargetLocator)var3.get()).resolve(var1);
            if (var4 == null) {
               this.plugin.msg().send(var1, "&cBlock target is unavailable right now.");
            } else {
               this.navigationManager
                  .startNavigation(
                     var1, (TargetLocator)var3.get(), this.navigationManager.getCurrentOrDefaultMode(var1), "chat-block", RouteDescriptor.location(var4)
                  );
            }
         }
      }
   }

   private void sendQuickSuggestions(Player var1, TargetInputType var2) {
      if (this.plugin.config().getBoolean("chat_suggestions.enabled", true)) {
         List<String> var3;
         if (var2 == TargetInputType.PLAYER) {
            var3 = Bukkit.getOnlinePlayers()
               .stream()
               .<String>map(Player::getName)
               .filter(var1x -> !var1x.equalsIgnoreCase(var1.getName()))
               .limit(this.plugin.config().getInt("chat_suggestions.max_player_suggestions", 6))
               .toList();
            if (var3.isEmpty()) {
               var3 = this.plugin.config().getStringList("chat_suggestions.player_fallback");
            }
         } else {
            String var4 = switch (var2) {
               case MOB -> "chat_suggestions.mob";
               case LOCATION -> "chat_suggestions.location";
               case NPC -> "chat_suggestions.npc";
               case BLOCK -> "chat_suggestions.block";
               default -> "chat_suggestions.player_fallback";
            };
            var3 = this.plugin.config().getStringList(var4);
         }

         if (!var3.isEmpty()) {
            Component var8 = Color.component("&6[GPS] &eQuick input examples: ");
            var1.sendMessage(var8);

            for (String var6 : var3) {
               Component var7 = Color.component(" &7- ")
                  .append(
                     Color.component("&b" + var6)
                        .clickEvent(ClickEvent.suggestCommand(var6))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to insert into chat")))
                  );
               var1.sendMessage(var7);
            }

            Component var9 = Color.component(" &7- ")
               .append(
                  Color.component("&c cancel")
                     .clickEvent(ClickEvent.suggestCommand("cancel"))
                     .hoverEvent(HoverEvent.showText(Component.text("Cancel pending GPS input")))
               );
            var1.sendMessage(var9);
         }
      }
   }

   private void sendCustomSuggestions(Player var1, List<String> var2) {
      if (var2 != null && !var2.isEmpty()) {
         Component var3 = Color.component("&6[GPS] &eQuick input examples: ");
         var1.sendMessage(var3);

         for (String var5 : var2) {
            Component var6 = Color.component(" &7- ")
               .append(
                  Color.component("&b" + var5)
                     .clickEvent(ClickEvent.suggestCommand(var5))
                     .hoverEvent(HoverEvent.showText(Component.text("Click to insert into chat")))
               );
            var1.sendMessage(var6);
         }
      }
   }

   private String permissionFor(TargetInputType var1) {
      return switch (var1) {
         case PLAYER -> "enorials.gps.track.player";
         case MOB -> "enorials.gps.track.mob";
         case LOCATION -> "enorials.gps.coordinates";
         case NPC -> "enorials.gps.track.npc";
         case BLOCK -> "enorials.gps.track.block";
      };
   }

   private record PendingCustomInput(String prompt, List<String> suggestions, Consumer<String> consumer) {
   }
}
