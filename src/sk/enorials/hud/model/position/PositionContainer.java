/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.model.position;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import sk.enorials.hud.model.position.PositionCase;
import sk.enorials.hud.model.position.ShaderInfo;

public class PositionContainer {
    private static final String CASE_FORMAT = "case %d:";
    private static final Function<Integer, Integer> ID_TO_ASCENT = n -> (n * 1000 + 500) * -1;
    private final List<PositionCase> positions = new LinkedList<PositionCase>();
    private static final int[][] EXTENDED_RANGES = new int[][]{{1155, 1240}, {1405, 1490}, {1520, 1590}, {1610, 1690}, {1855, 1990}, {2155, 2240}, {2405, 2490}, {2655, 2740}, {2905, 2990}, {3155, 3240}, {3405, 3490}, {3655, 3740}, {3770, 3990}};

    public PositionCase add(ShaderInfo shaderInfo) {
        PositionCase positionCase = new PositionCase(shaderInfo);
        if (!this.positions.contains(positionCase)) {
            this.positions.add(positionCase);
        }
        return positionCase;
    }

    private int getId(PositionCase positionCase) {
        return PositionContainer.shaderId(this.positions.indexOf(positionCase));
    }

    static int shaderId(int n) {
        if (n < 0 || n >= 1400) {
            throw new IllegalStateException("HUD layout exceeds the supported shader position count");
        }
        if (n < 400) {
            return n + 100;
        }
        int n2 = n - 400;
        for (int[] nArray : EXTENDED_RANGES) {
            int n3 = nArray[1] - nArray[0];
            if (n2 < n3) {
                return nArray[0] + n2;
            }
            n2 -= n3;
        }
        throw new IllegalStateException("HUD layout exceeds the reserved shader position count");
    }

    public boolean usesExtendedIds() {
        return this.positions.size() > 400;
    }

    private int getAscent(PositionCase positionCase) {
        return ID_TO_ASCENT.apply(this.getId(positionCase));
    }

    public int addThenGetAscent(ShaderInfo shaderInfo) {
        return this.getAscent(this.add(shaderInfo));
    }

    public List<String> build() {
        LinkedList<String> linkedList = new LinkedList<String>();
        for (PositionCase positionCase : this.positions) {
            linkedList.add(PositionContainer.getTabs(3) + String.format(CASE_FORMAT, this.getId(positionCase)));
            linkedList.addAll(positionCase.getLines());
            linkedList.add(PositionContainer.getTabs(4) + "break;");
        }
        return linkedList;
    }

    public static String getTabs(int n) {
        return "    ".repeat(Math.max(0, n));
    }
}

