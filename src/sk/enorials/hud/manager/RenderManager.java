/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  net.kyori.adventure.bossbar.BossBar
 *  net.kyori.adventure.bossbar.BossBar$Color
 *  net.kyori.adventure.bossbar.BossBar$Overlay
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.RayTraceResult
 */
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.condition.ConditionHolder;
import sk.enorials.hud.manager.DisplayManager;
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

public class RenderManager
implements Listener {
    private final EnorialsHUD plugin;
    private final Map<UUID, BossBar> bars = new HashMap<UUID, BossBar>();
    private final Map<UUID, BossBar> airBars = new HashMap<UUID, BossBar>();
    private final Map<UUID, BossBarHandler> protocolBars = new HashMap<UUID, BossBarHandler>();
    private final Map<UUID, Component> minimapOverlays = new HashMap<UUID, Component>();
    private final Map<UUID, Component> conversationOverlays = new HashMap<UUID, Component>();
    private final Map<UUID, Component> questLogOverlays = new HashMap<UUID, Component>();
    private final Map<UUID, PopupState> popups = new HashMap<UUID, PopupState>();
    private final Map<UUID, CombatTarget> combatTargets = new HashMap<UUID, CombatTarget>();
    private final boolean protocolEnabled;
    private final Properties effectGlyphs = new Properties();
    private BukkitTask task;
    private final PartyPanel partyPanel;
    private final PlayerPanel playerPanel;
    private final GpsPanel gpsPanel;
    private final TargetPanel targetPanel;
    private long renderTick;
    private long idleRenderPeriod = 5L;

    public RenderManager(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.partyPanel = new PartyPanel(enorialsHUD);
        this.playerPanel = new PlayerPanel(enorialsHUD);
        this.gpsPanel = new GpsPanel(enorialsHUD);
        this.targetPanel = new TargetPanel(enorialsHUD);
        try (InputStream inputStream = enorialsHUD.getResource("effect-icons.properties");){
            if (inputStream != null) {
                this.effectGlyphs.load(inputStream);
            }
        }
        catch (IOException iOException) {
            enorialsHUD.getLogger().warning("Could not read HUD effect glyphs: " + iOException.getMessage());
        }
        this.protocolEnabled = true;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
    }

    public void start() {
        if (this.task == null) {
            this.idleRenderPeriod = Math.max(1L, this.plugin.cfg().getLong("render.update_ticks", 5L));
            this.task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::tick, 1L, 1L);
        }
    }

    public void stop() {
        Player player;
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        for (Map.Entry<UUID, BossBar> entry : this.bars.entrySet()) {
            player = Bukkit.getPlayer((UUID)entry.getKey());
            if (player == null) continue;
            player.hideBossBar(entry.getValue());
        }
        this.bars.clear();
        for (Map.Entry<UUID, BossBar> entry : this.airBars.entrySet()) {
            player = Bukkit.getPlayer((UUID)entry.getKey());
            if (player == null) continue;
            player.hideBossBar(entry.getValue());
        }
        this.airBars.clear();
        for (BossBarHandler bossBarHandler : this.protocolBars.values()) {
            bossBarHandler.disable();
        }
        this.protocolBars.clear();
        this.minimapOverlays.clear();
        this.conversationOverlays.clear();
        this.questLogOverlays.clear();
        this.popups.clear();
    }

    public void attach(Player player) {
        if (!PackHostingSetup.ready(player)) {
            return;
        }
        if (this.protocolEnabled) {
            BossBarHandler bossBarHandler = new BossBarHandler(player);
            BossBarHandler bossBarHandler2 = this.protocolBars.put(player.getUniqueId(), bossBarHandler);
            if (bossBarHandler2 != null) {
                bossBarHandler2.disable();
            }
            bossBarHandler.setMinimapOverlay(this.minimapOverlays.getOrDefault(player.getUniqueId(), (Component)Component.empty()));
            bossBarHandler.setConversationOverlay(this.conversationOverlays.getOrDefault(player.getUniqueId(), (Component)Component.empty()));
            bossBarHandler.setQuestLogOverlay(this.questLogOverlays.getOrDefault(player.getUniqueId(), (Component)Component.empty()));
            bossBarHandler.init();
        } else {
            BossBar bossBar = BossBar.bossBar((Component)Component.empty(), (float)0.0f, (BossBar.Color)BossBar.Color.YELLOW, (BossBar.Overlay)BossBar.Overlay.PROGRESS);
            this.bars.put(player.getUniqueId(), bossBar);
            player.showBossBar(bossBar);
        }
    }

    public void detach(Player player) {
        this.minimapOverlays.remove(player.getUniqueId());
        this.conversationOverlays.remove(player.getUniqueId());
        this.questLogOverlays.remove(player.getUniqueId());
        if (this.protocolEnabled) {
            BossBarHandler bossBarHandler = this.protocolBars.remove(player.getUniqueId());
            if (bossBarHandler != null) {
                bossBarHandler.disable();
            }
        } else {
            BossBar bossBar;
            BossBar bossBar2 = this.bars.remove(player.getUniqueId());
            if (bossBar2 != null) {
                player.hideBossBar(bossBar2);
            }
            if ((bossBar = this.airBars.remove(player.getUniqueId())) != null) {
                player.hideBossBar(bossBar);
            }
        }
        this.popups.remove(player.getUniqueId());
        this.combatTargets.remove(player.getUniqueId());
    }

    public boolean setMinimapOverlay(Player player, Component component) {
        TextComponent textComponent;
        if (!player.isOnline()) {
            return false;
        }
        UUID uUID = player.getUniqueId();
        boolean bl = component == null || component.equals((Object)Component.empty());
        Object object = textComponent = bl ? Component.empty() : component.style(builder -> TextCompat.shadow(builder, 0));
        if (bl) {
            this.minimapOverlays.remove(uUID);
        } else {
            this.minimapOverlays.put(uUID, (Component)textComponent);
        }
        BossBarHandler bossBarHandler = this.protocolBars.get(uUID);
        if (bossBarHandler != null) {
            bossBarHandler.setMinimapOverlay((Component)textComponent);
            return true;
        }
        return !this.protocolEnabled && this.bars.containsKey(uUID);
    }

    public void clearMinimapOverlay(Player player) {
        this.setMinimapOverlay(player, (Component)Component.empty());
    }

    public void setConversationOverlay(Player player, Component component) {
        TextComponent textComponent;
        Object object = textComponent = component == null ? Component.empty() : component;
        if (textComponent.equals((Object)Component.empty())) {
            this.conversationOverlays.remove(player.getUniqueId());
        } else {
            this.conversationOverlays.put(player.getUniqueId(), (Component)textComponent);
        }
        BossBarHandler bossBarHandler = this.protocolBars.get(player.getUniqueId());
        if (bossBarHandler != null) {
            bossBarHandler.setConversationOverlay((Component)textComponent);
        }
    }

    public void setQuestLogOverlay(Player player, Component component) {
        TextComponent textComponent;
        Object object = textComponent = component == null ? Component.empty() : component;
        if (textComponent.equals((Object)Component.empty())) {
            this.questLogOverlays.remove(player.getUniqueId());
        } else {
            this.questLogOverlays.put(player.getUniqueId(), (Component)textComponent);
        }
        BossBarHandler bossBarHandler = this.protocolBars.get(player.getUniqueId());
        if (bossBarHandler != null) {
            bossBarHandler.setQuestLogOverlay((Component)textComponent);
        }
    }

    public void refreshTheme(Player player) {
        BossBarHandler bossBarHandler = this.protocolBars.get(player.getUniqueId());
        if (bossBarHandler != null) {
            bossBarHandler.refreshTheme();
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onHeldItem(PlayerItemHeldEvent playerItemHeldEvent) {
        if (!PackHostingSetup.ready(playerItemHeldEvent.getPlayer()) || !HotbarOverlay.enabled(this.plugin, playerItemHeldEvent.getPlayer())) {
            return;
        }
        BossBarHandler bossBarHandler = this.protocolBars.get(playerItemHeldEvent.getPlayer().getUniqueId());
        if (bossBarHandler != null) {
            bossBarHandler.refreshHotbar(playerItemHeldEvent.getNewSlot());
        }
    }

    public boolean hasMinimapOverlay(Player player) {
        UUID uUID = player.getUniqueId();
        return this.minimapOverlays.containsKey(uUID) && (this.protocolEnabled ? this.protocolBars.containsKey(uUID) : this.bars.containsKey(uUID));
    }

    public void showPopup(Player player, String string, String[] stringArray, long l) {
        if (this.plugin.popups().has(string)) {
            long l2 = System.currentTimeMillis() + l * 50L;
            this.popups.put(player.getUniqueId(), new PopupState(string, stringArray, l2));
        }
    }

    private void tick() {
        if (!PackHostingSetup.ready()) {
            return;
        }
        boolean bl = this.renderTick++ % this.idleRenderPeriod == 0L;
        PackRegistry packRegistry = this.plugin.packs().registry();
        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bossBar;
            BossBarHandler bossBarHandler;
            HudPopup hudPopup;
            Component component;
            Object object2;
            if (!PackHostingSetup.ready(player)) {
                this.detach(player);
                continue;
            }
            if (!bl && (this.plugin.gps() == null || !this.plugin.gps().navigation().hasActiveNavigation(player))) continue;
            TextComponent textComponent = Component.empty();
            TextComponent textComponent2 = Component.empty();
            String string = this.plugin.storage().getLayout(player.getUniqueId());
            if (string != null && this.plugin.layouts().has(string)) {
                object2 = this.plugin.layouts().get(string);
                ArrayList<HudElement> arrayList = new ArrayList<HudElement>();
                ArrayList<HudElement> arrayList2 = new ArrayList<HudElement>();
                this.splitAirElements(((HudLayout)object2).getElements(), arrayList, arrayList2);
                textComponent = textComponent.append(this.renderHolder("layout", ((HudLayout)object2).getKey(), arrayList, player, packRegistry, null));
                textComponent2 = textComponent2.append(this.renderHolder("layout", ((HudLayout)object2).getKey(), arrayList2, player, packRegistry, null));
            } else if (this.plugin.displays().hasDisplays()) {
                for (DisplayManager.DisplayEntry displayEntry : this.plugin.displays().getDisplays()) {
                    if (displayEntry.conditions().result(player).isHidden()) continue;
                    HudLayout hudLayout = displayEntry.layout();
                    component = new ArrayList();
                    ArrayList<HudElement> arrayList = new ArrayList<HudElement>();
                    this.splitAirElements(hudLayout.getElements(), (List<HudElement>)component, (List<HudElement>)arrayList);
                    textComponent = textComponent.append(this.renderHolder("layout", displayEntry.layoutKey(), (Iterable<HudElement>)component, player, packRegistry, null));
                    textComponent2 = textComponent2.append(this.renderHolder("layout", displayEntry.layoutKey(), arrayList, player, packRegistry, null));
                }
            } else {
                for (HudLayout hudLayout : this.plugin.layouts().getDefaults()) {
                    ArrayList<HudElement> arrayList = new ArrayList<HudElement>();
                    component = new ArrayList();
                    this.splitAirElements(hudLayout.getElements(), (List<HudElement>)arrayList, (List<HudElement>)component);
                    textComponent = textComponent.append(this.renderHolder("layout", hudLayout.getKey(), arrayList, player, packRegistry, null));
                    textComponent2 = textComponent2.append(this.renderHolder("layout", hudLayout.getKey(), (Iterable<HudElement>)component, player, packRegistry, null));
                }
            }
            object2 = this.popups.get(player.getUniqueId());
            if (object2 != null && System.currentTimeMillis() <= ((PopupState)object2).expireAt() && (hudPopup = this.plugin.popups().get(((PopupState)object2).popupKey())) != null) {
                textComponent = textComponent.append(this.renderHolder("popup", hudPopup.getKey(), hudPopup.getElements(), player, packRegistry, ((PopupState)object2).args()));
            }
            int n = Math.round(packRegistry.metrics().measure((Component)textComponent).advance());
            textComponent = textComponent.append(RenderManager.shift(this.playerPanel.render(player, packRegistry).append(this.targetPanel.render(player, packRegistry)), n / 2 - n));
            if (!HudAccess.hud(player) || !this.plugin.storage().preference(player.getUniqueId(), "hud", true)) {
                textComponent = Component.empty();
                textComponent2 = Component.empty();
            }
            component = this.partyPanel.render(player, packRegistry).append(this.gpsPanel.render(player, packRegistry));
            int n2 = Math.round(packRegistry.metrics().measure((Component)textComponent).advance());
            textComponent = textComponent.append(RenderManager.shift(component, n2 / 2 - n2));
            if (this.protocolEnabled) {
                bossBarHandler = this.protocolBars.get(player.getUniqueId());
                if (bossBarHandler == null) {
                    bossBarHandler = new BossBarHandler(player);
                    this.protocolBars.put(player.getUniqueId(), bossBarHandler);
                    bossBarHandler.setMinimapOverlay(this.minimapOverlays.getOrDefault(player.getUniqueId(), (Component)Component.empty()));
                    bossBarHandler.init();
                }
                int n3 = Math.round(packRegistry.metrics().measure((Component)textComponent).advance());
                bossBarHandler.setHud(textComponent.append(RenderManager.shift((Component)textComponent2, n3 / 2 - n3)).style(builder -> TextCompat.shadow(builder, 0)));
                continue;
            }
            bossBarHandler = this.bars.computeIfAbsent(player.getUniqueId(), uUID -> {
                BossBar bossBar = BossBar.bossBar((Component)Component.empty(), (float)0.0f, (BossBar.Color)BossBar.Color.YELLOW, (BossBar.Overlay)BossBar.Overlay.PROGRESS);
                player.showBossBar(bossBar);
                return bossBar;
            });
            Component component2 = ThemeStyle.paint(textComponent.style(builder -> TextCompat.shadow(builder, 0)), this.plugin.storage().theme(player.getUniqueId()));
            component2 = this.plugin.avatars().paint(player, component2);
            bossBarHandler.name(BossBarHandler.combineCentered(this.minimapOverlays.getOrDefault(player.getUniqueId(), (Component)Component.empty()).append(HotbarOverlay.render(this.plugin, player)), component2, (int)Math.ceil(packRegistry.metrics().measure(component2).advance())));
            if (this.isUnderwater(player)) {
                bossBar = this.airBars.computeIfAbsent(player.getUniqueId(), uUID -> {
                    BossBar bossBar = BossBar.bossBar((Component)Component.empty(), (float)0.0f, (BossBar.Color)BossBar.Color.YELLOW, (BossBar.Overlay)BossBar.Overlay.PROGRESS);
                    player.showBossBar(bossBar);
                    return bossBar;
                });
                bossBar.name(textComponent2.style(builder -> TextCompat.shadow(builder, 0)));
                continue;
            }
            bossBar = this.airBars.remove(player.getUniqueId());
            if (bossBar == null) continue;
            player.hideBossBar(bossBar);
        }
    }

    private void splitAirElements(Iterable<HudElement> iterable, List<HudElement> list, List<HudElement> list2) {
        for (HudElement hudElement : iterable) {
            if (hudElement == null || !hudElement.getKey().startsWith("enorialshud-air-")) {
                list.add(hudElement);
                continue;
            }
            list2.add(hudElement);
        }
    }

    /*
     * WARNING - void declaration
     */
    private Component renderHolder(String string, String string2, Iterable<HudElement> iterable, Player player, PackRegistry packRegistry, String[] stringArray) {
        TextComponent textComponent = Component.empty();
        ArrayList<HudElement> arrayList = new ArrayList<HudElement>();
        ArrayList<HudElement> arrayList2 = new ArrayList<HudElement>();
        ArrayList<HudElement> arrayList3 = new ArrayList<HudElement>();
        for (HudElement object2 : iterable) {
            if (!"layout".equalsIgnoreCase(string) || !object2.getKey().startsWith("enorialshud-air-")) {
                if ("layout".equalsIgnoreCase(string) && "target-frame".equalsIgnoreCase(object2.getKey())) {
                    arrayList3.add(object2);
                    continue;
                }
                arrayList.add(object2);
                continue;
            }
            arrayList2.add(object2);
        }
        arrayList2.sort(Comparator.comparingInt(hudElement -> "enorialshud-air-frame".equals(hudElement.getKey()) ? 0 : 1));
        arrayList.addAll(arrayList2);
        arrayList.addAll(arrayList3);
        Object object3 = null;
        Object var12_13 = null;
        Float f = null;
        Float f2 = null;
        float f3 = 0.0f;
        float f4 = 0.0f;
        for (HudElement hudElement2 : arrayList) {
            float f5;
            HudLayer hudLayer2;
            ConditionHolder.ConditionResult conditionResult;
            if (!this.shouldRenderDynamicElement(string, hudElement2, player) || (conditionResult = hudElement2.getAsset().getConditions().result(player)).isHidden()) continue;
            TextComponent textComponent2 = Component.empty();
            RelativeWidth relativeWidth = null;
            for (HudLayer hudLayer2 : hudElement2.getAsset().getLayers()) {
                Object object;
                Object object2;
                ConditionHolder.ConditionResult conditionResult2 = hudLayer2.getConditions().result(player);
                if (conditionResult2.isHidden()) continue;
                if (hudLayer2 instanceof TextureLayer) {
                    TextureLayer hudLayer3 = (TextureLayer)hudLayer2;
                    object2 = conditionResult2.hasTexturePath() ? conditionResult2.getTexturePath() : hudLayer3.getTexturePath();
                    object = this.renderTexture(string, string2, hudElement2, hudLayer3, player, packRegistry, (String)object2);
                    if (object == null || ((RenderedTexture)object).component() == null) continue;
                    textComponent2 = textComponent2.append(((RenderedTexture)object).component());
                    if (((RenderedTexture)object).relativeWidth() == null) continue;
                    relativeWidth = relativeWidth == null ? ((RenderedTexture)object).relativeWidth() : relativeWidth.merge(((RenderedTexture)object).relativeWidth());
                    continue;
                }
                object2 = null;
                if (!(hudLayer2 instanceof TextLayer) || (object2 = this.renderText(string, string2, hudElement2, (TextLayer)(object = (TextLayer)hudLayer2), player, packRegistry, stringArray, conditionResult2)) == null || ((RenderedText)object2).component() == null) continue;
                textComponent2 = textComponent2.append(((RenderedText)object2).component());
                if (((RenderedText)object2).relativeWidth() == null) continue;
                relativeWidth = relativeWidth == null ? ((RenderedText)object2).relativeWidth() : relativeWidth.merge(((RenderedText)object2).relativeWidth());
            }
            if (relativeWidth != null) {
                textComponent2 = hudElement2.getAlignment().relativeAlign((Component)textComponent2, relativeWidth);
            }
            if (conditionResult.hasColor()) {
                textComponent2 = textComponent2.color(conditionResult.getColor());
            }
            String string3 = hudElement2.getKey().replace("-compact", "");
            hudLayer2 = hudElement2.getAsset().getLayers().iterator().next();
            float f6 = hudElement2.getScreenPosition().getOffset().getX() + hudLayer2.getOffset().getX();
            if ("enorialshud-panel".equals(string3)) {
                float f7 = f4 + packRegistry.metrics().measure((Component)textComponent2).left() + f6;
                f = Float.valueOf(f7 + 124.0f);
                f2 = Float.valueOf(f7 + 52.0f);
            }
            if ("enorialshud-avatar-head".equals(string3)) {
                Float f8 = Float.valueOf(f4 + packRegistry.metrics().measure((Component)textComponent2).center() + f6);
            }
            if (f != null && Set.of("enorialshud-health-text", "enorialshud-mana-text", "enorialshud-food-text").contains(string3) && Float.isFinite(f5 = packRegistry.metrics().firstCenter((Component)textComponent2, 47))) {
                int n = Math.round(f.floatValue() - f4 - f6 - f5);
                textComponent2 = RenderManager.shift((Component)textComponent2, n);
            }
            if (string3.startsWith("enorialshud-effects-") && f2 != null) {
                textComponent2 = RenderManager.shift((Component)textComponent2, Math.round(f2.floatValue() - f4 - f6));
            }
            if ("corehud-armor-icon".equals(hudElement2.getKey()) || "corehud-armor-text".equals(hudElement2.getKey())) {
                HudLayer hudLayer3 = hudElement2.getAsset().getLayers().iterator().next();
                float f9 = f4 + packRegistry.metrics().measure((Component)textComponent2).center() + (float)hudElement2.getScreenPosition().getOffset().getX() + (float)hudLayer3.getOffset().getX();
                if ("corehud-armor-icon".equals(hudElement2.getKey())) {
                    void var12_14;
                    if (var12_14 != null) {
                        int n = Math.round(var12_14.floatValue() - f9);
                        textComponent2 = RenderManager.shift((Component)textComponent2, n);
                        f9 += (float)n;
                    }
                    object3 = Float.valueOf(f9);
                    f3 = hudElement2.getScreenPosition().getRelativeX();
                } else if (object3 != null && f3 == hudElement2.getScreenPosition().getRelativeX()) {
                    int n = Math.round(((Float)object3).floatValue() - f9);
                    textComponent2 = ((TextComponent)((TextComponent)Component.empty().append(SpaceUtil.space(n))).append((Component)textComponent2)).append(SpaceUtil.space(-n));
                }
            }
            if ("target-frame".equals(hudElement2.getKey()) || "popup".equals(string) || string3.startsWith("enorialshud-air-") || string3.startsWith("enorialshud-effects-")) {
                textComponent2 = packRegistry.metrics().neutral((Component)textComponent2);
            }
            if ("target-frame".equals(string3)) {
                textComponent2 = RenderManager.shift((Component)textComponent2, (int)(f4 / 2.0f) - Math.round(f4));
            }
            if (("enorialshud-mana-fill".equals(string3) || "enorialshud-mana-text".equals(string3)) && !this.plugin.listeners().hasResource(player)) {
                textComponent2 = SpaceUtil.space(Math.round(packRegistry.metrics().measure((Component)textComponent2).advance()));
            }
            f4 += packRegistry.metrics().measure((Component)textComponent2).advance();
            textComponent = textComponent.append((Component)textComponent2);
        }
        return textComponent;
    }

    private boolean shouldRenderDynamicElement(String string, HudElement hudElement, Player player) {
        if ("layout".equalsIgnoreCase(string) && hudElement != null && hudElement.getKey() != null) {
            if ("target-frame".equals(hudElement.getKey()) && this.getCombatTarget(player) != null) {
                return false;
            }
            String string2 = hudElement.getKey().replace("-compact", "");
            if (string2.startsWith("enorialshud-effects-") || Set.of("enorialshud-panel", "enorialshud-avatar-head", "enorialshud-health-fill", "enorialshud-health-text", "enorialshud-mana-fill", "enorialshud-mana-text", "enorialshud-hunger-fill", "enorialshud-food-text", "corehud-armor-icon", "corehud-armor-text").contains(string2)) {
                return false;
            }
            if ((Set.of("enorialshud-panel", "enorialshud-hunger-fill", "enorialshud-food-text").contains(string2) || string2.startsWith("enorialshud-effects-")) && hudElement.getKey().endsWith("-compact") == this.plugin.listeners().hasResource(player)) {
                return false;
            }
            if (string2.startsWith("enorialshud-air-")) {
                return this.isUnderwater(player);
            }
            return switch (hudElement.getKey().toLowerCase(Locale.ROOT)) {
                case "event-info" -> this.isEventActiveOrAnnounced(player);
                case "arena-queue" -> this.isArenaQueueActive(player);
                case "party-buff" -> this.hasActiveBuffs(player);
                case "enorialshud-air-fill", "enorialshud-air-text" -> this.isUnderwater(player);
                default -> true;
            };
        }
        return true;
    }

    private static Component shift(Component component, int n) {
        return n == 0 ? component : ((TextComponent)((TextComponent)Component.empty().append(SpaceUtil.space(n))).append(component)).append(SpaceUtil.space(-n));
    }

    private boolean isUnderwater(Player player) {
        if (player.getRemainingAir() < player.getMaximumAir()) {
            return true;
        }
        Material material = player.getEyeLocation().getBlock().getType();
        return material == Material.WATER || material == Material.BUBBLE_COLUMN;
    }

    private boolean isEventActiveOrAnnounced(Player player) {
        if (!(this.isTruthyPlaceholder(player, "%enorialsevents_active%") || this.isTruthyPlaceholder(player, "%enorialsevents_in_progress%") || this.isTruthyPlaceholder(player, "%enorialsevents_announced%"))) {
            String[] stringArray = new String[]{"%enorialsevents_event%", "%enorialsevents_event_name%", "%enorialsevents_current_event%", "%enorialsevents_status%", "%enorialsevents_type%"};
            Set<String> set = Set.of("none", "no_event", "no event", "idle", "false", "0", "-", "n/a", "null", "unknown");
            for (String string : stringArray) {
                if (!this.isMeaningfulPlaceholder(player, string, set)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean isArenaQueueActive(Player player) {
        return !this.isTruthyPlaceholder(player, "%enorialsarena_in_queue%") && !this.isTruthyPlaceholder(player, "%enorialsarena_queueing%") && !this.isTruthyPlaceholder(player, "%enorialsarena_queue_active%") ? this.isMeaningfulPlaceholder(player, "%enorialsarena_queue_mode%", Set.of("none", "false", "0", "-", "n/a", "null", "off", "unknown")) : true;
    }

    private boolean hasActiveBuffs(Player player) {
        return this.isMeaningfulPlaceholder(player, "%enorialsspells_buffs%", Set.of("none", "no buffs", "false", "0", "-", "n/a", "null", "empty"));
    }

    private boolean isTruthyPlaceholder(Player player, String string) {
        String string2 = this.resolvePlaceholderValue(player, string);
        if (string2 == null) {
            return false;
        }
        String string3 = string2.trim().toLowerCase(Locale.ROOT);
        return string3.equals("true") || string3.equals("1") || string3.equals("yes") || string3.equals("on");
    }

    private boolean isMeaningfulPlaceholder(Player player, String string, Set<String> set) {
        String string2 = this.resolvePlaceholderValue(player, string);
        if (string2 == null) {
            return false;
        }
        String string3 = string2.trim();
        if (string3.isEmpty()) {
            return false;
        }
        String string4 = string3.toLowerCase(Locale.ROOT);
        return string4.contains("%") ? false : !set.contains(string4);
    }

    private String resolvePlaceholderValue(Player player, String string) {
        if (string != null && !string.isEmpty()) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                return string;
            }
            try {
                return PlaceholderAPI.setPlaceholders((Player)player, (String)string);
            }
            catch (Throwable throwable) {
                return string;
            }
        }
        return null;
    }

    private RenderedTexture renderTexture(String string, String string2, HudElement hudElement, TextureLayer textureLayer, Player player, PackRegistry packRegistry, String string3) {
        PackRegistry.TextureSteps textureSteps = packRegistry.getTextureSteps(string, string2, hudElement.getKey(), textureLayer.getKey(), string3);
        if (textureSteps != null) {
            int n = this.getStepIndex(player, textureSteps.function(), textureSteps.steps().size());
            if (n <= 0) {
                return null;
            }
            PackRegistry.GlyphInfo glyphInfo = textureSteps.steps().get(n - 1);
            Component component = glyphInfo.component(packRegistry);
            if ("target-frame".equals(hudElement.getKey()) || "enorialshud-air-fill".equals(hudElement.getKey())) {
                int n2 = textureSteps.steps().get((int)(textureSteps.steps().size() - 1)).width;
                return new RenderedTexture(packRegistry.metrics().neutral(RenderManager.shift(component, -n2 / 2)), null);
            }
            if ("enorialshud-health-fill".equals(hudElement.getKey())) {
                component = component.color(TextColor.color((int)HudStatus.snapshot(player).tone().rgb()));
            }
            SizedComponent sizedComponent = new SizedComponent(component, glyphInfo.width);
            RelativeWidth relativeWidth = textureLayer.getAlignment().calculateRelativeWidth(glyphInfo.width, textureLayer.getOffset());
            Component component2 = textureLayer.getAlignment().align(sizedComponent);
            component2 = this.applyHorizontalOffset(component2, player, hudElement, textureLayer);
            return new RenderedTexture(component2, relativeWidth);
        }
        PackRegistry.GlyphInfo glyphInfo = packRegistry.getTexture(string, string2, hudElement.getKey(), textureLayer.getKey(), string3);
        if (glyphInfo == null) {
            return null;
        }
        SizedComponent sizedComponent = new SizedComponent(glyphInfo.component(packRegistry), glyphInfo.width);
        RelativeWidth relativeWidth = textureLayer.getAlignment().calculateRelativeWidth(glyphInfo.width, textureLayer.getOffset());
        Component component = textureLayer.getAlignment().align(sizedComponent);
        component = this.applyHorizontalOffset(component, player, hudElement, textureLayer);
        return new RenderedTexture(component, relativeWidth);
    }

    private RenderedText renderText(String string, String string2, HudElement hudElement, TextLayer textLayer, Player player, PackRegistry packRegistry, String[] stringArray, ConditionHolder.ConditionResult conditionResult) {
        int n;
        String string3;
        String string4 = packRegistry.getTextFont(string, string2, hudElement.getKey(), textLayer.getKey());
        if (hudElement.getKey().startsWith("enorialshud-effects-")) {
            String string5 = hudElement.getKey().replace("-compact", "");
            int n2 = Integer.parseInt(string5.substring(string5.lastIndexOf(45) + 1));
            Component component = this.renderEffects(player, string4, hudElement.getKey().contains("timers"), n2, packRegistry);
            return new RenderedText(packRegistry.metrics().neutral(component), null);
        }
        String string6 = this.resolveText(player, textLayer.getText(), stringArray);
        Component component = this.legacyToComponent(string6);
        if (string4 != null) {
            component = LegacyText.font(component, Key.key((String)string4));
        }
        component = component.style(builder -> TextCompat.shadow(builder, 0));
        if (conditionResult != null && conditionResult.hasColor()) {
            component = component.color(conditionResult.getColor());
        }
        if (("target-frame".equals(hudElement.getKey()) || "enorialshud-air-text".equals(hudElement.getKey())) && textLayer.getAlignment() == HudAlignment.CENTER) {
            Component component2 = RenderManager.shift(component, -Math.round(packRegistry.metrics().measure(component).center()));
            component2 = packRegistry.metrics().neutral(component2);
            return new RenderedText(this.applyHorizontalOffset(component2, player, hudElement, textLayer), null);
        }
        switch (hudElement.getKey().replace("-compact", "")) {
            case "enorialshud-health-text": 
            case "enorialshud-food-text": {
                String string7 = "88/88";
                break;
            }
            case "enorialshud-mana-text": {
                String string7 = "888/888";
                break;
            }
            case "corehud-armor-text": {
                String string7 = "88";
                break;
            }
            default: {
                String string7 = string3 = null;
            }
        }
        if (string3 != null) {
            Component component3 = Component.text((String)string3).style(component.style());
            if ("corehud-armor-text".equals(hudElement.getKey())) {
                component3 = component3.decorate(TextDecoration.BOLD);
            }
            component = string3.contains("/") ? packRegistry.metrics().fieldAt(component, component3, 47) : packRegistry.metrics().field(component, component3);
            n = this.estimateMonoDigitsWidth(string3);
        } else {
            n = this.estimateTextWidth(hudElement, textLayer, string4, string6);
        }
        SizedComponent sizedComponent = new SizedComponent(component, n);
        RelativeWidth relativeWidth = textLayer.getAlignment().calculateRelativeWidth(n, textLayer.getOffset());
        Component component4 = textLayer.getAlignment().align(sizedComponent);
        component4 = this.applyHorizontalOffset(component4, player, hudElement, textLayer);
        return new RenderedText(component4, relativeWidth);
    }

    public Component renderEffects(Player player, String string, boolean bl, int n, PackRegistry packRegistry) {
        if (!HudAccess.has((CommandSender)player, "enorials.hud.effects") || !this.plugin.storage().preference(player.getUniqueId(), "effects", true)) {
            return Component.empty();
        }
        TextComponent textComponent = Component.empty();
        List list = HudStatus.snapshot(player).effects().stream().skip((long)n * 12L).limit(12L).toList();
        if (string == null) {
            return textComponent;
        }
        for (HudStatus.Effect effect : list) {
            TextComponent textComponent2 = Component.empty();
            if (bl) {
                var11_11 = effect.timer();
                if (((String)var11_11).endsWith("s")) {
                    var11_11 = ((String)var11_11).substring(0, ((String)var11_11).length() - 1);
                }
                if (((String)var11_11).length() > 3 && effect.remainingTicks() > 0) {
                    var11_11 = Math.max(1, effect.remainingTicks() / 72000) + "h";
                }
                var12_12 = HudEffectText.timer((String)var11_11, string);
                textComponent2 = HudEffectText.center(var12_12, string, packRegistry.metrics());
            } else {
                var11_11 = this.effectGlyphs.getProperty(effect.key());
                if (var11_11 != null) {
                    var12_12 = LegacyText.font((Component)Component.text((char)((char)Integer.parseInt((String)var11_11))), Key.key((String)string));
                    Component component = LegacyText.font((Component)Component.text((char)(effect.harmful() ? (char)'\ue901' : '\ue900')), Key.key((String)string));
                    textComponent2 = packRegistry.metrics().neutral(var12_12).append(component);
                }
            }
            textComponent = textComponent.append(packRegistry.metrics().neutral((Component)textComponent2)).append(SpaceUtil.space(12));
        }
        return packRegistry.metrics().neutral((Component)textComponent).style(builder -> TextCompat.shadow(builder, 0));
    }

    private int estimateTextWidth(HudElement hudElement, TextLayer textLayer, String string, String string2) {
        String string3;
        if (hudElement != null && "target-frame".equalsIgnoreCase(hudElement.getKey())) {
            return this.estimateVanillaWidth(string2);
        }
        String string4 = textLayer != null ? textLayer.getFont() : "";
        String string5 = string3 = string != null ? string.toLowerCase() : "";
        if ("enorialsavatar".equalsIgnoreCase(string4) || string3.contains("enorialsavatar")) {
            return Math.max(1, this.plugin.cfg().getInt("avatars.glyph-width", 40));
        }
        return !"default-small-mono".equalsIgnoreCase(string4) && !string3.contains("mono") ? TextWidthEstimator.estimate(string2) : this.estimateMonoDigitsWidth(string2);
    }

    private int estimateMonoDigitsWidth(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.replaceAll("\u0102\u201a\u00c2\u00a7[0-9A-FK-ORa-fk-or]", "");
            string2 = string2.replaceAll("<#[0-9a-fA-F]{6}>", "");
            string2 = string2.replaceAll("<[^>]+>", "");
            int n = 0;
            for (int i = 0; i < string2.length(); ++i) {
                char c = string2.charAt(i);
                n += (switch (c) {
                    case ' ', '\u200c' -> 4;
                    case '-', '.', '/', ':' -> 4;
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> 4;
                    default -> 6;
                });
            }
            return n;
        }
        return 0;
    }

    private int estimateVanillaWidth(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.replaceAll("\u0102\u201a\u00c2\u00a7[0-9A-FK-ORa-fk-or]", "");
            string2 = string2.replaceAll("<#[0-9a-fA-F]{6}>", "");
            string2 = string2.replaceAll("<[^>]+>", "");
            int n = 0;
            for (int i = 0; i < string2.length(); ++i) {
                n += this.vanillaCharWidth(string2.charAt(i));
            }
            return n;
        }
        return 0;
    }

    private int vanillaCharWidth(char c) {
        return switch (c) {
            case ' ', '\u200c' -> 4;
            case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2;
            case '\"', '(', ')', 'I', '[', ']', '`', 'l', 't', '{', '}' -> 4;
            case '<', '>', 'f', 'k' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }

    private int getStepIndex(Player player, FunctionData functionData, int n) {
        if (functionData != null && n > 0) {
            double d = this.plugin.listeners().getCurrent(player, functionData.listener());
            double d2 = this.plugin.listeners().getMax(player, functionData.listener());
            if (d2 <= 0.0) {
                return 0;
            }
            double d3 = Math.max(0.0, Math.min(1.0, d / d2));
            return (int)Math.ceil(d3 * (double)n);
        }
        return 0;
    }

    private String resolveText(Player player, String string, String[] stringArray) {
        String string2 = string;
        if (stringArray != null) {
            for (int i = 0; i < stringArray.length; ++i) {
                string2 = string2.replace("%arg-" + (i + 1) + "%", stringArray[i]);
            }
        }
        String string3 = this.plugin.cfg().getString("decimal-format", "00##");
        string2 = string2.replace("%health-bar-value%", this.formatListenerValue("health", this.plugin.listeners().getCurrent(player, "health"), string3));
        string2 = string2.replace("%health-bar-max%", this.formatListenerValue("health", this.plugin.listeners().getMax(player, "health"), string3));
        string2 = string2.replace("%resource-bar-value%", this.formatListenerValue("resource", this.plugin.listeners().getCurrent(player, "resource"), string3));
        string2 = string2.replace("%resource-bar-max%", this.formatListenerValue("resource", this.plugin.listeners().getMax(player, "resource"), string3));
        string2 = string2.replace("%food-bar-value%", this.formatListenerValue("food", this.plugin.listeners().getCurrent(player, "food"), string3));
        string2 = string2.replace("%food-bar-max%", this.formatListenerValue("food", this.plugin.listeners().getMax(player, "food"), string3));
        string2 = string2.replace("%armor-bar-value%", this.formatListenerValue("armor", this.plugin.listeners().getCurrent(player, "armor"), string3));
        string2 = string2.replace("%armor-bar-max%", this.formatListenerValue("armor", this.plugin.listeners().getMax(player, "armor"), string3));
        string2 = string2.replace("%air-bar-value%", this.formatListenerValue("air", this.plugin.listeners().getCurrent(player, "air"), string3));
        string2 = string2.replace("%air-bar-max%", this.formatListenerValue("air", this.plugin.listeners().getMax(player, "air"), string3));
        string2 = string2.replace("%coord-x%", String.valueOf(player.getLocation().getBlockX()));
        string2 = string2.replace("%coord-y%", String.valueOf(player.getLocation().getBlockY()));
        string2 = string2.replace("%coord-z%", String.valueOf(player.getLocation().getBlockZ()));
        string2 = string2.replace("%enorialshud_avatar_giantHead%", this.plugin.avatars().getAvatarGlyph(player));
        TargetSnapshot targetSnapshot = this.resolveTarget(player);
        string2 = string2.replace("%target_exists%", targetSnapshot.exists ? "true" : "false");
        string2 = string2.replace("%target_name%", targetSnapshot.name);
        string2 = string2.replace("%target_health%", targetSnapshot.health);
        string2 = string2.replace("%target_max_health%", targetSnapshot.maxHealth);
        string2 = string2.replace("%target_distance%", targetSnapshot.distance);
        string2 = string2.replace("%target_hp_percent%", targetSnapshot.hpPercent);
        string2 = string2.replace("%target_hp_bar%", targetSnapshot.hpBar);
        for (String string4 : this.plugin.listeners().getListeners().keySet()) {
            double d = this.plugin.listeners().getCurrent(player, string4);
            double d2 = this.plugin.listeners().getMax(player, string4);
            string2 = string2.replace("%" + string4 + "-bar-value%", this.formatListenerValue(string4, d, string3));
            string2 = string2.replace("%" + string4 + "-bar-max%", this.formatListenerValue(string4, d2, string3));
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                string2 = PlaceholderAPI.setPlaceholders((Player)player, (String)string2);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return string2;
    }

    private String formatListenerValue(String string, double d, String string2) {
        if (!Double.isFinite(d)) {
            return "0";
        }
        if ("air".equalsIgnoreCase(string)) {
            return String.valueOf((int)Math.round(Math.max(0.0, d)));
        }
        if ("resource".equalsIgnoreCase(string) || "mana".equalsIgnoreCase(string)) {
            return String.format(Locale.ROOT, "%03d", (int)Math.round(d));
        }
        if ("armor".equalsIgnoreCase(string)) {
            return String.format(Locale.ROOT, "%02d", (int)Math.round(d));
        }
        return this.shouldUseWholeNumber(string) ? String.valueOf((int)Math.round(d)) : FunctionalListeners.format(d, string2);
    }

    private boolean shouldUseWholeNumber(String string) {
        if (string == null) {
            return false;
        }
        return switch (string.toLowerCase()) {
            case "health", "resource", "food", "armor", "air", "mana", "rage", "targethp" -> true;
            default -> false;
        };
    }

    private TargetSnapshot resolveTarget(Player player) {
        LivingEntity livingEntity = this.getCurrentTarget(player);
        if (livingEntity == null) {
            return TargetSnapshot.empty();
        }
        double d = Math.max(0.0, livingEntity.getHealth());
        double d2 = Math.max(1.0, livingEntity.getAttribute(HudAttributes.MAX_HEALTH) != null ? livingEntity.getAttribute(HudAttributes.MAX_HEALTH).getValue() : livingEntity.getMaxHealth());
        double d3 = player.getLocation().distance(livingEntity.getLocation());
        double d4 = Math.max(0.0, Math.min(1.0, d / d2));
        int n = 14;
        int n2 = (int)Math.round(d4 * (double)n);
        String string = "<green>" + "|".repeat(Math.max(0, n2)) + "<dark_gray>" + "|".repeat(Math.max(0, n - n2));
        String string2 = this.plugin.cfg().getString("decimal-format", "00##");
        String string3 = String.valueOf((int)Math.round(d));
        String string4 = String.valueOf((int)Math.round(d2));
        String string5 = String.valueOf((int)Math.round(d3));
        String string6 = String.valueOf((int)Math.round(d4 * 100.0));
        String string7 = this.plugin.listeners().getTargetName(player, livingEntity);
        return new TargetSnapshot(true, string7, string3, string4, string5, string6, string);
    }

    public LivingEntity getCombatTarget(Player player) {
        Mob mob;
        LivingEntity livingEntity;
        if (player == null || !player.isOnline()) {
            return null;
        }
        CombatTarget combatTarget = this.combatTargets.get(player.getUniqueId());
        if (combatTarget == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity((UUID)combatTarget.targetId());
        if (!(entity instanceof LivingEntity) || !(livingEntity = (LivingEntity)entity).isValid() || livingEntity.isDead() || !livingEntity.getWorld().equals((Object)player.getWorld()) || livingEntity.getLocation().distanceSquared(player.getLocation()) > Math.pow(Math.max(8.0, this.plugin.cfg().getDouble("target-frame.combat-max-distance", 64.0)), 2.0)) {
            this.combatTargets.remove(player.getUniqueId());
            return null;
        }
        boolean bl = livingEntity instanceof Mob && (mob = (Mob)livingEntity).getTarget() != null && mob.getTarget().isValid() && !mob.getTarget().isDead();
        long l = System.currentTimeMillis();
        if (bl) {
            this.combatTargets.put(player.getUniqueId(), new CombatTarget(livingEntity.getUniqueId(), l));
        } else if (l - combatTarget.lastHitAtMs() > this.getTargetFrameTimeoutMs()) {
            this.combatTargets.remove(player.getUniqueId());
            return null;
        }
        return livingEntity;
    }

    public LivingEntity getCurrentTarget(Player player) {
        LivingEntity livingEntity = this.getCombatTarget(player);
        return livingEntity != null ? livingEntity : (player != null && player.isOnline() ? this.resolveLookTarget(player) : null);
    }

    private LivingEntity resolveLookTarget(Player player) {
        LivingEntity livingEntity;
        double d = Math.max(3.0, this.plugin.cfg().getDouble("target-frame.max-distance", 32.0));
        RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), d, entity -> {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                if (entity.equals((Object)player)) {
                    return false;
                }
                return livingEntity instanceof Player ? false : !entity.isDead();
            }
            return false;
        });
        Entity entity2 = rayTraceResult != null ? rayTraceResult.getHitEntity() : null;
        return entity2 instanceof LivingEntity ? (livingEntity = (LivingEntity)entity2) : null;
    }

    private Player resolveAttacker(Entity entity) {
        Player player;
        Projectile projectile;
        ProjectileSource projectileSource;
        if (entity instanceof Player) {
            Player player2 = (Player)entity;
            return player2;
        }
        return entity instanceof Projectile && (projectileSource = (projectile = (Projectile)entity).getShooter()) instanceof Player ? (player = (Player)projectileSource) : null;
    }

    private long getTargetFrameTimeoutMs() {
        return Math.max(1000L, this.plugin.cfg().getLong("target-frame.timeout-ms", 10000L));
    }

    private Component legacyToComponent(String string) {
        String string2 = this.convertMiniToLegacy(string);
        return LegacyComponentSerializer.legacySection().deserialize(string2);
    }

    private String convertMiniToLegacy(String string) {
        String string2 = string;
        string2 = string2.replace("<white>", ChatColor.WHITE.toString());
        string2 = string2.replace("<black>", ChatColor.BLACK.toString());
        string2 = string2.replace("<red>", ChatColor.RED.toString());
        string2 = string2.replace("<green>", ChatColor.GREEN.toString());
        string2 = string2.replace("<blue>", ChatColor.BLUE.toString());
        string2 = string2.replace("<yellow>", ChatColor.YELLOW.toString());
        string2 = string2.replace("<gray>", ChatColor.GRAY.toString());
        string2 = string2.replace("<dark_gray>", ChatColor.DARK_GRAY.toString());
        string2 = string2.replace("<gold>", ChatColor.GOLD.toString());
        string2 = string2.replace("<dark_red>", ChatColor.DARK_RED.toString());
        string2 = string2.replace("<dark_green>", ChatColor.DARK_GREEN.toString());
        string2 = string2.replace("<dark_blue>", ChatColor.DARK_BLUE.toString());
        string2 = string2.replace("<aqua>", ChatColor.AQUA.toString());
        string2 = string2.replace("<dark_aqua>", ChatColor.DARK_AQUA.toString());
        string2 = string2.replace("<light_purple>", ChatColor.LIGHT_PURPLE.toString());
        string2 = string2.replace("<dark_purple>", ChatColor.DARK_PURPLE.toString());
        string2 = string2.replace("<bold>", ChatColor.BOLD.toString());
        string2 = string2.replace("</bold>", ChatColor.RESET.toString());
        string2 = string2.replace("<b>", ChatColor.BOLD.toString());
        string2 = string2.replace("</b>", ChatColor.RESET.toString());
        string2 = string2.replace("<reset>", ChatColor.RESET.toString());
        Pattern pattern = Pattern.compile("<#[0-9a-fA-F]{6}>");
        Matcher matcher = pattern.matcher(string2);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String string3 = matcher.group();
            String string4 = string3.substring(2, string3.length() - 1);
            Object object = "\u0102\u201a\u00c2\u00a7x";
            for (char c : string4.toCharArray()) {
                object = (String)object + "\u0102\u201a\u00c2\u00a7" + c;
            }
            matcher.appendReplacement(stringBuffer, (String)object);
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    public void openLayoutGui(Player player) {
        int n = this.plugin.layouts().getLayouts().size();
        int n2 = Math.min(54, Math.max(9, (n + 1 + 8) / 9 * 9));
        Inventory inventory = Bukkit.createInventory(null, (int)n2, (String)"HUD Layouts");
        String string = this.plugin.storage().getLayout(player.getUniqueId());
        int n3 = 0;
        for (HudLayout hudLayout : this.plugin.layouts().getLayouts()) {
            if (n3 >= n2 - 1) break;
            boolean bl = hudLayout.getKey().equalsIgnoreCase(string);
            ItemStack itemStack = this.button(Material.PAPER, String.valueOf(bl ? ChatColor.GREEN : ChatColor.YELLOW) + hudLayout.getKey(), bl ? "Selected" : "Click to select");
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setLore(List.of(String.valueOf(ChatColor.GRAY) + (bl ? "Selected" : "Click to select"), String.valueOf(ChatColor.DARK_GRAY) + hudLayout.getKey()));
            itemStack.setItemMeta(itemMeta);
            inventory.setItem(n3++, itemStack);
        }
        inventory.setItem(n2 - 1, this.button(Material.BARRIER, String.valueOf(ChatColor.RED) + "Reset to default", "Use server default HUD"));
        player.openInventory(inventory);
    }

    private ItemStack button(Material material, String string, String string2) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(string);
        itemMeta.setLore(List.of(String.valueOf(ChatColor.GRAY) + string2));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        this.attach(playerJoinEvent.getPlayer());
        this.plugin.avatars().handleJoin(playerJoinEvent.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.detach(playerQuitEvent.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Entity entity;
        Projectile projectile;
        if (entityDamageByEntityEvent.getFinalDamage() <= 0.0) {
            return;
        }
        long l = System.currentTimeMillis();
        UUID uUID = entityDamageByEntityEvent.getEntity().getUniqueId();
        Entity entity2 = entityDamageByEntityEvent.getDamager();
        Entity entity3 = entity2 instanceof Projectile && (entity2 = (projectile = (Projectile)entity2).getShooter()) instanceof Entity ? (entity = entity2) : entityDamageByEntityEvent.getDamager();
        this.combatTargets.replaceAll((uUID2, combatTarget) -> combatTarget.targetId().equals(uUID) || combatTarget.targetId().equals(entity3.getUniqueId()) ? new CombatTarget(combatTarget.targetId(), l) : combatTarget);
        projectile = entityDamageByEntityEvent.getEntity();
        if (projectile instanceof LivingEntity && (!((entity = (LivingEntity)projectile) instanceof Player) || entity.hasMetadata("NPC")) && (projectile = this.resolveAttacker(entityDamageByEntityEvent.getDamager())) != null && projectile.isOnline() && !entity.isDead()) {
            this.combatTargets.put(projectile.getUniqueId(), new CombatTarget(entity.getUniqueId(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent entityDeathEvent) {
        LivingEntity livingEntity = entityDeathEvent.getEntity();
        UUID uUID = livingEntity.getUniqueId();
        this.combatTargets.entrySet().removeIf(entry -> uUID.equals(((CombatTarget)entry.getValue()).targetId()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        String string;
        if (inventoryClickEvent.getView() != null && "HUD Layouts".equals(string = inventoryClickEvent.getView().getTitle())) {
            inventoryClickEvent.setCancelled(true);
            HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
            if (!(humanEntity instanceof Player)) {
                return;
            }
            Player player = (Player)humanEntity;
            if (!HudAccess.has((CommandSender)player, "enorialshud.layout")) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                return;
            }
            humanEntity = inventoryClickEvent.getCurrentItem();
            if (humanEntity == null || !humanEntity.hasItemMeta()) {
                return;
            }
            if (humanEntity.getType() == Material.BARRIER) {
                this.plugin.storage().clearLayout(player.getUniqueId());
                player.sendMessage(String.valueOf(ChatColor.YELLOW) + "HUD layout reset to default.");
                player.closeInventory();
                return;
            }
            ItemMeta itemMeta = humanEntity.getItemMeta();
            if (itemMeta.getLore() == null || itemMeta.getLore().isEmpty()) {
                return;
            }
            String string2 = ChatColor.stripColor((String)((String)itemMeta.getLore().get(itemMeta.getLore().size() - 1)));
            if (string2 == null || string2.isBlank() || !this.plugin.layouts().has(string2)) {
                return;
            }
            this.plugin.storage().setLayout(player.getUniqueId(), string2);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "HUD layout set to " + string2 + ".");
            player.closeInventory();
        }
    }

    private Component applyHorizontalOffset(Component component, Player player, HudElement hudElement, HudLayer hudLayer) {
        int n;
        int n2 = 0;
        int n3 = Math.max(0, this.plugin.cfg().getInt("layout.runtime-shift-left", 0));
        String string = hudElement.getAsset().getKey();
        if (n3 > 0 && string.startsWith("enorialshud-") && !"enorialshud-panel".equals(string)) {
            n2 -= n3;
        }
        return (n = n2) == 0 ? component : ((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append(SpaceUtil.space(n))).append(component)).append(SpaceUtil.space(n * -1))).asComponent();
    }

    private record PopupState(String popupKey, String[] args, long expireAt) {
    }

    private record RenderedTexture(Component component, RelativeWidth relativeWidth) {
    }

    private record RenderedText(Component component, RelativeWidth relativeWidth) {
    }

    private static class TargetSnapshot {
        final boolean exists;
        final String name;
        final String health;
        final String maxHealth;
        final String distance;
        final String hpPercent;
        final String hpBar;

        private TargetSnapshot(boolean bl, String string, String string2, String string3, String string4, String string5, String string6) {
            this.exists = bl;
            this.name = string;
            this.health = string2;
            this.maxHealth = string3;
            this.distance = string4;
            this.hpPercent = string5;
            this.hpBar = string6;
        }

        static TargetSnapshot empty() {
            return new TargetSnapshot(false, "-", "0", "0", "0", "0", "<dark_gray>||||||||||||||");
        }
    }

    private record CombatTarget(UUID targetId, long lastHitAtMs) {
    }
}

