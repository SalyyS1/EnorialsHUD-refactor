/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside.gui;

import sk.enorials.minimap.clientside.gui.GuiMotion;

class GuiPlayerPose {
    long x;
    long z;
    long tick;
    int dx;
    int dz;
    boolean initialized;

    GuiPlayerPose() {
    }

    boolean sample(double d, double d2, long l) {
        if (!Double.isFinite(d) || !Double.isFinite(d2)) {
            return false;
        }
        long l2 = Math.round(d * 8.0) * 4L;
        long l3 = Math.round(d2 * 8.0) * 4L;
        long l4 = l2 - this.x;
        long l5 = l3 - this.z;
        boolean bl = this.initialized && l == this.tick + 1L && GuiMotion.continuous(l4, l5);
        int n = bl ? (int)l4 : 0;
        int n2 = bl ? (int)l5 : 0;
        boolean bl2 = !this.initialized || l2 != this.x || l3 != this.z || n != this.dx || n2 != this.dz;
        this.x = l2;
        this.z = l3;
        this.dx = n;
        this.dz = n2;
        this.tick = l;
        this.initialized = true;
        return bl2;
    }

    double x() {
        return (double)this.x / 32.0;
    }

    double z() {
        return (double)this.z / 32.0;
    }

    double previousX() {
        return (double)(this.x - (long)this.dx) / 32.0;
    }

    double previousZ() {
        return (double)(this.z - (long)this.dz) / 32.0;
    }

    void reset() {
        this.initialized = false;
        this.dz = 0;
        this.dx = 0;
    }
}

