/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.model.position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import sk.enorials.hud.model.position.OffsetPosition;

public class ScreenPosition {
    private final float relativeX;
    private final float relativeY;
    private final OffsetPosition offset;

    public ScreenPosition(float f, float f2, OffsetPosition offsetPosition) {
        this.relativeX = new BigDecimal(f).setScale(1, RoundingMode.HALF_UP).floatValue();
        this.relativeY = new BigDecimal(f2).setScale(1, RoundingMode.HALF_UP).floatValue();
        this.offset = offsetPosition != null ? offsetPosition : OffsetPosition.EMPTY;
    }

    public ScreenPosition mergeOffset(OffsetPosition ... offsetPositionArray) {
        OffsetPosition offsetPosition = this.offset;
        for (OffsetPosition offsetPosition2 : offsetPositionArray) {
            if (offsetPosition2 == null || offsetPosition2 == OffsetPosition.EMPTY) continue;
            offsetPosition = offsetPosition.merge(offsetPosition2);
        }
        return new ScreenPosition(this.relativeX, this.relativeY, offsetPosition);
    }

    public OffsetPosition getOffset() {
        return this.offset;
    }

    public float getRelativeX() {
        return this.relativeX;
    }

    public float getRelativeY() {
        return this.relativeY;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && this.getClass() == object.getClass()) {
            ScreenPosition screenPosition = (ScreenPosition)object;
            return Float.compare(screenPosition.relativeX, this.relativeX) == 0 && Float.compare(screenPosition.relativeY, this.relativeY) == 0 && Objects.equals(this.offset, screenPosition.offset);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Float.valueOf(this.relativeX), Float.valueOf(this.relativeY), this.offset);
    }
}

