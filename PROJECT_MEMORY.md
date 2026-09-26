# PROJECT_MEMORY.md

## Purpose

This file is the mandatory working memory and operating procedure for the DVR Video Player project.

**Repository:** auxz2jz/Slot-9  
**Project:** Android DVR Video Player  
**Current planned version:** v0.4.5  
**Status:** Planning / pre-code  
**Source of truth priority:** 1) this file, 2) DVR_PLAYER_ROADMAP.md, 3) TESTING_DIAGNOSTICS.md, 4) current source code and saved test/diagnostic reports, 5) conversation history.

The project must remain understandable and recoverable even if the chat history disappears.

---

# NON-NEGOTIABLE STARTUP RULE

Before doing any work on this project, the developer/agent MUST:

1. Read this entire file.
2. Read `DVR_PLAYER_ROADMAP.md`.
3. Read `TESTING_DIAGNOSTICS.md`.
4. Inspect the current project version and the files relevant to the requested change.
5. Read the latest test/diagnostic report and known-good checkpoint if they exist.
6. Review current known bugs and failed approaches.
7. Update the **Current Task** section below with the new request.
8. Write the intended implementation plan BEFORE editing code.
9. Identify the files expected to change.
10. Preserve the last known working state before risky changes.
11. Only then begin implementation.

If these steps have not been completed, coding has not started correctly.

---

# DEVELOPMENT DELIVERY WORKFLOW

Active development should normally be performed in a local project workspace rather than by writing every source file through GitHub.

Default workflow:

1. Read the permanent project documents.
2. Record the plan before coding.
3. Develop/edit in the local working project.
4. Build/check locally where practical.
5. Deliver an Android Studio-ready ZIP to the user.
6. Save important checkpoints, roadmap changes, test reports, known-good states and recovery information to GitHub.
7. Use GitHub Actions CI when independent compile verification or reproducible APK generation is useful.

GitHub is the durable project memory/checkpoint repository, not a mandatory active editing environment.

See `BUILD_WORKFLOW.md` for the normal development workflow and `GITHUB_APK_BUILD_PROCESS.md` for the exact source-to-APK GitHub build process used for v0.1.0.

---

# GOLDEN WORKFLOW

Always work in this order:

**READ -> PLAN -> RECORD PLAN -> CHECKPOINT -> MODIFY -> BUILD -> TEST -> RECORD RESULT -> SAVE/CHECKPOINT -> CONTINUE**

Never use this order:

**MODIFY -> MODIFY -> MODIFY -> GUESS -> TRY TO REMEMBER WHAT CHANGED**

Important project state must never exist only in temporary reasoning or chat context.

---

# SAVE-WORK RULES

1. Save after every meaningful milestone.
2. Keep changes small and logically grouped.
3. Create a recoverable checkpoint before:
   - major refactors,
   - replacing a working algorithm,
   - changing playback architecture,
   - changing tracking architecture,
   - changing several core files at once.
4. Do not destroy the last known working implementation while experimenting.
5. If a new approach is experimental, preserve the working approach until the replacement is proven.
6. Each tested working version should have a clear version/checkpoint and test report.
7. Documentation must be updated before ending a work session.

---

# ONE-CHANGE RULE

Whenever practical, change one logical thing at a time.

After each logical change:

1. Build.
2. Test.
3. Record PASS / FAIL / PARTIAL.
4. Fix or understand the result before adding unrelated changes.

Do not combine several unrelated feature changes into one debugging step.

---

# ANTI-LOOP RULE

Never repeat substantially the same failed solution without new evidence.

If essentially the same approach fails twice:

**STOP.**

Record:

- What was attempted.
- What happened.
- Exact error or bad behavior.
- What was learned.
- Which assumption may have been wrong.
- What evidence is needed next.

Then choose a meaningfully different approach.

Do not:
- reapply the same patch repeatedly,
- bounce between two failed implementations,
- rebuild the same broken code without a reason,
- randomly alter working code,
- guess when logs or direct inspection can answer the question.

---

# THREE-FAILURE RULE

If three meaningfully different attempts at the same problem fail:

1. Stop implementation on that problem.
2. Record all three approaches in **Failed Approaches**.
3. Re-read the relevant source.
4. Re-check the architecture and assumptions.
5. Inspect logs, diagnostics, sample media and device behavior.
6. Write a new diagnostic plan.
7. Continue only when the next attempt is based on new evidence.

The goal is to prevent development loops and accidental damage to working features.

---

# PROTECT WORKING FEATURES

Working behavior is presumed valuable.

Before replacing working code:

1. Explain why replacement is necessary.
2. Record what the current implementation does.
3. Preserve a checkpoint.
4. Change only what is necessary.
5. Retest existing related features afterward.

Do not fix Feature B by rewriting working Feature A unless the architecture truly requires it.

---

# NO SILENT SCOPE CHANGES

Do not silently redesign the application while implementing a requested feature.

New ideas belong in `DVR_PLAYER_ROADMAP.md` unless they are required for the current task.

If an implementation requires a major architecture change, record the reason before making it.

---

# CURRENT PROJECT STATE

**Project:** DVR Video Player  
**Platform:** Android  
**Language:** Kotlin  
**UI direction:** Jetpack Compose  
**Playback foundation:** AndroidX Media3 / ExoPlayer  
**Advanced vision direction:** OpenCV and/or an Android-compatible object-detection/tracking layer where useful  
**Repository:** auxz2jz/Slot-9  
**Current version:** v0.4.5 development
**Last known working version:** v0.3.1 — physical-device guided test PASS
**Build status:** v0.3.1 remains known-good; v0.4.4 compiled/ran but CSRT target initialization failed on every valid selection; v0.4.5 OpenCV runtime-loader correction in progress
**Current phase:** OpenCV runtime initialization correction before CSRT tracking validation

---

# CURRENT TASK

## User Request

Review v0.4.4 diagnostic ZIPs after the guided test failed during target selection/initialization, and improve the project so CSRT can actually initialize before further tracking-quality work.

## Diagnostic Findings

Both uploaded v0.4.4 ZIPs are duplicate exports of the same session:
- session ID: b872b16e-a4fc-48ea-927d-92ab618baaa1
- test ID: target_tracking_v4_3

Results:
- Open media PASS
- Landscape preservation PASS
- Select target FAIL after the tester stopped the step
- No TRACK_TARGET_CONFIRMED event occurred
- Seven TARGET_INITIALIZATION_FAILED events occurred
- All failures used reason=invalid_target_or_capture
- The selected target boxes were not too small. Examples ranged from approximately 34x47 to 110x148 analysis pixels, all above the tracker minimum.
- Because initialize() can return null before frame conversion only when OpenCV runtime loading fails (or when target dimensions are invalid, which diagnostics rule out), the leading hypothesis is OpenCVLoader.initLocal() returning false with the contrib AAR on this device/runtime.

## Goal

Create v0.4.5 as a targeted OpenCV runtime-initialization fix. Do not change CSRT tracking behavior, zoom-assisted selection, coordinate mapping or the v0.4.3 guided-test contract until the tracker can initialize successfully.

## Implementation Plan

1. Start from exact v0.4.4 source.
2. Preserve CSRT tracking/appearance/template/jump logic unchanged.
3. Replace the single OpenCVLoader.initLocal() gate with a staged loader:
   - try OpenCVLoader.initLocal()
   - verify the native runtime with Core.getVersionString()
   - if initLocal fails or runtime verification fails, try System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
   - verify again with Core.getVersionString()
4. Record detailed initialization diagnostics in OpenCvCsrtTracker: load method, stage, OpenCV version, exception type/message/stack where applicable.
5. On TARGET_INITIALIZATION_FAILED, include those exact diagnostics plus frame dimensions and selected target pixel dimensions instead of generic invalid_target_or_capture only.
6. Add a positive OPENCV_RUNTIME_READY diagnostic when initialization succeeds.
7. Increase version to v0.4.5 / versionCode 10.
8. Keep guided test ID target_tracking_v4_3 because behavior under test is unchanged.
9. Package Android Studio ZIP and require compile/device retest.
10. Keep v0.3.1 as last physical-device known-good baseline.

## Files Expected to Change

- OpenCvCsrtTracker.kt
- DvrPlayerApp.kt diagnostics around target initialization
- app/build.gradle.kts version
- README.md/version labels
- PROJECT_MEMORY.md
- TESTING_DIAGNOSTICS.md
- v0.4.4 failure report
- v0.4.5 source candidate report

## Files That Should NOT Be Changed

- CSRT tracking algorithm behavior
- zoom/source-coordinate mapping
- guided-test PASS/FAIL thresholds
- playback/DVR controls
- v0.3.1 known-good files

---

# WORK IN PROGRESS

- v0.4.5 OpenCV runtime-loader correction implemented; Android Studio ZIP packaging/verification is the current handoff step.
- v0.4.5 tries OpenCVLoader.initLocal(), then System.loadLibrary(Core.NATIVE_LIBRARY_NAME), and verifies with Core.getVersionString().
- Initialization failures now identify the exact stage and exception instead of generic invalid_target_or_capture.

- v0.4.4 device diagnostics isolated failure to target initialization before CSRT tracking began.
- Seven valid-sized target selections all failed initialization; no tracking samples were produced.
- v0.4.5 will add robust OpenCV native loading plus exact runtime-init diagnostics before changing tracker behavior.

- v0.4.3 Android Studio compile failed because the official OpenCV AAR does not include `org.opencv.tracking.TrackerCSRT`.
- v0.4.4 keeps the CSRT/zoom-assisted tracking source unchanged and swaps to the maintained contrib Android AAR that includes the tracking module.
- v0.4.4 is arm64-v8a at runtime, matching the current Samsung phone test device.

- v0.4.3 Android Studio-ready ZIP packaged.
- v0.4.3 replaces MIL with CSRT, increases analysis to 640x360, and adds zoom-assisted target selection mapped back to source coordinates.
- v0.4.3 adds finer inner-box color appearance and fixed grayscale-template similarity diagnostics.
- Local source/contract checks passed; full local Gradle build remains unavailable because services.gradle.org cannot be resolved.

- v0.4.2 device diagnostics confirmed stable wrong-target/background lock despite teleport rejection.
- v0.4.2 landscape preservation passed.
- v0.4.3 will replace MIL with CSRT and add zoom-assisted target selection mapped back to original video coordinates.

- v0.4.1 Android Studio compile reached Kotlin compilation and failed only because `GuidedTestController.summaryText()` was missing.
- v0.4.2 restores the prior proven diagnostic summary helper and is packaged as the current Android Studio candidate; no tracking/orientation feature behavior was changed.

- New roadmap idea recorded: non-destructive video image adjustment panel with brightness/exposure, contrast, sharpness, saturation/color, hue, gamma, temperature/tint, highlights/shadows, Reset, Original/Adjusted comparison, and diagnostic logging. This is future work and does not change v0.4.1.

- v0.4.0 physical-device diagnostics confirmed tracker drift/jumps in two independent sessions.
- v0.4.0 movement Test 3 was a false positive; it counted tracker drift as successful movement.
- v0.4.0 TRACK LOST happened only after the tester had already stopped the failed test.
- v0.4.0 landscape rotation reset/disappeared the player because Activity configuration changes were not preserved.
- v0.4.1 replaces the tracker with OpenCV MIL, rejects teleports, adds appearance validation, requires tester confirmation, and preserves state during rotation.
- v0.4.1 Android Studio-ready ZIP packaged; Android Studio compile and physical-device target_tracking_v4_1 test are pending.

- v0.4.0 Android Studio-ready ZIP packaged.
- v0.4.0 adds manual target selection, blue tracking overlay, lightweight local TextureView template tracking, confidence diagnostics and explicit TRACK LOST behavior.
- Local tracker synthetic-motion test succeeded at approximately 0.87 confidence.
- Full Android Studio compile and physical-device target_tracking_v4 test are pending.

- v0.3.1 physical-device guided test PASS is now the known-good recovery baseline.
- v0.4.0 manual target-selection/basic tracking implementation started.
- Tracking will use the existing TextureView for local displayed-frame capture at reduced resolution; no cloud dependency is required.

- v0.3.0 device diagnostics failed specifically at forward held-frame stepping.
- v0.3.0 passed Open, Play, and single forward-frame steps before the failure.
- v0.3.1 corrects frame-boundary timestamps and uses an independent held-frame cursor.
- v0.3.1 adds MediaExtractor frame-rate detection before fallback_30fps.
- 2x/4x and forward/reverse DVR scan remain pending because the guided test did not reach those steps.

- v0.2.0 physical-device guided test PASS is now the known-good recovery baseline.
- v0.3.0 local source candidate adds frame counter, press-and-hold frame stepping, scrollable controls/diagnostics, 2x/4x playback and 8x-64x forward/reverse DVR scan.
- v0.3.0 Android Studio-ready ZIP packaged; Android Studio compile and physical-device `dvr_review_v3` test are pending.

- v0.2.0 started from the confirmed v0.1.0 Android Studio project.
- v0.1.0 remains the recovery baseline and must not be overwritten.
- v0.2.0 Android Studio source candidate packaged with live scrubbing, zoom/pan, Reset Zoom, approximate frame stepping, slow speeds, and inspection_controls_v2 diagnostics.
- Local XML/manifest and syntax-oriented checks passed; full Gradle compile is pending because the local environment lacks Android SDK/network dependency access.
- Android Studio ZIP handoff prepared for user compilation and physical-device guided testing.
- First v0.2.0 Android Studio compile reached Kotlin compilation and exposed only a wrong `clipToBounds` import; corrected ZIP generated.

- GitHub slot search completed.
- Slot-8 is occupied.
- Slot-9 is the next empty repository and has been selected.
- Project memory/instructions are being initialized.
- DVR player roadmap is being initialized.
- Initial planning files were approved.
- New requirement: guided self-verifying tests and downloadable diagnostics for every feature.
- Testing/diagnostic architecture is being added before the first playback build.
- Android v0.1.0 source initialization is now authorized.
- First batch source-write attempt failed before any source files were written due to orchestration quoting; failure recorded in Error Log and implementation method changed.
- Android project foundation, Media3 playback screen, diagnostic event logger, device/media collectors, guided-test controller, crash persistence, ZIP exporter and CI workflow are committed.
- GitHub CI run 9 compiled successfully after removing an incompatible explicit Compose weight import.
- GitHub CI run 10 compiled successfully and published the debug APK artifact `DVR-Video-Player-v0.1.0-debug`.
- User later confirmed the GitHub-built v0.1.0 APK installed and worked on the physical Android phone.
- Baseline guided diagnostic ZIPs were uploaded and reviewed on 2026-09-24. Open Video, Play, Pause, and Seek all PASSed with no recorded errors or crashes.

---

# CURRENT KNOWN GOOD STATE

**Version:** v0.3.1  
**Status:** Physical-device guided test PASS  
**Confirmed:** v0.1/v0.2 baseline behavior plus frame counter, press-and-hold frame stepping, scrollable controls/diagnostics, 2x/4x playback, and 16x forward/reverse DVR scanning.  
**Latest confirmed diagnostic session:** `dvr_review_v3` PASS on Samsung SM-S908U1 / Android 16.  
**Measured scan rates:** approximately 15.99x forward and 15.77x reverse at requested 16x.  
**Frame-rate detection:** MediaExtractor reported 23 fps for the tested 4K HEVC MKV.

v0.3.1 remains the recovery point until v0.4.2 target tracking/orientation passes its physical-device guided test.

---

# VERSIONING RULES

Use semantic-style development versions:

- v0.1.0 = first usable playback foundation
- v0.2.0 = substantial feature addition
- v0.2.1 = bug-fix iteration
- etc.

Do not increase the version merely because code was edited. Increase it when a testable build/checkpoint is produced.

---

# TEST REPORT RULES

For each testable version, maintain a test report, for example:

`test_reports/DVR_Player_v0.1.0_test_report.txt`

Each report should record:

- version/build,
- date,
- test device,
- Android version,
- sample video/file information,
- features tested,
- expected result,
- actual result,
- PASS / FAIL / PARTIAL,
- crashes/errors/logs,
- playback/seek/tracking observations,
- regressions,
- next recommended action.

A feature is not **DONE** merely because code exists. DONE means it has been successfully tested.

---

# FEATURE STATUS WORDS

Use only these status labels in the roadmap:

- PLANNED
- IN PROGRESS
- PARTIAL
- WORKING
- DONE
- BLOCKED
- FAILED
- DEFERRED

---

# ERROR LOG

## Error — v0.4.3 CSRT package missing from official OpenCV AAR

- Date: 2026-09-26
- Version: v0.4.3
- Feature: OpenCV CSRT target tracker
- Exact symptom/error: Android Studio reached `:app:compileDebugKotlin` and reported unresolved package `org.opencv.tracking`, unresolved `TrackerCSRT`, and follow-on unresolved `update` / `trackingScore` references.
- Diagnostic evidence: Android resources, manifest processing, native OpenCV packaging and dependency processing completed before Kotlin compilation failed.
- Root cause: v0.4.3 still depended on `org.opencv:opencv:5.0.0.1`. The official OpenCV Android Maven AAR includes core/video modules but does not ship the contrib `tracking` module containing CSRT/KCF.
- Fix in v0.4.4: replace the official AAR dependency with `dev.ffmpegkit-maintained:opencv-contrib-android:5.0.0`, whose maintained Android package explicitly includes OpenCV core plus the contrib tracking module (CSRT/KCF/MOSSE). Preserve the v0.4.3 CSRT source and tracking behavior unchanged.
- Compatibility note: the free contrib AAR is arm64-v8a. This matches the current Samsung physical test device; x86/x86_64 emulator runtime is not supported by that free artifact.
- Validation: no stale official OpenCV Gradle dependency remains; XML/source structure checks pass; OpenCvCsrtTracker compiles against the documented CSRT Java contract using local stubs. Full local Gradle resolution is unavailable because this environment cannot reach Maven/Gradle services.
- Result: v0.4.4 Android Studio candidate prepared for compile/device test.

## Error — v0.4.1 missing GuidedTestController.summaryText

- Date: 2026-09-26
- Version: v0.4.1
- Feature: diagnostic ZIP summary generation
- Exact symptom/error: Android Studio `:app:compileDebugKotlin` failed in `DiagnosticExporter.kt` with unresolved reference `summaryText`.
- Diagnostic evidence: resources, manifest processing, native/OpenCV packaging and dependency processing completed; Kotlin compile then stopped at the call `testController.summaryText()`.
- Root cause: the v0.4.1 rewrite of `GuidedTestController.kt` accidentally omitted the existing `summaryText()` helper while `DiagnosticExporter.kt` still depended on it.
- Fix in v0.4.2: restore the exact proven `summaryText()` implementation used by earlier versions; preserve v0.4.1 tracking/orientation behavior unchanged.
- Validation: GuidedTestController + DiagnosticExporter compile together under a local stubbed Kotlin contract check; Android XML parses; ZIP integrity passes. Full Gradle build remains unavailable locally because services.gradle.org cannot be resolved.
- Result: v0.4.2 Android Studio candidate packaged for Android Studio compile and device test.

## Error — v0.3.0 forward held-frame timestamp stall

- Date: 2026-09-25
- Version: v0.3.0
- Feature: press-and-hold forward frame stepping
- Exact symptom: hold input generated repeated frame-step requests, but forward frame position did not advance.
- Diagnostic evidence: start frame 234, end frame 234, repeatRequests=6, advancedFrames=0. Repeated target frame 235 mapped to the same 7,833 ms timestamp already occupied by the player.
- Root cause: rounded frame-boundary timestamp calculation could map the next frame to the current millisecond; repeated hold logic also recalculated from player.currentPosition, so asynchronous seek state could cause repeated identical requests.
- Fix in v0.3.1: use CEIL-based target-frame timestamps and an independent logical frame cursor during held stepping.
- Additional improvement: add MediaExtractor / MediaFormat.KEY_FRAME_RATE lookup before the 30 fps fallback.
- Result: v0.3.1 Android Studio candidate prepared for retest.
- Important: 2x/4x and DVR scan steps were not reached, so they remain UNVALIDATED rather than failed.

## Error — v0.2.0 clipToBounds import

- Date: 2026-09-24
- Version: v0.2.0 development
- Feature: Zoom/pan video viewport clipping
- Exact symptom/error: Android Studio `:app:compileDebugKotlin` reported unresolved reference `clipToBounds` at the import and modifier call in `DvrPlayerApp.kt`.
- Cause: `clipToBounds` was imported from `androidx.compose.foundation`; the modifier belongs to `androidx.compose.ui.draw`.
- Fix: changed the import to `androidx.compose.ui.draw.clipToBounds`.
- Result: corrected v0.2.0 Android Studio ZIP generated; Android Studio rebuild is the next verification.
- Lesson: preserve the Compose UI draw import for `clipToBounds` in this dependency set.

## Error — CI environment/setup sequence

- Date: 2026-09-23
- Version: v0.1.0 development
- Feature: Android CI
- Failures encountered:
  1. `android-actions/setup-android@v3` attempted to install obsolete SDK package `tools`.
  2. After removing that action, `sdkmanager` was not on PATH.
  3. After using the explicit SDK-manager path, `platforms;android-37` was unavailable from the runner's configured repository.
- Resolution:
  - Use the runner's existing cmdline-tools path directly.
  - Build the first app checkpoint against Android API 36.
  - Use an API-36-compatible Compose dependency set.
- Result: CI reached the actual Gradle/Kotlin compile stage.
- Lesson: do not restore the failed setup-android/Android-37 CI configuration without new evidence.

## Error — Compose RowScope weight import

- Date: 2026-09-23
- Version: v0.1.0 development
- Feature: Player UI compile
- Exact symptom/error: `Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.`
- Cause: explicit `androidx.compose.foundation.layout.weight` import was incompatible with the selected Compose set.
- Fix: removed the explicit import and let `Modifier.weight()` resolve from RowScope.
- Result: GitHub CI run 9 passed; run 10 also passed and produced the APK.
- Lesson: preserve this fix when refactoring Compose layout imports.

## Error — source batch generation quoting failure
- Date: 2026-09-23
- Version: v0.1.0 development
- Feature: Initial source creation
- Exact symptom/error: JavaScript orchestration parser reported "SyntaxError: Unexpected token ':'" before any GitHub source-file writes executed.
- Cause: Kotlin source contained \${...} string templates inside JavaScript template literals, causing the orchestration script to parse Kotlin interpolation as JavaScript.
- Result: No Android source files were created by the failed batch; documentation checkpoint remained intact.
- Follow-up: Stop using a single large JavaScript template-literal batch. Create source files using interpolation-safe encoded strings/smaller atomic commits and verify after creation.

When an important error occurs, record:

## Error
- Date:
- Version:
- Feature:
- Exact symptom/error:
- Relevant log:
- Suspected cause:
- Fix attempted:
- Result:
- Follow-up:

---

# FAILED APPROACHES

None yet.

Failed approaches stay recorded so they are not accidentally repeated later.

For each failed approach record:

- Problem:
- Approach:
- Why it was chosen:
- Result:
- Why it failed:
- What was learned:
- Whether it could be useful later:

---

# DESIGN DECISIONS

## D1 — Separate DVR player app

The DVR Video Player is a separate Android project rather than another mode inside FFmpeg Studio.

**Reason:** Keep playback/analysis responsive and avoid overloading the encoder application.

## D2 — Preserve aspect ratio during zoom

Pinch zoom and pan must never stretch the video independently on X/Y axes.

**Reason:** Zoom should crop/magnify, not distort evidence.

## D3 — Extreme speeds use DVR-style scanning when necessary

Normal playback can be used where practical. Very high rates such as 8x, 16x, 32x and 64x may require frame skipping / rapid seeking rather than trying to decode every frame.

## D4 — Original video is read-only by default

Tracking, target lock, enhancement and frame stacking are analysis/view operations. The original media must not be modified.

Exports are new files only.

## D5 — Tracking and alignment precede stacking

Frame stacking depends on reliable target tracking and frame alignment. Build those capabilities first.

## D6 — Guided tests are mandatory for new features

Every user-facing feature must have a guided test procedure. The app must tell the tester exactly what to do and record objective internal signals that indicate whether the requested behavior occurred.

A test must not rely only on the tester saying "it worked."

## D7 — Diagnostics are structured and exportable

The app records user actions, relevant player callbacks, timestamps, positions, errors, device/app information and media information. Test results and logs must be exportable as a ZIP package the user can upload for analysis.

Core diagnostics remain local/offline and must not upload automatically.

## D8 — Testing code is reusable infrastructure

Guided tests and diagnostic logging are not temporary debug code. They are a permanent subsystem that future playback, zoom, DVR scan, tracking, Target Lock and frame-stacking tests will reuse.


---

# ARCHITECTURE RECORD

Initial proposed module direction:

```
app/
  player/
    playback
    timeline
    zoom_pan
    dvr_scan
  tracking/
    manual_target_selection
    object_tracker
    target_lock
  enhancement/
    frame_extractor
    frame_alignment
    best_frame
    frame_stacking
  export/
    screenshots
    enhanced_stills
    stabilized_clips
  diagnostics/
    event_logger
    device_info
    media_info
    diagnostic_export
  testing/
    guided_test_controller
    test_definitions
    test_results
```

This is a planning map, not a requirement to create every file immediately.

Update this section when the actual architecture is established.

---

# NEXT STEPS

1. User opens the v0.4.5 Android Studio ZIP and compiles it.
2. If it builds, install/run on the arm64 Samsung phone.
3. Run **v0.4.5 Test** / `target_tracking_v4_3`.
4. Open media and verify landscape preservation.
5. Pause on a sharp frame, zoom/pan if useful, draw a tight target box and Confirm Target.
6. If initialization succeeds, diagnostics must include `OPENCV_RUNTIME_READY` with load method and OpenCV version.
7. If initialization fails, export immediately; `TARGET_INITIALIZATION_FAILED` now includes exact stage, runtime load method, exception type/message/stack and target pixel size.
8. Only after CSRT initializes should tracking quality be judged with **Tracking Looks Correct / Tracking Is Wrong**.
9. Export/upload the diagnostic ZIP whether PASS or FAIL.
10. Keep v0.3.1 as the known-good recovery baseline until v0.4.5 passes.


---

# END-OF-WORK PROCEDURE

Before stopping work for any reason:

1. Save all changed source files.
2. Update **Current Task**.
3. Update **Work in Progress**.
4. Record successful work.
5. Record failures and errors.
6. Update roadmap feature statuses.
7. Update **Current Known Good State** if applicable.
8. Update **Next Steps**.
9. Save a checkpoint/commit when appropriate.
10. Make sure another developer could continue without the current chat.

---

# RECOVERY PROCEDURE

If development becomes confused:

1. Stop changing code.
2. Read this entire file.
3. Read `DVR_PLAYER_ROADMAP.md`.
4. Find **Current Known Good State**.
5. Read **Current Task**.
6. Read **Failed Approaches** and **Error Log**.
7. Inspect the current source.
8. Compare against the last working checkpoint.
9. Identify the exact unfinished step.
10. Write a new plan before continuing.

Do not guess your way out of a confused project state.

---

# CONTEXT-LOSS RULE

Assume chat context can disappear at any time.

Development must be recoverable from:

1. Source code.
2. `PROJECT_MEMORY.md`.
3. `DVR_PLAYER_ROADMAP.md`.
4. `TESTING_DIAGNOSTICS.md`.
5. Saved test and diagnostic reports.
6. Git/version checkpoints.

If losing the conversation would make the next action unclear, the project documentation is not current enough.

---

# FINAL PRINCIPLE

**Read first. Plan before coding. Save constantly. Protect working code. Test small changes. Record failures. Break loops deliberately. Leave a recoverable checkpoint.**
