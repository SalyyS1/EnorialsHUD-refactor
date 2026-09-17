/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import sk.enorials.hud.EnorialsHUD;

public final class StaticPackHosting {
    private StaticPackHosting() {
    }

    public static boolean enabled(EnorialsHUD enorialsHUD) {
        return !enorialsHUD.cfg().getString("resource-pack.static-url", "").isBlank();
    }

    public static String base(EnorialsHUD enorialsHUD) throws IOException {
        return StaticPackHosting.parse(enorialsHUD.cfg().getString("resource-pack.static-url", ""));
    }

    public static String parse(String string) throws IOException {
        try {
            URI uRI = URI.create(string.trim());
            if (!List.of("http", "https").contains(uRI.getScheme()) || uRI.getHost() == null || uRI.getUserInfo() != null || uRI.getRawQuery() != null || uRI.getRawFragment() != null || uRI.getPort() == 0 || uRI.getPort() > 65535 || !uRI.normalize().equals(uRI)) {
                throw new IllegalArgumentException();
            }
            String string2 = uRI.getRawPath();
            if (string2.endsWith(".zip") || string2.contains("%") || string2.contains("\\")) {
                throw new IllegalArgumentException();
            }
            String string3 = uRI.toASCIIString();
            while (string3.endsWith("/")) {
                string3 = string3.substring(0, string3.length() - 1);
            }
            return string3;
        }
        catch (Exception exception) {
            throw new IOException("resource-pack.static-url must be the HTTP(S) folder where you upload hosting-export, for example https://example.com/hud (not a ZIP link).");
        }
    }
}

