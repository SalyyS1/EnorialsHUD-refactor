plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // ===== Server-provided (never bundled) =====
    // Paper API provides Bukkit, Adventure and Brigadier at runtime.
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")

    // ===== Bundled at runtime =====
    // SQLite JDBC: the original JAR embedded this library. Native binaries are
    // excluded from the shaded output (see shadowJar below) because they are
    // platform-specific and account for ~24MB. They are loaded from an external
    // directory at runtime instead.
    implementation("org.xerial:sqlite-jdbc:${providers.gradleProperty("sqliteJdbcVersion").get()}") {
        // The upstream artifact pulls a duplicate SLF4J binding; the server owns logging.
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // ORMLite: object-relational mapping over SQLite, used by the minimap database.
    implementation("com.j256.ormlite:ormlite-jdbc:${providers.gradleProperty("ormliteVersion").get()}")

    // Optional plugin APIs supplied by the local Maven cache / local-libs.
    compileOnly(files("$rootDir/local-libs/placeholderapi-2.11.5.jar"))
    compileOnly(fileTree("local-libs") { include("*.jar") })
}

// Both source roots are non-standard: the decompiled tree uses `src/` directly
// and resources live in `resources/`. Configure source sets rather than
// physically moving 3,400 files, which would destroy diff continuity.
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("resources"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    // The decompiled source predates current javac strictness and contains
    // unchecked reflection into third-party plugins.
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:-deprecation",
            "-Xlint:-unchecked",
            "-Xlint:-removal",
            "-Xlint:-rawtypes",
        )
    )
}

tasks.processResources {
    // Substitute the build version into plugin.yml instead of hand-editing it.
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("EnorialsHUD")
    archiveClassifier.set("")

    // Relocate bundled libraries so they cannot collide with the same library
    // loaded by another plugin on the server.
    relocate("org.sqlite", "sk.enorials.libs.sqlite")
    relocate("com.j256.ormlite", "sk.enorials.libs.ormlite")

    // Native binaries are platform-specific and were the bulk of the JAR.
    // They are provisioned externally by SqliteNativeLoader at runtime.
    exclude("org/sqlite/native/**")
    exclude("**/*.so")
    exclude("**/*.dll")
    exclude("**/*.dylib")
    exclude("**/*.jnilib")

    // Strip the vendor signature: modifying a signed JAR invalidates it, and a
    // stale signature makes the JVM reject the shaded artifact.
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/ENORIAL.*")

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
