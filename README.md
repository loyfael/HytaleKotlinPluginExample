# Kotlin Hytale Server Plugin Template (Beginner Friendly)

This repository is a **Kotlin** template for building a **Hytale server plugin**, even if you’re starting from absolute zero.

Goals of this README:

- show you **where to put the server JAR**
- show you how to **build** the plugin
- show you where the **final plugin .jar** is
- show you how to **install** it on a Hytale server
- explain where to start coding (main class, `manifest.json`, lifecycle)

## 0) What is a Hytale server plugin?
A Hytale server plugin is a **.jar** loaded by the server. It can:

- add commands
- listen to events
- register systems/components
- save data and configs

The server discovers and loads your plugin using the `manifest.json` included in your JAR.

## 1) Requirements

You need:

- **Java 25 (JDK 25)**
- Gradle (optional) — this project already includes the wrapper `./gradlew`

Check your Java version:

```bash
java -version
```

You should see `25.x`.

## 2) Get the Hytale server JAR (required to compile)

You need the Hytale server JAR to compile this plugin.

### Steps (simple)

1) Download and run the official Hytale Downloader.

Example (Linux):

```bash
wget https://downloader.hytale.com/hytale-downloader.zip
unzip hytale-downloader.zip
./hytale-downloader-linux-amd64
```

2) After the download completes, find the server JAR in the downloaded files (usually inside a `server/` folder).

3) Copy it into this project root and name it exactly:

- `HytaleServer.jar`

### ⚠️ Security note

- Never commit `HytaleServer.jar` to git.
- Never commit `.hytale-downloader-credentials.json` to git.

Recommended `.gitignore` entries:

```gitignore
HytaleServer.jar
.hytale-downloader-credentials.json
```

## 3) Where are the important files?
- Kotlin source code: `src/main/kotlin/`
- Plugin manifest: `src/main/resources/manifest.json`
- Server API JAR (compile-time): `HytaleServer.jar` (at the project root)
- Build config: `build.gradle.kts`
- Build output (server-ready JAR): `mods/ExamplePlugin-all.jar`

Plugin entrypoint:

- Main class: `src/main/kotlin/com/loyfael/exampleplugin/ExamplePlugin.kt`
- In the manifest, the `Main` field must exactly match the class (package + class name): `com.loyfael.exampleplugin.ExamplePlugin`

## 4) Most important step: put the server JAR at the project root

To compile, your plugin must have access to the Hytale server API.

➡️ Put **your Hytale server JAR** here (exact filename):

- `HytaleServer.jar`

Important notes:

- This JAR is used as **compileOnly**: it is required to compile, but it is **NOT** bundled into your plugin.
- That’s expected: at runtime, the **server** provides its own classes.

Quick check (should say “Java archive data (JAR)”):

```bash
file HytaleServer.jar
```

## 5) Build the plugin (main command)

From the project root:

```bash
./gradlew clean build
```

If everything is fine, you should see `BUILD SUCCESSFUL`.

## 6) Where is the final plugin `.jar`?
The server-ready JAR is generated here:

- `mods/`

This project produces **one single** server-ready JAR:

- `mods/ExamplePlugin-all.jar`

Why only one JAR?

- Kotlin plugins need the **Kotlin runtime** (`kotlin-stdlib`) to run.
- This template builds a **fat-jar**: it includes Kotlin runtime and any external dependencies (example: Gson).
- Result: you copy **one file** to the server and it runs.

## 7) Install the plugin on your Hytale server

1) Build:

```bash
./gradlew clean build
```

2) Copy the JAR into your server `mods/` folder.

Example:

```bash
cp mods/ExamplePlugin-all.jar /path/to/your/server/mods/
```

3) Start the server and check the logs.

You should see logs during plugin load (setup / start).

## 8) Understanding `manifest.json` (beginner explanation)

File: `src/main/resources/manifest.json`

Important fields:

- `Group` + `Name`: plugin identifier → `Group:Name`
- `Version`: plugin version
- `Description`: description text
- `Main`: **main class** (this project: `com.loyfael.exampleplugin.ExamplePlugin`)
- `IncludesAssetPack`: whether the plugin ships UI/assets from `src/main/resources/Common/...`

During the build, Gradle replaces `${version}` with the version defined in `build.gradle.kts`.

## 9) Where do I code? (plugin lifecycle)

File: `src/main/kotlin/com/loyfael/exampleplugin/ExamplePlugin.kt`

Main methods:

- `setup()`: register commands, events, systems, assets…
- `start()`: logic that depends on other plugins being set up
- `shutdown()`: cleanup

Logging (important):

The server logger uses a “Flogger-like” API. In Kotlin:

```kotlin
logger.at(java.util.logging.Level.INFO).log("Hello!")
```

## 10) External dependencies (example)

This template can bundle external libraries directly into your plugin JAR.

What happens:

- you add a dependency using `implementation(...)`
- Gradle bundles it into the fat-jar
- we can also **relocate** (shade) it to avoid conflicts with libraries shipped by the server

So you can use external dependencies without installing anything else on the server.

## 11) Common issues (and fixes)

### Build fails: “Unresolved reference … com.hypixel…”

Cause: `HytaleServer.jar` is missing (project root) or not a valid JAR.

Fix:

- ensure the file exists at `HytaleServer.jar`
- verify it’s a real JAR: `file HytaleServer.jar`

### Server doesn’t load the plugin / “Main class not found”

Cause: wrong `Main` value in the manifest.

Fix:

- make sure `Main` is exactly `com.loyfael.exampleplugin.ExamplePlugin` (or your package/class)

### Gradle daemon weirdness

Simple fix:

```bash
./gradlew --stop
./gradlew clean build --no-daemon
```

## 12) Next steps (easy ideas)

- add a `/hello` command
- listen to a “player join” event
- add a config via `withConfig(...)` like SimpleClaims

Tell me what you want first and I’ll scaffold it in Kotlin.

## 13) How found my other plugins?
Simply search in my other branches ! ;)
