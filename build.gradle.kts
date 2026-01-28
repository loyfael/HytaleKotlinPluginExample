import org.gradle.jvm.tasks.Jar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
}

version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files(rootProject.layout.projectDirectory.file("HytaleServer.jar")))
    // Optional integration: lets the plugin query LuckPerms directly when installed on the server.
    // Kept as compileOnly so it won't be bundled into the fat-jar.
    compileOnly("net.luckperms:api:5.4")
    implementation(kotlin("stdlib"))
}

// Replace version placeholder in manifest.json with gradle version
val currentVersion = project.version.toString()
tasks.named<ProcessResources>("processResources") {
    filesMatching("manifest.json") {
        // Use a precomputed value so expand does not access project at task execution time
        expand("version" to currentVersion)
    }
}

val modsDir = rootProject.layout.projectDirectory.dir("mods")

// Disable publishing the standard jar to mods to avoid duplicates
tasks.named<Jar>("jar") {
    archiveBaseName.set("StaffChat")
    // Keep default destination (build/libs) and avoid placing in mods
}

tasks.named<ShadowJar>("shadowJar") {
    destinationDirectory.set(modsDir)
    archiveBaseName.set("StaffChat")
    archiveVersion.set("")  // Remove version from filename
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn("shadowJar")
}
