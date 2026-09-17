/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

public final class CopyUtil {
    private CopyUtil() {
    }

    public static void copyDirectory(final Path path, final Path path2) throws IOException {
        Files.walkFileTree(path, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult preVisitDirectory(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                Path path22 = path2.resolve(path.relativize(path3));
                Files.createDirectories(path22, new FileAttribute[0]);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                Path path22 = path2.resolve(path.relativize(path3));
                Files.copy(path3, path22, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path, new LinkOption[0])) {
            Files.walkFileTree(path, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.deleteIfExists(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException iOException) throws IOException {
                    Files.deleteIfExists(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}

