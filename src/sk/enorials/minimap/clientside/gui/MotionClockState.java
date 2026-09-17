/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside.gui;

import java.util.Map;

public record MotionClockState(long age, Map<Object, Clock> clocks) {
    public boolean continues(MotionClockState motionClockState) {
        if (motionClockState == null || this.age < motionClockState.age || this.age - motionClockState.age > 200L || !this.clocks.keySet().equals(motionClockState.clocks.keySet())) {
            return false;
        }
        long l = this.age - motionClockState.age;
        for (Map.Entry<Object, Clock> entry : this.clocks.entrySet()) {
            Clock clock = entry.getValue();
            Clock clock2 = motionClockState.clocks.get(entry.getKey());
            if (Double.isFinite(clock.ticks) && Double.isFinite(clock.rate) && clock.rate == clock2.rate && !(Math.abs(clock.ticks - clock2.ticks - (double)l * clock2.rate) > 0.002)) continue;
            return false;
        }
        return true;
    }

    public record Clock(double ticks, double rate) {
    }
}

