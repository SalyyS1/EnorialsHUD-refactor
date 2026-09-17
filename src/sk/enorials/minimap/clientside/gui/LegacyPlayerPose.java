/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside.gui;

import sk.enorials.minimap.clientside.gui.GuiMotion;
import sk.enorials.minimap.clientside.gui.GuiPlayerPose;

final class LegacyPlayerPose
extends GuiPlayerPose {
    double exactX;
    double exactZ;
    double previousExactX;
    double previousExactZ;
    private double inputX;
    private double inputZ;
    private long lastInputMove = Long.MIN_VALUE;
    private double observedX;
    private double observedZ;
    private double observedDx;
    private double observedDz;
    private long observedTick = Long.MIN_VALUE;
    private double inputDx;
    private double inputDz;

    LegacyPlayerPose() {
    }

    void observe(double d, double d2, double d3, double d4, long l) {
        double d5 = d3 - d;
        double d6 = d4 - d2;
        if (!Double.isFinite(d5) || !Double.isFinite(d6) || Math.abs(d5) > 8.0 || Math.abs(d6) > 8.0) {
            return;
        }
        if (d5 == 0.0 && d6 == 0.0) {
            return;
        }
        this.observedX = d3;
        this.observedZ = d4;
        this.observedDx = d5;
        this.observedDz = d6;
        this.observedTick = l;
    }

    @Override
    boolean sample(double d, double d2, long l) {
        if (!Double.isFinite(d) || !Double.isFinite(d2)) {
            return false;
        }
        if (this.initialized && l == this.tick) {
            return false;
        }
        boolean bl = this.initialized && l == this.tick + 1L && Math.abs(d - this.inputX) <= 8.0 && Math.abs(d2 - this.inputZ) <= 8.0;
        boolean bl2 = d != this.inputX || d2 != this.inputZ;
        double d3 = d;
        double d4 = d2;
        if (bl) {
            double d5;
            d3 = this.inputX;
            d4 = this.inputZ;
            if (bl2 && this.lastInputMove == l - 2L) {
                d3 = (this.inputX + d) * 0.5;
                d4 = (this.inputZ + d2) * 0.5;
            }
            if ((d5 = Math.hypot(this.inputDx, this.inputDz)) > 1.0E-5 && this.observedTick != Long.MIN_VALUE && l - this.observedTick <= 2L && (bl2 || this.lastInputMove >= l - 1L)) {
                double d6 = this.exactX + this.inputDx;
                double d7 = this.exactZ + this.inputDz;
                double d8 = this.inputDx / d5;
                double d9 = this.inputDz / d5;
                double d10 = (d3 - d6) * d8 + (d4 - d7) * d9;
                double d11 = Math.copySign(Math.max(0.0, Math.abs(d10) - d5 * 1.25), d10) * 0.25;
                d3 += d8 * (d11 - d10);
                d4 += d9 * (d11 - d10);
            }
        }
        boolean bl3 = this.observedTick != Long.MIN_VALUE && l - this.observedTick <= 2L && Math.abs(d - this.observedX) < 1.0E-5 && Math.abs(d2 - this.observedZ) < 1.0E-5;
        this.inputDx = bl3 ? this.observedDx : 0.0;
        this.inputDz = bl3 ? this.observedDz : 0.0;
        this.inputX = d;
        this.inputZ = d2;
        if (!bl) {
            this.lastInputMove = Long.MIN_VALUE;
        }
        if (bl2) {
            this.lastInputMove = l;
        }
        long l2 = Math.round(d3 * 32.0);
        long l3 = Math.round(d4 * 32.0);
        long l4 = l2 - this.x;
        long l5 = l3 - this.z;
        boolean bl4 = bl && GuiMotion.continuous(l4, l5);
        int n = bl4 ? (int)l4 : 0;
        int n2 = bl4 ? (int)l5 : 0;
        boolean bl5 = !this.initialized || d3 != this.exactX || d4 != this.exactZ || this.exactX != this.previousExactX || this.exactZ != this.previousExactZ || l2 != this.x || l3 != this.z || n != this.dx || n2 != this.dz;
        this.previousExactX = bl4 ? this.exactX : d3;
        this.previousExactZ = bl4 ? this.exactZ : d4;
        this.exactX = d3;
        this.exactZ = d4;
        this.x = l2;
        this.z = l3;
        this.dx = n;
        this.dz = n2;
        this.tick = l;
        this.initialized = true;
        return bl5;
    }

    @Override
    double x() {
        return this.exactX;
    }

    @Override
    double z() {
        return this.exactZ;
    }

    @Override
    double previousX() {
        return this.previousExactX;
    }

    @Override
    double previousZ() {
        return this.previousExactZ;
    }

    @Override
    void reset() {
        this.initialized = false;
        this.dz = 0;
        this.dx = 0;
        this.inputDz = 0.0;
        this.inputDx = 0.0;
        this.observedTick = Long.MIN_VALUE;
    }
}

