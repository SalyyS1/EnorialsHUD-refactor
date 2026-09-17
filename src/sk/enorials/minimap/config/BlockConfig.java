/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.material.MapColor
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.block.data.CraftBlockData
 */
package sk.enorials.minimap.config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import sk.enorials.hud.config.YamlSettings;
import sk.enorials.minimap.serializer.LoadResult;

public class BlockConfig
extends YamlSettings {
    private static final BlockConfig INSTANCE = new BlockConfig();
    public Map<String, String> blockOverrides = Map.of();
    private transient Map<BlockState, MapColor> resolvedOverrides = Map.of();

    BlockConfig() {
    }

    public static BlockConfig instance() {
        return INSTANCE;
    }

    private static BlockState state(String string) {
        return ((CraftBlockData)Bukkit.createBlockData((String)string)).getState();
    }

    @Override
    public LoadResult reload(Path path) {
        LoadResult loadResult = super.reload(path);
        HashMap<BlockState, MapColor> hashMap = new HashMap<BlockState, MapColor>();
        if (this.blockOverrides != null) {
            for (Map.Entry<String, String> entry : this.blockOverrides.entrySet()) {
                try {
                    String string = entry.getValue();
                    MapColor mapColor = string.matches("[+-]?[0-9]+") ? MapColor.byId((int)Integer.parseInt(string)) : BlockConfig.state(string).getMapColor(null, null);
                    hashMap.put(BlockConfig.state(entry.getKey()), Objects.requireNonNull(mapColor, "Undefined map colour"));
                }
                catch (RuntimeException runtimeException) {
                    throw new IllegalArgumentException("Invalid map colour override for " + entry.getKey() + ": " + entry.getValue(), runtimeException);
                }
            }
        }
        this.resolvedOverrides = Map.copyOf(hashMap);
        return loadResult;
    }

    public Map<BlockState, MapColor> getResolvedOverrides() {
        return this.resolvedOverrides;
    }
}

