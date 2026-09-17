/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.partyhud.invite;

import java.time.Instant;
import java.util.UUID;

public class PartyInvite {
    private final UUID id = UUID.randomUUID();
    private final UUID partyId;
    private final UUID leader;
    private final UUID target;
    private final long expiresAtMs;

    public PartyInvite(UUID uUID, UUID uUID2, long l) {
        this(uUID, uUID2, l, null);
    }

    public PartyInvite(UUID uUID, UUID uUID2, long l, UUID uUID3) {
        this.partyId = uUID3;
        this.leader = uUID;
        this.target = uUID2;
        this.expiresAtMs = l;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getPartyId() {
        return this.partyId;
    }

    public UUID getLeader() {
        return this.leader;
    }

    public UUID getTarget() {
        return this.target;
    }

    public long getExpiresAtMs() {
        return this.expiresAtMs;
    }

    public boolean isExpired() {
        return Instant.now().toEpochMilli() > this.expiresAtMs;
    }
}

