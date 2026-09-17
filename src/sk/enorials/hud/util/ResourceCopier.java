/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourceCopier {
    private ResourceCopier() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void copyDirectoryFromJar(JavaPlugin javaPlugin, String string, Path path) throws IOException {
        try (JarFile jarFile = new JarFile(ResourceCopier.getPluginJarPath(javaPlugin).toFile());){
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                if (!jarEntry.getName().startsWith(string + "/") || jarEntry.isDirectory()) continue;
                String string2 = jarEntry.getName().substring(string.length() + 1);
                Path path2 = path.resolve(string2);
                Files.createDirectories(path2.getParent(), new FileAttribute[0]);
                if (Files.exists(path2, new LinkOption[0])) continue;
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                try {
                    Files.copy(inputStream, path2, StandardCopyOption.REPLACE_EXISTING);
                }
                finally {
                    if (inputStream == null) continue;
                    inputStream.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void syncDirectoryFromJar(JavaPlugin javaPlugin, String string, Path path, boolean bl) throws IOException {
        try (JarFile jarFile = new JarFile(ResourceCopier.getPluginJarPath(javaPlugin).toFile());){
            HashSet<String> hashSet = new HashSet<String>();
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                if (!jarEntry.getName().startsWith(string + "/") || jarEntry.isDirectory()) continue;
                String string2 = jarEntry.getName().substring(string.length() + 1).replace('\\', '/');
                hashSet.add(string2);
                Path path2 = path.resolve(string2);
                Files.createDirectories(path2.getParent(), new FileAttribute[0]);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                try {
                    Files.copy(inputStream, path2, StandardCopyOption.REPLACE_EXISTING);
                }
                finally {
                    if (inputStream == null) continue;
                    inputStream.close();
                }
            }
            if (bl) {
                ResourceCopier.deleteStaleManagedFiles(path, hashSet);
            }
        }
    }

    private static void deleteStaleManagedFiles(Path path, Set<String> set) throws IOException {
        HashSet<String> hashSet = new HashSet<String>();
        for (String string : set) {
            int n = string.indexOf(47);
            if (n > 0) {
                hashSet.add(string.substring(0, n));
                continue;
            }
            hashSet.add(string);
        }
        for (String string : hashSet) {
            List<Path> list;
            Path path2 = path.resolve(string);
            if (!Files.exists(path2, new LinkOption[0])) continue;
            try (Stream<Path> stream = Files.walk(path2, new FileVisitOption[0]);){
                list = stream.sorted(Comparator.reverseOrder()).toList();
            }
            for (Path path3 : list) {
                Object object;
                if (Files.isRegularFile(path3, new LinkOption[0])) {
                    object = path.relativize(path3).toString().replace('\\', '/');
                    if (set.contains(object)) continue;
                    Files.deleteIfExists(path3);
                    continue;
                }
                if (!Files.isDirectory(path3, new LinkOption[0]) || path3.equals(path2)) continue;
                object = Files.list(path3);
                try {
                    if (object.findAny().isPresent()) continue;
                }
                finally {
                    if (object == null) continue;
                    object.close();
                    continue;
                }
                Files.deleteIfExists(path3);
            }
        }
    }

    private static Path getPluginJarPath(JavaPlugin javaPlugin) throws IOException {
        try {
            return Path.of(javaPlugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (URISyntaxException uRISyntaxException) {
            throw new IOException("Failed to resolve plugin jar path", uRISyntaxException);
        }
    }
}

