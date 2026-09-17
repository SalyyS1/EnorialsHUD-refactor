/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.net.URI;

public final class HostingOrigin {
    private HostingOrigin() {
    }

    public static String parse(String string) throws IOException {
        if (string == null || string.isBlank()) {
            throw new IOException("Set resource-pack.local.base-url to your pack host origin (for example https://packs.example.com), then restart.");
        }
        try {
            URI uRI = URI.create(string.trim());
            String string2 = uRI.getScheme();
            if (!"http".equals(string2) && !"https".equals(string2) || uRI.getHost() == null || uRI.getUserInfo() != null || uRI.getRawQuery() != null || uRI.getRawFragment() != null || !uRI.getRawPath().isEmpty() && !uRI.getRawPath().equals("/") || uRI.getPort() == 0 || uRI.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
            return new URI(string2, null, uRI.getHost(), uRI.getPort(), null, null, null).toASCIIString();
        }
        catch (Exception exception) {
            throw new IOException("resource-pack.local.base-url must be an HTTP(S) origin, for example https://packs.example.com; omit paths, credentials and query parameters.");
        }
    }
}

