/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.compat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

final class SpacingFont {
    private SpacingFont() {
    }

    private static byte[] table(int n, Consumer<ByteBuffer> consumer) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(n).order(ByteOrder.BIG_ENDIAN);
        consumer.accept(byteBuffer);
        return byteBuffer.array();
    }

    private static long checksum(byte[] byArray) {
        long l = 0L;
        for (int i = 0; i < byArray.length; i += 4) {
            long l2 = 0L;
            for (int j = 0; j < 4; ++j) {
                l2 = l2 << 8 | (long)(i + j < byArray.length ? byArray[i + j] & 0xFF : 0);
            }
            l = l + l2 & 0xFFFFFFFFL;
        }
        return l;
    }

    static byte[] build(SortedMap<Integer, Integer> sortedMap) throws IOException {
        int n;
        int n2 = sortedMap.size() + 1;
        if (n2 > 8190) {
            throw new IOException("Too many spacing glyphs");
        }
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        treeMap.put("head", SpacingFont.table(54, byteBuffer -> {
            byteBuffer.putInt(65536).putInt(65536).putInt(0).putInt(1594834165).putShort((short)0).putShort((short)16).putLong(0L).putLong(0L);
            byteBuffer.putShort((short)0).putShort((short)0).putShort((short)1).putShort((short)1).putShort((short)0).putShort((short)1).putShort((short)2).putShort((short)1).putShort((short)0);
        }));
        treeMap.put("hhea", SpacingFont.table(36, byteBuffer -> {
            byteBuffer.putInt(65536).putShort((short)4).putShort((short)0).putShort((short)0).putShort((short)((Integer)Collections.max(sortedMap.values())).intValue());
            byteBuffer.putShort((short)0).putShort((short)0).putShort((short)1).putShort((short)1).putShort((short)0).putShort((short)0);
            for (int i = 0; i < 5; ++i) {
                byteBuffer.putShort((short)0);
            }
            byteBuffer.putShort((short)n2);
        }));
        treeMap.put("maxp", SpacingFont.table(32, byteBuffer -> byteBuffer.putInt(65536).putShort((short)n2).putShort((short)3).putShort((short)1).putShort((short)0).putShort((short)0).putShort((short)2)));
        treeMap.put("hmtx", SpacingFont.table(n2 * 4, byteBuffer -> {
            byteBuffer.putInt(0);
            Iterator iterator = sortedMap.values().iterator();
            while (iterator.hasNext()) {
                int n = (Integer)iterator.next();
                byteBuffer.putShort((short)n).putShort((short)0);
            }
        }));
        byte[] byArray = SpacingFont.table(24, byteBuffer -> {
            byteBuffer.putShort((short)1).putShort((short)0).putShort((short)0).putShort((short)1).putShort((short)1).putShort((short)2).putShort((short)0);
            byteBuffer.put((byte)49).put((byte)55).put((byte)7).put((byte)1).put((byte)1).put((byte)1).put((byte)1);
        });
        treeMap.put("glyf", SpacingFont.table(n2 * 24, byteBuffer -> {
            for (int i = 0; i < n2; ++i) {
                byteBuffer.put(byArray);
            }
        }));
        treeMap.put("loca", SpacingFont.table((n2 + 1) * 4, byteBuffer -> {
            for (int i = 0; i <= n2; ++i) {
                byteBuffer.putInt(i * 24);
            }
        }));
        int n3 = n2;
        int n4 = Integer.highestOneBit(n3) * 2;
        treeMap.put("cmap", SpacingFont.table(28 + 8 * n3, byteBuffer -> {
            int n3;
            byteBuffer.putShort((short)0).putShort((short)1).putShort((short)3).putShort((short)1).putInt(12);
            byteBuffer.putShort((short)4).putShort((short)(16 + 8 * n3)).putShort((short)0).putShort((short)(n3 * 2)).putShort((short)n4).putShort((short)Integer.numberOfTrailingZeros(Integer.highestOneBit(n3))).putShort((short)(n3 * 2 - n4));
            Iterator iterator = sortedMap.keySet().iterator();
            while (iterator.hasNext()) {
                n3 = (Integer)iterator.next();
                byteBuffer.putShort((short)n3);
            }
            byteBuffer.putShort((short)-1).putShort((short)0);
            iterator = sortedMap.keySet().iterator();
            while (iterator.hasNext()) {
                n3 = (Integer)iterator.next();
                byteBuffer.putShort((short)n3);
            }
            byteBuffer.putShort((short)-1);
            int n4 = 1;
            Iterator iterator2 = sortedMap.keySet().iterator();
            while (iterator2.hasNext()) {
                int n5 = (Integer)iterator2.next();
                byteBuffer.putShort((short)(n4++ - n5));
            }
            byteBuffer.putShort((short)1);
            for (int i = 0; i < n3; ++i) {
                byteBuffer.putShort((short)0);
            }
        }));
        treeMap.put("name", SpacingFont.table(6, byteBuffer -> byteBuffer.putShort((short)0).putShort((short)0).putShort((short)6)));
        treeMap.put("post", SpacingFont.table(32, byteBuffer -> byteBuffer.putInt(196608)));
        int n5 = n = 12 + 16 * treeMap.size();
        for (byte[] byArray2 : treeMap.values()) {
            n5 += byArray2.length + 3 & 0xFFFFFFFC;
        }
        ByteBuffer byteBuffer2 = ByteBuffer.allocate(n5).order(ByteOrder.BIG_ENDIAN);
        int n6 = Integer.highestOneBit(treeMap.size()) * 16;
        byteBuffer2.putInt(65536).putShort((short)treeMap.size()).putShort((short)n6).putShort((short)Integer.numberOfTrailingZeros(Integer.highestOneBit(treeMap.size()))).putShort((short)(treeMap.size() * 16 - n6));
        int n7 = 0;
        for (Map.Entry entry : treeMap.entrySet()) {
            byte[] byArray3 = (byte[])entry.getValue();
            byteBuffer2.put(((String)entry.getKey()).getBytes(StandardCharsets.US_ASCII)).putInt((int)SpacingFont.checksum(byArray3)).putInt(n).putInt(byArray3.length);
            System.arraycopy(byArray3, 0, byteBuffer2.array(), n, byArray3.length);
            if (((String)entry.getKey()).equals("head")) {
                n7 = n;
            }
            n += byArray3.length + 3 & 0xFFFFFFFC;
        }
        byteBuffer2.putInt(n7 + 8, (int)(2981146554L - SpacingFont.checksum(byteBuffer2.array())));
        return byteBuffer2.array();
    }
}

