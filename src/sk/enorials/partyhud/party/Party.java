/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.partyhud.party;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private final UUID id;
    private UUID leader;
    private final LinkedHashSet<UUID> members = new LinkedHashSet();
    private boolean open;
    private VoteKick voteKick;

    public Party(UUID uUID, boolean bl) {
        this.id = UUID.randomUUID();
        this.leader = uUID;
        this.open = bl;
        this.members.add(uUID);
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getLeader() {
        return this.leader;
    }

    public void setLeader(UUID uUID) {
        this.leader = uUID;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean bl) {
        this.open = bl;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(this.members);
    }

    public boolean isMember(UUID uUID) {
        return this.members.contains(uUID);
    }

    public boolean addMember(UUID uUID, int n) {
        return this.members.size() >= n ? false : this.members.add(uUID);
    }

    public boolean removeMember(UUID uUID) {
        boolean bl = this.members.remove(uUID);
        if (this.voteKick != null && (this.voteKick.target().equals(uUID) || this.voteKick.kickVoters().contains(uUID) || this.voteKick.stayVoters().contains(uUID))) {
            this.voteKick = null;
        }
        return bl;
    }

    public int size() {
        return this.members.size();
    }

    public VoteKick getVoteKick() {
        return this.voteKick;
    }

    public void setVoteKick(VoteKick voteKick) {
        this.voteKick = voteKick;
    }

    public record VoteKick(UUID target, Set<UUID> kickVoters, Set<UUID> stayVoters, long expiresAtMillis) {
    }
}

