/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventException
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.compat;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class OptionalPlayerEvents {
    private OptionalPlayerEvents() {
    }

    public static boolean register(Plugin plugin, Listener listener2, String string, Consumer<Player> consumer) {
        Method method;
        Class<Event> clazz;
        try {
            clazz = Class.forName(string).asSubclass(Event.class);
            method = clazz.getMethod("getPlayer", new Class[0]);
        }
        catch (ClassNotFoundException classNotFoundException) {
            return false;
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Invalid player event: " + string, reflectiveOperationException);
        }
        Bukkit.getPluginManager().registerEvent(clazz, listener2, EventPriority.MONITOR, (listener, event) -> {
            if (!clazz.isInstance(event)) {
                return;
            }
            try {
                consumer.accept((Player)method.invoke((Object)event, new Object[0]));
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                throw new EventException((Throwable)reflectiveOperationException);
            }
        }, plugin);
        return true;
    }
}

