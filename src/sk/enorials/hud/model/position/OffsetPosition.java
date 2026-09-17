/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.model.position;

import java.util.Objects;

public class OffsetPosition {
    public static final OffsetPosition EMPTY = new OffsetPosition(0, 0, 0.0);
    private final int x;
    private final int y;
    private final double z;

    public OffsetPosition(int n, int n2, double d) {
        this.x = n;
        this.y = n2;
        this.z = d;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public OffsetPosition merge(OffsetPosition offsetPosition) {
        return new OffsetPosition(this.x + offsetPosition.x, this.y + offsetPosition.y, offsetPosition.z);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && this.getClass() == object.getClass()) {
            OffsetPosition offsetPosition = (OffsetPosition)object;
            return this.x == offsetPosition.x && this.y == offsetPosition.y && Double.compare(offsetPosition.z, this.z) == 0;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }
}

