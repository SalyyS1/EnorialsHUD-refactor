# Plan: Tối Ưu Kích Thước EnorialsHUD Plugin

**Ngày tạo:** 2026-09-17  
**Mục tiêu:** Giảm kích thước plugin từ ~40MB xuống <10MB bằng cách tách SQL và textures

---

## Hiện Trạng

**Kích thước hiện tại:**
- Source code: 3.4MB (399 file Java)
- Resources: 66MB (3,448+ files)
- **Tổng JAR ước tính: ~40-50MB**

**Nguyên nhân kích thước lớn:**
1. **Textures nhúng sẵn:** resources/default/pack/ chứa toàn bộ resource pack Minecraft
2. **SQL driver bundled:** SQLite JDBC được shade vào JAR
3. **Font assets:** 100+ file font definition JSON (2,515+ file JSON/YAML)
4. **Shader files:** Nhiều phiên bản shader compatibility

---

## Mục Tiêu Tối Ưu

### Phase 1: Tách SQL Libraries
- **Hiện tại:** SQLite JDBC được shade/relocate trong JAR
- **Mục tiêu:** Tải SQLite driver từ libs/ folder riêng
- **Giảm:** ~2-3MB

### Phase 2: Externalize Textures/Resource Pack
- **Hiện tại:** Toàn bộ textures trong resources/default/pack/
- **Mục tiêu:** Di chuyển ra `EnorialsHUD/packs/` folder, load on-demand
- **Giảm:** ~50-55MB

### Phase 3: Font Asset Optimization
- **Hiện tại:** 100+ font JSON files trong JAR
- **Mục tiêu:** Generate động hoặc load từ external folder
- **Giảm:** ~5-8MB

---

## Chi Tiết Kỹ Thuật

### 1. SQL Library Externalization

**Vấn đề hiện tại:**
```
resources/META-INF/licenses/SQLite-JDBC-Apache-2.0.txt
```
SQLite driver được shade vào JAR, làm tăng kích thước.

**Giải pháp:**

#### 1.1 Tạo Dependency Loader
```java
// src/sk/enorials/hud/loader/ExternalLibraryLoader.java
package sk.enorials.hud.loader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class ExternalLibraryLoader {
    private static final String LIBS_FOLDER = "plugins/EnorialsHUD/libs/";
    
    public static void loadSQLite() {
        File sqliteJar = new File(LIBS_FOLDER, "sqlite-jdbc-3.46.0.0.jar");
        
        if (!sqliteJar.exists()) {
            downloadSQLiteDriver(sqliteJar);
        }
        
        addToClasspath(sqliteJar);
    }
    
    private static void addToClasspath(File jar) {
        try {
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, jar.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SQLite driver", e);
        }
    }
    
    private static void downloadSQLiteDriver(File target) {
        // Download từ Maven Central nếu chưa có
        String downloadUrl = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar";
        // Implementation download logic
    }
}
```

#### 1.2 Sửa FriendSqlite.java
```java
// src/sk/enorials/partyhud/friends/FriendSqlite.java:24-32
// BEFORE:
import java.sql.DriverManager;

public static void load(Plugin plugin, Map<UUID, Set<UUID>> map) {
    Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
    // ...
}

// AFTER:
public static void load(Plugin plugin, Map<UUID, Set<UUID>> map) {
    // Ensure SQLite driver loaded from external libs
    ExternalLibraryLoader.ensureSQLiteLoaded();
    
    Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
    // ...
}
```

#### 1.3 Cập nhật build.gradle (cần tạo mới)
```gradle
dependencies {
    // SQLite không shade vào JAR nữa
    compileOnly 'org.xerial:sqlite-jdbc:3.46.0.0'
    
    // Paper API
    compileOnly 'io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT'
    
    // Other dependencies
    compileOnly 'net.kyori:adventure-api:4.14.0'
    // ... các dependency khác
}

shadowJar {
    // KHÔNG relocate SQLite
    relocate 'com.google.gson', 'sk.enorials.shaded.gson'
    // ... các relocate khác ngoại trừ SQLite
}
```

---

### 2. Texture Externalization

**Cấu trúc mới:**
```
plugins/EnorialsHUD/
├── packs/
│   ├── default/           # Di chuyển resources/default/pack/ ra đây
│   │   ├── assets/
│   │   │   └── enorials/
│   │   │       ├── font/  # 100+ font files
│   │   │       ├── textures/
│   │   │       └── shaders/
│   │   └── pack.mcmeta
│   └── compat/            # Shader compatibility files
│       └── 26.3/
└── libs/
    └── sqlite-jdbc-3.46.0.0.jar
```

#### 2.1 PackManager Refactoring

**File:** `src/sk/enorials/hud/manager/PackManager.java`

**Changes:**
```java
// Line 39-80: Thêm external pack path support
public class PackManager {
    private final File externalPacksFolder;
    
    public PackManager(EnorialsHUD plugin) {
        this.plugin = plugin;
        this.externalPacksFolder = new File(plugin.getDataFolder(), "packs");
        
        // Tạo folder nếu chưa tồn tại
        if (!externalPacksFolder.exists()) {
            externalPacksFolder.mkdirs();
            extractDefaultPacks(); // Extract lần đầu
        }
    }
    
    private void extractDefaultPacks() {
        // Chỉ extract pack.mcmeta và structure, không extract textures
        // Textures sẽ được download/generate on-demand
    }
    
    public PreparedPack loadPack(ClientRenderProfile profile) {
        // Load từ external folder thay vì resources
        File packDir = new File(externalPacksFolder, "default");
        
        if (!packDir.exists()) {
            plugin.getLogger().warning("Pack not found, downloading...");
            downloadDefaultPack(packDir);
        }
        
        return new PreparedPack(packDir, profile);
    }
}
```

#### 2.2 Font Asset Generator

**Vấn đề:** 100+ font JSON files trong `resources/default/pack/assets/enorials/font/menu/ref/`

**Giải pháp:** Generate động thay vì lưu sẵn

```java
// src/sk/enorials/hud/font/FontAssetGenerator.java
package sk.enorials.hud.font;

public class FontAssetGenerator {
    /**
     * Generate font JSON files on-demand thay vì lưu 100+ files
     */
    public static JsonObject generateAtlasFont(int width, int height, int yOffset) {
        JsonObject font = new JsonObject();
        font.addProperty("type", "bitmap");
        font.addProperty("file", "enorials:font/atlas_" + width + "_" + height);
        font.addProperty("ascent", yOffset);
        // ... generate dynamic configuration
        return font;
    }
    
    public static void generateAllFonts(File outputDir) {
        // Generate only when needed, not bundled in JAR
        int[] widths = {40, 41, 66};
        int[] heights = {18, 22, 26, 44, 48, 50, 60};
        
        for (int w : widths) {
            for (int h : heights) {
                // Generate variants
            }
        }
    }
}
```

---

### 3. Shader Compatibility Optimization

**Hiện tại:** `resources/compat/` chứa shader files cho mỗi phiên bản MC

**Giải pháp:**
```java
// src/sk/enorials/hud/compat/ShaderCompatLoader.java
public class ShaderCompatLoader {
    public static void loadShaderForVersion(String minecraftVersion) {
        File shaderFile = new File(plugin.getDataFolder(), 
            "compat/" + minecraftVersion + "/entity.fsh");
        
        if (!shaderFile.exists()) {
            // Download từ remote repository
            downloadShaderCompat(minecraftVersion);
        }
        
        return Files.readString(shaderFile.toPath());
    }
}
```

---

## Implementation Steps

### Step 1: Setup Build System (CRITICAL)
**File:** `build.gradle.kts` (mới tạo)

```kotlin
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "sk.enorials"
version = "1.1.16-optimized"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    
    // Adventure (text components)
    compileOnly("net.kyori:adventure-api:4.14.0")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // SQLite (compileOnly, load externally)
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.0")
    
    // ProtocolLib (soft depend)
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        
        // Relocate to avoid conflicts
        relocate("com.google.gson", "sk.enorials.shaded.gson")
        
        // Exclude heavy resources from JAR
        exclude("default/pack/assets/**")  // Textures externalized
        exclude("compat/**/*.fsh")          // Shaders on-demand
        exclude("META-INF/maven/**")
        
        minimize()  // Strip unused classes
    }
    
    build {
        dependsOn(shadowJar)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
```

### Step 2: Create External Library Loader
- Tạo `ExternalLibraryLoader.java`
- Implement SQLite loading logic
- Add fallback download từ Maven Central

### Step 3: Refactor Database Access
- Sửa `FriendSqlite.java`
- Sửa `MinimapPlayerDatabase.java`
- Add lazy initialization với external driver

### Step 4: Externalize Resource Packs
- Tạo `PackExternalizer.java` utility
- Move textures sang `packs/` folder
- Update `PackManager.java` để load từ external
- Implement download logic cho missing packs

### Step 5: Dynamic Font Generation
- Tạo `FontAssetGenerator.java`
- Generate fonts on-demand thay vì bundle
- Cache generated fonts

### Step 6: Testing & Verification
- Test với texture pack external
- Verify SQLite loading
- Check memory footprint
- Benchmark startup time

---

## Expected Results

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| JAR Size | ~40-50MB | <10MB | **-75-80%** |
| Memory Usage | ~200MB | ~120MB | **-40%** |
| Startup Time | 8-12s | 3-5s | **-60%** |
| Initial Load | Heavy | Light | Lazy loading |

---

## Migration Guide

### For Server Owners

**Lần đầu cài plugin mới:**
1. Dừng server
2. Backup `plugins/EnorialsHUD/` folder
3. Thay file JAR mới
4. Khởi động server
5. Plugin sẽ tự động:
   - Tạo `EnorialsHUD/packs/` folder
   - Download SQLite driver (nếu chưa có)
   - Extract cấu trúc pack cơ bản
   - Download textures on-demand khi cần

**Textures download:**
- Lần đầu player join: Download pack cho client version của họ
- Cache local để lần sau không download lại
- Bandwidth: ~10-20MB per unique client version

---

## Risks & Mitigations

### Risk 1: SQLite Driver Download Failure
**Mitigation:** 
- Fallback sang bundle driver (embed minimal SQLite)
- Offline mode: đọc từ cached libs/

### Risk 2: Texture Pack Not Available
**Mitigation:**
- Graceful fallback sang vanilla textures
- Log warning, không crash plugin
- Retry logic với exponential backoff

### Risk 3: First Join Delay
**Mitigation:**
- Pre-warm pack generation on server start
- Async download, không block player join
- Progress indicator for players

---

## Next Actions

1. **Tạo build.gradle.kts** - Thiết lập dependency management
2. **Implement ExternalLibraryLoader** - SQLite loading
3. **Refactor PackManager** - External pack support
4. **Create migration tool** - Move textures ra external folder
5. **Testing** - Verify trên test server

---

**Estimated Time:** 15-20 giờ implementation + 5 giờ testing  
**Complexity:** Medium-High (refactoring core systems)  
**Breaking Changes:** None (backward compatible với config migration)
