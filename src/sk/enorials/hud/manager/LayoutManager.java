/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.model.HudLayout;

public class LayoutManager {
    private final Map<String, HudLayout> layouts = new HashMap<String, HudLayout>();
    private final List<HudLayout> defaults = new ArrayList<HudLayout>();

    public void reload(EnorialsHUD enorialsHUD) {
        Object object;
        this.layouts.clear();
        File[] fileArray = new File(enorialsHUD.getDataFolder(), "layout").listFiles((file, string) -> string.endsWith(".yml"));
        if (fileArray != null) {
            object = fileArray;
            int n = ((File[])object).length;
            for (int i = 0; i < n; ++i) {
                File file2 = object[i];
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file2);
                for (String string2 : yamlConfiguration.getKeys(false)) {
                    if (!yamlConfiguration.isConfigurationSection(string2)) continue;
                    try {
                        HudLayout hudLayout = new HudLayout(yamlConfiguration.getConfigurationSection(string2));
                        this.layouts.put(hudLayout.getKey(), hudLayout);
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        enorialsHUD.getLogger().log(Level.WARNING, "Could not load layout '" + string2 + "': " + illegalArgumentException.getMessage());
                    }
                }
            }
        }
        this.defaults.clear();
        if (enorialsHUD.cfg().isList("layout.default")) {
            for (String string3 : enorialsHUD.cfg().getStringList("layout.default")) {
                if (!this.has(string3)) continue;
                this.defaults.add(this.get(string3));
            }
        } else if (enorialsHUD.cfg().isString("layout.default") && this.has((String)(object = enorialsHUD.cfg().getString("layout.default")))) {
            this.defaults.add(this.get((String)object));
        }
    }

    public boolean has(String string) {
        return string != null && this.layouts.containsKey(string.toLowerCase(Locale.ROOT));
    }

    public HudLayout get(String string) {
        return this.layouts.get(string.toLowerCase(Locale.ROOT));
    }

    public Collection<HudLayout> getLayouts() {
        return this.layouts.values();
    }

    public Collection<HudLayout> getDefaults() {
        return this.defaults;
    }
}

