/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

public record PackDeliverySettings(Source source, String url, String sha1) {
    public boolean single() {
        return this.source != Source.MANAGED;
    }

    public static PackDeliverySettings select(String string, String string2, String string3, String string4, String string5) throws IOException {
        string = PackDeliverySettings.clean(string).toLowerCase(Locale.ROOT);
        string2 = PackDeliverySettings.clean(string2);
        string4 = PackDeliverySettings.clean(string4);
        if (!Set.of("auto", "server-properties", "plugin").contains(string)) {
            throw new IOException("resource-pack.delivery must be auto, server-properties or plugin. Restart after changing it.");
        }
        if (string.equals("plugin") && !string2.isEmpty()) {
            throw new IOException("Two pack senders are configured: delivery=plugin but server.properties resource-pack is not empty. Clear that property and restart, or choose delivery=auto/server-properties.");
        }
        if (string.equals("server-properties") || string.equals("auto") && !string2.isEmpty()) {
            return new PackDeliverySettings(Source.SERVER_PROPERTIES, string2, PackDeliverySettings.clean(string3).toLowerCase(Locale.ROOT));
        }
        if (!string4.isEmpty()) {
            return new PackDeliverySettings(Source.CONFIG_URL, string4, PackDeliverySettings.clean(string5).toLowerCase(Locale.ROOT));
        }
        return new PackDeliverySettings(Source.MANAGED, "", "");
    }

    public URI validate() throws IOException {
        if (!this.single()) {
            throw new IOException("This delivery mode does not use a single ZIP URL.");
        }
        if (this.url.isBlank()) {
            throw new IOException("server.properties resource-pack is empty. Upload server-pack/EnorialsHUD-pack.zip, set its direct ZIP URL and resource-pack-sha1, then restart.");
        }
        if (!this.sha1.isEmpty() && !this.sha1.matches("[0-9a-f]{40}")) {
            throw new IOException(this.source == Source.SERVER_PROPERTIES ? "server.properties resource-pack-sha1 must be a 40-character SHA-1 hash from server-pack/SHA1SUMS.txt." : "config.yml resource-pack.sha1 must be empty or a 40-character SHA-1 hash from server-pack/SHA1SUMS.txt.");
        }
        return PackDeliverySettings.zipUrl(this.url);
    }

    public static URI zipUrl(String string) throws IOException {
        try {
            URI uRI = new URI(string);
            if (!Set.of("http", "https").contains(uRI.getScheme()) || uRI.getHost() == null || uRI.getUserInfo() != null || uRI.getFragment() != null || uRI.getPort() == 0 || uRI.getPort() > 65535 || !uRI.normalize().equals(uRI) || string.contains("\\")) {
                throw new IllegalArgumentException();
            }
            return uRI;
        }
        catch (Exception exception) {
            throw new IOException("Use a complete http:// or https:// direct ZIP download URL, without credentials, fragments or dot segments. A web folder belongs in resource-pack.static-url instead.");
        }
    }

    private static String clean(String string) {
        return string == null ? "" : string.trim();
    }

    public static enum Source {
        SERVER_PROPERTIES,
        CONFIG_URL,
        MANAGED;

    }
}

