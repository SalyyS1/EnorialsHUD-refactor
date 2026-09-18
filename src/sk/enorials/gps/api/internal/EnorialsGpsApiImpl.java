package sk.enorials.gps.api.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.EnorialsGpsApi;
import sk.enorials.gps.api.GpsQuestRoute;
import sk.enorials.gps.api.GpsTargetType;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.TargetLocator;

public final class EnorialsGpsApiImpl implements EnorialsGpsApi {
   private static final String DEFAULT_SOURCE = "external-api";
   private final EnorialsGpsPlugin plugin;
   private final NavigationManager navigationManager;
   private final IntegrationManager integrationManager;

   public EnorialsGpsApiImpl(EnorialsGpsPlugin var1, NavigationManager var2, IntegrationManager var3) {
      this.plugin = var1;
      this.navigationManager = var2;
      this.integrationManager = var3;
   }

   @Override
   public String apiVersion() {
      return "1";
   }

   @Override
   public boolean isModeEnabled(NavigationMode var1) {
      return this.plugin.isModeEnabled(var1);
   }

   @Override
   public NavigationMode getPlayerMode(Player var1) {
      return this.navigationManager.getCurrentOrDefaultMode(var1);
   }

   @Override
   public void setPlayerMode(Player var1, NavigationMode var2) {
      if (var1 != null && var2 != null) {
         this.navigationManager.setMode(var1, var2);
      }
   }

   @Override
   public boolean hasActiveNavigation(Player var1) {
      return var1 != null && this.navigationManager.hasActiveNavigation(var1);
   }

   @Override
   public boolean stopNavigation(Player var1, String var2) {
      if (var1 == null) {
         return false;
      }

      String var3 = this.blankToDefault(var2, "external_api");
      return this.navigationManager.stopNavigation(var1, var3);
   }

   @Override
   public boolean startNavigation(Player var1, GpsTargetType var2, String var3) {
      return var1 != null && var2 != null && !this.isBlank(var3) ? this.startNavigation(var1, var2, var3, this.getPlayerMode(var1), "external-api") : false;
   }

   @Override
   public boolean startNavigation(Player var1, GpsTargetType var2, String var3, NavigationMode var4, String var5) {
      if (var1 != null && var2 != null && !this.isBlank(var3)) {
         RouteDescriptor var6 = this.toDescriptor(var2, var3);
         return this.startNavigation(var1, var6, var4, var5);
      } else {
         return false;
      }
   }

   @Override
   public boolean startNavigation(Player var1, RouteDescriptor var2, NavigationMode var3, String var4) {
      if (var1 != null && var2 != null && var2.value() != null && !var2.value().isBlank()) {
         NavigationMode var5 = var3 == null ? this.getPlayerMode(var1) : var3;
         String var6 = this.blankToDefault(var4, "external-api");
         Optional var7 = this.integrationManager.resolveDescriptor(var1, var2);
         return var7.isEmpty() ? false : this.navigationManager.startNavigation(var1, (TargetLocator)var7.get(), var5, var6, var2);
      } else {
         return false;
      }
   }

   @Override
   public boolean addQuestRouteButton(Player var1, String var2, String var3, GpsTargetType var4, String var5) {
      return var1 != null && var4 != null && !this.isBlank(var5)
         ? this.addQuestRouteButton(var1, var2, var3, var4, var5, this.getPlayerMode(var1), "external-api")
         : false;
   }

   @Override
   public boolean addQuestRouteButton(Player var1, String var2, String var3, GpsTargetType var4, String var5, NavigationMode var6, String var7) {
      if (var1 != null && var4 != null && !this.isBlank(var5)) {
         String var8 = this.normalizeRouteId(var2, var4, var5, var1);
         if (var8.isBlank()) {
            return false;
         }

         String var9 = var3 != null && !var3.isBlank() ? var3 : this.autoDisplay(var4, var5);
         NavigationMode var10 = var6 == null ? this.getPlayerMode(var1) : var6;
         String var11 = this.blankToDefault(var7, "external-api");
         RouteDescriptor var12 = this.toDescriptor(var4, var5);
         if (this.integrationManager.resolveDescriptor(var1, var12).isEmpty()) {
            return false;
         }

         this.integrationManager.putDynamicQuestRoute(var1.getUniqueId(), var8, var9, var12, var10, var11);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean removeQuestRouteButton(Player var1, String var2) {
      return var1 != null && !this.isBlank(var2) ? this.integrationManager.removeDynamicQuestRoute(var1.getUniqueId(), var2) : false;
   }

   @Override
   public int clearQuestRouteButtons(Player var1) {
      return var1 == null ? 0 : this.integrationManager.clearDynamicQuestRoutes(var1.getUniqueId());
   }

   @Override
   public Map<String, GpsQuestRoute> getQuestRouteButtons(Player var1) {
      if (var1 == null) {
         return Map.of();
      }

      Map<String, IntegrationManager.AutoRoute> var2 = this.integrationManager.getAutoRoutes(var1);
      LinkedHashMap<String, GpsQuestRoute> var3 = new LinkedHashMap<>();

      for (Entry<String, IntegrationManager.AutoRoute> var5 : var2.entrySet()) {
         IntegrationManager.AutoRoute var6 = var5.getValue();
         var3.put(
            var5.getKey(),
            new GpsQuestRoute(
               var6.key(), var6.displayName(), this.fromDescriptorType(var6.descriptor().type()), var6.descriptor().value(), var6.mode(), var6.source()
            )
         );
      }

      return var3;
   }

   private RouteDescriptor toDescriptor(GpsTargetType var1, String var2) {
      return new RouteDescriptor(this.toDescriptorType(var1), var2);
   }

   private RouteDescriptor.Type toDescriptorType(GpsTargetType var1) {
      return switch (var1) {
         case PLAYER -> RouteDescriptor.Type.PLAYER;
         case MOB -> RouteDescriptor.Type.MOB;
         case NPC -> RouteDescriptor.Type.NPC;
         case POI -> RouteDescriptor.Type.POI;
         case LOCATION -> RouteDescriptor.Type.LOCATION;
      };
   }

   private GpsTargetType fromDescriptorType(RouteDescriptor.Type var1) {
      return switch (var1) {
         case PLAYER -> GpsTargetType.PLAYER;
         case MOB -> GpsTargetType.MOB;
         case NPC -> GpsTargetType.NPC;
         case POI -> GpsTargetType.POI;
         case LOCATION -> GpsTargetType.LOCATION;
      };
   }

   private String normalizeRouteId(String var1, GpsTargetType var2, String var3, Player var4) {
      String var5 = var1;
      if (var5 == null || var5.isBlank()) {
         var5 = "q_" + var2.name().toLowerCase(Locale.ROOT) + "_" + this.sanitizeId(var3);
      }

      if ((var5 = this.sanitizeId(var5)).isBlank()) {
         return "route";
      }

      Set var6 = this.integrationManager.getDynamicQuestRouteKeys(var4.getUniqueId());
      String var7 = var5;
      int var8 = 2;

      while (this.containsIgnoreCase(var6, var7)) {
         var7 = var5 + "_" + var8++;
      }

      return var7;
   }

   private boolean containsIgnoreCase(Set<String> var1, String var2) {
      for (String var4 : var1) {
         if (var4 != null && var4.equalsIgnoreCase(var2)) {
            return true;
         }
      }

      return false;
   }

   private String autoDisplay(GpsTargetType var1, String var2) {
      String var3 = var2 == null ? "" : var2.trim();
      if (var1 == GpsTargetType.POI) {
         return this.integrationManager.getPoiDisplayName(var3);
      }

      int var5 = var3.indexOf(58);
      if (var5 > 0 && var5 + 1 < var3.length()) {
         var3 = var3.substring(var5 + 1);
      }

      if ((var3 = var3.replace('_', ' ').trim()).isEmpty()) {
         var3 = var1.name();
      }

      return var3;
   }

   private String sanitizeId(String var1) {
      return var1 == null ? "" : var1.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
   }

   private String blankToDefault(String var1, String var2) {
      return this.isBlank(var1) ? var2 : var1.trim();
   }

   private boolean isBlank(String var1) {
      return var1 == null || var1.trim().isEmpty();
   }
}
