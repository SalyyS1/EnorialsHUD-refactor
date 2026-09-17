/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.betonquest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.betonquest.ExpectedVersionWarnings;

public final class BetonQuestWarningBridge
implements AutoCloseable {
    private final List<ExpectedVersionWarnings> filters = new ArrayList<ExpectedVersionWarnings>();

    public static BetonQuestWarningBridge install(Plugin plugin) {
        BetonQuestWarningBridge betonQuestWarningBridge = new BetonQuestWarningBridge();
        Plugin plugin2 = Bukkit.getPluginManager().getPlugin("BetonQuest");
        if (plugin2 == null || !plugin2.isEnabled()) {
            return betonQuestWarningBridge;
        }
        try {
            if (!"3.2.0".equals(plugin2.getDescription().getVersion())) {
                throw new IllegalStateException("unsupported BetonQuest logger version");
            }
            Class<?> clazz = Class.forName("org.betonquest.betonquest.api.integration.IntegrationService", false, plugin2.getClass().getClassLoader());
            Object object = Bukkit.getServicesManager().load(clazz);
            Object object2 = BetonQuestWarningBridge.field(object, "integrationManager");
            Set set = Collections.newSetFromMap(new IdentityHashMap());
            for (Object e : (List)BetonQuestWarningBridge.field(object2, "integrations")) {
                if (BetonQuestWarningBridge.field(e, "integrationProvider") != plugin2) continue;
                Object object3 = BetonQuestWarningBridge.field(e, "logger");
                if (!object3.getClass().getName().equals("org.betonquest.betonquest.logger.DefaultBetonQuestLogger")) {
                    throw new IllegalStateException("unsupported BetonQuest logger implementation");
                }
                Logger logger = (Logger)BetonQuestWarningBridge.field(object3, "logger");
                if (!logger.getClass().getName().equals("org.betonquest.betonquest.logger.TopicLogger") || logger.getParent() == null || !"BetonQuest".equals(logger.getParent().getName())) {
                    throw new IllegalStateException("unexpected BetonQuest logger scope");
                }
                if (!set.add(logger)) continue;
                betonQuestWarningBridge.filters.add(ExpectedVersionWarnings.install(logger, Bukkit.getMinecraftVersion()));
            }
            if (set.isEmpty()) {
                throw new IllegalStateException("no BetonQuest integration logger found");
            }
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            betonQuestWarningBridge.close();
            plugin.getLogger().warning("Expected BetonQuest version warnings remain visible: " + exception.getMessage());
        }
        return betonQuestWarningBridge;
    }

    private static Object field(Object object, String string) throws ReflectiveOperationException {
        if (object == null) {
            throw new IllegalStateException("BetonQuest service not ready");
        }
        Field field = object.getClass().getDeclaredField(string);
        field.setAccessible(true);
        return field.get(object);
    }

    @Override
    public void close() {
        for (ExpectedVersionWarnings expectedVersionWarnings : this.filters) {
            expectedVersionWarnings.close();
        }
        this.filters.clear();
    }
}

