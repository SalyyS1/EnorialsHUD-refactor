/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.api.HudValue;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.hud.party.QuestNpcNames;
import sk.enorials.hud.party.TargetNpcNames;

public class ListenerManager {
    private EnorialsHUD plugin;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[\\.,]\\d+)?");
    private static final Pattern UNRESOLVED_PLACEHOLDER_PATTERN = Pattern.compile("%[^%]+%");
    private static final Map<Material, Integer> FOOD_SLOT_ICON_INDEX = ListenerManager.buildFoodSlotIconIndex();
    private static final int FOOD_SLOT_ICON_MAX = FOOD_SLOT_ICON_INDEX.size();
    private static final long HUNGER_CACHE_TTL_MS = 100L;
    private static final Set<String> RESOURCE_PROXY_TYPES = Set.of("Mushroom_Red", "Wild_Berry", "Cactus", "Shamrock", "Four_Leaf_Clover", "Glowing_Mushroom", "Magical_Herb", "Deathbloom");
    private final Set<String> hiddenTargetNameTypes = new HashSet<String>(RESOURCE_PROXY_TYPES);
    private final Set<String> hiddenTargetNpcs = new HashSet<String>();
    private final QuestNpcNames questNpcNames = new QuestNpcNames();
    private final Map<String, ListenerDefinition> listeners = new HashMap<String, ListenerDefinition>();
    private final Map<UUID, HungerSlots> hungerSlotsCache = new HashMap<UUID, HungerSlots>();

    public void reload(EnorialsHUD enorialsHUD) {
        Object object;
        Object object2;
        this.plugin = enorialsHUD;
        this.listeners.clear();
        this.hiddenTargetNameTypes.clear();
        this.hiddenTargetNameTypes.addAll(RESOURCE_PROXY_TYPES);
        this.hiddenTargetNameTypes.addAll(enorialsHUD.cfg().getStringList("target-frame.hidden-name-mythic-mobs"));
        this.hiddenTargetNpcs.clear();
        try {
            object2 = enorialsHUD.getResource("target-hidden-npcs.txt");
            try {
                if (object2 != null) {
                    new BufferedReader(new InputStreamReader((InputStream)object2, StandardCharsets.UTF_8)).lines().map(String::trim).filter(string -> !string.isEmpty() && !string.startsWith("#")).forEach(this.hiddenTargetNpcs::add);
                }
            }
            finally {
                if (object2 != null) {
                    ((InputStream)object2).close();
                }
            }
        }
        catch (IOException iOException) {
            enorialsHUD.getLogger().warning("Cannot load hidden target NPC identities: " + iOException.getMessage());
        }
        object2 = enorialsHUD.getDataFolder().toPath().resolve("target-hidden-npcs.txt");
        if (Files.isRegularFile((Path)object2, LinkOption.NOFOLLOW_LINKS)) {
            try {
                java.util.stream.Stream<String> lines = Files.lines((Path)object2, StandardCharsets.UTF_8);
                try {
                    lines.map(String::trim).filter(string -> !string.isEmpty() && !string.startsWith("#")).forEach(this.hiddenTargetNpcs::add);
                }
                finally {
                    lines.close();
                }
            }
            catch (IOException iOException) {
                enorialsHUD.getLogger().warning("Cannot read local target-hidden-npcs.txt: " + iOException.getMessage());
            }
        }
        this.hiddenTargetNpcs.addAll(enorialsHUD.cfg().getStringList("target-frame.hidden-name-citizens"));
        object = new File(enorialsHUD.getDataFolder(), "listeners");
        File[] fileArray = ((File)object).listFiles((file, string) -> string.endsWith(".yml"));
        if (fileArray != null) {
            for (File file2 : fileArray) {
                try {
                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file2);
                    for (String string2 : yamlConfiguration.getKeys(false)) {
                        ConfigurationSection configurationSection = yamlConfiguration.getConfigurationSection(string2);
                        if (configurationSection == null) continue;
                        String string3 = configurationSection.getString("value");
                        String string4 = configurationSection.getString("max");
                        this.listeners.put(string2.toLowerCase(Locale.ROOT), new ListenerDefinition(string2.toLowerCase(Locale.ROOT), string3, string4));
                    }
                }
                catch (Exception exception) {
                    enorialsHUD.getLogger().log(Level.WARNING, "Could not load listener file: " + file2.getName(), exception);
                }
            }
        }
    }

    public Map<String, ListenerDefinition> getListeners() {
        return this.listeners;
    }

    public String getTargetName(LivingEntity livingEntity) {
        if (livingEntity == null || this.isResourceProxy(livingEntity) || TargetNpcNames.hidden(livingEntity, this.hiddenTargetNpcs)) {
            return "";
        }
        String string = livingEntity.getCustomName();
        return string != null && !string.isBlank() ? string : livingEntity.getName();
    }

    public String getTargetName(Player player, LivingEntity livingEntity) {
        String string = this.getTargetName(livingEntity);
        if (string.isEmpty() || !this.plugin.cfg().getBoolean("target-frame.betonquest-names", true)) {
            return string;
        }
        String string2 = this.questNpcNames.name(player, livingEntity);
        return string2 == null ? string : string2;
    }

    private boolean isResourceProxy(LivingEntity livingEntity) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            String string;
            Object object = plugin.getClass().getMethod("getAPIHelper", new Class[0]).invoke((Object)plugin, new Object[0]);
            Object object2 = object.getClass().getMethod("getMythicMobInstance", Entity.class).invoke(object, livingEntity);
            if (object2 == null) {
                return false;
            }
            Object object3 = object2.getClass().getMethod("getType", new Class[0]).invoke(object2, new Object[0]);
            Object object4 = object3.getClass().getMethod("getInternalName", new Class[0]).invoke(object3, new Object[0]);
            return object4 instanceof String && this.hiddenTargetNameTypes.contains(string = (String)object4);
        }
        catch (LinkageError | ReflectiveOperationException throwable) {
            return false;
        }
    }

    public boolean has(String string) {
        return string != null && this.listeners.containsKey(string.toLowerCase(Locale.ROOT));
    }

    public ListenerDefinition get(String string) {
        return this.listeners.get(string.toLowerCase(Locale.ROOT));
    }

    public ResourceState getResourceState(Player player) {
        String string;
        String string2;
        Plugin plugin = this.findSpellsPlugin();
        String string3 = string2 = plugin == null ? null : this.getEnorialsSpellsResourceType(player);
        if ("none".equalsIgnoreCase(string2)) {
            return ResourceState.unavailable();
        }
        String string4 = string = "rage".equalsIgnoreCase(string2) ? "rage" : "mana";
        if (plugin != null) {
            double d = this.getEnorialsSpellsResource(player, string, false);
            double d2 = this.getEnorialsSpellsResource(player, string, true);
            if (this.hasResourcePlaceholderProviders(this.listeners.get(string))) {
                if (!Double.isFinite(d) || d < 0.0) {
                    d = this.parsePlaceholder(player, string, true);
                }
                if (!Double.isFinite(d2) || d2 <= 0.0) {
                    d2 = this.parsePlaceholder(player, string, false);
                }
            }
            if (ResourceState.valid(d, d2)) {
                return new ResourceState(true, string, d, d2);
            }
        }
        if (plugin != null) {
            return ResourceState.unavailable();
        }
        for (String string5 : new String[]{"resource", "mana", "rage"}) {
            HudValue object;
            HudValue hudValue = object = this.plugin.api() == null ? null : this.plugin.api().read(player, string5);
            if (object == null) continue;
            return new ResourceState(true, string5, object.current(), object.maximum());
        }
        Plugin plugin2 = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (plugin2 != null && plugin2.isEnabled()) {
            for (String string5 : new String[]{"resource", string}) {
                double d;
                double d2;
                ListenerDefinition listenerDefinition = this.listeners.get(string5);
                if (listenerDefinition == null || listenerDefinition.value() == null || !UNRESOLVED_PLACEHOLDER_PATTERN.matcher(listenerDefinition.value()).find() || !this.hasResourcePlaceholderProviders(listenerDefinition) || !ResourceState.valid(d2 = this.parsePlaceholder(player, string5, true), d = this.parsePlaceholder(player, string5, false))) continue;
                return new ResourceState(true, string5, d2, d);
            }
        }
        return ResourceState.unavailable();
    }

    private boolean hasResourcePlaceholderProviders(ListenerDefinition listenerDefinition) {
        if (listenerDefinition == null) {
            return false;
        }
        for (String string : new String[]{listenerDefinition.value(), listenerDefinition.max()}) {
            if (string == null) continue;
            Matcher matcher = UNRESOLVED_PLACEHOLDER_PATTERN.matcher(string);
            while (matcher.find()) {
                Plugin plugin;
                String string2 = matcher.group().toLowerCase(Locale.ROOT);
                String string3 = string2.startsWith("%enorialsspells_") ? "EnorialsSpells" : (string2.startsWith("%mmocore_") ? "MMOCore" : null);
                if (string3 == null || (plugin = Bukkit.getPluginManager().getPlugin(string3)) != null && plugin.isEnabled()) continue;
                return false;
            }
        }
        return true;
    }

    public boolean hasResource(Player player) {
        return this.getResourceState(player).available();
    }

    public double getCurrent(Player player, String string) {
        String string2;
        HudValue hudValue;
        if (Set.of("resource", "mana", "rage").contains(string.toLowerCase(Locale.ROOT)) && this.findSpellsPlugin() != null) {
            ResourceState resourceState = this.getResourceState(player);
            return resourceState.available() ? resourceState.current() : 0.0;
        }
        HudValue hudValue2 = hudValue = this.plugin.api() == null ? null : this.plugin.api().read(player, string.toLowerCase(Locale.ROOT));
        if (hudValue != null) {
            return hudValue.current();
        }
        return switch (string2 = string.toLowerCase(Locale.ROOT)) {
            case "health" -> Math.max(0.0, player.getHealth() + player.getAbsorptionAmount());
            case "food" -> player.getFoodLevel();
            case "mana" -> {
                String var7_8 = this.resolveSharedResourceKey(player);
                double var8_13 = this.getEnorialsSpellsResource(player, var7_8, false);
                if (Double.isFinite(var8_13) && var8_13 >= 0.0) {
                    yield var8_13;
                }
                double var10_15 = this.parsePlaceholder(player, var7_8, true);
                if (Double.isFinite(var10_15) && var10_15 >= 0.0) {
                    yield var10_15;
                }
                yield this.getMax(player, string2);
            }
            case "rage" -> {
                double var7_9 = this.getEnorialsSpellsResource(player, "rage", false);
                if (!Double.isNaN(var7_9)) {
                    yield var7_9;
                }
                yield this.parsePlaceholder(player, string2, true);
            }
            case "armor" -> {
                if (player.getAttribute(HudAttributes.ARMOR) != null) {
                    yield player.getAttribute(HudAttributes.ARMOR).getValue();
                }
                yield 0.0;
            }
            case "targethp" -> {
                LivingEntity var7_10 = this.resolveTargetEntity(player);
                if (var7_10 == null) {
                    yield 0.0;
                }
                yield Math.max(0.0, var7_10.getHealth());
            }
            case "foodslot1" -> this.getEnorialsHungerSlotCode(player, 0);
            case "foodslot2" -> this.getEnorialsHungerSlotCode(player, 1);
            case "foodslot3" -> this.getEnorialsHungerSlotCode(player, 2);
            case "resource" -> {
                String var7_11 = this.resolveSharedResourceKey(player);
                double var8_14 = this.getEnorialsSpellsResource(player, var7_11, false);
                if (Double.isFinite(var8_14) && var8_14 >= 0.0) {
                    yield var8_14;
                }
                double var10_16 = this.parsePlaceholder(player, var7_11, true);
                if (Double.isFinite(var10_16) && var10_16 >= 0.0) {
                    yield var10_16;
                }
                yield this.getMax(player, "resource");
            }
            case "foodslots" -> {
                double var7_12 = this.getEnorialsHungerSlots(player);
                if (!Double.isNaN(var7_12)) {
                    yield var7_12;
                }
                yield this.parsePlaceholder(player, string2, true);
            }
            case "air" -> player.getRemainingAir();
            default -> this.parsePlaceholder(player, string2, true);
        };
    }

    public double getMax(Player player, String string) {
        String string2;
        HudValue hudValue;
        if (Set.of("resource", "mana", "rage").contains(string.toLowerCase(Locale.ROOT)) && this.findSpellsPlugin() != null) {
            ResourceState resourceState = this.getResourceState(player);
            return resourceState.available() ? resourceState.max() : 0.0;
        }
        HudValue hudValue2 = hudValue = this.plugin.api() == null ? null : this.plugin.api().read(player, string.toLowerCase(Locale.ROOT));
        if (hudValue != null) {
            return hudValue.maximum();
        }
        return switch (string2 = string.toLowerCase(Locale.ROOT)) {
            case "health" -> {
                if (player.getAttribute(HudAttributes.MAX_HEALTH) != null) {
                    yield player.getAttribute(HudAttributes.MAX_HEALTH).getValue();
                }
                yield 20.0;
            }
            case "food" -> 20.0;
            case "mana" -> {
                String var7_8 = this.resolveSharedResourceKey(player);
                double var8_12 = this.getEnorialsSpellsResource(player, var7_8, true);
                if (!Double.isNaN(var8_12) && var8_12 > 0.0) {
                    yield var8_12;
                }
                double var10_14 = this.parsePlaceholder(player, var7_8, false);
                if (!Double.isNaN(var10_14) && var10_14 > 0.0) {
                    yield var10_14;
                }
                yield 100.0;
            }
            case "rage" -> {
                double var7_9 = this.getEnorialsSpellsResource(player, "rage", true);
                if (!Double.isNaN(var7_9)) {
                    yield var7_9;
                }
                yield this.parsePlaceholder(player, string2, false);
            }
            case "armor" -> 20.0;
            case "targethp" -> {
                LivingEntity var7_10 = this.resolveTargetEntity(player);
                if (var7_10 == null) {
                    yield 20.0;
                }
                if (var7_10.getAttribute(HudAttributes.MAX_HEALTH) != null) {
                    yield Math.max(1.0, var7_10.getAttribute(HudAttributes.MAX_HEALTH).getValue());
                }
                yield Math.max(1.0, var7_10.getMaxHealth());
            }
            case "foodslot1", "foodslot2", "foodslot3" -> FOOD_SLOT_ICON_MAX;
            case "resource" -> {
                String var7_11 = this.resolveSharedResourceKey(player);
                double var8_13 = this.getEnorialsSpellsResource(player, var7_11, true);
                if (!Double.isNaN(var8_13) && var8_13 > 0.0) {
                    yield var8_13;
                }
                double var10_15 = this.parsePlaceholder(player, var7_11, false);
                if (!Double.isNaN(var10_15) && var10_15 > 0.0) {
                    yield var10_15;
                }
                yield 100.0;
            }
            case "foodslots" -> 3.0;
            case "air" -> player.getMaximumAir();
            default -> this.parsePlaceholder(player, string2, false);
        };
    }

    private String resolveSharedResourceKey(Player player) {
        String string = this.getEnorialsSpellsResourceType(player);
        if ("rage".equalsIgnoreCase(string)) {
            return "rage";
        }
        if ("mana".equalsIgnoreCase(string)) {
            return "mana";
        }
        double d = this.getEnorialsSpellsResource(player, "mana", false);
        double d2 = this.getEnorialsSpellsResource(player, "rage", false);
        return Double.isNaN(d2) || !(d2 > 0.0) || !Double.isNaN(d) && !(d <= 0.0) ? "mana" : "rage";
    }

    private LivingEntity resolveTargetEntity(Player player) {
        LivingEntity livingEntity;
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        if (enorialsHUD != null && enorialsHUD.renderer() != null && (livingEntity = enorialsHUD.renderer().getCurrentTarget(player)) != null) {
            return livingEntity;
        }
        return null;
    }

    private double getEnorialsHungerSlots(Player player) {
        HungerSlots hungerSlots = this.getCachedHungerSlots(player);
        return hungerSlots == null ? Double.NaN : hungerSlots.occupied();
    }

    private double getEnorialsHungerSlotCode(Player player, int n) {
        HungerSlots hungerSlots = this.getCachedHungerSlots(player);
        if (hungerSlots == null) {
            return 0.0;
        }
        return switch (n) {
            case 0 -> hungerSlots.slot1();
            case 1 -> hungerSlots.slot2();
            case 2 -> hungerSlots.slot3();
            default -> 0.0;
        };
    }

    private HungerSlots getCachedHungerSlots(Player player) {
        UUID uUID = player.getUniqueId();
        long l = System.currentTimeMillis();
        HungerSlots hungerSlots = this.hungerSlotsCache.get(uUID);
        if (hungerSlots != null && l - hungerSlots.loadedAt() <= 100L) {
            return hungerSlots;
        }
        HungerSlots hungerSlots2 = this.readHungerSlots(player);
        if (hungerSlots2 != null) {
            this.hungerSlotsCache.put(uUID, hungerSlots2);
            return hungerSlots2;
        }
        HungerSlots hungerSlots3 = new HungerSlots(l, 0.0, 0.0, 0.0, 0.0);
        this.hungerSlotsCache.put(uUID, hungerSlots3);
        return hungerSlots3;
    }

    private HungerSlots readHungerSlots(Player player) {
        Plugin plugin = this.findHungerPlugin();
        if (plugin == null) {
            return null;
        }
        HungerSlots hungerSlots = this.readHungerSlotsFromDataFile(plugin, player);
        if (hungerSlots != null) {
            return hungerSlots;
        }
        HungerSlots hungerSlots2 = this.readHungerSlotsFromStateMap(plugin, player);
        if (hungerSlots2 != null && hungerSlots2.occupied() > 0.0) {
            return hungerSlots2;
        }
        return hungerSlots2 != null ? hungerSlots2 : null;
    }

    private HungerSlots readHungerSlotsFromStateMap(Plugin plugin, Player player) {
        try {
            Field field = plugin.getClass().getDeclaredField("states");
            field.setAccessible(true);
            Object object = field.get(plugin);
            if (object instanceof Map) {
                Map map = (Map)object;
                if ((object = map.get(player.getUniqueId())) == null) {
                    object = map.get(player.getUniqueId().toString());
                }
                if (object == null) {
                    return null;
                }
                double d = this.extractSlotCodeFromState(object, 0);
                double d2 = this.extractSlotCodeFromState(object, 1);
                double d3 = this.extractSlotCodeFromState(object, 2);
                double d4 = 0.0;
                if (d > 0.0) {
                    d4 += 1.0;
                }
                if (d2 > 0.0) {
                    d4 += 1.0;
                }
                if (d3 > 0.0) {
                    d4 += 1.0;
                }
                return new HungerSlots(System.currentTimeMillis(), d, d2, d3, d4);
            }
            return null;
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private double extractSlotCodeFromState(Object object, int n) {
        try {
            Method method = object.getClass().getDeclaredMethod("getSlot", Integer.TYPE);
            method.setAccessible(true);
            Object object2 = method.invoke(object, n);
            if (object2 == null) {
                return 0.0;
            }
            Material material = this.extractMaterialFromSlot(object2);
            if (material == null) {
                return 0.0;
            }
            Integer n2 = FOOD_SLOT_ICON_INDEX.get(material);
            return n2 == null ? 0.0 : n2.doubleValue();
        }
        catch (Throwable throwable) {
            return 0.0;
        }
    }

    private HungerSlots readHungerSlotsFromDataFile(Plugin plugin, Player player) {
        try {
            File file = new File(plugin.getDataFolder(), "data.yml");
            if (!file.exists()) {
                return null;
            }
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
            String string = player.getUniqueId().toString();
            double d = this.slotCodeFromData(yamlConfiguration, string, 1);
            double d2 = this.slotCodeFromData(yamlConfiguration, string, 2);
            double d3 = this.slotCodeFromData(yamlConfiguration, string, 3);
            double d4 = 0.0;
            if (d > 0.0) {
                d4 += 1.0;
            }
            if (d2 > 0.0) {
                d4 += 1.0;
            }
            if (d3 > 0.0) {
                d4 += 1.0;
            }
            return new HungerSlots(System.currentTimeMillis(), d, d2, d3, d4);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private Material extractMaterialFromSlot(Object object) {
        Material material = this.extractMaterialViaMethod(object, "getMaterial");
        if (material != null) {
            return material;
        }
        material = this.extractMaterialViaMethod(object, "material");
        if (material != null) {
            return material;
        }
        material = this.extractMaterialViaMethod(object, "getType");
        if (material != null) {
            return material;
        }
        material = this.extractMaterialViaMethod(object, "type");
        if (material != null) {
            return material;
        }
        for (String string : new String[]{"material", "type", "item", "food"}) {
            material = this.extractMaterialViaField(object, string);
            if (material == null) continue;
            return material;
        }
        return null;
    }

    private Material extractMaterialViaMethod(Object object, String string) {
        try {
            Method method = object.getClass().getDeclaredMethod(string, new Class[0]);
            method.setAccessible(true);
            Object object2 = method.invoke(object, new Object[0]);
            return this.toMaterial(object2);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private Material extractMaterialViaField(Object object, String string) {
        try {
            Field field = object.getClass().getDeclaredField(string);
            field.setAccessible(true);
            Object object2 = field.get(object);
            return this.toMaterial(object2);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private Material toMaterial(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Material) {
            return (Material)object;
        }
        if (object instanceof ItemStack) {
            ItemStack itemStack = (ItemStack)object;
            return itemStack.getType();
        }
        if (object instanceof String) {
            String string = (String)object;
            String string2 = string.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            try {
                return Material.valueOf((String)string2);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return null;
            }
        }
        return null;
    }

    private double slotCodeFromData(YamlConfiguration yamlConfiguration, String string, int n) {
        long l = System.currentTimeMillis();
        String string2 = string + ".slot" + n;
        long l2 = yamlConfiguration.getLong(string2 + ".expires", Long.MAX_VALUE);
        if (l2 <= l) {
            return 0.0;
        }
        double d = this.slotCodeFromName(yamlConfiguration.getString(string2 + ".material"));
        if (d > 0.0) {
            return d;
        }
        d = this.slotCodeFromName(yamlConfiguration.getString(string2 + ".type"));
        if (d > 0.0) {
            return d;
        }
        d = this.slotCodeFromName(yamlConfiguration.getString(string2 + ".item"));
        if (d > 0.0) {
            return d;
        }
        String string3 = string + ".slots." + n;
        d = this.slotCodeFromName(yamlConfiguration.getString(string3 + ".material"));
        return d > 0.0 ? d : this.slotCodeFromName(yamlConfiguration.getString(string3 + ".type"));
    }

    private double slotCodeFromName(String string) {
        if (string != null && !string.isBlank()) {
            try {
                Material material = Material.valueOf((String)string);
                Integer n = FOOD_SLOT_ICON_INDEX.get(material);
                return n == null ? 0.0 : n.doubleValue();
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private Plugin findHungerPlugin() {
        for (String string : new String[]{"EnorialsHunger", "EnorialsGFS"}) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(string);
            if (plugin == null) continue;
            return plugin;
        }
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                Field states = plugin.getClass().getDeclaredField("states");
                states.setAccessible(true);
                Object stateValues = states.get(plugin);
                if (!(stateValues instanceof Map) && !(stateValues instanceof Collection)) continue;
                String pluginName = plugin.getName().toLowerCase(Locale.ROOT);
                String className = plugin.getClass().getName().toLowerCase(Locale.ROOT);
                if (!pluginName.contains("hunger") && !pluginName.contains("gfs") && !className.contains(".hunger.") && !className.contains("hungerplugin")) continue;
                return plugin;
            }
            catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static Map<Material, Integer> buildFoodSlotIconIndex() {
        String[] stringArray = new String[]{"APPLE", "BAKED_POTATO", "BEEF", "BEETROOT", "BEETROOT_SOUP", "BREAD", "CAKE", "CARROT", "CHICKEN", "CHORUS_FRUIT", "COD", "COOKED_BEEF", "COOKED_CHICKEN", "COOKED_COD", "COOKED_MUTTON", "COOKED_PORKCHOP", "COOKED_RABBIT", "COOKED_SALMON", "COOKIE", "DRIED_KELP", "ENCHANTED_GOLDEN_APPLE", "GLOW_BERRIES", "GOLDEN_APPLE", "GOLDEN_CARROT", "HONEY_BOTTLE", "MELON_SLICE", "MUSHROOM_STEW", "MUTTON", "POISONOUS_POTATO", "PORKCHOP", "POTATO", "PUFFERFISH", "PUMPKIN_PIE", "RABBIT", "RABBIT_STEW", "ROTTEN_FLESH", "SALMON", "SPIDER_EYE", "SUSPICIOUS_STEW", "SWEET_BERRIES", "TROPICAL_FISH"};
        LinkedHashMap<Material, Integer> linkedHashMap = new LinkedHashMap<Material, Integer>();
        int n = 1;
        for (String string : stringArray) {
            try {
                Material material = Material.valueOf((String)string);
                linkedHashMap.put(material, n++);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        return linkedHashMap;
    }

    private double getEnorialsSpellsResource(Player player, String string, boolean bl) {
        Plugin plugin = this.findSpellsPlugin();
        if (plugin == null) {
            return Double.NaN;
        }
        try {
            if (bl) {
                try {
                    return ((Number)plugin.getClass().getMethod(string.equals("rage") ? "getRageMax" : "getManaMax", Player.class).invoke((Object)plugin, player)).doubleValue();
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    Method method = plugin.getClass().getMethod("getConfig", new Class[0]);
                    Object object = method.invoke((Object)plugin, new Object[0]);
                    Method method2 = object.getClass().getMethod("getDouble", String.class, Double.TYPE);
                    Object object2 = method2.invoke(object, "resource." + string + ".max", 100.0);
                    return object2 instanceof Number ? ((Number)object2).doubleValue() : Double.NaN;
                }
            }
            Object object = plugin.getClass().getMethod(string.equals("rage") ? "getRage" : "getMana", Player.class).invoke((Object)plugin, player);
            return object instanceof Number ? ((Number)object).doubleValue() : Double.NaN;
        }
        catch (Throwable throwable) {
            return Double.NaN;
        }
    }

    private String getEnorialsSpellsResourceType(Player player) {
        Plugin plugin = this.findSpellsPlugin();
        if (plugin == null) {
            return null;
        }
        try {
            Method method = plugin.getClass().getMethod("getResourceType", Player.class);
            Object object = method.invoke((Object)plugin, player);
            return object instanceof String ? (String)object : null;
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private Plugin findSpellsPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EnorialsSpells");
        if (plugin != null && plugin.isEnabled()) {
            return plugin;
        }
        for (Plugin plugin2 : Bukkit.getPluginManager().getPlugins()) {
            if (!plugin2.isEnabled()) continue;
            try {
                plugin2.getClass().getMethod("getStateForPlaceholders", Player.class);
                return plugin2;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return null;
    }

    private double parsePlaceholder(Player player, String string, boolean bl) {
        String string2;
        ListenerDefinition listenerDefinition = this.listeners.get(string);
        if (listenerDefinition == null) {
            return Double.NaN;
        }
        String string3 = string2 = bl ? listenerDefinition.value() : listenerDefinition.max();
        if (string2 != null && !string2.isEmpty()) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    string2 = PlaceholderAPI.setPlaceholders((Player)player, (String)string2);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if (string2 == null || UNRESOLVED_PLACEHOLDER_PATTERN.matcher(string2).find()) {
                return Double.NaN;
            }
            Matcher matcher = NUMBER_PATTERN.matcher(string2);
            if (matcher.find()) {
                string2 = matcher.group();
            }
            try {
                return Double.parseDouble(string2.replace(",", "."));
            }
            catch (NumberFormatException numberFormatException) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    public record ListenerDefinition(String key, String value, String max) {
    }

    public record ResourceState(boolean available, String kind, double current, double max) {
        private static boolean valid(double d, double d2) {
            return Double.isFinite(d) && d >= 0.0 && Double.isFinite(d2) && d2 > 0.0;
        }

        private static ResourceState unavailable() {
            return new ResourceState(false, "none", 0.0, 0.0);
        }
    }

    private record HungerSlots(long loadedAt, double slot1, double slot2, double slot3, double occupied) {
    }
}

