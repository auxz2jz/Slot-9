# GITHUB_APK_BUILD_PROCESS.md

## Purpose

This document records the exact process used to create the first working DVR Video Player Android APK entirely from source stored in GitHub.

Repository:

`auxz2jz/Slot-9`

Initial app version:

`v0.1.0`

The purpose of this file is reproducibility. If the original chat disappears, this document explains how the source was created, compiled, fixed, packaged, and turned into a working Android APK.

---

# 1. PROJECT WAS CREATED AS A NORMAL ANDROID PROJECT

The Android project was created with these root files:

```
settings.gradle.kts
build.gradle.kts
gradle.properties
app/
```

The app module contains:

```
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/java/com/zaksecurity/dvrplayer/MainActivity.kt
app/src/main/java/com/zaksecurity/dvrplayer/ui/DvrPlayerApp.kt
app/src/main/java/com/zaksecurity/dvrplayer/diagnostics/DiagnosticLogger.kt
app/src/main/java/com/zaksecurity/dvrplayer/diagnostics/DeviceInfoCollector.kt
app/src/main/java/com/zaksecurity/dvrplayer/diagnostics/MediaInfoCollector.kt
app/src/main/java/com/zaksecurity/dvrplayer/diagnostics/DiagnosticExporter.kt
app/src/main/java/com/zaksecurity/dvrplayer/testing/GuidedTestController.kt
```

The application ID/package is:

`com.zaksecurity.dvrplayer`

---

# 2. ROOT GRADLE CONFIGURATION

The root build file was created as:

```kotlin
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
```

The project settings file was created with Google, Maven Central and Gradle Plugin Portal repositories and includes the `:app` module.

The project used Java 17.

---

# 3. APP GRADLE CONFIGURATION

The app module was configured with:

- compile SDK: Android API 36
- target SDK: Android API 36
- minimum SDK: Android API 26
- versionCode: 1
- versionName: 0.1.0
- Java source compatibility: Java 17
- Java target compatibility: Java 17
- Jetpack Compose enabled

Main Android dependencies:

```
androidx.core:core-ktx:1.18.0
androidx.activity:activity-compose:1.12.4
androidx.lifecycle:lifecycle-runtime-ktx:2.10.0
androidx.compose:compose-bom:2026.02.01
androidx.compose.ui:ui
androidx.compose.ui:ui-tooling-preview
androidx.compose.foundation:foundation
androidx.compose.material3:material3
androidx.media3:media3-exoplayer:1.11.1
androidx.media3:media3-ui:1.11.1
```

The exact current versions should always be checked in `app/build.gradle.kts` before changing them.

---

# 4. FIRST APPLICATION CODE

`MainActivity.kt` launches the Compose application:

`DvrPlayerApp()`

The player screen was built with AndroidX Media3 / ExoPlayer.

Initial working functions added:

- Android file picker for local video
- Media3 video playback
- custom Play/Pause button
- current playback time
- total video duration
- seek/timeline slider
- Media3 player event callbacks
- diagnostic event logging
- device/app information
- video/media metadata
- guided baseline test
- diagnostic ZIP export
- crash persistence

The original video remains read-only.

---

# 5. GUIDED TEST AND DIAGNOSTIC SYSTEM

The test system was created before advanced DVR features.

The initial test sequence is:

```
Open Video
Play
Pause
Seek
```

The important design is that pressing a button is not considered proof that the function worked.

Example:

```
PLAY_BUTTON_PRESSED
-> Media3 play request
-> isPlaying=true callback
-> playback position advances
-> test step PASS
```

Diagnostics are recorded as JSON Lines in an `events.jsonl` file.

The diagnostic export ZIP can contain:

```
README.txt
summary.txt
device_app_info.txt
media_info.txt
events.jsonl
guided_test_results.json
errors.txt
previous_crash.json
previous_crashed_session_events.jsonl
```

The source video is not automatically included.

---

# 6. GITHUB ACTIONS CI WAS CREATED

A workflow file was created at:

`.github/workflows/android-ci.yml`

The working CI environment uses:

- GitHub Ubuntu hosted runner
- JDK 17
- Android SDK API 36
- Android Build Tools 36.0.0
- Gradle 9.6

The important setup sequence is:

```yaml
- uses: actions/checkout@v5

- uses: actions/setup-java@v5
  with:
    distribution: temurin
    java-version: "17"

- name: Accept Android SDK licenses
  run: yes | /usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null || true

- name: Install Android SDK packages
  run: /usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"

- name: Set up Gradle 9.6
  uses: gradle/actions/setup-gradle@v4
  with:
    gradle-version: "9.6.0"
```

---

# 7. FIRST CI FAILURES AND HOW THEY WERE FIXED

The APK did not appear magically on the first try. CI was used as the compiler/debugging environment.

## Failure 1 — obsolete Android SDK tools package

The original workflow used:

`android-actions/setup-android@v3`

That action attempted to install the old Android SDK package:

`tools`

The runner reported that the package could not be found.

Resolution:

Remove that setup action and use the Android SDK already installed on the GitHub runner.

---

## Failure 2 — sdkmanager was not on PATH

After removing the setup action, the shell reported:

`sdkmanager: command not found`

Resolution:

Use its full runner path:

`/usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager`

---

## Failure 3 — Android API 37 unavailable in that runner repository

The first configuration attempted to install:

`platforms;android-37`

The runner reported:

`Failed to find package 'platforms;android-37'`

Resolution:

The app was moved to stable Android API 36:

```
compileSdk = 36
targetSdk = 36
```

The Compose dependency set was also moved to an API-36-compatible set.

---

## Failure 4 — Compose weight import

Once CI reached Kotlin compilation, the source produced:

`Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.`

Cause:

An explicit import of:

`androidx.compose.foundation.layout.weight`

was incompatible with the selected Compose version.

Resolution:

Remove the explicit `weight` import and allow `Modifier.weight()` to resolve from the `RowScope`.

After this change, the Kotlin project compiled successfully.

---

# 8. FIRST SUCCESSFUL APK BUILD

After the corrections above, GitHub Actions successfully ran the Android Gradle task:

`:app:assembleDebug`

The first known-good source build was recorded in the project documentation.

The generated APK path inside the build workspace is:

```
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions was then configured to upload that file as an artifact.

The artifact name was:

`DVR-Video-Player-v0.1.0-debug`

The APK produced by this process was later installed on the user's physical Android phone and the user confirmed that it worked.

---

# 9. GRADLE WRAPPER WAS THEN ADDED

To make the project portable and Android Studio-ready, the CI workflow was later updated to generate a Gradle Wrapper:

```bash
gradle wrapper --gradle-version 9.6.0
```

This created:

```
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

The project was then rebuilt using the wrapper itself:

```bash
./gradlew --no-daemon :app:assembleDebug
```

That wrapper-based build also succeeded.

This is important because it verifies the same project structure that is delivered to Android Studio.

---

# 10. APK ARTIFACT UPLOAD

The working CI upload step was:

```yaml
- name: Upload debug APK
  uses: actions/upload-artifact@v7
  with:
    name: DVR-Video-Player-v0.1.0-debug
    path: app/build/outputs/apk/debug/app-debug.apk
    if-no-files-found: error
    retention-days: 30
```

GitHub packaged the APK into a downloadable workflow artifact.

The artifact ZIP was downloaded, and the APK inside it was provided to the user.

The user installed that APK on the physical phone and later confirmed that it worked.

---

# 11. ANDROID STUDIO PROJECT ARTIFACT

The CI workflow was later extended to also upload the complete Android Studio project:

```yaml
- name: Upload Android Studio project
  uses: actions/upload-artifact@v7
  with:
    name: DVR-Video-Player-v0.1.0-Android-Studio
    path: |
      settings.gradle.kts
      build.gradle.kts
      gradle.properties
      gradlew
      gradlew.bat
      gradle/wrapper/**
      app/build.gradle.kts
      app/src/**
      README.md
      PROJECT_MEMORY.md
      DVR_PLAYER_ROADMAP.md
      TESTING_DIAGNOSTICS.md
      BUILD_WORKFLOW.md
      test_reports/**
```

That produced the Android Studio-ready ZIP delivered to the user.

---

# 12. EXACT SOURCE-TO-APK PIPELINE

The complete flow was:

```
Project plan
  ->
Create Android project files
  ->
Create Kotlin/Compose/Media3 source
  ->
Commit source to GitHub
  ->
GitHub Actions checks out repository
  ->
Set up JDK 17
  ->
Install Android API 36 + Build Tools 36.0.0
  ->
Set up Gradle 9.6
  ->
Generate Gradle Wrapper
  ->
Run ./gradlew :app:assembleDebug
  ->
Android Gradle Plugin compiles Kotlin/resources/manifest
  ->
APK created at app/build/outputs/apk/debug/app-debug.apk
  ->
GitHub Actions uploads APK artifact
  ->
Artifact downloaded
  ->
APK installed on Android phone
  ->
User confirmed the APK worked
```

---

# 13. REPRODUCING THE BUILD OUTSIDE GITHUB

On a normal development computer with Android Studio:

1. Extract/open the Android Studio-ready project.
2. Allow Android Studio to use/download the required Android SDK 36 components.
3. Use JDK 17 if Android Studio does not select a compatible JDK automatically.
4. Allow Gradle sync.
5. Build the Debug APK.

Command-line equivalent from the project root:

```bash
./gradlew :app:assembleDebug
```

Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Result:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

# 14. WHY THE GITHUB-BUILT APK WAS USEFUL

Although writing every source file through GitHub was slower than local development, the approach proved several useful things:

- the source was stored permanently,
- the build was reproducible,
- compiler errors were captured objectively,
- the project could be corrected without guessing,
- the APK was generated by a clean CI environment,
- the build did not depend on hidden files on a development computer,
- the resulting APK worked on the user's real Android phone.

---

# 15. FUTURE DEVELOPMENT RULE

For future DVR Video Player versions:

**Primary development environment:** local project workspace

**Primary handoff:** Android Studio-ready ZIP

**Permanent records/checkpoints:** GitHub Slot-9

**Independent build verification:** GitHub Actions when useful

**APK production:** either Android Studio/local Gradle or GitHub CI, depending on which is most efficient for the specific checkpoint.

This preserves the successful GitHub APK process while avoiding unnecessary GitHub file-by-file editing during normal development.
