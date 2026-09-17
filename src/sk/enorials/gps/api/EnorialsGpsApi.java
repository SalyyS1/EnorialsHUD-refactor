/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.gps.api;

import java.util.Map;
import org.bukkit.entity.Player;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.GpsQuestRoute;
import sk.enorials.gps.api.GpsTargetType;
import sk.enorials.gps.nav.RouteDescriptor;

public interface EnorialsGpsApi {
    public String apiVersion();

    public boolean isModeEnabled(NavigationMode var1);

    public NavigationMode getPlayerMode(Player var1);

    public void setPlayerMode(Player var1, NavigationMode var2);

    public boolean hasActiveNavigation(Player var1);

    public boolean stopNavigation(Player var1, String var2);

    public boolean startNavigation(Player var1, GpsTargetType var2, String var3);

    public boolean startNavigation(Player var1, GpsTargetType var2, String var3, NavigationMode var4, String var5);

    public boolean startNavigation(Player var1, RouteDescriptor var2, NavigationMode var3, String var4);

    public boolean addQuestRouteButton(Player var1, String var2, String var3, GpsTargetType var4, String var5);

    public boolean addQuestRouteButton(Player var1, String var2, String var3, GpsTargetType var4, String var5, NavigationMode var6, String var7);

    public boolean removeQuestRouteButton(Player var1, String var2);

    public int clearQuestRouteButtons(Player var1);

    public Map<String, GpsQuestRoute> getQuestRouteButtons(Player var1);
}

