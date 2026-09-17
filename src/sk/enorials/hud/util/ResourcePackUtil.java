/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;

public final class ResourcePackUtil {
    private ResourcePackUtil() {
    }

    public static byte[] resolveHash(EnorialsHUD enorialsHUD, FileConfiguration fileConfiguration) {
        String string = fileConfiguration.getString("resource-pack.sha1", "").trim();
        if (!(string.equalsIgnoreCase("disabled") || string.equalsIgnoreCase("off") || string.equals("-"))) {
            byte[] byArray;
            if (!string.isEmpty() && (byArray = ResourcePackUtil.parseHex(string)) != null) {
                return byArray;
            }
            File file = ResourcePackUtil.resolveZip(enorialsHUD, fileConfiguration);
            if (file != null && file.exists()) {
                byte[] byArray2;
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    int n;
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                    byte[] byArray3 = new byte[8192];
                    while ((n = fileInputStream.read(byArray3)) > 0) {
                        messageDigest.update(byArray3, 0, n);
                    }
                    byArray2 = messageDigest.digest();
                }
                catch (Throwable throwable) {
                    try {
                        try {
                            fileInputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                        throw throwable;
                    }
                    catch (Exception exception) {
                        return null;
                    }
                }
                fileInputStream.close();
                return byArray2;
            }
            return null;
        }
        return null;
    }

    public static String resolveUrl(EnorialsHUD enorialsHUD, Player player) {
        String string;
        boolean bl = enorialsHUD.cfg().getBoolean("resource-pack.allow-player-url", false);
        if (bl && player != null && (string = enorialsHUD.storage().getPackUrl(player.getUniqueId())) != null && !string.isBlank()) {
            return string;
        }
        string = enorialsHUD.cfg().getString("resource-pack.url", "").trim();
        return string.isEmpty() ? null : string;
    }

    public static File resolveZip(EnorialsHUD enorialsHUD, FileConfiguration fileConfiguration) {
        String string = enorialsHUD.getDescription().getVersion();
        Object object = fileConfiguration.getString("copy-resource-pack.zip-path");
        if (object == null || ((String)object).isBlank()) {
            object = "EnorialsHUD/EnorialsHUD-pack-" + string + ".zip";
        }
        object = ((String)object).replace("{version}", string);
        return new File(enorialsHUD.getDataFolder().getParentFile(), (String)object);
    }

    private static byte[] parseHex(String string) {
        String string2 = string.replaceAll("[^0-9a-fA-F]", "");
        if (string2.length() != 40) {
            return null;
        }
        byte[] byArray = new byte[20];
        for (int i = 0; i < byArray.length; ++i) {
            int n = Character.digit(string2.charAt(i * 2), 16);
            int n2 = Character.digit(string2.charAt(i * 2 + 1), 16);
            if (n < 0 || n2 < 0) {
                return null;
            }
            byArray[i] = (byte)((n << 4) + n2);
        }
        return byArray;
    }
}

