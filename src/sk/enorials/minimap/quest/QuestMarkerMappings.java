package sk.enorials.minimap.quest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;

public final class QuestMarkerMappings {
   public static final String RESOURCE = "configs/quest-markers.yml";
   private final Map<String, QuestMarkerState> states;
   private final Set<String> ambiguous;
   private QuestMarkerMappings.NpcSources npcSources = new QuestMarkerMappings.NpcSources(Set.of("citizens"), true);

   public QuestMarkerMappings.NpcSources npcSources() {
      return this.npcSources;
   }

   private QuestMarkerMappings(Map<String, QuestMarkerState> var1, Set<String> var2) {
      this.states = Map.copyOf(var1);
      this.ambiguous = Set.copyOf(var2);
   }

   public static QuestMarkerMappings defaults() {
      return QuestMarkerMappings.Defaults.MAPPING;
   }

   public static QuestMarkerMappings fromConfig(YamlConfiguration var0, Consumer<String> var1) {
      if (var0.contains("marker-items") && !var0.isConfigurationSection("marker-items")) {
         throw new IllegalArgumentException("marker-items must be a section of state names and item ID lists");
      }

      QuestMarkerMappings var2 = compile(
         var0.getConfigurationSection("marker-items"), QuestMarkerMappings.Defaults.CONFIG.getConfigurationSection("marker-items"), var1
      );
      if (var0.contains("npc-sources") && !var0.isConfigurationSection("npc-sources")) {
         throw new IllegalArgumentException("npc-sources must be a section");
      }

      Object var3 = var0.get("npc-sources.providers", QuestMarkerMappings.Defaults.CONFIG.get("npc-sources.providers"));
      List var4 = var3 instanceof String var6 ? List.of(var6) : (var3 instanceof List var5 ? var5 : null);
      if (var4 == null) {
         throw new IllegalArgumentException("npc-sources.providers must be a list of BetonQuest NPC selector names");
      }

      LinkedHashSet var9 = new LinkedHashSet();

      for (Object var7 : var4) {
         if (!(var7 instanceof String var8) || !var8.trim().matches("[a-zA-Z0-9_:-]+")) {
            throw new IllegalArgumentException("Invalid NPC provider: " + var7);
         }

         var9.add(var8.trim().toLowerCase(Locale.ROOT));
      }

      if (var0.get("npc-sources.legacy-citizens-ids", QuestMarkerMappings.Defaults.CONFIG.get("npc-sources.legacy-citizens-ids")) instanceof Boolean var12) {
         var2.npcSources = new QuestMarkerMappings.NpcSources(var9, var12);
         return var2;
      } else {
         throw new IllegalArgumentException("npc-sources.legacy-citizens-ids must be true or false");
      }
   }

   private static QuestMarkerMappings compile(ConfigurationSection var0, ConfigurationSection var1, Consumer<String> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      HashSet var4 = new HashSet();
      HashSet var5 = new HashSet();

      for (QuestMarkerState var9 : QuestMarkerState.values()) {
         String var10 = var9.name().toLowerCase(Locale.ROOT);
         var5.add(var10);
         Object var11 = var0 != null && var0.contains(var10) ? var0.get(var10) : (var1 == null ? null : var1.get(var10));
         if (var11 != null) {
            List var12;
            if (var11 instanceof String var14) {
               var12 = List.of(var14);
            } else {
               if (!(var11 instanceof List var13)) {
                  var2.accept("marker-items." + var10 + " must be an item ID or a list; this state mapping is ignored");
                  continue;
               }

               var12 = var13;
            }

            for (Object var15 : var12) {
               if (var15 instanceof String var16) {
                  String var17 = normalize(var16);
                  if (var17.isEmpty() || var17.chars().anyMatch(Character::isWhitespace) || var17.endsWith(">")) {
                     var2.accept("Invalid item ID in marker-items." + var10 + ": '" + var16 + "'; entry ignored");
                  } else if (!var4.contains(var17)) {
                     QuestMarkerState var18 = var3.putIfAbsent(var17, var9);
                     if (var18 != null && var18 != var9) {
                        var3.remove(var17);
                        var4.add(var17);
                        var2.accept(
                           "Item ID '"
                              + var17
                              + " is assigned to both "
                              + var18.name().toLowerCase(Locale.ROOT)
                              + " and "
                              + var10
                              + "; ambiguous ID ignored until corrected"
                        );
                     }
                  }
               } else {
                  var2.accept("marker-items." + var10 + " contains a non-text item ID; entry ignored");
               }
            }
         }
      }

      if (var0 != null) {
         for (String var20 : var0.getKeys(false)) {
            if (!var5.contains(var20)) {
               var2.accept("Unknown quest marker state '" + var20 + "'; use " + new TreeSet(var5));
            }
         }
      }

      return new QuestMarkerMappings(var3, var4);
   }

   private static String normalize(String var0) {
      String var1 = var0.trim().toLowerCase(Locale.ROOT);
      return var1.startsWith("item:") ? var1.substring(5).trim() : var1;
   }

   public QuestMarkerState fromLines(List<String> var1) {
      return this.fromLines(var1, null);
   }

   public QuestMarkerState fromLines(List<String> var1, String var2) {
      for (String var4 : var1) {
         if (var4 != null && var4.trim().regionMatches(true, 0, "item:", 0, 5)) {
            String var5 = normalize(var4);
            if (var5.indexOf(62) < 0 && var2 != null && !var2.isBlank()) {
               var5 = normalize(var2) + ">" + var5;
            }

            if (!this.ambiguous.contains(var5)) {
               QuestMarkerState var6 = this.states.get(var5);
               if (var6 == null && var5.lastIndexOf(62) >= 0) {
                  var6 = this.states.get(var5.substring(var5.lastIndexOf(62) + 1));
               }

               if (var6 != null) {
                  return var6;
               }
            }
         }
      }

      return null;
   }

   private static final class Defaults {
      static final YamlConfiguration CONFIG = read();
      static final QuestMarkerMappings MAPPING = QuestMarkerMappings.compile(CONFIG.getConfigurationSection("marker-items"), null, var0 -> {
         throw new IllegalStateException("Invalid bundled quest marker mapping: " + var0);
      });

      private static YamlConfiguration read() {
         try (InputStream var0 = QuestMarkerMappings.class.getClassLoader().getResourceAsStream("modules/gps/configs/quest-markers.yml")) {
            if (var0 == null) {
               throw new IOException("Missing quest marker defaults");
            }

            YamlConfiguration var1 = new YamlConfiguration();
            var1.loadFromString(new String(var0.readAllBytes(), StandardCharsets.UTF_8));
            return var1;
         } catch (IOException | InvalidConfigurationException var5) {
            throw new IllegalStateException(var5);
         }
      }
   }

   public record NpcSources(Set<String> providers, boolean legacyCitizensIds) {
      public NpcSources(Set<String> providers, boolean legacyCitizensIds) {
         providers = Set.copyOf(providers);
         this.providers = providers;
         this.legacyCitizensIds = legacyCitizensIds;
      }

      public boolean allows(String var1) {
         String var2 = var1.toLowerCase(Locale.ROOT);
         return this.providers.contains(var2) || var2.startsWith("betonquest:") && this.providers.contains(var2.substring(11));
      }
   }

   public static final class Store {
      private final EnorialsGpsPlugin plugin;
      private final Path file;
      private byte[] lastRead;
      private String lastIoError;
      private QuestMarkerMappings current = QuestMarkerMappings.defaults();

      public Store(EnorialsGpsPlugin var1) {
         this.plugin = var1;
         this.file = var1.getDataFolder().toPath().resolve("configs/quest-markers.yml");
         this.reloadIfChanged();
      }

      public QuestMarkerMappings reloadIfChanged() {
         try {
            if (!Files.exists(this.file)) {
               this.plugin.saveResource("configs/quest-markers.yml", false);
            }

            byte[] var1 = Files.readAllBytes(this.file);
            this.lastIoError = null;
            if (Arrays.equals(this.lastRead, var1)) {
               return this.current;
            }

            this.lastRead = var1;
            YamlConfiguration var5 = new YamlConfiguration();
            var5.loadFromString(new String(var1, StandardCharsets.UTF_8).replaceFirst("^\ufeff", ""));
            this.current = QuestMarkerMappings.fromConfig(var5, var1x -> this.plugin.getLogger().warning("Quest marker config: " + var1x));
         } catch (InvalidConfigurationException | IllegalArgumentException var3) {
            this.plugin.getLogger().warning("Cannot read configs/quest-markers.yml; keeping the last valid marker mapping: " + var3.getMessage());
         } catch (IOException | IllegalStateException var4) {
            String var2 = var4.getMessage();
            if (!Objects.equals(this.lastIoError, var2)) {
               this.plugin.getLogger().warning("Cannot read configs/quest-markers.yml; keeping the last valid marker mapping: " + var2);
            }

            this.lastIoError = var2;
         }

         return this.current;
      }
   }
}
