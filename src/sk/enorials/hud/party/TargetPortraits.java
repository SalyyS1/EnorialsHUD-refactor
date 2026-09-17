/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.ticxo.modelengine.api.ModelEngineAPI
 *  com.ticxo.modelengine.api.model.ModeledEntity
 *  io.lumine.mythic.bukkit.MythicBukkit
 *  io.lumine.mythic.core.mobs.ActiveMob
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.LivingEntity
 */
package sk.enorials.hud.party;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import sk.enorials.hud.EnorialsHUD;

public final class TargetPortraits {
    private TargetPortraits() {
    }

    public static void install(EnorialsHUD enorialsHUD) {
        try {
            String string;
            YamlConfiguration yamlConfiguration;
            Object object;
            String string2 = "textures/assets/enorialshud/target_custom_heads.png";
            Path path = enorialsHUD.getDataFolder().toPath().resolve(string2);
            if (!Files.exists(path, new LinkOption[0])) {
                Files.createDirectories(path.getParent(), new FileAttribute[0]);
                object = enorialsHUD.getResource("default/" + string2);
                try {
                    if (object == null) {
                        throw new IOException("Missing custom target portrait atlas");
                    }
                    Files.copy((InputStream)object, path, new CopyOption[0]);
                }
                finally {
                    if (object != null) {
                        ((InputStream)object).close();
                    }
                }
            }
            if (!(yamlConfiguration = YamlConfiguration.loadConfiguration((File)(object = new File(enorialsHUD.getDataFolder(), "assets/font/target-heads.yml")))).contains(string = "target-heads.providers.enorials-model-portraits")) {
                yamlConfiguration.set(string + ".type", (Object)"bitmap");
                yamlConfiguration.set(string + ".texture", (Object)"assets/enorialshud/target_custom_heads.png");
                yamlConfiguration.set(string + ".height", (Object)24);
                yamlConfiguration.set(string + ".chars", List.of("\uf180\uf181"));
                Files.createDirectories(((File)object).toPath().getParent(), new FileAttribute[0]);
                yamlConfiguration.save((File)object);
            }
        }
        catch (IOException iOException) {
            throw new UncheckedIOException("Cannot install custom target portraits", iOException);
        }
    }

    static String select(EnorialsHUD enorialsHUD, LivingEntity livingEntity, Properties properties) {
        try {
            Object object;
            Object object2;
            if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs") && (object2 = Mythic.id(livingEntity)) != null && (object = properties.getProperty("MYTHIC." + (String)object2)) != null) {
                return object;
            }
            if (Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
                object2 = Models.ids(livingEntity);
                object = object2.iterator();
                while (object.hasNext()) {
                    String string = (String)object.next();
                    String string2 = properties.getProperty("MODEL." + string);
                    if (string2 == null) continue;
                    return string2;
                }
                if (!object2.isEmpty()) {
                    return properties.getProperty("UNKNOWN", "f16c");
                }
            }
        }
        catch (LinkageError | RuntimeException throwable) {
            return properties.getProperty("UNKNOWN", "f16c");
        }
        return null;
    }

    private static final class Mythic {
        private Mythic() {
        }

        static String id(LivingEntity livingEntity) {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(livingEntity.getUniqueId()).orElse(null);
            return activeMob == null ? null : activeMob.getType().getInternalName();
        }
    }

    private static final class Models {
        private Models() {
        }

        static Set<String> ids(LivingEntity livingEntity) {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity((UUID)livingEntity.getUniqueId());
            return modeledEntity == null ? Set.of() : modeledEntity.getModels().keySet();
        }
    }
}

