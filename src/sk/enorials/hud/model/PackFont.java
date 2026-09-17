/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public class PackFont {
    private final String key;
    private final List<FontProvider> providers = new ArrayList<FontProvider>();

    public PackFont(ConfigurationSection configurationSection) {
        this.key = configurationSection.getName();
        if (configurationSection.isConfigurationSection("providers")) {
            ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection("providers");
            for (String string : configurationSection2.getKeys(false)) {
                ConfigurationSection configurationSection3 = configurationSection2.getConfigurationSection(string);
                if (configurationSection3 == null) continue;
                this.providers.add(FontProvider.from(configurationSection3));
            }
        }
        if (configurationSection.isConfigurationSection("advances")) {
            this.providers.add(FontProvider.fromAdvances(configurationSection.getConfigurationSection("advances")));
        }
    }

    public String getKey() {
        return this.key;
    }

    public List<JsonObject> buildProviders(int n) {
        ArrayList<JsonObject> arrayList = new ArrayList<JsonObject>();
        for (FontProvider fontProvider : this.providers) {
            arrayList.add(fontProvider.toJson(n));
        }
        return arrayList;
    }

    public List<File> getReferencedTextures(File file) {
        ArrayList<File> arrayList = new ArrayList<File>();
        for (FontProvider fontProvider : this.providers) {
            if (fontProvider.texturePath == null) continue;
            arrayList.add(new File(file, "textures/" + fontProvider.texturePath));
        }
        return arrayList;
    }

    private static class FontProvider {
        private final String type;
        private final String texturePath;
        private final int height;
        private final Integer ascent;
        private final List<String> chars;
        private final ConfigurationSection advances;

        private FontProvider(String string, String string2, int n, Integer n2, List<String> list, ConfigurationSection configurationSection) {
            this.type = string;
            this.texturePath = string2;
            this.height = n;
            this.ascent = n2;
            this.chars = list;
            this.advances = configurationSection;
        }

        static FontProvider from(ConfigurationSection configurationSection) {
            String string = configurationSection.getString("type", null);
            if (string == null) {
                string = configurationSection.isString("texture") ? "bitmap" : "space";
            }
            String string2 = configurationSection.getString("texture", null);
            int n = configurationSection.getInt("height", 8);
            Integer n2 = configurationSection.isInt("ascent") ? Integer.valueOf(configurationSection.getInt("ascent")) : null;
            List list = configurationSection.getStringList("chars");
            return new FontProvider(string, string2, n, n2, list, null);
        }

        static FontProvider fromAdvances(ConfigurationSection configurationSection) {
            return new FontProvider("space", null, 0, null, null, configurationSection);
        }

        JsonObject toJson(int n) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", this.type);
            if ("space".equalsIgnoreCase(this.type)) {
                JsonObject jsonObject2 = new JsonObject();
                if (this.advances != null) {
                    for (String string : this.advances.getKeys(false)) {
                        jsonObject2.addProperty(string, (Number)this.advances.getDouble(string));
                    }
                }
                jsonObject.add("advances", (JsonElement)jsonObject2);
                return jsonObject;
            }
            if (this.texturePath != null) {
                jsonObject.addProperty("file", "enorialshud:font/" + this.texturePath);
            }
            jsonObject.addProperty("height", (Number)this.height);
            jsonObject.addProperty("ascent", (Number)(n != 0 ? n : (this.ascent != null ? this.ascent : 0)));
            JsonArray jsonArray = new JsonArray();
            if (this.chars != null) {
                for (String string : this.chars) {
                    jsonArray.add(string);
                }
            }
            jsonObject.add("chars", (JsonElement)jsonArray);
            return jsonObject;
        }
    }
}

