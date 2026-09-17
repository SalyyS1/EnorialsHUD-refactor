/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

public final class NameSkinLookup
implements AutoCloseable {
    private static final int MAX_ENTRIES = 512;
    private static final long REQUEST_INTERVAL = 3000L;
    private static final long ERROR_BACKOFF = 60000L;
    private final Executor executor;
    private final Resolver resolver;
    private final LongSupplier clock;
    private final ScheduledExecutorService timer;
    private final BiConsumer<Runnable, Long> schedule;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap(16, 0.75f, true);
    private long nextRequest;
    private long backoffUntil;
    private boolean closed;

    public NameSkinLookup(Executor executor) {
        this(executor, NameSkinLookup::fetch, System::currentTimeMillis);
    }

    NameSkinLookup(Executor executor, Resolver resolver, LongSupplier longSupplier) {
        this(executor, resolver, longSupplier, null);
    }

    NameSkinLookup(Executor executor, Resolver resolver, LongSupplier longSupplier, BiConsumer<Runnable, Long> biConsumer) {
        this.executor = executor;
        this.resolver = resolver;
        this.clock = longSupplier;
        this.timer = biConsumer == null ? Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Enorials-name-lookup-queue");
            thread.setDaemon(true);
            return thread;
        }) : null;
        this.schedule = biConsumer == null ? (runnable, l) -> this.timer.schedule((Runnable)runnable, (long)l, TimeUnit.MILLISECONDS) : biConsumer;
    }

    public synchronized String skin(String string, boolean bl, int n, int n2) {
        Entry entry = this.request(string, bl, n, n2, false);
        return entry == null ? "" : entry.skin;
    }

    public synchronized CompletableFuture<String> skinAsync(String string, boolean bl, int n, int n2) {
        Entry entry = this.request(string, bl, n, n2, true);
        return entry == null ? CompletableFuture.completedFuture("") : entry.completion;
    }

    private Entry request(String string, boolean bl, int n, int n2, boolean bl2) {
        Object object;
        if (this.closed || string == null || !string.matches("[A-Za-z0-9_]{1,16}")) {
            return null;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        Entry entry2 = this.entries.get(string2);
        long l = this.clock.getAsLong();
        if (entry2 != null && (entry2.pending || !bl && l < entry2.expires)) {
            return entry2;
        }
        if (l < this.backoffUntil || !bl2 && l < this.nextRequest) {
            return entry2;
        }
        if (this.entries.values().stream().filter(entry -> entry.pending).count() >= 128L) {
            return entry2;
        }
        if (entry2 == null) {
            if (this.entries.size() >= 512) {
                object = this.entries.entrySet().iterator();
                while (object.hasNext()) {
                    if (((Entry)((Map.Entry)object.next()).getValue()).pending) continue;
                    object.remove();
                    break;
                }
                if (this.entries.size() >= 512) {
                    return null;
                }
            }
            entry2 = new Entry();
            this.entries.put(string2, entry2);
        }
        object = entry2;
        ((Entry)object).pending = true;
        ((Entry)object).completion = new CompletableFuture();
        long l2 = Math.max(0L, this.nextRequest - l);
        this.nextRequest = Math.max(l, this.nextRequest) + 3000L;
        long l3 = (long)Math.max(1, Math.min(10080, n)) * 60000L;
        long l4 = (long)Math.max(1, Math.min(1440, n2)) * 60000L;
        Runnable runnable = () -> this.lambda$request$1(string2, (Entry)object, string, l4, l3);
        Runnable runnable2 = () -> this.lambda$request$2(runnable, (Entry)object);
        try {
            if (l2 == 0L) {
                runnable2.run();
            } else {
                this.schedule.accept(runnable2, l2);
            }
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            this.unavailable((Entry)object);
        }
        return object;
    }

    private synchronized void unavailable(Entry entry) {
        entry.pending = false;
        entry.expires = this.clock.getAsLong() + 60000L;
        entry.completion.complete(entry.skin);
    }

    static String fetch(String string) throws IOException {
        JsonObject jsonObject = NameSkinLookup.getJson(URI.create("https://api.mojang.com/users/profiles/minecraft/" + string));
        if (jsonObject == null) {
            return "";
        }
        String string2 = NameSkinLookup.string(jsonObject, "id");
        if (!string2.matches("[a-fA-F0-9]{32}") || !string.equalsIgnoreCase(NameSkinLookup.string(jsonObject, "name"))) {
            throw new IOException("Unexpected account identity");
        }
        JsonObject jsonObject2 = NameSkinLookup.getJson(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + string2));
        if (jsonObject2 == null) {
            return "";
        }
        return NameSkinLookup.profileSkin(jsonObject2, string2, string);
    }

    static String profileSkin(JsonObject jsonObject, String string, String string2) throws IOException {
        try {
            if (!string.equalsIgnoreCase(NameSkinLookup.string(jsonObject, "id")) || !string2.equalsIgnoreCase(NameSkinLookup.string(jsonObject, "name"))) {
                throw new IOException("Unexpected skin profile identity");
            }
            JsonArray jsonArray = jsonObject.getAsJsonArray("properties");
            if (jsonArray == null) {
                return "";
            }
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                if (!"textures".equals(NameSkinLookup.string(jsonObject2, "name"))) continue;
                String string3 = NameSkinLookup.string(jsonObject2, "value");
                if (string3.length() > 65536) {
                    throw new IOException("Texture metadata exceeds limit");
                }
                JsonObject jsonObject3 = JsonParser.parseString((String)new String(Base64.getDecoder().decode(string3), StandardCharsets.UTF_8)).getAsJsonObject();
                if (!string.equalsIgnoreCase(NameSkinLookup.string(jsonObject3, "profileId")) || !string2.equalsIgnoreCase(NameSkinLookup.string(jsonObject3, "profileName"))) {
                    throw new IOException("Unexpected texture identity");
                }
                JsonObject jsonObject4 = jsonObject3.getAsJsonObject("textures");
                if (jsonObject4 == null || !jsonObject4.has("SKIN")) {
                    return "";
                }
                String string4 = NameSkinLookup.string(jsonObject4.getAsJsonObject("SKIN"), "url");
                NameSkinLookup.validateSkin(string4);
                return string4;
            }
            return "";
        }
        catch (JsonParseException | ClassCastException | IllegalArgumentException | IllegalStateException throwable) {
            throw new IOException("Invalid texture metadata", throwable);
        }
    }

    private static String string(JsonObject jsonObject, String string) throws IOException {
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isString()) {
            throw new IOException("Missing profile field: " + string);
        }
        return jsonElement.getAsString();
    }

    static void validateSkin(String string) throws IOException {
        try {
            URI uRI = URI.create(string);
            if (!(List.of("http", "https").contains(uRI.getScheme()) && "textures.minecraft.net".equalsIgnoreCase(uRI.getHost()) && uRI.getPath() != null && uRI.getPath().matches("/texture/[a-zA-Z0-9]+") && uRI.getRawUserInfo() == null && uRI.getRawQuery() == null && uRI.getRawFragment() == null && uRI.getPort() == -1)) {
                throw new IOException("Unsupported skin origin");
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new IOException("Invalid skin URL", illegalArgumentException);
        }
    }

    private static JsonObject getJson(URI uRI) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection)uRI.toURL().openConnection();
        httpURLConnection.setInstanceFollowRedirects(false);
        httpURLConnection.setConnectTimeout(4000);
        httpURLConnection.setReadTimeout(5000);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("User-Agent", "EnorialsHUD-SkinLookup");
        try {
            byte[] byArray;
            int n = httpURLConnection.getResponseCode();
            if (n == 204 || n == 404) {
                JsonObject jsonObject = null;
                return jsonObject;
            }
            if (n != 200) {
                throw new IOException("Profile HTTP status " + n);
            }
            try (InputStream inputStream = httpURLConnection.getInputStream();){
                byArray = inputStream.readNBytes(131073);
            }
            if (byArray.length > 131072) {
                throw new IOException("Profile exceeds limit");
            }
            try {
                inputStream = JsonParser.parseString((String)new String(byArray, StandardCharsets.UTF_8)).getAsJsonObject();
                return inputStream;
            }
            catch (JsonParseException | IllegalStateException throwable) {
                throw new IOException("Invalid profile JSON", throwable);
            }
        }
        finally {
            httpURLConnection.disconnect();
        }
    }

    @Override
    public synchronized void close() {
        this.closed = true;
        if (this.timer != null) {
            this.timer.shutdownNow();
        }
        for (Entry entry : this.entries.values()) {
            entry.completion.complete("");
        }
        this.entries.clear();
    }

    private /* synthetic */ void lambda$request$2(Runnable runnable, Entry entry) {
        try {
            this.executor.execute(runnable);
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            this.unavailable(entry);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private /* synthetic */ void lambda$request$1(String string, Entry entry, String string2, long l, long l2) {
        Object object = this;
        synchronized (object) {
            if (this.closed || this.entries.get(string) != entry || this.clock.getAsLong() < this.backoffUntil) {
                entry.pending = false;
                entry.completion.complete(entry.skin);
                return;
            }
        }
        object = "";
        boolean bl = false;
        try {
            object = this.resolver.resolve(string2);
            if (!((String)object).isEmpty()) {
                NameSkinLookup.validateSkin((String)object);
            }
        }
        catch (Exception exception) {
            bl = true;
        }
        NameSkinLookup nameSkinLookup = this;
        synchronized (nameSkinLookup) {
            if (this.closed || this.entries.get(string) != entry) {
                entry.completion.complete("");
                return;
            }
            long l3 = this.clock.getAsLong();
            if (!bl) {
                entry.skin = object;
            }
            entry.expires = l3 + (bl || ((String)object).isEmpty() ? l : l2);
            entry.pending = false;
            if (bl) {
                this.backoffUntil = l3 + 60000L;
                this.nextRequest = Math.max(this.nextRequest, this.backoffUntil);
            }
            entry.completion.complete(entry.skin);
        }
    }

    @FunctionalInterface
    static interface Resolver {
        public String resolve(String var1) throws IOException;
    }

    private static final class Entry {
        String skin = "";
        long expires;
        boolean pending;
        CompletableFuture<String> completion = CompletableFuture.completedFuture("");

        private Entry() {
        }
    }
}

