# BUILD_WORKFLOW.md

## Purpose

This document records exactly how the first DVR Video Player Android project and APK were created, and defines the more efficient workflow to use for future versions.

Repository: `auxz2jz/Slot-9`

Project: DVR Video Player

Initial source version: v0.1.0

---

# WHAT WAS DONE FOR v0.1.0

The first version was created directly through GitHub-backed source files rather than first building the project in a local working directory.

The process was:

1. Read and update the permanent project documents:
   - `PROJECT_MEMORY.md`
   - `DVR_PLAYER_ROADMAP.md`
   - `TESTING_DIAGNOSTICS.md`

2. Create the Android project structure in GitHub:
   - root Gradle files
   - app Gradle module
   - AndroidManifest
   - resources
   - Kotlin source folders
   - player UI
   - diagnostics classes
   - guided-test controller

3. Add AndroidX Media3 / ExoPlayer playback.

4. Add the initial player functions:
   - local video picker
   - video display
   - play/pause
   - current time / duration
   - seek bar

5. Add diagnostics:
   - structured JSONL event log
   - device/app information
   - media metadata
   - player callbacks
   - button/action logging
   - crash persistence
   - ZIP export

6. Add the baseline guided test:
   - Open Video
   - Play
   - Pause
   - Seek

7. Add a GitHub Actions Android CI workflow.

8. Use CI to compile the source and diagnose build errors.

9. Fix the build problems discovered by CI:
   - obsolete Android SDK setup package
   - sdkmanager path issue
   - unavailable API-37 package in that CI environment
   - incompatible explicit Compose `weight` import

10. Rebuild until CI completed successfully.

11. Configure GitHub Actions to upload the generated debug APK as an artifact.

12. Download the successful APK artifact and provide it for installation/testing.

---

# WAS THAT WORKFLOW EFFICIENT?

It was useful for:

- preserving every important change,
- making the project recoverable,
- creating a visible change history,
- proving that the source compiled,
- producing a reproducible APK,
- preventing work from existing only in chat context.

However, it was NOT the most efficient way to perform active development.

Writing many source files individually through GitHub introduces extra round trips and makes large edits more cumbersome than working directly in a normal project directory.

---

# PREFERRED WORKFLOW FROM NOW ON

Use GitHub primarily for permanent storage, important checkpoints, recovery information, and version history.

Use a local working directory for active development.

Preferred sequence:

1. Read:
   - `PROJECT_MEMORY.md`
   - `DVR_PLAYER_ROADMAP.md`
   - `TESTING_DIAGNOSTICS.md`

2. Record the current task and implementation plan.

3. Create or update the Android Studio project in the local working environment.

4. Make related source edits locally.

5. Run structural checks and, where possible, build/compile checks locally.

6. Package the entire project as an Android Studio-ready ZIP.

7. Provide that ZIP to the user.

8. Save important project state/checkpoints to GitHub:
   - project documents,
   - roadmap changes,
   - source checkpoint when appropriate,
   - test reports,
   - known-good state,
   - failed approaches that should not be repeated.

9. Use GitHub CI when it materially helps verify compilation or produce a reproducible build, but do not require every small development edit to be performed through GitHub.

---

# USER DELIVERY FORMAT

Unless the user specifically asks for an APK or GitHub-only delivery, the default development handoff should be:

`DVR_Video_Player_vX.Y.Z_Android_Studio.zip`

The ZIP should contain the complete Android Studio project root, including:

- `settings.gradle.kts`
- root `build.gradle.kts`
- `gradle.properties`
- `app/`
- AndroidManifest
- Kotlin source
- resources
- all required project configuration

The user should be able to:

1. Extract the ZIP.
2. Open the extracted project folder in Android Studio.
3. Allow Gradle sync.
4. Build/install the app normally.

When practical, include the Gradle Wrapper so the project does not depend on a separately installed Gradle version.

---

# GITHUB'S ROLE

GitHub is the durable project memory and source checkpoint repository.

It is NOT required to be the active editing environment.

Use GitHub for:

- permanent project instructions,
- roadmap,
- testing/diagnostic specification,
- source checkpoints,
- version history,
- test reports,
- known-good builds,
- recovery information,
- major architecture decisions,
- important failed approaches.

---

# CURRENT DECISION

For future DVR Video Player development:

**Active development:** local project workspace

**Primary user handoff:** Android Studio-ready ZIP

**Persistent project memory/checkpoints:** GitHub Slot-9

**Optional independent compile verification:** GitHub Actions CI

This workflow should be used unless the user requests a different delivery method.
