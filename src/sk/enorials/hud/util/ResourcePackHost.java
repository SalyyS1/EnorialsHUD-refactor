/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.util.HostingOrigin;
import sk.enorials.hud.util.StaticPackHosting;

public final class ResourcePackHost {
    private static HttpServer server;
    private static ThreadPoolExecutor executor;

    private ResourcePackHost() {
    }

    public static boolean enabled(EnorialsHUD enorialsHUD) {
        return (enorialsHUD.singlePack() == null || !enorialsHUD.singlePack().single()) && !StaticPackHosting.enabled(enorialsHUD) && enorialsHUD.cfg().getBoolean("resource-pack.local.enabled", false) && enorialsHUD.cfg().getBoolean("resource-pack.local.host.enabled", false);
    }

    public static Path directory(EnorialsHUD enorialsHUD) {
        Path path = enorialsHUD.getDataFolder().toPath().toAbsolutePath();
        if (StaticPackHosting.enabled(enorialsHUD)) {
            return path.resolve("hosting-export/generated");
        }
        return ResourcePackHost.enabled(enorialsHUD) ? path.resolve("pack-host/generated") : path.getParent().getParent().resolve("resource-pack/generated");
    }

    public static synchronized void start(EnorialsHUD enorialsHUD) throws IOException {
        if (!ResourcePackHost.enabled(enorialsHUD) || server != null) {
            return;
        }
        HostingOrigin.parse(enorialsHUD.cfg().getString("resource-pack.local.base-url", ""));
        int n = enorialsHUD.cfg().getInt("resource-pack.local.host.port", 25599);
        if (n < 1 || n > 65535) {
            throw new IOException("Invalid resource-pack.local.host.port");
        }
        ResourcePackHost.start(ResourcePackHost.directory(enorialsHUD), new InetSocketAddress(enorialsHUD.cfg().getString("resource-pack.local.host.bind-host", "127.0.0.1"), n));
        enorialsHUD.getLogger().info("Resource pack HTTP host listening on port " + n);
    }

    static synchronized int start(Path path, InetSocketAddress inetSocketAddress) throws IOException {
        if (server != null) {
            throw new IOException("Pack host is already running");
        }
        Files.createDirectories(path, new FileAttribute[0]);
        Path path2 = path.toRealPath(new LinkOption[0]);
        HttpServer httpServer = HttpServer.create(inetSocketAddress, 32);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(64), runnable -> {
            Thread thread = new Thread(runnable, "Enorials-Pack-HTTP");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
        httpServer.setExecutor(threadPoolExecutor);
        httpServer.createContext("/", httpExchange -> ResourcePackHost.serve(httpExchange, path2));
        try {
            httpServer.start();
            server = httpServer;
            executor = threadPoolExecutor;
            return httpServer.getAddress().getPort();
        }
        catch (RuntimeException runtimeException) {
            httpServer.stop(0);
            threadPoolExecutor.shutdownNow();
            throw runtimeException;
        }
    }

    private static void serve(HttpExchange httpExchange, Path path) throws IOException {
        try (HttpExchange httpExchange2 = httpExchange;){
            String string = httpExchange.getRequestMethod();
            if (!string.equals("GET") && !string.equals("HEAD")) {
                httpExchange.getResponseHeaders().set("Allow", "GET, HEAD");
                httpExchange.sendResponseHeaders(405, -1L);
                return;
            }
            URI uRI = httpExchange.getRequestURI();
            if (uRI.getRawQuery() != null || !uRI.getRawPath().matches("/generated/[0-9a-f]{40}\\.zip")) {
                httpExchange.sendResponseHeaders(404, -1L);
                return;
            }
            Path path2 = path.resolve(uRI.getRawPath().substring(11));
            if (!Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS)) {
                httpExchange.sendResponseHeaders(404, -1L);
                return;
            }
            try (SeekableByteChannel seekableByteChannel = Files.newByteChannel(path2, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);){
                long l = seekableByteChannel.size();
                Headers headers = httpExchange.getResponseHeaders();
                headers.set("Content-Type", "application/zip");
                headers.set("Content-Length", Long.toString(l));
                headers.set("Cache-Control", "public, max-age=31536000, immutable");
                headers.set("X-Content-Type-Options", "nosniff");
                headers.set("ETag", "\"" + String.valueOf(path2.getFileName()) + "\"");
                httpExchange.sendResponseHeaders(200, string.equals("HEAD") ? -1L : l);
                if (string.equals("GET")) {
                    Channels.newInputStream(seekableByteChannel).transferTo(httpExchange.getResponseBody());
                }
            }
            catch (NoSuchFileException noSuchFileException) {
                // empty catch block
            }
        }
    }

    public static synchronized void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}

