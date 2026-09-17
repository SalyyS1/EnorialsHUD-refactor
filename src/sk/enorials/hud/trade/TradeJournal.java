/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import sk.enorials.hud.trade.TradeMoney;

public final class TradeJournal {
    private final Path directory;
    private final Map<UUID, Set<UUID>> unresolved = new LinkedHashMap<UUID, Set<UUID>>();
    private boolean healthy = true;

    public TradeJournal(Path path2) throws IOException {
        this.directory = path2;
        Files.createDirectories(path2, new FileAttribute[0]);
        try (Stream<Path> stream = Files.list(path2);){
            for (Path path3 : stream.filter(path -> path.getFileName().toString().endsWith(".jsonl")).toList()) {
                try {
                    List<String> list = Files.readAllLines(path3, StandardCharsets.UTF_8);
                    JsonObject jsonObject = JsonParser.parseString((String)list.get(0)).getAsJsonObject();
                    UUID uUID = UUID.fromString(jsonObject.get("id").getAsString());
                    Set<UUID> set = Set.of(UUID.fromString(jsonObject.get("left").getAsString()), UUID.fromString(jsonObject.get("right").getAsString()));
                    String string = "";
                    for (int i = 1; i < list.size(); ++i) {
                        try {
                            string = JsonParser.parseString((String)list.get(i)).getAsJsonObject().get("stage").getAsString();
                            continue;
                        }
                        catch (RuntimeException runtimeException) {
                            string = "REVIEW";
                        }
                    }
                    if (Set.of("COMPLETE", "CANCELLED", "RESOLVED").contains(string)) continue;
                    this.unresolved.put(uUID, set);
                }
                catch (RuntimeException runtimeException) {
                    this.healthy = false;
                }
            }
        }
    }

    public boolean blocked(UUID uUID) {
        return !this.healthy || this.unresolved.values().stream().anyMatch(set -> set.contains(uUID));
    }

    public Set<UUID> pending() {
        return Set.copyOf(this.unresolved.keySet());
    }

    public boolean healthy() {
        return this.healthy;
    }

    public Entry prepare(UUID uUID, UUID uUID2, UUID uUID3, JsonObject jsonObject) throws IOException {
        if (this.blocked(uUID2) || this.blocked(uUID3)) {
            throw new IOException("Unresolved trade journal");
        }
        jsonObject.addProperty("id", uUID.toString());
        jsonObject.addProperty("left", uUID2.toString());
        jsonObject.addProperty("right", uUID3.toString());
        jsonObject.addProperty("created", (Number)System.currentTimeMillis());
        Path path = this.directory.resolve(String.valueOf(uUID) + ".jsonl");
        this.unresolved.put(uUID, Set.of(uUID2, uUID3));
        TradeJournal.append(path, jsonObject, true);
        Entry entry = new Entry(uUID, path);
        entry.stage("PREPARED");
        return entry;
    }

    public void resolve(UUID uUID) throws IOException {
        if (!this.unresolved.containsKey(uUID)) {
            throw new IllegalArgumentException("Unknown unresolved trade ID.");
        }
        new Entry(uUID, this.directory.resolve(String.valueOf(uUID) + ".jsonl")).stage("RESOLVED");
    }

    private static void append(Path path, JsonObject jsonObject, boolean bl) throws IOException {
        byte[] byArray = (jsonObject.toString() + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.WRITE, bl ? StandardOpenOption.CREATE_NEW : StandardOpenOption.APPEND);){
            ByteBuffer byteBuffer = ByteBuffer.wrap(byArray);
            while (byteBuffer.hasRemaining()) {
                fileChannel.write(byteBuffer);
            }
            fileChannel.force(true);
        }
    }

    public final class Entry
    implements TradeMoney.Journal {
        private final UUID id;
        private final Path file;

        private Entry(UUID uUID, Path path) {
            this.id = uUID;
            this.file = path;
        }

        @Override
        public void stage(String string) throws IOException {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", (Number)System.currentTimeMillis());
            jsonObject.addProperty("stage", string);
            TradeJournal.append(this.file, jsonObject, false);
            if (Set.of("COMPLETE", "CANCELLED", "RESOLVED").contains(string)) {
                TradeJournal.this.unresolved.remove(this.id);
            }
        }
    }
}

