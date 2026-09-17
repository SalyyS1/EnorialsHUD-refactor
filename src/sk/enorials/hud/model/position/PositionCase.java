/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.model.position;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import sk.enorials.hud.model.LayerScale;
import sk.enorials.hud.model.position.PositionContainer;
import sk.enorials.hud.model.position.ScreenPosition;
import sk.enorials.hud.model.position.ShaderInfo;

public class PositionCase {
    private final ScreenPosition position;
    private final boolean outlined;
    private final LayerScale scale;
    private final List<String> lines = new LinkedList<String>();

    public PositionCase(ShaderInfo shaderInfo) {
        this.position = shaderInfo.getPosition();
        this.outlined = shaderInfo.isOutlined();
        this.scale = shaderInfo.getScale();
        int n = this.position.getOffset().getX();
        int n2 = this.position.getOffset().getY();
        if (this.scale != LayerScale.UNSCALED) {
            double d = this.scale.getX();
            double d2 = this.scale.getY();
            if (d != 1.0) {
                this.lines.add(PositionContainer.getTabs(4) + "scale.x = " + d + ";");
            }
            if (d2 != 1.0) {
                this.lines.add(PositionContainer.getTabs(4) + "scale.y = " + d2 + ";");
            }
        }
        this.lines.add(this.getPositionLine('x', this.position.getRelativeX() * -1.0f, n * -1));
        this.lines.add(this.getPositionLine('y', 100.0f - this.position.getRelativeY(), n2 * -1));
        this.lines.add(PositionContainer.getTabs(4) + "layer = " + this.position.getOffset().getZ() + ";");
        if (shaderInfo.isNativeCenter()) {
            this.lines.add(PositionContainer.getTabs(4) + "pos.x += fract(guiSize.x * 0.5) - (guiScale == 3 ? 1.45 : 0.0);");
        }
        if (this.outlined) {
            this.lines.add(PositionContainer.getTabs(4) + "outlined = true;");
        }
    }

    public ScreenPosition getPosition() {
        return this.position;
    }

    public List<String> getLines() {
        return this.lines;
    }

    private String getPositionLine(char c, double d, int n) {
        StringBuilder stringBuilder = new StringBuilder(PositionContainer.getTabs(4)).append(String.format("%cOffset = int(guiSize.%<c * (%.1f/100))", Character.valueOf(c), d).replace(',', '.'));
        if (n != 0) {
            if (n > 0) {
                stringBuilder.append('+');
            }
            stringBuilder.append(n);
        }
        stringBuilder.append(';');
        return stringBuilder.toString();
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && this.getClass() == object.getClass()) {
            PositionCase positionCase = (PositionCase)object;
            return this.outlined == positionCase.outlined && Objects.equals(this.position, positionCase.position) && Objects.equals(this.scale, positionCase.scale) && Objects.equals(this.lines, positionCase.lines);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.position, this.outlined, this.scale, this.lines);
    }
}

