# DVR Video Player

Android DVR-style video review project.

Repository source-of-truth files:

1. `PROJECT_MEMORY.md` — mandatory startup, checkpoint, anti-loop and recovery rules.
2. `DVR_PLAYER_ROADMAP.md` — permanent product roadmap.
3. `TESTING_DIAGNOSTICS.md` — guided testing, objective verification and downloadable diagnostics specification.

## Current checkpoint

**v0.1.0 development**

Initial source includes:

- Android/Kotlin/Jetpack Compose project foundation
- AndroidX Media3 / ExoPlayer playback
- local video picker
- play/pause
- seek bar
- structured JSONL diagnostic logger
- device/app diagnostics
- media metadata diagnostics
- self-verifying baseline guided test
- exportable ZIP diagnostics package

The first guided test verifies:

1. Open a video
2. Play and confirm actual position advance
3. Pause and confirm actual player state
4. Seek and confirm the player reaches the requested target

The source is not marked DONE until it has been built and tested on a real Android device and the exported diagnostic ZIP has been reviewed.
