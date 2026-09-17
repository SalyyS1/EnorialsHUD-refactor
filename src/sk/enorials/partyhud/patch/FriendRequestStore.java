/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.partyhud.patch;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FriendRequestStore {
    private final Map<UUID, Set<UUID>> incoming = new ConcurrentHashMap<UUID, Set<UUID>>();

    public void send(UUID uUID2, UUID uUID3) {
        this.incoming.computeIfAbsent(uUID3, uUID -> ConcurrentHashMap.newKeySet()).add(uUID2);
    }

    public boolean has(UUID uUID) {
        Set<UUID> set = this.incoming.get(uUID);
        return set != null && !set.isEmpty();
    }

    public Set<UUID> getIncoming(UUID uUID) {
        Set<UUID> set = this.incoming.get(uUID);
        return set == null ? Collections.emptySet() : new HashSet<UUID>(set);
    }

    public boolean remove(UUID uUID, UUID uUID2) {
        Set<UUID> set = this.incoming.get(uUID);
        if (set == null) {
            return false;
        }
        boolean bl = set.remove(uUID2);
        if (set.isEmpty()) {
            this.incoming.remove(uUID);
        }
        return bl;
    }

    public void clear(UUID uUID) {
        this.incoming.remove(uUID);
    }
}

