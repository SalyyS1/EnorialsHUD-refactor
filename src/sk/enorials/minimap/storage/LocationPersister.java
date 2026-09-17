/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 */
package sk.enorials.minimap.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationPersister
extends StringType {
    private static final LocationPersister INSTANCE = new LocationPersister();
    private static final Gson JSON = new GsonBuilder().serializeNulls().create();

    protected LocationPersister() {
        super(SqlType.STRING, new Class[]{Location.class});
    }

    public static LocationPersister getSingleton() {
        return INSTANCE;
    }

    public static UUID worldIdentity(Location location) {
        if (location instanceof StoredLocation) {
            StoredLocation storedLocation = (StoredLocation)location;
            return storedLocation.identity;
        }
        return location.getWorld() == null ? null : location.getWorld().getUID();
    }

    public static Location preserveWorldIdentity(Location location) {
        return location instanceof StoredLocation ? location.clone() : LocationPersister.atWorld(LocationPersister.worldIdentity(location), location.getX(), location.getY(), location.getZ());
    }

    public static Location atWorld(UUID uUID, double d, double d2, double d3) {
        return new StoredLocation(uUID, d, d2, d3);
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object object) {
        if (object == null) {
            return null;
        }
        Location location = (Location)object;
        return JSON.toJson((Object)new Coordinates(LocationPersister.worldIdentity(location), location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object object, int n) {
        if (object == null) {
            return null;
        }
        JsonObject jsonObject = JsonParser.parseString((String)((String)object)).getAsJsonObject();
        UUID uUID = jsonObject.has("world") && !jsonObject.get("world").isJsonNull() ? UUID.fromString(jsonObject.get("world").getAsString()) : null;
        return LocationPersister.atWorld(uUID, jsonObject.get("x").getAsDouble(), jsonObject.get("y").getAsDouble(), jsonObject.get("z").getAsDouble());
    }

    private static final class StoredLocation
    extends Location {
        private UUID identity;

        StoredLocation(UUID uUID, double d, double d2, double d3) {
            super(uUID == null ? null : Bukkit.getWorld((UUID)uUID), d, d2, d3);
            this.identity = uUID;
        }

        public World getWorld() {
            return this.identity == null ? null : Bukkit.getWorld((UUID)this.identity);
        }

        public void setWorld(World world) {
            super.setWorld(world);
            if (world != null) {
                this.identity = world.getUID();
            }
        }
    }

    private record Coordinates(UUID world, double x, double y, double z) {
    }
}

