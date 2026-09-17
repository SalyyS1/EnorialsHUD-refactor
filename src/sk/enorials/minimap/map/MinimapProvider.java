/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.map;

import org.bukkit.entity.Player;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.clientside.SteerableViewFactory;
import sk.enorials.minimap.listener.MinimapBlockListener;
import sk.enorials.minimap.listener.MinimapListener;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.icon.provider.MinimapIconProvider;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.provider.MinimapWorldRendererProvider;
import sk.enorials.minimap.storage.MinimapPlayerDatabase;

public interface MinimapProvider {
    public ClientsideMinimapFactory clientsideMinimapFactory();

    public MinimapPacketSender packetSender();

    public WorldMinimapRenderer worldRenderer();

    public MinimapIconProvider iconProvider();

    public MinimapWorldRendererProvider worldRendererProvider();

    public MinimapListener minimapListener();

    public MinimapBlockListener minimapBlockListener();

    public SteerableViewFactory steerableViewFactory();

    public MinimapPlayerDatabase playerDataStorage();

    public Minimap getPlayerMinimap(Player var1);

    public FullscreenMinimap getFullscreenMinimap(Player var1);

    public static MinimapProvider get() {
        return EnorialsMinimap.get();
    }
}

