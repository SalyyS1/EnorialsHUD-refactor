/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.minimap.serializer.LoadResult;

public abstract class YamlSettings {
    public synchronized LoadResult reload(Path path) {
        try {
            ArrayList<Assignment> arrayList;
            boolean bl;
            boolean bl2 = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
            if (bl2 && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Expected a regular settings file: " + String.valueOf(path));
            }
            YamlConfiguration yamlConfiguration = new YamlConfiguration();
            yamlConfiguration.options().pathSeparator('\u0000').parseComments(true);
            if (bl2) {
                yamlConfiguration.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
            }
            if ((bl = YamlSettings.read(this, (ConfigurationSection)yamlConfiguration, arrayList = new ArrayList<Assignment>(), "")) || !bl2) {
                YamlSettings.write(path, yamlConfiguration.saveToString());
            }
            for (Assignment assignment : arrayList) {
                assignment.apply();
            }
            return bl2 ? LoadResult.SUCCESS : LoadResult.CONFIG_NOT_EXISTS;
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("Cannot load minimap settings " + String.valueOf(path) + ": " + exception.getMessage(), exception);
        }
    }

    private static boolean read(Object object, ConfigurationSection configurationSection, List<Assignment> list, String string) throws ReflectiveOperationException {
        boolean bl = false;
        for (Field field : object.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            String string2 = YamlSettings.key(field.getName());
            String string3 = string + string2;
            Object object2 = field.get(object);
            if (!configurationSection.contains(string2)) {
                configurationSection.set(string2, YamlSettings.serialize(object2));
                bl = true;
                continue;
            }
            if (YamlSettings.isGroup(field.getType())) {
                ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string2);
                if (configurationSection2 == null) {
                    throw new IllegalArgumentException(string3 + " must be a YAML section");
                }
                bl |= YamlSettings.read(object2, configurationSection2, list, string3 + ".");
                continue;
            }
            list.add(new Assignment(object, field, YamlSettings.convert(configurationSection.get(string2), field.getType(), string3)));
        }
        return bl;
    }

    private static String key(String string) {
        return string.replaceAll("([a-z])([A-Z0-9])", "$1-$2").replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static boolean isGroup(Class<?> clazz) {
        return !clazz.isPrimitive() && !clazz.isEnum() && clazz != String.class && !Map.class.isAssignableFrom(clazz) && !List.class.isAssignableFrom(clazz);
    }

    private static Object convert(Object object, Class<?> clazz, String string) {
        Object object2;
        double d;
        if (clazz == String.class && object != null && !(object instanceof Collection) && !(object instanceof ConfigurationSection)) {
            return String.valueOf(object);
        }
        if (clazz == Boolean.TYPE && object instanceof Boolean) {
            return object;
        }
        if (clazz == Integer.TYPE && object instanceof Number && Double.isFinite(d = ((Number)(object2 = (Number)object)).doubleValue()) && d == Math.rint(d) && d >= -2.147483648E9 && d <= 2.147483647E9) {
            return (int)d;
        }
        if (clazz == Double.TYPE && object instanceof Number && Double.isFinite(((Number)(object2 = (Number)object)).doubleValue())) {
            return ((Number)object2).doubleValue();
        }
        if (clazz.isEnum() && object instanceof String) {
            object2 = (String)object;
            for (Object object3 : clazz.getEnumConstants()) {
                if (!((Enum)object3).name().equals(object2)) continue;
                return object3;
            }
        }
        if (List.class.isAssignableFrom(clazz) && object instanceof List) {
            object2 = (List)object;
            ArrayList<String> arrayList = new ArrayList<String>();
            Iterator iterator = object2.iterator();
            while (iterator.hasNext()) {
                Object e = iterator.next();
                if (!(e instanceof String)) {
                    throw new IllegalArgumentException(string + " must contain strings");
                }
                arrayList.add((String)e);
            }
            return arrayList;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            if (object instanceof ConfigurationSection) {
                ConfigurationSection configurationSection = (ConfigurationSection)object;
                v0 = configurationSection.getValues(false);
            } else {
                Map map;
                v0 = object2 = object instanceof Map ? (map = (Map)object) : null;
            }
            if (object2 != null) {
                LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
                for (Map.Entry entry : object2.entrySet()) {
                    Object object3;
                    object3 = entry.getValue();
                    if (object3 == null || object3 instanceof Map || object3 instanceof Collection || object3 instanceof ConfigurationSection) {
                        throw new IllegalArgumentException(string + " must map strings to scalar values");
                    }
                    linkedHashMap.put(String.valueOf(entry.getKey()), String.valueOf(object3));
                }
                return linkedHashMap;
            }
        }
        throw new IllegalArgumentException("Invalid value for " + string + " (expected " + clazz.getSimpleName() + ")");
    }

    private static Object serialize(Object object) throws ReflectiveOperationException {
        if (object instanceof Enum) {
            Enum enum_ = (Enum)object;
            return enum_.name();
        }
        if (object == null || !YamlSettings.isGroup(object.getClass()) || object instanceof Number || object instanceof Boolean) {
            return object;
        }
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        for (Field field : object.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            linkedHashMap.put(YamlSettings.key(field.getName()), YamlSettings.serialize(field.get(object)));
        }
        return linkedHashMap;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void write(Path path, String string) throws IOException {
        Path path2 = path.toAbsolutePath();
        Path path3 = path2.getParent();
        Files.createDirectories(path3, new FileAttribute[0]);
        Path path4 = Files.createTempFile(path3, ".settings-", ".tmp", new FileAttribute[0]);
        try {
            Files.writeString(path4, (CharSequence)string, StandardCharsets.UTF_8, new OpenOption[0]);
            try {
                Files.move(path4, path2, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                Files.move(path4, path2, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(path4);
        }
    }

    private record Assignment(Object target, Field field, Object value) {
        void apply() throws IllegalAccessException {
            this.field.set(this.target, this.value);
        }
    }
}

