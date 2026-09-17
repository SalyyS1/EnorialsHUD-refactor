/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.pack;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import sk.enorials.common.pack.BetonQuestPack;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.PackProfile;
import sk.enorials.hud.pack.ServerResourcePacks;
import sk.enorials.hud.util.CopyUtil;

public final class PreparedPack {
    private final Map<String, byte[]> entries;
    private final Map<String, byte[]> visibleBars;
    private final String profile;
    private final ClientRenderProfile client;

    public ClientRenderProfile client() {
        return this.client;
    }

    private PreparedPack(Map<String, byte[]> map, Map<String, byte[]> map2, String string) {
        this(map, map2, string, ClientRenderProfile.nativeProfile());
    }

    private PreparedPack(Map<String, byte[]> map, Map<String, byte[]> map2, String string, ClientRenderProfile clientRenderProfile) {
        this.client = clientRenderProfile;
        this.entries = Collections.unmodifiableMap(map);
        this.visibleBars = Collections.unmodifiableMap(map2);
        this.profile = string;
    }

    public String profile() {
        return this.profile;
    }

    public byte[] entry(String string) {
        byte[] byArray = this.entries.get(string);
        return byArray == null ? null : (byte[])byArray.clone();
    }

    public Map<String, byte[]> visibleBars() {
        return this.visibleBars;
    }

    public PreparedPack withQuestPack(BetonQuestPack.Snapshot snapshot) throws IOException {
        return new PreparedPack(BetonQuestPack.merge(this.entries, snapshot), this.visibleBars, this.profile, this.client);
    }

    public static PreparedPack read(Path path2) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        try (Stream<Path> stream = Files.walk(path2, new FileVisitOption[0]);){
            for (Path path3 : stream.filter(path -> Files.isRegularFile(path, new LinkOption[0])).toList()) {
                treeMap.put(path2.relativize(path3).toString().replace('\\', '/'), Files.readAllBytes(path3));
            }
        }
        return new PreparedPack(treeMap, Map.of(), "");
    }

    public PreparedPack withServerPacks(Path path, String string, Map<String, byte[]> map, boolean bl, long l, int n) throws IOException {
        return this.withServerPacks(path, string, map, bl, l, n, Map.of());
    }

    public PreparedPack withServerPacks(Path path, String string, Map<String, byte[]> map, boolean bl, long l, int n, Map<String, byte[]> map2) throws IOException {
        ServerResourcePacks.Result result = ServerResourcePacks.merge(path, this.entries, map, bl, l, n, map2, this.client);
        return new PreparedPack(result.entries(), result.visibleBars(), PreparedPack.contentProfile(string, result.entries(), result.visibleBars()), this.client);
    }

    public PreparedPack forClient(Path path, ClientRenderProfile clientRenderProfile) throws IOException {
        Map.Entry<String, byte[]> entry2;
        if (!clientRenderProfile.supported()) {
            throw new IOException("Unsupported client resource profile");
        }
        if (clientRenderProfile.resourceMajor() == 88) {
            TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>(this.entries);
            treeMap.put("pack.mcmeta", PackProfile.metadata(clientRenderProfile));
            return new PreparedPack(treeMap, Map.of(), "", clientRenderProfile);
        }
        if (Files.exists(path, new LinkOption[0])) {
            CopyUtil.deleteDirectory(path);
        }
        Files.createDirectories(path, new FileAttribute[0]);
        for (Map.Entry<String, byte[]> entry2 : this.entries.entrySet()) {
            Path path2 = path.resolve(entry2.getKey()).normalize();
            if (!path2.startsWith(path.normalize())) {
                throw new IOException("Invalid generated resource path");
            }
            Files.createDirectories(path2.getParent(), new FileAttribute[0]);
            Files.write(path2, (byte[])entry2.getValue(), new OpenOption[0]);
        }
        PackProfile.apply(path, clientRenderProfile);
        PreparedPack preparedPack = PreparedPack.read(path);
        entry2 = new TreeMap();
        preparedPack.entries.forEach((arg_0, arg_1) -> this.lambda$forClient$0((TreeMap)((Object)entry2), arg_0, arg_1));
        return new PreparedPack((Map<String, byte[]>)((Object)entry2), Map.of(), "", clientRenderProfile);
    }

    public PreparedPack shareWith(PreparedPack preparedPack) {
        if (preparedPack == null) {
            return this;
        }
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        this.entries.forEach((string, byArray) -> treeMap.put((String)string, Arrays.equals(preparedPack.entries.get(string), byArray) ? preparedPack.entries.get(string) : byArray));
        return new PreparedPack(treeMap, this.visibleBars, this.profile, this.client);
    }

    private static String contentProfile(String string, Map<String, byte[]> map, Map<String, byte[]> map2) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(string.getBytes(StandardCharsets.UTF_8));
            messageDigest.update((byte)0);
            byte[] byArray = new byte[4];
            for (Map<String, byte[]> map3 : List.of(map, map2)) {
                PreparedPack.updateSize(messageDigest, byArray, map3.size());
                for (Map.Entry<String, byte[]> entry : new TreeMap<String, byte[]>(map3).entrySet()) {
                    byte[] byArray2 = entry.getKey().getBytes(StandardCharsets.UTF_8);
                    PreparedPack.updateSize(messageDigest, byArray, byArray2.length);
                    messageDigest.update(byArray2);
                    PreparedPack.updateSize(messageDigest, byArray, entry.getValue().length);
                    messageDigest.update(entry.getValue());
                }
            }
            return "content-v1-" + HexFormat.of().formatHex(messageDigest.digest());
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private static void updateSize(MessageDigest messageDigest, byte[] byArray, int n) {
        byArray[0] = (byte)(n >>> 24);
        byArray[1] = (byte)(n >>> 16);
        byArray[2] = (byte)(n >>> 8);
        byArray[3] = (byte)n;
        messageDigest.update(byArray);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void write(Path path, Map<String, byte[]> map) throws IOException {
        Path path2 = Files.createTempFile(path.toAbsolutePath().getParent(), "pack-", ".tmp", new FileAttribute[0]);
        try {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(Files.newOutputStream(path2, new OpenOption[0]), 131072);){
                this.write(bufferedOutputStream, map);
            }
            try {
                Files.move(path2, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(path2);
        }
    }

    private void write(OutputStream outputStream, Map<String, byte[]> map) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);){
            for (Map.Entry<String, byte[]> object : this.entries.entrySet()) {
                PreparedPack.put(zipOutputStream, object.getKey(), map.getOrDefault(object.getKey(), object.getValue()));
            }
            for (String string : new TreeSet<String>(map.keySet())) {
                if (this.entries.containsKey(string)) continue;
                PreparedPack.put(zipOutputStream, string, map.get(string));
            }
        }
    }

    private static void put(ZipOutputStream zipOutputStream, String string, byte[] byArray) throws IOException {
        ZipEntry zipEntry = new ZipEntry(string);
        zipEntry.setTime(0L);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(byArray);
        zipOutputStream.closeEntry();
    }

    private /* synthetic */ void lambda$forClient$0(TreeMap treeMap, String string, byte[] byArray) {
        treeMap.put(string, Arrays.equals(this.entries.get(string), byArray) ? this.entries.get(string) : byArray);
    }
}

