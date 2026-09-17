/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.trade;

public final class TradeAgreement {
    private long revision;
    private final boolean[] locked = new boolean[2];
    private final boolean[] confirmed = new boolean[2];
    private long reviewAfter = Long.MAX_VALUE;

    public long revision() {
        return this.revision;
    }

    public boolean locked(int n) {
        return this.locked[n];
    }

    public boolean confirmed(int n) {
        return this.confirmed[n];
    }

    public boolean reviewing() {
        return this.locked[0] && this.locked[1];
    }

    public void change() {
        ++this.revision;
        this.confirmed[1] = false;
        this.confirmed[0] = false;
        this.locked[1] = false;
        this.locked[0] = false;
        this.reviewAfter = Long.MAX_VALUE;
    }

    public boolean lock(int n, long l, long l2) {
        if (l != this.revision || this.locked[n]) {
            return false;
        }
        this.locked[n] = true;
        if (this.reviewing()) {
            this.reviewAfter = l2 + 2000L;
        }
        return true;
    }

    public boolean confirm(int n, long l, long l2) {
        if (l != this.revision || !this.reviewing() || l2 < this.reviewAfter) {
            return false;
        }
        this.confirmed[n] = true;
        return this.confirmed[0] && this.confirmed[1];
    }

    public boolean unlock(int n, long l) {
        if (l != this.revision || !this.locked[n]) {
            return false;
        }
        this.change();
        return true;
    }

    public long waitSeconds(long l) {
        return this.reviewing() ? Math.max(0L, (this.reviewAfter - l + 999L) / 1000L) : 0L;
    }
}

