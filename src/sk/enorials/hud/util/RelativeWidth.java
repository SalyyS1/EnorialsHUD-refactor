/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

public class RelativeWidth {
    private final int left;
    private final int right;

    public RelativeWidth(int n, int n2) {
        this.left = n;
        this.right = n2;
    }

    public int getWidth() {
        return Math.max(0, this.left + this.right);
    }

    public int getLeft() {
        return this.left;
    }

    public int getRight() {
        return this.right;
    }

    public RelativeWidth merge(RelativeWidth relativeWidth) {
        if (relativeWidth == null) {
            return this;
        }
        int n = relativeWidth.left > this.left ? relativeWidth.left : this.left;
        int n2 = relativeWidth.right > this.right ? relativeWidth.right : this.right;
        return new RelativeWidth(n, n2);
    }
}

