/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.util;

import java.io.IOException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.util.HostingOrigin;
import sk.enorials.hud.util.ResourcePackHost;
import sk.enorials.hud.util.StaticPackHosting;

public final class PackHostingSetup {
    private static volatile boolean transportReady;
    private static volatile boolean buildReady;
    private static volatile EnorialsHUD active;
    private static volatile String problem;

    private PackHostingSetup() {
    }

    public static boolean canBuild() {
        return buildReady;
    }

    public static boolean ready() {
        EnorialsHUD enorialsHUD = active;
        return transportReady && (enorialsHUD == null || enorialsHUD.singlePack() == null || !enorialsHUD.singlePack().single() || enorialsHUD.singlePack().ready());
    }

    public static boolean ready(Player player) {
        return !(!PackHostingSetup.ready() || active != null && active.clientProfiles() != null && !active.clientProfiles().supports(player) || active != null && active.singlePack() != null && !active.singlePack().loaded(player));
    }

    public static void initialize(EnorialsHUD enorialsHUD) {
        PackHostingSetup.start(enorialsHUD);
        if (PackHostingSetup.canBuild()) {
            enorialsHUD.packs().requestBuild();
        }
    }

    public static boolean start(EnorialsHUD enorialsHUD) {
        active = enorialsHUD;
        transportReady = false;
        buildReady = true;
        try {
            if (enorialsHUD.singlePack() != null && enorialsHUD.singlePack().selectionError() != null) {
                throw new IOException(enorialsHUD.singlePack().selectionError());
            }
            if (enorialsHUD.singlePack() != null && enorialsHUD.singlePack().single()) {
                ResourcePackHost.close();
                enorialsHUD.singlePack().announce();
                transportReady = true;
                return PackHostingSetup.ready();
            }
            if (StaticPackHosting.enabled(enorialsHUD)) {
                StaticPackHosting.base(enorialsHUD);
            } else if (enorialsHUD.cfg().getBoolean("resource-pack.local.enabled", false)) {
                HostingOrigin.parse(enorialsHUD.cfg().getString("resource-pack.local.base-url", ""));
            }
            ResourcePackHost.start(enorialsHUD);
            transportReady = true;
            enorialsHUD.getLogger().info("Resource-pack sender: EnorialsHUD managed hosting.");
        }
        catch (IOException | IllegalArgumentException exception) {
            problem = exception.getMessage();
            enorialsHUD.getLogger().warning("EnorialsHUD resource-pack setup: " + problem);
            enorialsHUD.getLogger().warning("Choose a direct combined ZIP in server.properties resource-pack, config.yml resource-pack.url, or a web folder in resource-pack.static-url. Local hosting uses resource-pack.local.base-url. Restart after changing delivery settings. Graphics wait; server data is unchanged.");
        }
        return PackHostingSetup.ready();
    }

    public static void explain(CommandSender commandSender) {
        if (active != null && active.singlePack() != null && active.singlePack().single()) {
            active.singlePack().explain(commandSender);
            return;
        }
        commandSender.sendMessage("EnorialsHUD graphics are waiting for resource-pack setup.");
        if (!(commandSender instanceof Player) || commandSender.isOp() || commandSender.hasPermission("enorials.hud.admin") || commandSender.hasPermission("enorialshud.admin")) {
            commandSender.sendMessage(problem + " Choose the pack source in config.yml resource-pack.delivery, then restart. Use delivery: server-properties to export one combined ZIP before adding its URL.");
        } else {
            commandSender.sendMessage("Ask the server administrator to fix the resource-pack setup. You can continue playing normally.");
        }
    }

    static {
        problem = "Resource-pack hosting is not configured.";
    }
}

