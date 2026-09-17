/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.LegacyBarAtlas;
import sk.enorials.hud.compat.PackProfile;
import sk.enorials.hud.pack.PreparedPack;

public final class VanillaBarPack {
    private VanillaBarPack() {
    }

    public static byte[] merge(EnorialsHUD enorialsHUD, Path path, boolean bl, PreparedPack preparedPack) throws IOException {
        Map<String, byte[]> map = VanillaBarPack.replacements(enorialsHUD, bl, preparedPack);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipFile zipFile = new ZipFile(path.toFile());
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);){
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (zipEntry.isDirectory()) continue;
                byte[] object = map.remove(zipEntry.getName());
                if (object == null) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    try {
                        VanillaBarPack.put(zipOutputStream, zipEntry.getName(), inputStream.readAllBytes());
                        continue;
                    }
                    finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        continue;
                    }
                }
                VanillaBarPack.put(zipOutputStream, zipEntry.getName(), object);
            }
            for (Map.Entry entry : map.entrySet()) {
                VanillaBarPack.put(zipOutputStream, (String)entry.getKey(), (byte[])entry.getValue());
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static Map<String, byte[]> replacements(EnorialsHUD enorialsHUD, boolean bl, PreparedPack preparedPack) throws IOException {
        byte[] byArray;
        Map<String, byte[]> map;
        Map<String, byte[]> map2 = map = bl ? VanillaBarPack.defaults(enorialsHUD, true, preparedPack.client()) : new TreeMap<String, byte[]>(preparedPack.visibleBars());
        if (bl && preparedPack.client().resourceMajor() < 18 && (byArray = preparedPack.visibleBars().get("assets/minecraft/textures/gui/icons.png")) != null) {
            map.put("assets/minecraft/textures/gui/icons.png", LegacyBarAtlas.hidden(byArray));
        }
        return map;
    }

    public static Map<String, byte[]> defaults(EnorialsHUD enorialsHUD, boolean bl) throws IOException {
        return VanillaBarPack.defaults(enorialsHUD, bl, ClientRenderProfile.nativeProfile());
    }

    public static Map<String, byte[]> defaults(EnorialsHUD enorialsHUD, boolean bl, ClientRenderProfile clientRenderProfile) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        try (Iterator iterator = enorialsHUD.getResource("vanilla-bars/" + (bl ? "all" : "visible") + ".zip");){
            if (iterator == null) {
                throw new IOException("Missing vanilla-bar pack variant");
            }
            try (Object object = new ZipInputStream((InputStream)((Object)iterator));){
                ZipEntry zipEntry;
                while ((zipEntry = ((ZipInputStream)object).getNextEntry()) != null) {
                    if (zipEntry.isDirectory() || !zipEntry.getName().startsWith("assets/minecraft/textures/gui/sprites/hud/")) continue;
                    treeMap.put(zipEntry.getName(), ((ZipInputStream)object).readAllBytes());
                }
            }
        }
        if (clientRenderProfile.resourceMajor() < 18) {
            iterator = (Iterator)LegacyBarAtlas.visible(enorialsHUD, treeMap);
            treeMap.put("assets/minecraft/textures/gui/icons.png", (byte[])(bl ? (Object)LegacyBarAtlas.hidden(iterator) : iterator));
        }
        for (Object object : new ArrayList(treeMap.keySet())) {
            if (!((String)object).endsWith(".png")) continue;
            treeMap.putIfAbsent((String)object + ".mcmeta", "{}".getBytes(StandardCharsets.UTF_8));
        }
        return treeMap;
    }

    public static byte[] overlay(EnorialsHUD enorialsHUD, boolean bl, Map<String, byte[]> map) throws IOException {
        return VanillaBarPack.overlay(enorialsHUD, bl, map, ClientRenderProfile.nativeProfile());
    }

    public static byte[] overlay(EnorialsHUD enorialsHUD, boolean bl, Map<String, byte[]> map, ClientRenderProfile clientRenderProfile) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);){
            VanillaBarPack.put(zipOutputStream, "pack.mcmeta", PackProfile.metadata(clientRenderProfile));
            for (Map.Entry<String, byte[]> entry : new TreeMap<String, byte[]>(bl ? VanillaBarPack.defaults(enorialsHUD, true, clientRenderProfile) : map).entrySet()) {
                VanillaBarPack.put(zipOutputStream, entry.getKey(), entry.getValue());
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void put(ZipOutputStream zipOutputStream, String string, byte[] byArray) throws IOException {
        ZipEntry zipEntry = new ZipEntry(string);
        zipEntry.setTime(0L);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(byArray);
        zipOutputStream.closeEntry();
    }
}

