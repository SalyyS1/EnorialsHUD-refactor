/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.RegisteredServiceProvider
 */
package sk.enorials.gps.api;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import sk.enorials.gps.api.EnorialsGpsApi;

public final class EnorialsGpsApiProvider {
    private EnorialsGpsApiProvider() {
    }

    public static Optional<EnorialsGpsApi> get() {
        RegisteredServiceProvider registeredServiceProvider = Bukkit.getServicesManager().getRegistration(EnorialsGpsApi.class);
        if (registeredServiceProvider == null) {
            return Optional.empty();
        }
        return Optional.of((EnorialsGpsApi)registeredServiceProvider.getProvider());
    }
}

