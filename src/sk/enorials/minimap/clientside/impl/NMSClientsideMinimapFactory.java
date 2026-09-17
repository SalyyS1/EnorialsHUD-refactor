/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.decoration.ItemFrame
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.saveddata.maps.MapId
 *  org.bukkit.World
 *  org.bukkit.craftbukkit.CraftWorld
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.clientside.EntityHandle;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;

public class NMSClientsideMinimapFactory
implements ClientsideMinimapFactory {
    private static final AtomicInteger IDS = new AtomicInteger(-100000);

    private static EntityHandle<ItemFrame> frame(World world, int n, boolean bl) {
        ItemStack itemStack = new ItemStack((ItemLike)Items.FILLED_MAP);
        itemStack.set(DataComponents.MAP_ID, (Object)new MapId(n));
        ItemFrame itemFrame = new ItemFrame(EntityType.ITEM_FRAME, (Level)((CraftWorld)world).getHandle());
        itemFrame.setItem(itemStack);
        itemFrame.setInvisible(true);
        itemFrame.setDirection(bl ? Direction.DOWN : Direction.UP);
        return new EntityHandle<ItemFrame>(itemFrame);
    }

    @Override
    public MinimapLayer createMinimapLayer(World world, MinimapLayerRenderer minimapLayerRenderer) {
        int n = IDS.getAndDecrement();
        return new MinimapLayer(n, NMSClientsideMinimapFactory.frame(world, n, true), NMSClientsideMinimapFactory.frame(world, n, false), minimapLayerRenderer);
    }

    @Override
    public Minimap createMinimap(Player player, MinimapScreenPosition minimapScreenPosition, MinimapLayer minimapLayer, Map<String, SecondaryMinimapLayer> map) {
        return new Minimap(player, minimapScreenPosition, minimapLayer, map == null ? new LinkedHashMap<String, SecondaryMinimapLayer>() : new LinkedHashMap<String, SecondaryMinimapLayer>(map));
    }
}

