/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import sk.enorials.hud.util.PackDeliverySettings;

public final class PackDownloadCheck {
    private PackDownloadCheck() {
    }

    public static Identity identity(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);){
            Identity identity = PackDownloadCheck.hash(inputStream, Long.MAX_VALUE, 0L);
            return identity;
        }
    }

    private static Identity hash(InputStream inputStream, long l, long l2) throws IOException {
        try {
            int n;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            MessageDigest messageDigest2 = MessageDigest.getInstance("SHA-256");
            long l3 = 0L;
            byte[] byArray = new byte[65536];
            while ((n = inputStream.read(byArray)) != -1) {
                if ((l3 += (long)n) > l) {
                    throw new IOException("Pack download is larger than the generated ZIP (or exceeds 256 MiB). Use the exact server-pack/EnorialsHUD-pack.zip export.");
                }
                if (l2 > 0L && System.nanoTime() > l2) {
                    throw new IOException("Pack download timed out. Check your host and run /enohud hud packcheck again.");
                }
                messageDigest.update(byArray, 0, n);
                messageDigest2.update(byArray, 0, n);
            }
            return new Identity(l3, HexFormat.of().formatHex(messageDigest.digest()), HexFormat.of().formatHex(messageDigest2.digest()));
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new AssertionError((Object)noSuchAlgorithmException);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void verify(URI uRI, Identity identity) throws IOException {
        long l = System.nanoTime() + 30000000000L;
        for (int i = 0; i <= 3; ++i) {
            HttpURLConnection httpURLConnection = (HttpURLConnection)uRI.toURL().openConnection();
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setRequestProperty("Accept", "application/zip, application/octet-stream");
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
            httpURLConnection.setRequestProperty("User-Agent", "EnorialsHUD-PackCheck/1");
            try {
                URI uRI2;
                int n = httpURLConnection.getResponseCode();
                if (n >= 300 && n < 400) {
                    String string = httpURLConnection.getHeaderField("Location");
                    if (string == null || i == 3) {
                        throw new IOException("Pack URL has a missing or excessive redirect. Use its direct ZIP download URL.");
                    }
                    uRI2 = PackDeliverySettings.zipUrl(uRI.resolve(string).toString());
                    if (uRI.getScheme().equals("https") && !uRI2.getScheme().equals("https")) {
                        throw new IOException("Pack URL redirects from HTTPS to HTTP. Use a direct HTTPS ZIP URL.");
                    }
                    if (System.nanoTime() > l) {
                        throw new IOException("Pack URL redirects timed out.");
                    }
                } else {
                    long l2;
                    if (n != 200) {
                        throw new IOException("Pack URL returned HTTP " + n + ". Upload the generated ZIP and make it downloadable without signing in.");
                    }
                    long l3 = httpURLConnection.getContentLengthLong();
                    if (l3 > (l2 = Math.min(identity.bytes(), 0x10000000L))) {
                        throw new IOException("Pack URL returns a different-sized file. Upload the current server-pack/EnorialsHUD-pack.zip.");
                    }
                    try (PushbackInputStream pushbackInputStream = new PushbackInputStream(httpURLConnection.getInputStream(), 4);){
                        byte[] byArray = pushbackInputStream.readNBytes(4);
                        if (byArray.length != 4 || byArray[0] != 80 || byArray[1] != 75 || byArray[2] != 3 || byArray[3] != 4) {
                            throw new IOException("Pack URL does not return a ZIP. It may be a folder, sign-in page or download preview.");
                        }
                        pushbackInputStream.unread(byArray);
                        Identity identity2 = PackDownloadCheck.hash(pushbackInputStream, l2, l);
                        if (!identity2.equals(identity)) {
                            throw new IOException("The hosted ZIP does not match the current combined HUD pack. Upload server-pack/EnorialsHUD-pack.zip again; keep your own pack merged into that export.");
                        }
                    }
                    return;
                }
                uRI = uRI2;
                continue;
            }
            finally {
                httpURLConnection.disconnect();
            }
        }
        throw new IOException("Pack URL could not be verified.");
    }

    public record Identity(long bytes, String sha1, String sha256) {
    }
}

