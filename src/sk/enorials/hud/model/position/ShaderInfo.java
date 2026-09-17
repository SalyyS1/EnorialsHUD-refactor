/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.model.position;

import sk.enorials.hud.model.HudLayer;
import sk.enorials.hud.model.LayerScale;
import sk.enorials.hud.model.position.ScreenPosition;

public class ShaderInfo {
    private final ScreenPosition position;
    private final boolean outlined;
    private final LayerScale scale;
    private final boolean nativeCenter;

    private ShaderInfo(Builder builder) {
        this.position = builder.position;
        this.outlined = builder.outlined;
        this.scale = builder.scale;
        this.nativeCenter = builder.nativeCenter;
    }

    public ScreenPosition getPosition() {
        return this.position;
    }

    public boolean isOutlined() {
        return this.outlined;
    }

    public boolean isNativeCenter() {
        return this.nativeCenter;
    }

    public LayerScale getScale() {
        return this.scale;
    }

    public static Builder position(ScreenPosition screenPosition) {
        return new Builder(screenPosition);
    }

    public static class Builder {
        private final ScreenPosition position;
        private boolean outlined = false;
        private boolean nativeCenter = false;
        private LayerScale scale = LayerScale.UNSCALED;

        private Builder(ScreenPosition screenPosition) {
            this.position = screenPosition;
        }

        public Builder layer(HudLayer hudLayer) {
            this.outlined = hudLayer.isOutlined();
            this.scale = hudLayer.getScale();
            return this;
        }

        public ShaderInfo build() {
            return new ShaderInfo(this);
        }

        public Builder nativeCenter() {
            this.nativeCenter = true;
            return this;
        }
    }
}

