/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.HexFormat;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.util.HostingOrigin;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.hud.util.ResourcePackHost;
import sk.enorials.hud.util.StaticPackHosting;

public final class LocalPackPublisher {
    private LocalPackPublisher() {
    }

    public static String publish(EnorialsHUD enorialsHUD, File file) throws Exception {
        return LocalPackPublisher.publish(enorialsHUD, Files.readAllBytes(file.toPath()));
    }

    public static String publish(EnorialsHUD enorialsHUD, byte[] byArray) throws Exception {
        if (enorialsHUD.singlePack() != null && enorialsHUD.singlePack().single()) {
            return null;
        }
        if (!PackHostingSetup.ready()) {
            return null;
        }
        if (!StaticPackHosting.enabled(enorialsHUD) && !enorialsHUD.cfg().getBoolean("resource-pack.local.enabled", false)) {
            return null;
        }
        String string = LocalPackPublisher.digest(byArray);
        String string2 = LocalPackPublisher.urlForHash(enorialsHUD, string);
        ResourcePackHost.start(enorialsHUD);
        LocalPackPublisher.publish(ResourcePackHost.directory(enorialsHUD), byArray, string);
        return string2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void publish(Path path, byte[] byArray, String string) throws Exception {
        Files.createDirectories(path, new FileAttribute[0]);
        Path path2 = path.resolve(string + ".zip");
        if (Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS) && Files.size(path2) == (long)byArray.length && LocalPackPublisher.digest(path2).equals(string)) {
            return;
        }
        if (Files.exists(path2, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Existing pack is not an intact regular file: " + String.valueOf(path2.getFileName()));
        }
        Path path3 = Files.createTempFile(path, "pack-", ".tmp", new FileAttribute[0]);
        try {
            Files.write(path3, byArray, new OpenOption[0]);
            try {
                Files.move(path3, path2, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                Files.move(path3, path2, new CopyOption[0]);
            }
            catch (FileAlreadyExistsException fileAlreadyExistsException) {
                if (!Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS) || !LocalPackPublisher.digest(path2).equals(string)) {
                    throw fileAlreadyExistsException;
                }
            }
        }
        finally {
            Files.deleteIfExists(path3);
        }
    }

    static String digest(byte[] byArray) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(byArray));
    }

    static String digest(Path path) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        try (InputStream inputStream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS);){
            int n;
            byte[] byArray = new byte[65536];
            while ((n = inputStream.read(byArray)) >= 0) {
                messageDigest.update(byArray, 0, n);
            }
        }
        return HexFormat.of().formatHex(messageDigest.digest());
    }

    public static String urlForHash(EnorialsHUD enorialsHUD, String string) throws IOException {
        if (string == null || !string.matches("[0-9a-f]{40}")) {
            throw new IOException("Invalid resource pack SHA-1");
        }
        String string2 = StaticPackHosting.enabled(enorialsHUD) ? StaticPackHosting.base(enorialsHUD) : HostingOrigin.parse(enorialsHUD.cfg().getString("resource-pack.local.base-url", ""));
        return string2 + "/generated/" + string + ".zip";
    }
}

