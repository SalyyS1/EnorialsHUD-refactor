/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Server
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.PluginDescriptionFile
 */
package sk.enorials.hud.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import sk.enorials.hud.EnorialsHUD;

public abstract class HudModule {
    private final EnorialsHUD owner;
    private final String key;
    private final File folder;
    private FileConfiguration config;

    protected HudModule(EnorialsHUD enorialsHUD, String string, String string2) {
        this.owner = enorialsHUD;
        this.key = string;
        this.folder = new File(enorialsHUD.getDataFolder(), string);
        Path path = enorialsHUD.getDataFolder().toPath().resolveSibling(string2);
        try {
            if (!this.folder.exists() && Files.isDirectory(path, new LinkOption[0])) {
                try (Stream<Path> stream = Files.walk(path, new FileVisitOption[0]);){
                    for (Path path2 : stream.toList()) {
                        Path path3 = this.folder.toPath().resolve(path.relativize(path2));
                        if (Files.isDirectory(path2, new LinkOption[0])) {
                            Files.createDirectories(path3, new FileAttribute[0]);
                            continue;
                        }
                        if (!Files.isRegularFile(path2, new LinkOption[0])) continue;
                        Files.createDirectories(path3.getParent(), new FileAttribute[0]);
                        Files.copy(path2, path3, new CopyOption[0]);
                    }
                }
                this.getLogger().info("Imported " + string2 + " data into " + string + "; original retained.");
            }
            Files.createDirectories(this.folder.toPath(), new FileAttribute[0]);
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Cannot import " + string2, iOException);
        }
    }

    public EnorialsHUD owner() {
        return this.owner;
    }

    public File getDataFolder() {
        return this.folder;
    }

    public Logger getLogger() {
        return this.owner.getLogger();
    }

    public Server getServer() {
        return this.owner.getServer();
    }

    public boolean isEnabled() {
        return this.owner.isEnabled();
    }

    public PluginDescriptionFile getDescription() {
        return this.owner.getDescription();
    }

    public InputStream getResource(String string) {
        InputStream inputStream = this.owner.getResource("modules/" + this.key + "/" + string);
        if (inputStream == null && this.key.equals("minimap") && string.startsWith("gui-pack/assets/")) {
            return this.owner.getResource("default/pack/" + string.substring("gui-pack/".length()));
        }
        return inputStream;
    }

    public void saveResource(String string, boolean bl) {
        Path path = this.folder.toPath().resolve(string).normalize();
        if (!path.startsWith(this.folder.toPath())) {
            throw new IllegalArgumentException(string);
        }
        if (Files.exists(path, new LinkOption[0]) && !bl) {
            return;
        }
        try (InputStream inputStream = this.getResource(string);){
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing module resource: " + string);
            }
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
    }

    public FileConfiguration getConfig() {
        if (this.config == null) {
            this.reloadConfig();
        }
        return this.config;
    }

    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration((File)new File(this.folder, "config.yml"));
    }

    public void saveDefaultConfig() {
        this.saveResource("config.yml", false);
        this.reloadConfig();
    }

    public void saveConfig() {
        try {
            this.getConfig().save(new File(this.folder, "config.yml"));
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
    }

    public PluginCommand getCommand(String string) {
        return this.owner.getCommand(string);
    }
}

