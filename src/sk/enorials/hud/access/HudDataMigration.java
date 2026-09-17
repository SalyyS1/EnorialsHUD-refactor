/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.access;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.Stream;
import sk.enorials.hud.EnorialsHUD;

public final class HudDataMigration {
    private HudDataMigration() {
    }

    public static boolean prepare(EnorialsHUD enorialsHUD) {
        Path path = enorialsHUD.getDataFolder().toPath().toAbsolutePath().normalize();
        Path path2 = path.getParent();
        Path path3 = path2.resolve("EnorialsAnchorHUD");
        try {
            if (!Files.isDirectory(path3, new LinkOption[0])) {
                HudDataMigration.updateExportPath(path);
                return true;
            }
            if (Files.isSymbolicLink(path3) || Files.isSymbolicLink(path)) {
                throw new IllegalStateException("Migrate linked HUD folders explicitly before starting the server.");
            }
            if (Files.exists(path, new LinkOption[0])) {
                try (Stream<Path> stream = Files.list(path);){
                    if (stream.findAny().isPresent()) {
                        throw new IllegalStateException("Both HUD data folders contain files. Merge the old and new folders before starting; nothing was overwritten.");
                    }
                }
                Files.delete(path);
            }
            Files.move(path3, path, new CopyOption[0]);
            HudDataMigration.updateExportPath(path);
            enorialsHUD.getLogger().info("Migrated existing HUD data to EnorialsHUD.");
            return true;
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Cannot migrate HUD data to EnorialsHUD", iOException);
        }
    }

    private static void updateExportPath(Path path) throws IOException {
        String string;
        Path path2 = path.resolve("config.yml");
        if (!Files.isRegularFile(path2, new LinkOption[0])) {
            return;
        }
        String string2 = Files.readString(path2);
        if (!string2.equals(string = string2.replaceAll("(?m)^(\\s*zip-path\\s*:.*)EnorialsAnchorHUD(?=[/\\\\])", "$1EnorialsHUD"))) {
            Path path3 = path.resolve("config-before-name-migration.yml.bak");
            if (!Files.exists(path3, new LinkOption[0])) {
                Files.copy(path2, path3, new CopyOption[0]);
            }
            Files.writeString(path2, (CharSequence)string, new OpenOption[0]);
        }
    }
}

