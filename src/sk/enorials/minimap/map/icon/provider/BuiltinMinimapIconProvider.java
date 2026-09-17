/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.icon.provider;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import sk.enorials.hud.map.PointerIcons;
import sk.enorials.minimap.map.icon.MarkerIcons;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.icon.provider.MinimapIconProvider;

public class BuiltinMinimapIconProvider
implements MinimapIconProvider {
    private static final String RESOURCE = "modules/minimap/minimap/";
    private static final Set<String> SPECIAL = Set.of("player", "death", "offscreen_player");
    private final Map<String, MinimapIcon> cache = new ConcurrentHashMap<String, MinimapIcon>();
    private final Path lookupDirectory;
    private Set<String> loadedKeys = Set.of();

    public BuiltinMinimapIconProvider(Path path) {
        this.lookupDirectory = path;
        try {
            this.loadKeys();
        }
        catch (IOException | URISyntaxException exception) {
            throw new IllegalStateException("Cannot index minimap icons", exception);
        }
    }

    @Override
    public MinimapIcon getIcon(String string) {
        return this.cache.computeIfAbsent(string, this::lookupIconSilent);
    }

    public MinimapIcon lookupIconSilent(String string) {
        try {
            return this.lookupIcon(string);
        }
        catch (IOException iOException) {
            return null;
        }
    }

    public MinimapIcon lookupIcon(String string) throws IOException {
        Object object;
        Path path = this.lookupDirectory.toAbsolutePath().normalize();
        Path path2 = path.resolve(string + ".png").normalize();
        if (!path2.startsWith(path) || string.indexOf(92) >= 0 || string.indexOf(47) >= 0 || string.indexOf(0) >= 0) {
            return null;
        }
        if (!Files.exists(path2, new LinkOption[0]) && (object = PointerIcons.create(string)) != null) {
            return object;
        }
        object = Files.exists(path2, new LinkOption[0]) ? Files.newInputStream(path2, new OpenOption[0]) : this.getClass().getClassLoader().getResourceAsStream(RESOURCE + string + ".png");
        try {
            BufferedImage bufferedImage = object == null ? null : ImageIO.read((InputStream)object);
            MinimapIcon minimapIcon = bufferedImage == null ? null : MinimapIcon.fromBufferedImage(string, bufferedImage);
            return minimapIcon;
        }
        finally {
            if (object != null) {
                ((InputStream)object).close();
            }
        }
    }

    @Override
    public void registerIcon(String string, MinimapIcon minimapIcon) {
        this.cache.put(string, minimapIcon);
    }

    protected void loadKeys(Set<String> set, Path path) throws IOException {
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, "*.png");){
            for (Path path2 : directoryStream) {
                if (!Files.isRegularFile(path2, new LinkOption[0])) continue;
                BuiltinMinimapIconProvider.addName(set, path2.getFileName().toString());
            }
        }
    }

    private static void addName(Set<String> set, String string) {
        if (string.endsWith(".png")) {
            set.add(string.substring(string.lastIndexOf(47) + 1, string.length() - 4));
        }
    }

    protected void loadClasspathKeys(Set<String> set, String string) throws IOException, URISyntaxException {
        String string2 = string.replaceAll("^/+|/+$", "") + "/";
        URL uRL = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (!uRL.getProtocol().equals("file")) {
            throw new IOException("Unsupported icon container: " + uRL.getProtocol());
        }
        Path path = Path.of(uRL.toURI());
        if (Files.isDirectory(path, new LinkOption[0])) {
            this.loadKeys(set, path.resolve(string2));
            return;
        }
        try (JarFile jarFile = new JarFile(path.toFile());){
            jarFile.stream().filter(jarEntry -> !jarEntry.isDirectory() && jarEntry.getName().startsWith(string2)).forEach(jarEntry -> BuiltinMinimapIconProvider.addName(set, jarEntry.getName()));
        }
    }

    protected void loadKeys() throws IOException, URISyntaxException {
        TreeSet<String> treeSet = new TreeSet<String>(SPECIAL);
        treeSet.addAll(MarkerIcons.BUILTIN);
        this.loadKeys(treeSet, this.lookupDirectory);
        this.loadClasspathKeys(treeSet, RESOURCE);
        this.loadedKeys = Set.copyOf(treeSet);
    }

    @Override
    public Set<String> allKeys() {
        return this.loadedKeys;
    }

    @Override
    public Set<String> specialIconKeys() {
        HashSet<String> hashSet = new HashSet<String>(this.loadedKeys);
        hashSet.retainAll(SPECIAL);
        return hashSet;
    }

    @Override
    public Set<String> genericIconKeys() {
        HashSet<String> hashSet = new HashSet<String>(this.loadedKeys);
        hashSet.removeAll(SPECIAL);
        return hashSet;
    }

    public Map<String, MinimapIcon> getCache() {
        return this.cache;
    }

    public Path getLookupDirectory() {
        return this.lookupDirectory;
    }

    public Set<String> getLoadedKeys() {
        return this.loadedKeys;
    }
}

