/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipUtil {
    private ZipUtil() {
    }

    public static void zipDirectory(File file, File file2) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file2), 131072));){
            ZipUtil.zip(file, file, zipOutputStream);
        }
    }

    private static void zip(File file, File file2, ZipOutputStream zipOutputStream) throws IOException {
        if (!file2.isDirectory()) {
            String string = file.toPath().relativize(file2.toPath()).toString().replace('\\', '/');
            ZipEntry zipEntry = new ZipEntry(string);
            zipEntry.setTime(0L);
            zipOutputStream.putNextEntry(zipEntry);
            try (FileInputStream fileInputStream = new FileInputStream(file2);){
                fileInputStream.transferTo(zipOutputStream);
            }
            zipOutputStream.closeEntry();
        } else {
            File[] fileArray = file2.listFiles();
            if (fileArray != null) {
                for (File file3 : fileArray) {
                    ZipUtil.zip(file, file3, zipOutputStream);
                }
            }
        }
    }
}

