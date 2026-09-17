/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.pack.PreparedPack;
import sk.enorials.hud.pack.VanillaBarPack;
import sk.enorials.hud.util.GitHubUploader;
import sk.enorials.hud.util.LocalPackPublisher;
import sk.enorials.hud.util.ResourcePackHost;

final class SharedAvatarPack {
    private final EnorialsHUD hud;
    private final Map<String, Entry> ready = new HashMap<String, Entry>();

    SharedAvatarPack(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
    }

    synchronized void invalidate() {
        this.ready.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    synchronized Entry get(PreparedPack preparedPack, boolean bl) throws Exception {
        Object object;
        Object object2;
        Object object3;
        String string = preparedPack.profile() + (bl ? "-hidden" : "-visible");
        Entry entry = this.ready.get(string);
        if (entry != null) {
            return entry;
        }
        Path path = this.hud.getDataFolder().toPath().resolve("shared-pack-cache");
        Files.createDirectories(path, new FileAttribute[0]);
        Path path2 = path.resolve(string + ".properties");
        if (Files.isRegularFile(path2, new LinkOption[0]) && this.hud.cfg().getBoolean("resource-pack.local.enabled", false)) {
            object3 = new Properties();
            object2 = Files.newInputStream(path2, new OpenOption[0]);
            try {
                ((Properties)object3).load((InputStream)object2);
            }
            finally {
                if (object2 != null) {
                    ((InputStream)object2).close();
                }
            }
            object2 = ((Properties)object3).getProperty("sha1", "");
            if (((String)object2).matches("[0-9a-f]{40}") && Files.isRegularFile((Path)(object = (Object)ResourcePackHost.directory(this.hud).resolve((String)object2 + ".zip")), new LinkOption[0]) && Long.toString(Files.size((Path)object)).equals(((Properties)object3).getProperty("bytes"))) {
                Entry entry2 = new Entry(LocalPackPublisher.urlForHash(this.hud, (String)object2), HexFormat.of().parseHex((CharSequence)object2), bl);
                this.ready.put(string, entry2);
                return entry2;
            }
        }
        object3 = Files.createTempFile(path, "shared-", ".zip", new FileAttribute[0]);
        try {
            Object object4;
            Object object5;
            Object object6;
            preparedPack.write((Path)object3, VanillaBarPack.replacements(this.hud, bl, preparedPack));
            object2 = Files.readAllBytes((Path)object3);
            object = MessageDigest.getInstance("SHA-1").digest((byte[])object2);
            String string2 = LocalPackPublisher.publish(this.hud, (byte[])object2);
            if (string2 == null) {
                object6 = this.hud.cfg().getString("upload.github.path", "rp/EnorialsHUD-pack.zip");
                object5 = ((String)object6).substring(0, Math.max(0, ((String)object6).lastIndexOf(47) + 1));
                object4 = GitHubUploader.upload(this.hud, object3.toFile(), (String)object5 + "shared/" + HexFormat.of().formatHex((byte[])object) + ".zip");
                if (((GitHubUploader.UploadResult)object4).success()) {
                    string2 = ((GitHubUploader.UploadResult)object4).rawUrl();
                }
            }
            if (string2 == null || string2.isBlank()) {
                throw new IOException("No shared resource-pack host configured");
            }
            object6 = new Properties();
            ((Properties)object6).setProperty("sha1", HexFormat.of().formatHex((byte[])object));
            ((Properties)object6).setProperty("bytes", Integer.toString(((Object)object2).length));
            object5 = Files.createTempFile(path, "reference-", ".tmp", new FileAttribute[0]);
            try {
                object4 = Files.newOutputStream((Path)object5, new OpenOption[0]);
                try {
                    ((Properties)object6).store((OutputStream)object4, "Shared HUD pack; independent of players and skins");
                }
                finally {
                    if (object4 != null) {
                        ((OutputStream)object4).close();
                    }
                }
                Files.move((Path)object5, path2, StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                Files.deleteIfExists((Path)object5);
            }
            object4 = new Entry(string2, (byte[])object, bl);
            this.ready.put(string, (Entry)object4);
            Object object7 = object4;
            return object7;
        }
        finally {
            Files.deleteIfExists((Path)object3);
        }
    }

    record Entry(String url, byte[] hash, boolean hidden) {
    }
}

