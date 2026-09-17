/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.plugin.RegisteredServiceProvider
 */
package sk.enorials.hud.trade;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

final class VaultEconomy {
    private final Class<?> api;
    private final Object provider;

    private VaultEconomy(Class<?> clazz, Object object) {
        this.api = clazz;
        this.provider = object;
    }

    static VaultEconomy find() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider registeredServiceProvider = Bukkit.getServicesManager().getRegistration(clazz);
            if (registeredServiceProvider == null || !registeredServiceProvider.getPlugin().isEnabled()) {
                return null;
            }
            VaultEconomy vaultEconomy = new VaultEconomy(clazz, registeredServiceProvider.getProvider());
            return (Boolean)vaultEconomy.call("isEnabled", new Class[0], new Object[0]) != false ? vaultEconomy : null;
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException throwable) {
            return null;
        }
    }

    boolean current() {
        VaultEconomy vaultEconomy = VaultEconomy.find();
        return vaultEconomy != null && vaultEconomy.provider == this.provider;
    }

    String name() throws Exception {
        return (String)this.call("getName", new Class[0], new Object[0]);
    }

    int digits() throws Exception {
        int n = (Integer)this.call("fractionalDigits", new Class[0], new Object[0]);
        return n < 0 ? 2 : Math.min(6, n);
    }

    BigDecimal balance(OfflinePlayer offlinePlayer) throws Exception {
        double d = (Double)this.call("getBalance", new Class[]{OfflinePlayer.class}, offlinePlayer);
        if (!Double.isFinite(d)) {
            throw new IllegalStateException("Invalid economy balance");
        }
        return BigDecimal.valueOf(d);
    }

    boolean change(boolean bl, OfflinePlayer offlinePlayer, BigDecimal bigDecimal) throws Exception {
        double d;
        if (!this.current()) {
            throw new IllegalStateException("Economy provider changed during trade");
        }
        Object object = this.call(bl ? "depositPlayer" : "withdrawPlayer", new Class[]{OfflinePlayer.class, Double.TYPE}, offlinePlayer, bigDecimal.doubleValue());
        Class<?> clazz = this.api.getMethod("withdrawPlayer", OfflinePlayer.class, Double.TYPE).getReturnType();
        boolean bl2 = (Boolean)clazz.getMethod("transactionSuccess", new Class[0]).invoke(object, new Object[0]);
        if (bl2 && (!Double.isFinite(d = clazz.getField("amount").getDouble(object)) || Math.abs(d - bigDecimal.doubleValue()) > Math.max(1.0E-7, Math.ulp(bigDecimal.doubleValue()) * 2.0))) {
            throw new IllegalStateException("Economy reported a different transferred amount");
        }
        return bl2;
    }

    private Object call(String string, Class<?>[] classArray, Object ... objectArray) throws ReflectiveOperationException {
        try {
            return this.api.getMethod(string, classArray).invoke(this.provider, objectArray);
        }
        catch (InvocationTargetException invocationTargetException) {
            throw new ReflectiveOperationException("Economy provider failed", invocationTargetException.getCause());
        }
    }
}

