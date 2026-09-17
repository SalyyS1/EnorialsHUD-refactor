/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import sk.enorials.hud.EnorialsHUD;

public final class GitHubUploader {
    private GitHubUploader() {
    }

    public static boolean uploadIfEnabled(EnorialsHUD enorialsHUD, File file) {
        return GitHubUploader.upload(enorialsHUD, file, null).success();
    }

    public static UploadResult upload(EnorialsHUD enorialsHUD, File file, String string) {
        if (file != null && file.exists()) {
            String string2;
            if (!enorialsHUD.cfg().getBoolean("upload.github.enabled", false)) {
                return new UploadResult(true, null, null, null);
            }
            String string3 = GitHubUploader.cfg(enorialsHUD, "upload.github.owner", "");
            String string4 = GitHubUploader.cfg(enorialsHUD, "upload.github.repo", "");
            String string5 = GitHubUploader.cfg(enorialsHUD, "upload.github.branch", "main");
            String string6 = string != null && !string.isBlank() ? string.trim() : GitHubUploader.cfg(enorialsHUD, "upload.github.path", "rp/EnorialsCoreHUD-pack-latest.zip");
            String string7 = GitHubUploader.cfg(enorialsHUD, "upload.github.token", "");
            if (string7.isBlank() && (string2 = System.getenv("GITHUB_TOKEN")) != null) {
                string7 = string2.trim();
            }
            if (!(string3.isBlank() || string4.isBlank() || string6.isBlank() || string7.isBlank())) {
                int n = Math.max(5000, enorialsHUD.cfg().getInt("upload.github.timeout-ms", 30000));
                String string8 = GitHubUploader.cfg(enorialsHUD, "upload.github.commit-message", "Update RP {version} @ {timestamp}");
                string8 = string8.replace("{version}", enorialsHUD.getDescription().getVersion());
                string8 = string8.replace("{timestamp}", Instant.now().toString());
                try {
                    HttpRequest httpRequest;
                    HttpResponse<String> httpResponse;
                    int n2;
                    String string9 = GitHubUploader.encodePath(string6);
                    String string10 = "https://api.github.com/repos/" + GitHubUploader.url(string3) + "/" + GitHubUploader.url(string4) + "/contents/" + string9;
                    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(n)).build();
                    String string11 = GitHubUploader.fetchCurrentSha(httpClient, string10, string5, string7, n);
                    byte[] byArray = Files.readAllBytes(file.toPath());
                    String string12 = Base64.getEncoder().encodeToString(byArray);
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("message", string8);
                    jsonObject.addProperty("content", string12);
                    jsonObject.addProperty("branch", string5);
                    if (string11 != null && !string11.isBlank()) {
                        jsonObject.addProperty("sha", string11);
                    }
                    if ((n2 = (httpResponse = httpClient.send(httpRequest = HttpRequest.newBuilder().uri(URI.create(string10)).timeout(Duration.ofMillis(n)).header("Authorization", "Bearer " + string7).header("Accept", "application/vnd.github+json").header("Content-Type", "application/json").header("User-Agent", "EnorialsHUD-RP-Uploader").PUT(HttpRequest.BodyPublishers.ofString(jsonObject.toString())).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))).statusCode()) >= 200 && n2 < 300) {
                        String string13 = GitHubUploader.extractContentSha(httpResponse.body());
                        String string14 = GitHubUploader.extractCommitSha(httpResponse.body());
                        String string15 = GitHubUploader.buildRawUrl(string3, string4, string5, string6, string13, string14);
                        return new UploadResult(true, string15, string13, string14);
                    }
                    enorialsHUD.getLogger().warning("GitHub upload failed with HTTP " + n2 + ": " + GitHubUploader.trim(httpResponse.body()));
                    return new UploadResult(false, null, null, null);
                }
                catch (Exception exception) {
                    enorialsHUD.getLogger().warning("GitHub upload failed: " + exception.getMessage());
                    return new UploadResult(false, null, null, null);
                }
            }
            enorialsHUD.getLogger().warning("GitHub upload enabled but configuration is incomplete.");
            return new UploadResult(false, null, null, null);
        }
        return new UploadResult(false, null, null, null);
    }

    public static int cleanupOldPlayerPacks(EnorialsHUD enorialsHUD, String string, int n, int n2) {
        if (!enorialsHUD.cfg().getBoolean("upload.github.enabled", false)) {
            return 0;
        }
        if (string != null && !string.isBlank()) {
            String string2;
            String string3 = GitHubUploader.cfg(enorialsHUD, "upload.github.owner", "");
            String string4 = GitHubUploader.cfg(enorialsHUD, "upload.github.repo", "");
            String string5 = GitHubUploader.cfg(enorialsHUD, "upload.github.branch", "main");
            String string6 = GitHubUploader.cfg(enorialsHUD, "upload.github.token", "");
            if (string6.isBlank() && (string2 = System.getenv("GITHUB_TOKEN")) != null) {
                string6 = string2.trim();
            }
            if (!(string3.isBlank() || string4.isBlank() || string6.isBlank())) {
                int n3 = Math.max(5000, enorialsHUD.cfg().getInt("upload.github.timeout-ms", 30000));
                int n4 = Math.max(1, n2);
                Instant instant = Instant.now().minus(Duration.ofDays(Math.max(1, n)));
                try {
                    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(n3)).build();
                    List<GitFileRef> list = GitHubUploader.listPlayerPackFiles(httpClient, string3, string4, string5, string6, n3, string);
                    int n5 = 0;
                    for (GitFileRef gitFileRef : list) {
                        if (n5 >= n4) break;
                        Instant instant2 = GitHubUploader.fetchLastCommitInstant(httpClient, string3, string4, string5, string6, n3, gitFileRef.path());
                        if (instant2 == null || !instant2.isBefore(instant) || !GitHubUploader.deleteFile(httpClient, string3, string4, string5, string6, n3, gitFileRef)) continue;
                        ++n5;
                    }
                    return n5;
                }
                catch (Exception exception) {
                    enorialsHUD.getLogger().warning("GitHub avatar RP cleanup failed: " + exception.getMessage());
                    return 0;
                }
            }
            return 0;
        }
        return 0;
    }

    private static String fetchCurrentSha(HttpClient httpClient, String string, String string2, String string3, int n) throws IOException, InterruptedException {
        String string4 = string + "?ref=" + URLEncoder.encode(string2, StandardCharsets.UTF_8);
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(string4)).timeout(Duration.ofMillis(n)).header("Authorization", "Bearer " + string3).header("Accept", "application/vnd.github+json").header("User-Agent", "EnorialsHUD-RP-Uploader").GET().build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int n2 = httpResponse.statusCode();
        if (n2 == 404) {
            return null;
        }
        if (n2 >= 200 && n2 < 300) {
            JsonObject jsonObject = JsonParser.parseString((String)httpResponse.body()).getAsJsonObject();
            return !jsonObject.has("sha") ? null : jsonObject.get("sha").getAsString();
        }
        return null;
    }

    private static List<GitFileRef> listPlayerPackFiles(HttpClient httpClient, String string, String string2, String string3, String string4, int n, String string5) throws IOException, InterruptedException {
        ArrayList<GitFileRef> arrayList = new ArrayList<GitFileRef>();
        JsonElement jsonElement = GitHubUploader.getContents(httpClient, string, string2, string3, string4, n, string5);
        if (jsonElement != null && jsonElement.isJsonArray()) {
            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                JsonElement jsonElement3;
                String string6;
                JsonObject jsonObject;
                String string7;
                if (!jsonElement2.isJsonObject() || !"dir".equals(string7 = GitHubUploader.asString(jsonObject = jsonElement2.getAsJsonObject(), "type")) || (string6 = GitHubUploader.asString(jsonObject, "path")) == null || string6.isBlank() || (jsonElement3 = GitHubUploader.getContents(httpClient, string, string2, string3, string4, n, string6)) == null || !jsonElement3.isJsonArray()) continue;
                for (JsonElement jsonElement4 : jsonElement3.getAsJsonArray()) {
                    JsonObject jsonObject2;
                    if (!jsonElement4.isJsonObject() || !"file".equals(GitHubUploader.asString(jsonObject2 = jsonElement4.getAsJsonObject(), "type"))) continue;
                    String string8 = GitHubUploader.asString(jsonObject2, "path");
                    String string9 = GitHubUploader.asString(jsonObject2, "sha");
                    if (string8 == null || string8.isBlank() || string9 == null || string9.isBlank()) continue;
                    arrayList.add(new GitFileRef(string8, string9));
                }
            }
            return arrayList;
        }
        return arrayList;
    }

    private static JsonElement getContents(HttpClient httpClient, String string, String string2, String string3, String string4, int n, String string5) throws IOException, InterruptedException {
        String string6 = GitHubUploader.encodePath(string5);
        String string7 = "https://api.github.com/repos/" + GitHubUploader.url(string) + "/" + GitHubUploader.url(string2) + "/contents/" + string6 + "?ref=" + URLEncoder.encode(string3, StandardCharsets.UTF_8);
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(string7)).timeout(Duration.ofMillis(n)).header("Authorization", "Bearer " + string4).header("Accept", "application/vnd.github+json").header("User-Agent", "EnorialsHUD-RP-Uploader").GET().build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int n2 = httpResponse.statusCode();
        if (n2 == 404) {
            return null;
        }
        return n2 >= 200 && n2 < 300 ? JsonParser.parseString((String)httpResponse.body()) : null;
    }

    private static Instant fetchLastCommitInstant(HttpClient httpClient, String string, String string2, String string3, String string4, int n, String string5) throws IOException, InterruptedException {
        String string6 = "https://api.github.com/repos/" + GitHubUploader.url(string) + "/" + GitHubUploader.url(string2) + "/commits?path=" + URLEncoder.encode(string5, StandardCharsets.UTF_8) + "&sha=" + URLEncoder.encode(string3, StandardCharsets.UTF_8) + "&per_page=1";
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(string6)).timeout(Duration.ofMillis(n)).header("Authorization", "Bearer " + string4).header("Accept", "application/vnd.github+json").header("User-Agent", "EnorialsHUD-RP-Uploader").GET().build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int n2 = httpResponse.statusCode();
        if (n2 >= 200 && n2 < 300) {
            JsonElement jsonElement = JsonParser.parseString((String)httpResponse.body());
            if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() != 0) {
                JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
                JsonObject jsonObject2 = jsonObject.has("commit") ? jsonObject.getAsJsonObject("commit") : null;
                JsonObject jsonObject3 = jsonObject2 != null && jsonObject2.has("committer") ? jsonObject2.getAsJsonObject("committer") : null;
                String string7 = jsonObject3 != null ? GitHubUploader.asString(jsonObject3, "date") : null;
                return string7 != null && !string7.isBlank() ? Instant.parse(string7) : null;
            }
            return null;
        }
        return null;
    }

    private static boolean deleteFile(HttpClient httpClient, String string, String string2, String string3, String string4, int n, GitFileRef gitFileRef) throws IOException, InterruptedException {
        String string5 = GitHubUploader.encodePath(gitFileRef.path());
        String string6 = "https://api.github.com/repos/" + GitHubUploader.url(string) + "/" + GitHubUploader.url(string2) + "/contents/" + string5;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "Cleanup stale avatar RP " + gitFileRef.path());
        jsonObject.addProperty("sha", gitFileRef.sha());
        jsonObject.addProperty("branch", string3);
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(string6)).timeout(Duration.ofMillis(n)).header("Authorization", "Bearer " + string4).header("Accept", "application/vnd.github+json").header("Content-Type", "application/json").header("User-Agent", "EnorialsHUD-RP-Uploader").method("DELETE", HttpRequest.BodyPublishers.ofString(jsonObject.toString())).build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int n2 = httpResponse.statusCode();
        return n2 >= 200 && n2 < 300;
    }

    private static String encodePath(String string) {
        String[] stringArray = string.replace('\\', '/').split("/");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < stringArray.length; ++i) {
            if (stringArray[i].isEmpty()) continue;
            if (stringBuilder.length() > 0) {
                stringBuilder.append('/');
            }
            stringBuilder.append(GitHubUploader.url(stringArray[i]));
        }
        return stringBuilder.toString();
    }

    private static String cfg(EnorialsHUD enorialsHUD, String string, String string2) {
        String string3 = enorialsHUD.cfg().getString(string, string2);
        return string3 == null ? string2 : string3.trim();
    }

    private static String url(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trim(String string) {
        if (string == null) {
            return "";
        }
        String string2 = string.replace("\n", " ").replace("\r", " ").trim();
        return string2.length() > 240 ? string2.substring(0, 240) + "..." : string2;
    }

    private static String buildRawUrl(String string, String string2, String string3, String string4, String string5, String string6) {
        String string7 = GitHubUploader.encodePath(string4);
        String string8 = string6 != null && !string6.isBlank() ? string6 : string3;
        String string9 = "https://raw.githubusercontent.com/" + GitHubUploader.url(string) + "/" + GitHubUploader.url(string2) + "/" + GitHubUploader.url(string8) + "/" + string7;
        String string10 = string5 != null && !string5.isBlank() ? string5 : String.valueOf(System.currentTimeMillis());
        return string9 + "?v=" + GitHubUploader.url(string10);
    }

    private static String extractContentSha(String string) {
        try {
            String string2;
            JsonObject jsonObject;
            JsonObject jsonObject2 = JsonParser.parseString((String)string).getAsJsonObject();
            if (jsonObject2.has("content") && (jsonObject = jsonObject2.getAsJsonObject("content")) != null && jsonObject.has("sha") && (string2 = jsonObject.get("sha").getAsString()) != null && !string2.isBlank()) {
                return string2;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static String extractCommitSha(String string) {
        try {
            String string2;
            JsonObject jsonObject;
            JsonObject jsonObject2 = JsonParser.parseString((String)string).getAsJsonObject();
            if (jsonObject2.has("commit") && (jsonObject = jsonObject2.getAsJsonObject("commit")) != null && jsonObject.has("sha") && (string2 = jsonObject.get("sha").getAsString()) != null && !string2.isBlank()) {
                return string2;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static String asString(JsonObject jsonObject, String string) {
        return jsonObject != null && string != null && jsonObject.has(string) && !jsonObject.get(string).isJsonNull() ? jsonObject.get(string).getAsString() : null;
    }

    public static final class UploadResult {
        private final boolean success;
        private final String rawUrl;
        private final String versionKey;
        private final String commitSha;

        public UploadResult(boolean bl, String string, String string2, String string3) {
            this.success = bl;
            this.rawUrl = string;
            this.versionKey = string2;
            this.commitSha = string3;
        }

        public boolean success() {
            return this.success;
        }

        public String rawUrl() {
            return this.rawUrl;
        }

        public String versionKey() {
            return this.versionKey;
        }

        public String commitSha() {
            return this.commitSha;
        }
    }

    private record GitFileRef(String path, String sha) {
    }
}

