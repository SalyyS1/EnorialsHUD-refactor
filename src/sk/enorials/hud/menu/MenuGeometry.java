/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.menu;

import java.util.ArrayList;
import java.util.List;

public final class MenuGeometry {
    public static final int CONTENT_OFFSET_Y = 2;

    private MenuGeometry() {
    }

    public static List<Card> cards(List<Kind> list) {
        ArrayList<Card> arrayList = new ArrayList<Card>();
        int n = (list.size() + 1) / 2;
        for (int i = 0; i < list.size(); i += 2) {
            int n2;
            int n3 = i / 2;
            int n4 = n2 = n == 3 ? 48 : 60;
            int n5 = n == 1 ? 78 : (n == 2 ? (n3 == 0 ? 46 : 122) : 30 + n3 * 54);
            boolean bl = i + 1 == list.size();
            for (int j = 0; j < (bl ? 1 : 2); ++j) {
                Kind kind = list.get(i + j);
                int n6 = n == 3 ? 48 : (kind == Kind.CATEGORY ? 60 : 50);
                arrayList.add(new Card(i + j, bl ? 55 : 18 + j * 74, n5 + (n2 - n6) / 2, 66, n6, kind));
            }
        }
        return List.copyOf(arrayList);
    }

    public static int navigation(int n) {
        int[] nArray = MenuGeometry.slotCenter(n, 6);
        if (nArray == null || nArray[1] < 198 || nArray[1] >= 214) {
            return -1;
        }
        if (nArray[0] >= 18 && nArray[0] < 58) {
            return 0;
        }
        if (nArray[0] >= 68 && nArray[0] < 88) {
            return 3;
        }
        if (nArray[0] >= 88 && nArray[0] < 108) {
            return 1;
        }
        if (nArray[0] >= 118 && nArray[0] < 158) {
            return 2;
        }
        return -1;
    }

    public static int[] slotCenter(int n, int n2) {
        int n3 = n2 * 9;
        if (n < 0 || n >= n3 + 36) {
            return null;
        }
        if (n < n3) {
            return new int[]{17 + n % 9 * 18, 27 + n / 9 * 18};
        }
        int n4 = n - n3;
        return new int[]{17 + n4 % 9 * 18, n2 * 18 + (n4 < 27 ? 41 + n4 / 9 * 18 : 99)};
    }

    public static int rootButton(int n) {
        int n2;
        int[] nArray = MenuGeometry.slotCenter(n, 6);
        if (nArray == null) {
            return -1;
        }
        if (nArray[1] < 20) {
            return -1;
        }
        if (nArray[0] >= 80 && nArray[0] < 98) {
            return -1;
        }
        int n3 = n2 = nArray[0] < 89 ? 0 : 1;
        return nArray[1] < 76 ? n2 : (nArray[1] < 132 ? 2 + n2 : -1);
    }

    public static Card rootExtra(int n, boolean bl, boolean bl2) {
        int n2 = bl ? 6 : 5;
        boolean bl3 = n < n2;
        boolean bl4 = bl3 ? bl : bl2;
        int n3 = bl3 ? n - 4 : n - n2;
        return new Card(n, -14 + (bl4 ? n3 * 106 : 0), bl3 ? 144 : 182, bl4 ? 98 : 204, 28, Kind.ACTION);
    }

    public static enum Kind {
        CATEGORY,
        ACTION,
        TOGGLE;

    }

    public record Card(int index, int x, int y, int width, int height, Kind kind) {
        public boolean contains(int n, int n2) {
            return n >= this.x && n < this.x + this.width && n2 >= this.y && n2 < this.y + this.height;
        }

        public int centerX() {
            return this.x + this.width / 2;
        }

        public int iconSize() {
            return this.height >= 58 ? 26 : (this.height <= 48 ? 18 : 22);
        }

        public int iconY() {
            return this.y + (this.height >= 58 ? 8 : (this.height == 50 ? 6 : 5)) + 2;
        }

        public int labelY() {
            return this.iconY() + this.iconSize() + (this.height >= 58 ? 3 : 2);
        }

        public int headSlot() {
            int n = -1;
            int n2 = Integer.MAX_VALUE;
            for (int i = 0; i < 81; ++i) {
                int[] nArray = MenuGeometry.slotCenter(i, 6);
                int n3 = Math.abs(nArray[0] - this.centerX()) + Math.abs(nArray[1] - (this.iconY() + this.iconSize() / 2));
                if (n3 >= n2) continue;
                n2 = n3;
                n = i;
            }
            return n;
        }

        public String headModel() {
            int[] nArray = MenuGeometry.slotCenter(this.headSlot(), 6);
            return "enorials:menu/head_" + (this.centerX() - nArray[0]) + "_" + (this.iconY() + this.iconSize() / 2 - nArray[1]) + "_" + this.iconSize();
        }
    }
}

