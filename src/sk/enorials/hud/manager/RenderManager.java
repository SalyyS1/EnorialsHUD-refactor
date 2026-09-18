package sk.enorials.hud.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.condition.ConditionHolder;
import sk.enorials.hud.model.FunctionData;
import sk.enorials.hud.model.HudAlignment;
import sk.enorials.hud.model.HudElement;
import sk.enorials.hud.model.HudLayer;
import sk.enorials.hud.model.HudLayout;
import sk.enorials.hud.model.HudPopup;
import sk.enorials.hud.model.TextLayer;
import sk.enorials.hud.model.TextureLayer;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.party.GpsPanel;
import sk.enorials.hud.party.PartyPanel;
import sk.enorials.hud.party.PlayerPanel;
import sk.enorials.hud.party.TargetPanel;
import sk.enorials.hud.protocol.BossBarHandler;
import sk.enorials.hud.theme.HotbarOverlay;
import sk.enorials.hud.theme.ThemeStyle;
import sk.enorials.hud.util.FunctionalListeners;
import sk.enorials.hud.util.HudEffectText;
import sk.enorials.hud.util.HudStatus;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.hud.util.RelativeWidth;
import sk.enorials.hud.util.SizedComponent;
import sk.enorials.hud.util.SpaceUtil;
import sk.enorials.hud.util.TextWidthEstimator;

public class RenderManager implements Listener {
   private final EnorialsHUD plugin;
   private final Map<UUID, BossBar> bars = new HashMap<>();
   private final Map<UUID, BossBar> airBars = new HashMap<>();
   private final Map<UUID, BossBarHandler> protocolBars = new HashMap<>();
   private final Map<UUID, Component> minimapOverlays = new HashMap<>();
   private final Map<UUID, Component> conversationOverlays = new HashMap<>();
   private final Map<UUID, Component> questLogOverlays = new HashMap<>();
   private final Map<UUID, RenderManager.PopupState> popups = new HashMap<>();
   private final Map<UUID, RenderManager.CombatTarget> combatTargets = new HashMap<>();
   private final boolean protocolEnabled;
   private final Properties effectGlyphs = new Properties();
   private BukkitTask task;
   private final PartyPanel partyPanel;
   private final PlayerPanel playerPanel;
   private final GpsPanel gpsPanel;
   private final TargetPanel targetPanel;
   private long renderTick;
   private long idleRenderPeriod = 5L;

   public RenderManager(EnorialsHUD var1) {
      this.plugin = var1;
      this.partyPanel = new PartyPanel(var1);
      this.playerPanel = new PlayerPanel(var1);
      this.gpsPanel = new GpsPanel(var1);
      this.targetPanel = new TargetPanel(var1);

      try (InputStream var2 = var1.getResource("effect-icons.properties")) {
         if (var2 != null) {
            this.effectGlyphs.load(var2);
         }
      } catch (IOException var7) {
         var1.getLogger().warning("Could not read HUD effect glyphs: " + var7.getMessage());
      }

      this.protocolEnabled = true;
      Bukkit.getPluginManager().registerEvents(this, var1);
   }

   public void start() {
      if (this.task == null) {
         this.idleRenderPeriod = Math.max(1L, this.plugin.cfg().getLong("render.update_ticks", 5L));
         this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 1L, 1L);
      }
   }

   public void stop() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }

      for (Entry var2 : this.bars.entrySet()) {
         Player var3 = Bukkit.getPlayer((UUID)var2.getKey());
         if (var3 != null) {
            var3.hideBossBar((BossBar)var2.getValue());
         }
      }

      this.bars.clear();

      for (Entry var6 : this.airBars.entrySet()) {
         Player var8 = Bukkit.getPlayer((UUID)var6.getKey());
         if (var8 != null) {
            var8.hideBossBar((BossBar)var6.getValue());
         }
      }

      this.airBars.clear();

      for (BossBarHandler var7 : this.protocolBars.values()) {
         var7.disable();
      }

      this.protocolBars.clear();
      this.minimapOverlays.clear();
      this.conversationOverlays.clear();
      this.questLogOverlays.clear();
      this.popups.clear();
   }

   public void attach(Player var1) {
      if (PackHostingSetup.ready(var1)) {
         if (this.protocolEnabled) {
            BossBarHandler var2 = new BossBarHandler(var1);
            BossBarHandler var3 = this.protocolBars.put(var1.getUniqueId(), var2);
            if (var3 != null) {
               var3.disable();
            }

            var2.setMinimapOverlay(this.minimapOverlays.getOrDefault(var1.getUniqueId(), Component.empty()));
            var2.setConversationOverlay(this.conversationOverlays.getOrDefault(var1.getUniqueId(), Component.empty()));
            var2.setQuestLogOverlay(this.questLogOverlays.getOrDefault(var1.getUniqueId(), Component.empty()));
            var2.init();
         } else {
            BossBar var4 = BossBar.bossBar(Component.empty(), 0.0F, Color.YELLOW, Overlay.PROGRESS);
            this.bars.put(var1.getUniqueId(), var4);
            var1.showBossBar(var4);
         }
      }
   }

   public void detach(Player var1) {
      this.minimapOverlays.remove(var1.getUniqueId());
      this.conversationOverlays.remove(var1.getUniqueId());
      this.questLogOverlays.remove(var1.getUniqueId());
      if (this.protocolEnabled) {
         BossBarHandler var2 = this.protocolBars.remove(var1.getUniqueId());
         if (var2 != null) {
            var2.disable();
         }
      } else {
         BossBar var4 = this.bars.remove(var1.getUniqueId());
         if (var4 != null) {
            var1.hideBossBar(var4);
         }

         BossBar var3 = this.airBars.remove(var1.getUniqueId());
         if (var3 != null) {
            var1.hideBossBar(var3);
         }
      }

      this.popups.remove(var1.getUniqueId());
      this.combatTargets.remove(var1.getUniqueId());
   }

   public boolean setMinimapOverlay(Player var1, Component var2) {
      if (!var1.isOnline()) {
         return false;
      }

      UUID var3 = var1.getUniqueId();
      boolean var4 = var2 == null || var2.equals(Component.empty());
      Object var5 = var4 ? Component.empty() : var2.style(var0 -> TextCompat.shadow(var0, 0));
      if (var4) {
         this.minimapOverlays.remove(var3);
      } else {
         this.minimapOverlays.put(var3, (Component)var5);
      }

      BossBarHandler var6 = this.protocolBars.get(var3);
      if (var6 != null) {
         var6.setMinimapOverlay((Component)var5);
         return true;
      } else {
         return !this.protocolEnabled && this.bars.containsKey(var3);
      }
   }

   public void clearMinimapOverlay(Player var1) {
      this.setMinimapOverlay(var1, Component.empty());
   }

   public void setConversationOverlay(Player var1, Component var2) {
      Object var3 = var2 == null ? Component.empty() : var2;
      if (var3.equals(Component.empty())) {
         this.conversationOverlays.remove(var1.getUniqueId());
      } else {
         this.conversationOverlays.put(var1.getUniqueId(), (Component)var3);
      }

      BossBarHandler var4 = this.protocolBars.get(var1.getUniqueId());
      if (var4 != null) {
         var4.setConversationOverlay((Component)var3);
      }
   }

   public void setQuestLogOverlay(Player var1, Component var2) {
      Object var3 = var2 == null ? Component.empty() : var2;
      if (var3.equals(Component.empty())) {
         this.questLogOverlays.remove(var1.getUniqueId());
      } else {
         this.questLogOverlays.put(var1.getUniqueId(), (Component)var3);
      }

      BossBarHandler var4 = this.protocolBars.get(var1.getUniqueId());
      if (var4 != null) {
         var4.setQuestLogOverlay((Component)var3);
      }
   }

   public void refreshTheme(Player var1) {
      BossBarHandler var2 = this.protocolBars.get(var1.getUniqueId());
      if (var2 != null) {
         var2.refreshTheme();
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onHeldItem(PlayerItemHeldEvent var1) {
      if (PackHostingSetup.ready(var1.getPlayer()) && HotbarOverlay.enabled(this.plugin, var1.getPlayer())) {
         BossBarHandler var2 = this.protocolBars.get(var1.getPlayer().getUniqueId());
         if (var2 != null) {
            var2.refreshHotbar(var1.getNewSlot());
         }
      }
   }

   public boolean hasMinimapOverlay(Player var1) {
      UUID var2 = var1.getUniqueId();
      return this.minimapOverlays.containsKey(var2) && (this.protocolEnabled ? this.protocolBars.containsKey(var2) : this.bars.containsKey(var2));
   }

   public void showPopup(Player var1, String var2, String[] var3, long var4) {
      if (this.plugin.popups().has(var2)) {
         long var6 = System.currentTimeMillis() + var4 * 50L;
         this.popups.put(var1.getUniqueId(), new RenderManager.PopupState(var2, var3, var6));
      }
   }

   private void tick() {
      if (PackHostingSetup.ready()) {
         boolean var1 = this.renderTick++ % this.idleRenderPeriod == 0L;
         PackRegistry var2 = this.plugin.packs().registry();

         for (Player var4 : Bukkit.getOnlinePlayers()) {
            if (!PackHostingSetup.ready(var4)) {
               this.detach(var4);
            } else if (var1 || this.plugin.gps() != null && this.plugin.gps().navigation().hasActiveNavigation(var4)) {
               Component var5 = Component.empty();
               Object var6 = Component.empty();
               String var7 = this.plugin.storage().getLayout(var4.getUniqueId());
               if (var7 != null && this.plugin.layouts().has(var7)) {
                  HudLayout var19 = this.plugin.layouts().get(var7);
                  ArrayList var22 = new ArrayList();
                  ArrayList var25 = new ArrayList();
                  this.splitAirElements(var19.getElements(), var22, var25);
                  var5 = var5.append(this.renderHolder("layout", var19.getKey(), var22, var4, var2, null));
                  var6 = var6.append(this.renderHolder("layout", var19.getKey(), var25, var4, var2, null));
               } else if (this.plugin.displays().hasDisplays()) {
                  for (DisplayManager.DisplayEntry var9 : this.plugin.displays().getDisplays()) {
                     if (!var9.conditions().result(var4).isHidden()) {
                        HudLayout var10 = var9.layout();
                        ArrayList var11 = new ArrayList();
                        ArrayList var12 = new ArrayList();
                        this.splitAirElements(var10.getElements(), var11, var12);
                        var5 = var5.append(this.renderHolder("layout", var9.layoutKey(), var11, var4, var2, null));
                        var6 = var6.append(this.renderHolder("layout", var9.layoutKey(), var12, var4, var2, null));
                     }
                  }
               } else {
                  for (HudLayout var21 : this.plugin.layouts().getDefaults()) {
                     ArrayList var24 = new ArrayList();
                     ArrayList var27 = new ArrayList();
                     this.splitAirElements(var21.getElements(), var24, var27);
                     var5 = var5.append(this.renderHolder("layout", var21.getKey(), var24, var4, var2, null));
                     var6 = var6.append(this.renderHolder("layout", var21.getKey(), var27, var4, var2, null));
                  }
               }

               RenderManager.PopupState var20 = this.popups.get(var4.getUniqueId());
               HudPopup var23;
               if (var20 != null && System.currentTimeMillis() <= var20.expireAt() && (var23 = this.plugin.popups().get(var20.popupKey())) != null) {
                  var5 = var5.append(this.renderHolder("popup", var23.getKey(), var23.getElements(), var4, var2, var20.args()));
               }

               int var26 = Math.round(var2.metrics().measure(var5).advance());
               var5 = var5.append(shift(this.playerPanel.render(var4, var2).append(this.targetPanel.render(var4, var2)), var26 / 2 - var26));
               if (!HudAccess.hud(var4) || !this.plugin.storage().preference(var4.getUniqueId(), "hud", true)) {
                  var5 = Component.empty();
                  var6 = Component.empty();
               }

               Component var28 = this.partyPanel.render(var4, var2).append(this.gpsPanel.render(var4, var2));
               int var29 = Math.round(var2.metrics().measure((Component)var5).advance());
               var5 = var5.append(shift(var28, var29 / 2 - var29));
               if (this.protocolEnabled) {
                  BossBarHandler var13 = this.protocolBars.get(var4.getUniqueId());
                  if (var13 == null) {
                     var13 = new BossBarHandler(var4);
                     this.protocolBars.put(var4.getUniqueId(), var13);
                     var13.setMinimapOverlay(this.minimapOverlays.getOrDefault(var4.getUniqueId(), Component.empty()));
                     var13.init();
                  }

                  int var14 = Math.round(var2.metrics().measure(var5).advance());
                  var13.setHud(var5.append(shift((Component)var6, var14 / 2 - var14)).style(var0 -> TextCompat.shadow(var0, 0)));
               } else {
                  BossBar var30 = this.bars.computeIfAbsent(var4.getUniqueId(), var1x -> {
                     BossBar var2x = BossBar.bossBar(Component.empty(), 0.0F, Color.YELLOW, Overlay.PROGRESS);
                     var4.showBossBar(var2x);
                     return var2x;
                  });
                  Component var31 = ThemeStyle.paint(var5.style(var0 -> TextCompat.shadow(var0, 0)), this.plugin.storage().theme(var4.getUniqueId()));
                  var31 = this.plugin.avatars().paint(var4, var31);
                  var30.name(
                     BossBarHandler.combineCentered(
                        this.minimapOverlays.getOrDefault(var4.getUniqueId(), Component.empty()).append(HotbarOverlay.render(this.plugin, var4)),
                        var31,
                        (int)Math.ceil(var2.metrics().measure(var31).advance())
                     )
                  );
                  if (this.isUnderwater(var4)) {
                     BossBar var15 = this.airBars.computeIfAbsent(var4.getUniqueId(), var1x -> {
                        BossBar var2x = BossBar.bossBar(Component.empty(), 0.0F, Color.YELLOW, Overlay.PROGRESS);
                        var4.showBossBar(var2x);
                        return var2x;
                     });
                     var15.name(var6.style(var0 -> TextCompat.shadow(var0, 0)));
                  } else {
                     BossBar var33 = this.airBars.remove(var4.getUniqueId());
                     if (var33 != null) {
                        var4.hideBossBar(var33);
                     }
                  }
               }
            }
         }
      }
   }

   private void splitAirElements(Iterable<HudElement> var1, List<HudElement> var2, List<HudElement> var3) {
      for (HudElement var5 : var1) {
         if (var5 != null && var5.getKey().startsWith("enorialshud-air-")) {
            var3.add(var5);
         } else {
            var2.add(var5);
         }
      }
   }

   private Component renderHolder(String var1, String var2, Iterable<HudElement> var3, Player var4, PackRegistry var5, String[] var6) {
      Object var7 = Component.empty();
      ArrayList var8 = new ArrayList();
      ArrayList var9 = new ArrayList();
      ArrayList var10 = new ArrayList();

      for (HudElement var12 : var3) {
         if ("layout".equalsIgnoreCase(var1) && var12.getKey().startsWith("enorialshud-air-")) {
            var9.add(var12);
         } else if ("layout".equalsIgnoreCase(var1) && "target-frame".equalsIgnoreCase(var12.getKey())) {
            var10.add(var12);
         } else {
            var8.add(var12);
         }
      }

      var9.sort(Comparator.comparingInt(var0 -> "enorialshud-air-frame".equals(var0.getKey()) ? 0 : 1));
      var8.addAll(var9);
      var8.addAll(var10);
      Float var28 = null;
      Float var29 = null;
      Float var13 = null;
      Float var14 = null;
      float var15 = 0.0F;
      float var16 = 0.0F;

      for (HudElement var18 : var8) {
         if (this.shouldRenderDynamicElement(var1, var18, var4)) {
            ConditionHolder.ConditionResult var19 = var18.getAsset().getConditions().result(var4);
            if (!var19.isHidden()) {
               Object var20 = Component.empty();
               RelativeWidth var21 = null;

               for (HudLayer var23 : var18.getAsset().getLayers()) {
                  ConditionHolder.ConditionResult var24 = var23.getConditions().result(var4);
                  if (!var24.isHidden()) {
                     if (var23 instanceof TextureLayer var25) {
                        String var26 = var24.hasTexturePath() ? var24.getTexturePath() : var25.getTexturePath();
                        RenderManager.RenderedTexture var27 = this.renderTexture(var1, var2, var18, var25, var4, var5, var26);
                        if (var27 != null && var27.component() != null) {
                           var20 = var20.append(var27.component());
                           if (var27.relativeWidth() != null) {
                              var21 = var21 == null ? var27.relativeWidth() : var21.merge(var27.relativeWidth());
                           }
                        }
                     } else {
                        RenderManager.RenderedText var36 = null;
                        TextLayer var40;
                        if (var23 instanceof TextLayer
                           && (var36 = this.renderText(var1, var2, var18, var40 = (TextLayer)var23, var4, var5, var6, var24)) != null
                           && var36.component() != null) {
                           var20 = var20.append(var36.component());
                           if (var36.relativeWidth() != null) {
                              var21 = var21 == null ? var36.relativeWidth() : var21.merge(var36.relativeWidth());
                           }
                        }
                     }
                  }
               }

               if (var21 != null) {
                  var20 = var18.getAlignment().relativeAlign((Component)var20, var21);
               }

               if (var19.hasColor()) {
                  var20 = var20.color(var19.getColor());
               }

               String var30 = var18.getKey().replace("-compact", "");
               HudLayer var31 = var18.getAsset().getLayers().iterator().next();
               float var32 = var18.getScreenPosition().getOffset().getX() + var31.getOffset().getX();
               if ("enorialshud-panel".equals(var30)) {
                  float var33 = var16 + var5.metrics().measure((Component)var20).left() + var32;
                  var13 = var33 + 124.0F;
                  var14 = var33 + 52.0F;
               }

               if ("enorialshud-avatar-head".equals(var30)) {
                  var29 = var16 + var5.metrics().measure((Component)var20).center() + var32;
               }

               if (var13 != null && Set.of("enorialshud-health-text", "enorialshud-mana-text", "enorialshud-food-text").contains(var30)) {
                  float var34 = var5.metrics().firstCenter((Component)var20, 47);
                  if (Float.isFinite(var34)) {
                     int var38 = Math.round(var13 - var16 - var32 - var34);
                     var20 = shift((Component)var20, var38);
                  }
               }

               if (var30.startsWith("enorialshud-effects-") && var14 != null) {
                  var20 = shift((Component)var20, Math.round(var14 - var16 - var32));
               }

               if ("corehud-armor-icon".equals(var18.getKey()) || "corehud-armor-text".equals(var18.getKey())) {
                  HudLayer var35 = var18.getAsset().getLayers().iterator().next();
                  float var39 = var16
                     + var5.metrics().measure((Component)var20).center()
                     + var18.getScreenPosition().getOffset().getX()
                     + var35.getOffset().getX();
                  if ("corehud-armor-icon".equals(var18.getKey())) {
                     if (var29 != null) {
                        int var41 = Math.round(var29 - var39);
                        var20 = shift((Component)var20, var41);
                        var39 += var41;
                     }

                     var28 = var39;
                     var15 = var18.getScreenPosition().getRelativeX();
                  } else if (var28 != null && var15 == var18.getScreenPosition().getRelativeX()) {
                     int var42 = Math.round(var28 - var39);
                     var20 = ((TextComponent)((TextComponent)Component.empty().append(SpaceUtil.space(var42))).append((Component)var20))
                        .append(SpaceUtil.space(-var42));
                  }
               }

               if ("target-frame".equals(var18.getKey())
                  || "popup".equals(var1)
                  || var30.startsWith("enorialshud-air-")
                  || var30.startsWith("enorialshud-effects-")) {
                  var20 = var5.metrics().neutral((Component)var20);
               }

               if ("target-frame".equals(var30)) {
                  var20 = shift((Component)var20, (int)(var16 / 2.0F) - Math.round(var16));
               }

               if (("enorialshud-mana-fill".equals(var30) || "enorialshud-mana-text".equals(var30)) && !this.plugin.listeners().hasResource(var4)) {
                  var20 = SpaceUtil.space(Math.round(var5.metrics().measure((Component)var20).advance()));
               }

               var16 += var5.metrics().measure((Component)var20).advance();
               var7 = var7.append((Component)var20);
            }
         }
      }

      return (Component)var7;
   }

   private boolean shouldRenderDynamicElement(String var1, HudElement var2, Player var3) {
      if (!"layout".equalsIgnoreCase(var1) || var2 == null || var2.getKey() == null) {
         return true;
      }

      if ("target-frame".equals(var2.getKey()) && this.getCombatTarget(var3) != null) {
         return false;
      }

      String var4 = var2.getKey().replace("-compact", "");
      if (!var4.startsWith("enorialshud-effects-")
         && !Set.of(
               "enorialshud-panel",
               "enorialshud-avatar-head",
               "enorialshud-health-fill",
               "enorialshud-health-text",
               "enorialshud-mana-fill",
               "enorialshud-mana-text",
               "enorialshud-hunger-fill",
               "enorialshud-food-text",
               "corehud-armor-icon",
               "corehud-armor-text"
            )
            .contains(var4)) {
         if ((Set.of("enorialshud-panel", "enorialshud-hunger-fill", "enorialshud-food-text").contains(var4) || var4.startsWith("enorialshud-effects-"))
            && var2.getKey().endsWith("-compact") == this.plugin.listeners().hasResource(var3)) {
            return false;
         }

         if (var4.startsWith("enorialshud-air-")) {
            return this.isUnderwater(var3);
         }

         return switch (var2.getKey().toLowerCase(Locale.ROOT)) {
            case "event-info" -> this.isEventActiveOrAnnounced(var3);
            case "arena-queue" -> this.isArenaQueueActive(var3);
            case "party-buff" -> this.hasActiveBuffs(var3);
            case "enorialshud-air-fill", "enorialshud-air-text" -> this.isUnderwater(var3);
            default -> true;
         };
      } else {
         return false;
      }
   }

   private static Component shift(Component var0, int var1) {
      return var1 == 0 ? var0 : ((TextComponent)((TextComponent)Component.empty().append(SpaceUtil.space(var1))).append(var0)).append(SpaceUtil.space(-var1));
   }

   private boolean isUnderwater(Player var1) {
      if (var1.getRemainingAir() < var1.getMaximumAir()) {
         return true;
      }

      Material var2 = var1.getEyeLocation().getBlock().getType();
      return var2 == Material.WATER || var2 == Material.BUBBLE_COLUMN;
   }

   private boolean isEventActiveOrAnnounced(Player var1) {
      if (!this.isTruthyPlaceholder(var1, "%enorialsevents_active%")
         && !this.isTruthyPlaceholder(var1, "%enorialsevents_in_progress%")
         && !this.isTruthyPlaceholder(var1, "%enorialsevents_announced%")) {
         String[] var2 = new String[]{
            "%enorialsevents_event%", "%enorialsevents_event_name%", "%enorialsevents_current_event%", "%enorialsevents_status%", "%enorialsevents_type%"
         };
         Set var3 = Set.of("none", "no_event", "no event", "idle", "false", "0", "-", "n/a", "null", "unknown");

         for (String var7 : var2) {
            if (this.isMeaningfulPlaceholder(var1, var7, var3)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean isArenaQueueActive(Player var1) {
      return !this.isTruthyPlaceholder(var1, "%enorialsarena_in_queue%")
            && !this.isTruthyPlaceholder(var1, "%enorialsarena_queueing%")
            && !this.isTruthyPlaceholder(var1, "%enorialsarena_queue_active%")
         ? this.isMeaningfulPlaceholder(var1, "%enorialsarena_queue_mode%", Set.of("none", "false", "0", "-", "n/a", "null", "off", "unknown"))
         : true;
   }

   private boolean hasActiveBuffs(Player var1) {
      return this.isMeaningfulPlaceholder(var1, "%enorialsspells_buffs%", Set.of("none", "no buffs", "false", "0", "-", "n/a", "null", "empty"));
   }

   private boolean isTruthyPlaceholder(Player var1, String var2) {
      String var3 = this.resolvePlaceholderValue(var1, var2);
      if (var3 == null) {
         return false;
      }

      String var4 = var3.trim().toLowerCase(Locale.ROOT);
      return var4.equals("true") || var4.equals("1") || var4.equals("yes") || var4.equals("on");
   }

   private boolean isMeaningfulPlaceholder(Player var1, String var2, Set<String> var3) {
      String var4 = this.resolvePlaceholderValue(var1, var2);
      if (var4 == null) {
         return false;
      }

      String var5 = var4.trim();
      if (var5.isEmpty()) {
         return false;
      }

      String var6 = var5.toLowerCase(Locale.ROOT);
      return var6.contains("%") ? false : !var3.contains(var6);
   }

   private String resolvePlaceholderValue(Player var1, String var2) {
      if (var2 != null && !var2.isEmpty()) {
         if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return var2;
         }

         try {
            return PlaceholderAPI.setPlaceholders(var1, var2);
         } catch (Throwable var4) {
            return var2;
         }
      } else {
         return null;
      }
   }

   private RenderManager.RenderedTexture renderTexture(
      String var1, String var2, HudElement var3, TextureLayer var4, Player var5, PackRegistry var6, String var7
   ) {
      PackRegistry.TextureSteps var8 = var6.getTextureSteps(var1, var2, var3.getKey(), var4.getKey(), var7);
      if (var8 != null) {
         int var15 = this.getStepIndex(var5, var8.function(), var8.steps().size());
         if (var15 <= 0) {
            return null;
         }

         PackRegistry.GlyphInfo var16 = var8.steps().get(var15 - 1);
         Component var17 = var16.component(var6);
         if (!"target-frame".equals(var3.getKey()) && !"enorialshud-air-fill".equals(var3.getKey())) {
            if ("enorialshud-health-fill".equals(var3.getKey())) {
               var17 = var17.color(TextColor.color(HudStatus.snapshot(var5).tone().rgb()));
            }

            SizedComponent var20 = new SizedComponent(var17, var16.width);
            RelativeWidth var13 = var4.getAlignment().calculateRelativeWidth(var16.width, var4.getOffset());
            Component var14 = var4.getAlignment().align(var20);
            var14 = this.applyHorizontalOffset(var14, var5, var3, var4);
            return new RenderManager.RenderedTexture(var14, var13);
         } else {
            int var19 = var8.steps().get(var8.steps().size() - 1).width;
            return new RenderManager.RenderedTexture(var6.metrics().neutral(shift(var17, -var19 / 2)), null);
         }
      } else {
         PackRegistry.GlyphInfo var9 = var6.getTexture(var1, var2, var3.getKey(), var4.getKey(), var7);
         if (var9 == null) {
            return null;
         }

         SizedComponent var10 = new SizedComponent(var9.component(var6), var9.width);
         RelativeWidth var11 = var4.getAlignment().calculateRelativeWidth(var9.width, var4.getOffset());
         Component var12 = var4.getAlignment().align(var10);
         var12 = this.applyHorizontalOffset(var12, var5, var3, var4);
         return new RenderManager.RenderedTexture(var12, var11);
      }
   }

   private RenderManager.RenderedText renderText(
      String var1, String var2, HudElement var3, TextLayer var4, Player var5, PackRegistry var6, String[] var7, ConditionHolder.ConditionResult var8
   ) {
      String var9 = var6.getTextFont(var1, var2, var3.getKey(), var4.getKey());
      if (var3.getKey().startsWith("enorialshud-effects-")) {
         String var17 = var3.getKey().replace("-compact", "");
         int var19 = Integer.parseInt(var17.substring(var17.lastIndexOf(45) + 1));
         Component var22 = this.renderEffects(var5, var9, var3.getKey().contains("timers"), var19, var6);
         return new RenderManager.RenderedText(var6.metrics().neutral(var22), null);
      }

      String var10 = this.resolveText(var5, var4.getText(), var7);
      Component var11 = this.legacyToComponent(var10);
      if (var9 != null) {
         var11 = LegacyText.font(var11, Key.key(var9));
      }

      var11 = var11.style(var0 -> TextCompat.shadow(var0, 0));
      if (var8 != null && var8.hasColor()) {
         var11 = var11.color(var8.getColor());
      }

      if (("target-frame".equals(var3.getKey()) || "enorialshud-air-text".equals(var3.getKey())) && var4.getAlignment() == HudAlignment.CENTER) {
         Component var20 = shift(var11, -Math.round(var6.metrics().measure(var11).center()));
         var20 = var6.metrics().neutral(var20);
         return new RenderManager.RenderedText(this.applyHorizontalOffset(var20, var5, var3, var4), null);
      }

      String var12 = switch (var3.getKey().replace("-compact", "")) {
         case "enorialshud-health-text", "enorialshud-food-text" -> "88/88";
         case "enorialshud-mana-text" -> "888/888";
         case "corehud-armor-text" -> "88";
         default -> null;
      };
      int var23;
      if (var12 != null) {
         Component var24 = Component.text(var12).style(var11.style());
         if ("corehud-armor-text".equals(var3.getKey())) {
            var24 = var24.decorate(TextDecoration.BOLD);
         }

         var11 = var12.contains("/") ? var6.metrics().fieldAt(var11, var24, 47) : var6.metrics().field(var11, var24);
         var23 = this.estimateMonoDigitsWidth(var12);
      } else {
         var23 = this.estimateTextWidth(var3, var4, var9, var10);
      }

      SizedComponent var25 = new SizedComponent(var11, var23);
      RelativeWidth var15 = var4.getAlignment().calculateRelativeWidth(var23, var4.getOffset());
      Component var16 = var4.getAlignment().align(var25);
      var16 = this.applyHorizontalOffset(var16, var5, var3, var4);
      return new RenderManager.RenderedText(var16, var15);
   }

   public Component renderEffects(Player var1, String var2, boolean var3, int var4, PackRegistry var5) {
      if (HudAccess.has(var1, "enorials.hud.effects") && this.plugin.storage().preference(var1.getUniqueId(), "effects", true)) {
         Object var6 = Component.empty();
         List var7 = HudStatus.snapshot(var1).effects().stream().skip(var4 * 12L).limit(12L).toList();
         if (var2 == null) {
            return (Component)var6;
         }

         for (HudStatus.Effect var9 : var7) {
            Object var10 = Component.empty();
            if (var3) {
               String var11 = var9.timer();
               if (var11.endsWith("s")) {
                  var11 = var11.substring(0, var11.length() - 1);
               }

               if (var11.length() > 3 && var9.remainingTicks() > 0) {
                  var11 = Math.max(1, var9.remainingTicks() / 72000) + "h";
               }

               Component var12 = HudEffectText.timer(var11, var2);
               var10 = HudEffectText.center(var12, var2, var5.metrics());
            } else {
               String var14 = this.effectGlyphs.getProperty(var9.key());
               if (var14 != null) {
                  Component var15 = LegacyText.font(Component.text((char)Integer.parseInt(var14)), Key.key(var2));
                  Component var13 = LegacyText.font(Component.text((char)(var9.harmful() ? '\ue901' : '\ue900')), Key.key(var2));
                  var10 = var5.metrics().neutral(var15).append(var13);
               }
            }

            var6 = var6.append(var5.metrics().neutral((Component)var10)).append(SpaceUtil.space(12));
         }

         return var5.metrics().neutral((Component)var6).style(var0 -> TextCompat.shadow(var0, 0));
      } else {
         return Component.empty();
      }
   }

   private int estimateTextWidth(HudElement var1, TextLayer var2, String var3, String var4) {
      if (var1 != null && "target-frame".equalsIgnoreCase(var1.getKey())) {
         return this.estimateVanillaWidth(var4);
      } else {
         String var5 = var2 != null ? var2.getFont() : "";
         String var6 = var3 != null ? var3.toLowerCase() : "";
         if (!"enorialsavatar".equalsIgnoreCase(var5) && !var6.contains("enorialsavatar")) {
            return !"default-small-mono".equalsIgnoreCase(var5) && !var6.contains("mono")
               ? TextWidthEstimator.estimate(var4)
               : this.estimateMonoDigitsWidth(var4);
         } else {
            return Math.max(1, this.plugin.cfg().getInt("avatars.glyph-width", 40));
         }
      }
   }

   private int estimateMonoDigitsWidth(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = var1.replaceAll("Ă‚Â§[0-9A-FK-ORa-fk-or]", "");
         var2 = var2.replaceAll("<#[0-9a-fA-F]{6}>", "");
         var2 = var2.replaceAll("<[^>]+>", "");
         byte var3 = 0;

         for (int var4 = 0; var4 < var2.length(); var4++) {
            char var5 = var2.charAt(var4);

            var3 += switch (var5) {
               case ' ', '\u200c' -> 4;
               case '-', '.', '/', ':' -> 4;
               case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> 4;
               default -> 6;
            };
         }

         return var3;
      } else {
         return 0;
      }
   }

   private int estimateVanillaWidth(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = var1.replaceAll("Ă‚Â§[0-9A-FK-ORa-fk-or]", "");
         var2 = var2.replaceAll("<#[0-9a-fA-F]{6}>", "");
         var2 = var2.replaceAll("<[^>]+>", "");
         int var3 = 0;

         for (int var4 = 0; var4 < var2.length(); var4++) {
            var3 += this.vanillaCharWidth(var2.charAt(var4));
         }

         return var3;
      } else {
         return 0;
      }
   }

   private int vanillaCharWidth(char var1) {
      return switch (var1) {
         case ' ', '\u200c' -> 4;
         case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2;
         case '"', '(', ')', 'I', '[', ']', '`', 'l', 't', '{', '}' -> 4;
         case '<', '>', 'f', 'k' -> 5;
         case '@', '~' -> 7;
         default -> 6;
      };
   }

   private int getStepIndex(Player var1, FunctionData var2, int var3) {
      if (var2 != null && var3 > 0) {
         double var4 = this.plugin.listeners().getCurrent(var1, var2.listener());
         double var6 = this.plugin.listeners().getMax(var1, var2.listener());
         if (var6 <= 0.0) {
            return 0;
         }

         double var8 = Math.max(0.0, Math.min(1.0, var4 / var6));
         return (int)Math.ceil(var8 * var3);
      } else {
         return 0;
      }
   }

   private String resolveText(Player var1, String var2, String[] var3) {
      String var4 = var2;
      if (var3 != null) {
         for (int var5 = 0; var5 < var3.length; var5++) {
            var4 = var4.replace("%arg-" + (var5 + 1) + "%", var3[var5]);
         }
      }

      String var36 = this.plugin.cfg().getString("decimal-format", "00##");
      var4 = var4.replace("%health-bar-value%", this.formatListenerValue("health", this.plugin.listeners().getCurrent(var1, "health"), var36));
      var4 = var4.replace("%health-bar-max%", this.formatListenerValue("health", this.plugin.listeners().getMax(var1, "health"), var36));
      var4 = var4.replace("%resource-bar-value%", this.formatListenerValue("resource", this.plugin.listeners().getCurrent(var1, "resource"), var36));
      var4 = var4.replace("%resource-bar-max%", this.formatListenerValue("resource", this.plugin.listeners().getMax(var1, "resource"), var36));
      var4 = var4.replace("%food-bar-value%", this.formatListenerValue("food", this.plugin.listeners().getCurrent(var1, "food"), var36));
      var4 = var4.replace("%food-bar-max%", this.formatListenerValue("food", this.plugin.listeners().getMax(var1, "food"), var36));
      var4 = var4.replace("%armor-bar-value%", this.formatListenerValue("armor", this.plugin.listeners().getCurrent(var1, "armor"), var36));
      var4 = var4.replace("%armor-bar-max%", this.formatListenerValue("armor", this.plugin.listeners().getMax(var1, "armor"), var36));
      var4 = var4.replace("%air-bar-value%", this.formatListenerValue("air", this.plugin.listeners().getCurrent(var1, "air"), var36));
      var4 = var4.replace("%air-bar-max%", this.formatListenerValue("air", this.plugin.listeners().getMax(var1, "air"), var36));
      var4 = var4.replace("%coord-x%", String.valueOf(var1.getLocation().getBlockX()));
      var4 = var4.replace("%coord-y%", String.valueOf(var1.getLocation().getBlockY()));
      var4 = var4.replace("%coord-z%", String.valueOf(var1.getLocation().getBlockZ()));
      var4 = var4.replace("%enorialshud_avatar_giantHead%", this.plugin.avatars().getAvatarGlyph(var1));
      RenderManager.TargetSnapshot var6 = this.resolveTarget(var1);
      var4 = var4.replace("%target_exists%", var6.exists ? "true" : "false");
      var4 = var4.replace("%target_name%", var6.name);
      var4 = var4.replace("%target_health%", var6.health);
      var4 = var4.replace("%target_max_health%", var6.maxHealth);
      var4 = var4.replace("%target_distance%", var6.distance);
      var4 = var4.replace("%target_hp_percent%", var6.hpPercent);
      var4 = var4.replace("%target_hp_bar%", var6.hpBar);

      for (String var8 : this.plugin.listeners().getListeners().keySet()) {
         double var9 = this.plugin.listeners().getCurrent(var1, var8);
         double var11 = this.plugin.listeners().getMax(var1, var8);
         var4 = var4.replace("%" + var8 + "-bar-value%", this.formatListenerValue(var8, var9, var36));
         var4 = var4.replace("%" + var8 + "-bar-max%", this.formatListenerValue(var8, var11, var36));
      }

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         try {
            var4 = PlaceholderAPI.setPlaceholders(var1, var4);
         } catch (Throwable var13) {
         }
      }

      return var4;
   }

   private String formatListenerValue(String var1, double var2, String var4) {
      if (!Double.isFinite(var2)) {
         return "0";
      } else if ("air".equalsIgnoreCase(var1)) {
         return String.valueOf((int)Math.round(Math.max(0.0, var2)));
      } else if ("resource".equalsIgnoreCase(var1) || "mana".equalsIgnoreCase(var1)) {
         return String.format(Locale.ROOT, "%03d", (int)Math.round(var2));
      } else if ("armor".equalsIgnoreCase(var1)) {
         return String.format(Locale.ROOT, "%02d", (int)Math.round(var2));
      } else {
         return this.shouldUseWholeNumber(var1) ? String.valueOf((int)Math.round(var2)) : FunctionalListeners.format(var2, var4);
      }
   }

   private boolean shouldUseWholeNumber(String var1) {
      if (var1 == null) {
         return false;
      }

      return switch (var1.toLowerCase()) {
         case "health", "resource", "food", "armor", "air", "mana", "rage", "targethp" -> true;
         default -> false;
      };
   }

   private RenderManager.TargetSnapshot resolveTarget(Player var1) {
      LivingEntity var2 = this.getCurrentTarget(var1);
      if (var2 == null) {
         return RenderManager.TargetSnapshot.empty();
      }

      double var3 = Math.max(0.0, var2.getHealth());
      double var5 = Math.max(
         1.0, var2.getAttribute(HudAttributes.MAX_HEALTH) != null ? var2.getAttribute(HudAttributes.MAX_HEALTH).getValue() : var2.getMaxHealth()
      );
      double var7 = var1.getLocation().distance(var2.getLocation());
      double var9 = Math.max(0.0, Math.min(1.0, var3 / var5));
      byte var11 = 14;
      int var12 = (int)Math.round(var9 * var11);
      String var13 = "<green>" + "|".repeat(Math.max(0, var12)) + "<dark_gray>" + "|".repeat(Math.max(0, var11 - var12));
      String var14 = this.plugin.cfg().getString("decimal-format", "00##");
      String var15 = String.valueOf((int)Math.round(var3));
      String var16 = String.valueOf((int)Math.round(var5));
      String var17 = String.valueOf((int)Math.round(var7));
      String var18 = String.valueOf((int)Math.round(var9 * 100.0));
      String var19 = this.plugin.listeners().getTargetName(var1, var2);
      return new RenderManager.TargetSnapshot(true, var19, var15, var16, var17, var18, var13);
   }

   public LivingEntity getCombatTarget(Player var1) {
      if (var1 != null && var1.isOnline()) {
         RenderManager.CombatTarget var2 = this.combatTargets.get(var1.getUniqueId());
         if (var2 == null) {
            return null;
         }

         if (Bukkit.getEntity(var2.targetId()) instanceof LivingEntity var4
            && var4.isValid()
            && !var4.isDead()
            && var4.getWorld().equals(var1.getWorld())
            && !(
               var4.getLocation().distanceSquared(var1.getLocation())
                  > Math.pow(Math.max(8.0, this.plugin.cfg().getDouble("target-frame.combat-max-distance", 64.0)), 2.0)
            )) {
            boolean var5 = var4 instanceof Mob var6 && var6.getTarget() != null && var6.getTarget().isValid() && !var6.getTarget().isDead();
            long var8 = System.currentTimeMillis();
            if (var5) {
               this.combatTargets.put(var1.getUniqueId(), new RenderManager.CombatTarget(var4.getUniqueId(), var8));
            } else if (var8 - var2.lastHitAtMs() > this.getTargetFrameTimeoutMs()) {
               this.combatTargets.remove(var1.getUniqueId());
               return null;
            }

            return var4;
         } else {
            this.combatTargets.remove(var1.getUniqueId());
            return null;
         }
      } else {
         return null;
      }
   }

   public LivingEntity getCurrentTarget(Player var1) {
      LivingEntity var2 = this.getCombatTarget(var1);
      return var2 != null ? var2 : (var1 != null && var1.isOnline() ? this.resolveLookTarget(var1) : null);
   }

   private LivingEntity resolveLookTarget(Player var1) {
      double var2 = Math.max(3.0, this.plugin.cfg().getDouble("target-frame.max-distance", 32.0));
      RayTraceResult var4 = var1.getWorld().rayTraceEntities(var1.getEyeLocation(), var1.getEyeLocation().getDirection(), var2, var1x -> {
         if (var1x instanceof LivingEntity var2x) {
            if (var1x.equals(var1)) {
               return false;
            } else {
               return var2x instanceof Player ? false : !var1x.isDead();
            }
         } else {
            return false;
         }
      });
      return (var4 != null ? var4.getHitEntity() : null) instanceof LivingEntity var5 ? var5 : null;
   }

   private Player resolveAttacker(Entity var1) {
      if (var1 instanceof Player var2) {
         return var2;
      } else {
         return var1 instanceof Projectile var4 && var4.getShooter() instanceof Player var3 ? var3 : null;
      }
   }

   private long getTargetFrameTimeoutMs() {
      return Math.max(1000L, this.plugin.cfg().getLong("target-frame.timeout-ms", 10000L));
   }

   private Component legacyToComponent(String var1) {
      String var2 = this.convertMiniToLegacy(var1);
      return LegacyComponentSerializer.legacySection().deserialize(var2);
   }

   private String convertMiniToLegacy(String var1) {
      String var2 = var1;
      var2 = var2.replace("<white>", ChatColor.WHITE.toString());
      var2 = var2.replace("<black>", ChatColor.BLACK.toString());
      var2 = var2.replace("<red>", ChatColor.RED.toString());
      var2 = var2.replace("<green>", ChatColor.GREEN.toString());
      var2 = var2.replace("<blue>", ChatColor.BLUE.toString());
      var2 = var2.replace("<yellow>", ChatColor.YELLOW.toString());
      var2 = var2.replace("<gray>", ChatColor.GRAY.toString());
      var2 = var2.replace("<dark_gray>", ChatColor.DARK_GRAY.toString());
      var2 = var2.replace("<gold>", ChatColor.GOLD.toString());
      var2 = var2.replace("<dark_red>", ChatColor.DARK_RED.toString());
      var2 = var2.replace("<dark_green>", ChatColor.DARK_GREEN.toString());
      var2 = var2.replace("<dark_blue>", ChatColor.DARK_BLUE.toString());
      var2 = var2.replace("<aqua>", ChatColor.AQUA.toString());
      var2 = var2.replace("<dark_aqua>", ChatColor.DARK_AQUA.toString());
      var2 = var2.replace("<light_purple>", ChatColor.LIGHT_PURPLE.toString());
      var2 = var2.replace("<dark_purple>", ChatColor.DARK_PURPLE.toString());
      var2 = var2.replace("<bold>", ChatColor.BOLD.toString());
      var2 = var2.replace("</bold>", ChatColor.RESET.toString());
      var2 = var2.replace("<b>", ChatColor.BOLD.toString());
      var2 = var2.replace("</b>", ChatColor.RESET.toString());
      var2 = var2.replace("<reset>", ChatColor.RESET.toString());
      Pattern var3 = Pattern.compile("<#[0-9a-fA-F]{6}>");
      Matcher var4 = var3.matcher(var2);
      StringBuffer var5 = new StringBuffer();

      while (var4.find()) {
         String var6 = var4.group();
         String var7 = var6.substring(2, var6.length() - 1);
         String var8 = "Ă‚Â§x";

         for (char var12 : var7.toCharArray()) {
            var8 = var8 + "Ă‚Â§" + var12;
         }

         var4.appendReplacement(var5, var8);
      }

      var4.appendTail(var5);
      return var5.toString();
   }

   public void openLayoutGui(Player var1) {
      int var2 = this.plugin.layouts().getLayouts().size();
      int var3 = Math.min(54, Math.max(9, (var2 + 1 + 8) / 9 * 9));
      Inventory var4 = Bukkit.createInventory(null, var3, "HUD Layouts");
      String var5 = this.plugin.storage().getLayout(var1.getUniqueId());
      int var6 = 0;

      for (HudLayout var8 : this.plugin.layouts().getLayouts()) {
         if (var6 >= var3 - 1) {
            break;
         }

         boolean var9 = var8.getKey().equalsIgnoreCase(var5);
         ItemStack var10 = this.button(Material.PAPER, (var9 ? ChatColor.GREEN : ChatColor.YELLOW) + var8.getKey(), var9 ? "Selected" : "Click to select");
         ItemMeta var11 = var10.getItemMeta();
         var11.setLore(List.of(ChatColor.GRAY + (var9 ? "Selected" : "Click to select"), ChatColor.DARK_GRAY + var8.getKey()));
         var10.setItemMeta(var11);
         var4.setItem(var6++, var10);
      }

      var4.setItem(var3 - 1, this.button(Material.BARRIER, ChatColor.RED + "Reset to default", "Use server default HUD"));
      var1.openInventory(var4);
   }

   private ItemStack button(Material var1, String var2, String var3) {
      ItemStack var4 = new ItemStack(var1);
      ItemMeta var5 = var4.getItemMeta();
      var5.setDisplayName(var2);
      var5.setLore(List.of(ChatColor.GRAY + var3));
      var4.setItemMeta(var5);
      return var4;
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent var1) {
      this.attach(var1.getPlayer());
      this.plugin.avatars().handleJoin(var1.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent var1) {
      this.detach(var1.getPlayer());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEntityDamageByEntity(EntityDamageByEntityEvent var1) {
      if (!(var1.getFinalDamage() <= 0.0)) {
         long var2 = System.currentTimeMillis();
         UUID var4 = var1.getEntity().getUniqueId();
         Entity var5 = var1.getDamager() instanceof Projectile var7 && var7.getShooter() instanceof Entity var6 ? var6 : var1.getDamager();
         this.combatTargets
            .replaceAll(
               (var4x, var5x) -> (RenderManager.CombatTarget)(!var5x.targetId().equals(var4) && !var5x.targetId().equals(var5.getUniqueId())
                  ? var5x
                  : new RenderManager.CombatTarget(var5x.targetId(), var2))
            );
         if (var1.getEntity() instanceof LivingEntity var9 && (!(var9 instanceof Player) || var9.hasMetadata("NPC"))) {
            Player var11 = this.resolveAttacker(var1.getDamager());
            if (var11 != null && var11.isOnline() && !var9.isDead()) {
               this.combatTargets.put(var11.getUniqueId(), new RenderManager.CombatTarget(var9.getUniqueId(), System.currentTimeMillis()));
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onEntityDeath(EntityDeathEvent var1) {
      LivingEntity var2 = var1.getEntity();
      UUID var3 = var2.getUniqueId();
      this.combatTargets.entrySet().removeIf(var1x -> var3.equals(var1x.getValue().targetId()));
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      if (var1.getView() != null) {
         String var2 = var1.getView().getTitle();
         if ("HUD Layouts".equals(var2)) {
            var1.setCancelled(true);
            if (!(var1.getWhoClicked() instanceof Player var3)) {
               return;
            }

            if (!HudAccess.has(var3, "enorialshud.layout")) {
               var3.sendMessage(ChatColor.RED + "No permission.");
               return;
            }

            ItemStack var7 = var1.getCurrentItem();
            if (var7 == null || !var7.hasItemMeta()) {
               return;
            }

            if (var7.getType() == Material.BARRIER) {
               this.plugin.storage().clearLayout(var3.getUniqueId());
               var3.sendMessage(ChatColor.YELLOW + "HUD layout reset to default.");
               var3.closeInventory();
               return;
            }

            ItemMeta var5 = var7.getItemMeta();
            if (var5.getLore() == null || var5.getLore().isEmpty()) {
               return;
            }

            String var6 = ChatColor.stripColor((String)var5.getLore().get(var5.getLore().size() - 1));
            if (var6 == null || var6.isBlank() || !this.plugin.layouts().has(var6)) {
               return;
            }

            this.plugin.storage().setLayout(var3.getUniqueId(), var6);
            var3.sendMessage(ChatColor.GREEN + "HUD layout set to " + var6 + ".");
            var3.closeInventory();
         }
      }
   }

   private Component applyHorizontalOffset(Component var1, Player var2, HudElement var3, HudLayer var4) {
      int var5 = 0;
      int var6 = Math.max(0, this.plugin.cfg().getInt("layout.runtime-shift-left", 0));
      String var7 = var3.getAsset().getKey();
      if (var6 > 0 && var7.startsWith("enorialshud-") && !"enorialshud-panel".equals(var7)) {
         var5 -= var6;
      }

      int var8 = var5;
      return var5 == 0
         ? var1
         : ((Builder)((Builder)((Builder)Component.text().append(SpaceUtil.space(var8))).append(var1)).append(SpaceUtil.space(var8 * -1))).asComponent();
   }

   private record CombatTarget(UUID targetId, long lastHitAtMs) {
   }

   private record PopupState(String popupKey, String[] args, long expireAt) {
   }

   private record RenderedText(Component component, RelativeWidth relativeWidth) {
   }

   private record RenderedTexture(Component component, RelativeWidth relativeWidth) {
   }

   private static class TargetSnapshot {
      final boolean exists;
      final String name;
      final String health;
      final String maxHealth;
      final String distance;
      final String hpPercent;
      final String hpBar;

      private TargetSnapshot(boolean var1, String var2, String var3, String var4, String var5, String var6, String var7) {
         this.exists = var1;
         this.name = var2;
         this.health = var3;
         this.maxHealth = var4;
         this.distance = var5;
         this.hpPercent = var6;
         this.hpBar = var7;
      }

      static RenderManager.TargetSnapshot empty() {
         return new RenderManager.TargetSnapshot(false, "-", "0", "0", "0", "0", "<dark_gray>||||||||||||||");
      }
   }
}
