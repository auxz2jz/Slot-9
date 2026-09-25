# TESTING_DIAGNOSTICS.md

## Purpose

This file defines the permanent guided-testing and diagnostic system for the Android DVR Video Player.

Read order for every development session:

1. `PROJECT_MEMORY.md`
2. `DVR_PLAYER_ROADMAP.md`
3. `TESTING_DIAGNOSTICS.md`
4. Relevant source files
5. Latest test/diagnostic report

Testing and diagnostics are part of the product architecture, not temporary debug code.

---

# CORE RULE

Whenever a user-facing feature is added or changed, the same development cycle must also define:

1. What the user is told to do.
2. What internal signal proves the action occurred.
3. What data must be logged if it fails.
4. What constitutes PASS / FAIL / PARTIAL.
5. What gets included in the downloadable diagnostic package.

A feature is not DONE until its guided test has been run successfully on a real device.

---

# GUIDED TEST EXPERIENCE

The app must include an on-screen **Guided Test** mode.

A guided test should:

- show one clear instruction at a time,
- identify the control or gesture to use,
- show progress such as Step 2 of 4,
- automatically start a diagnostic session,
- listen for the expected internal signal,
- advance automatically when success is confirmed,
- show a useful waiting/failure message when the expected state does not occur,
- allow the user to cancel,
- preserve partial results,
- offer **Export Diagnostic ZIP** at completion or failure.

The tester should not need to remember diagnostic details or manually explain ordinary state changes.

---

# SELF-VERIFYING TEST PRINCIPLE

Tests should distinguish between:

**Input happened**
- user pressed Play

and

**Feature actually worked**
- player callback reported `isPlaying = true`

Both events are recorded.

Example failure:

- UI event: PLAY_BUTTON_PRESSED
- requested state: PLAY
- actual player callback: never became isPlaying=true
- result: FAIL / timed out
- report: identifies the gap automatically

This is preferred over relying on "I pressed it but nothing happened."

---

# EVENT LOG FORMAT

Primary machine-readable log:

`events.jsonl`

One JSON object per line.

Recommended fields:

```json
{
  "sessionId": "...",
  "sequence": 42,
  "timestampUtc": "...",
  "elapsedMs": 15322,
  "category": "UI_ACTION",
  "event": "PLAY_BUTTON_PRESSED",
  "screen": "PLAYER",
  "mediaPositionMs": 12044,
  "details": {
    "requestedAction": "play"
  }
}
```

Required characteristics:

- monotonically increasing sequence number,
- UTC wall-clock timestamp,
- elapsed time since session start,
- current media position when meaningful,
- category,
- event name,
- structured details,
- no sensitive unrelated personal data.

---

# EVENT CATEGORIES

Initial categories:

- SESSION
- APP
- DEVICE
- MEDIA
- UI_ACTION
- GESTURE
- PLAYER
- SEEK
- SPEED
- TEST
- DIAGNOSTIC
- ERROR

Future categories:

- ZOOM
- PAN
- DVR_SCAN
- TRACKING
- TARGET_LOCK
- FRAME_EXTRACTION
- FRAME_ALIGNMENT
- FRAME_STACK
- EXPORT

---

# BASELINE v0.1.x GUIDED TEST

## Test ID

`baseline_playback_seek_v1`

## Purpose

Verify the minimum playback foundation without relying on manual description.

## Step 1 — Open video

Instruction:

**Tap Open Video and choose a local video file.**

Expected objective signal:

- media selection succeeds,
- Media3 receives/prepares the item,
- player becomes ready or usable.

Record:

- picker requested,
- picker result,
- media display name/MIME where available,
- media metadata,
- player state changes,
- preparation errors.

PASS when:
- media is opened and player reports a valid prepared/ready state.

---

## Step 2 — Play

Instruction:

**Tap Play and let the video run for at least 2 seconds.**

Record:
- play button press,
- playWhenReady request,
- isPlaying callback,
- position samples.

PASS when:
- `isPlaying=true`,
- media position advances by at least the minimum threshold.

FAIL if:
- button was pressed but playback never begins,
- fatal playback error occurs.

---

## Step 3 — Pause

Instruction:

**Tap Pause.**

Record:
- pause button press,
- player callback,
- position before/after.

PASS when:
- player reports `isPlaying=false`,
- position stops advancing beyond tolerance.

---

## Step 4 — Seek

Instruction:

**Drag the timeline to a noticeably different point and release it.**

Record:
- drag start,
- requested target position,
- drag updates sampled/throttled,
- release,
- seek request,
- position discontinuity,
- resulting player position,
- seek latency.

PASS when:
- requested position differs sufficiently from starting position,
- player lands within an acceptable tolerance of the requested target.

---

## Completion

Display:
- overall PASS / FAIL / PARTIAL,
- each step result,
- Export Diagnostic ZIP button.

---

# DEVICE / APP INFORMATION

Collect at diagnostic-session start:

- app name,
- version name,
- version code,
- build type if available,
- Android release,
- API level,
- manufacturer,
- model,
- device/product,
- supported ABIs,
- available processors,
- memory class,
- low-memory class flag,
- display width/height,
- density/density DPI,
- locale,
- session start time.

Optional future:
- thermal status,
- battery state,
- decoder capabilities.

Do not collect identifiers such as IMEI, phone number, contacts, accounts, precise location, advertising ID, or unrelated device data.

---

# MEDIA INFORMATION

For selected media, record where available:

- display name,
- MIME type,
- content URI scheme/type but avoid exposing unnecessary private path data,
- file size,
- duration,
- width,
- height,
- rotation,
- frame rate,
- bitrate,
- video MIME/codec,
- audio MIME/codec,
- track count and selected tracks,
- Media3-reported video size,
- playback error details.

The diagnostic ZIP must not contain the video itself unless a separate future feature explicitly asks the user to include a sample.

---

# PLAYER STATE DIAGNOSTICS

Record relevant Media3/Player callbacks:

- playback state,
- isPlaying,
- playWhenReady,
- playback parameters/speed,
- media transition,
- timeline changes where useful,
- video size,
- rendered first frame,
- position discontinuity/seek,
- player error.

During an active guided test, sample player state periodically at a reasonable interval, such as:

- media position,
- buffered position,
- duration,
- isPlaying,
- speed.

Avoid excessive logging that would itself harm playback.

---

# UI / CONTROL DIAGNOSTICS

Every important player control should log:

1. user input,
2. requested state/action,
3. resulting internal state callback.

Examples:

- OPEN_VIDEO_PRESSED
- PLAY_BUTTON_PRESSED
- PAUSE_BUTTON_PRESSED
- SEEK_DRAG_STARTED
- SEEK_DRAG_UPDATED
- SEEK_RELEASED
- SPEED_SELECTED
- RESET_ZOOM_PRESSED
- TRACK_TARGET_CONFIRMED

For continuous gestures, throttle/summarize updates so logs remain manageable.

---

# FUTURE FEATURE TEST CONTRACTS

## Pinch Zoom

Instruction:
**Pinch outward until the zoom indicator reaches at least 2.0x.**

Record:
- gesture begin/end,
- scale factors,
- internal zoom value,
- X/Y transform scale,
- viewport.

PASS when:
- zoom >= threshold,
- X and Y scale remain equal within tolerance,
- player remains alive.

---

## Pan

Instruction:
**While zoomed, drag the video to move the view.**

Record:
- gesture delta,
- offset before/after,
- zoom scale,
- player state.

PASS when:
- viewport offset changes meaningfully,
- zoom remains active,
- no seek is accidentally triggered.

---

## Playback Speed

Instruction:
**Select 2x and play for at least 2 seconds.**

Record:
- selected requested speed,
- actual player playback parameters,
- media-position delta vs wall-clock delta.

PASS when:
- player reports approximately requested speed,
- effective progression is consistent within practical tolerance.

---

## DVR Forward Scan

Instruction:
**Run 16x forward scan for 3 seconds, then stop.**

Record:
- requested scan rate,
- start media position,
- end media position,
- elapsed real time,
- number/timing of scan seeks or decoded updates,
- errors/stalls.

PASS when:
- media advances substantially faster than real time,
- UI remains responsive,
- scan stops when requested.

---

## DVR Reverse Scan

Equivalent to forward scan but requires media position to decrease at expected effective rate.

---

## Object Tracking

Instruction:
**Draw a box around the requested object and let the video play while it moves.**

Record sampled:
- frame/media timestamp,
- bounding box X/Y/W/H,
- tracker confidence,
- target-lost state,
- reacquisition events,
- processing time.

PASS when:
- tracker initializes,
- boxes update coherently,
- confidence remains above configured minimum for required interval,
- or target-loss is explicitly reported instead of silently tracking the wrong object.

---

## Target Lock

Record:
- target center,
- desired lock point,
- displayed target center after transform,
- lock error in pixels/normalized coordinates,
- scale/rotation transforms,
- lost-target state.

PASS when:
- target remains within defined lock tolerance for required portion of the test.

---

## Best Frame

Record:
- search interval,
- candidate timestamps,
- quality scores,
- selected best frame,
- rejected candidates/reasons.

PASS when:
- pipeline completes,
- selected result maps to a real decoded source frame,
- no synthetic content is substituted.

---

## Frame Alignment

Record:
- source frame timestamps,
- reference frame,
- transform model,
- alignment score/error,
- rejected frames.

PASS when:
- accepted aligned frames fall within configured alignment tolerance.

---

## Frame Stacking

Record:
- source timestamps,
- accepted/rejected frames,
- stacking method,
- per-frame weights for weighted mode,
- output dimensions,
- processing time,
- error state.

PASS when:
- requested method completes,
- output is generated from actual source frames,
- no silent fallback occurs.

---

# DIAGNOSTIC PACKAGE

Default filename pattern:

`DVR_Player_Diagnostics_<version>_<UTC timestamp>.zip`

Contents:

```
README.txt
summary.txt
device_app_info.txt
media_info.txt
events.jsonl
guided_test_results.json
errors.txt        (if errors occurred)
```

Future optional contents:
- screenshots explicitly created by the diagnostic system,
- tracker CSV/JSON,
- performance samples,
- alignment statistics,
- frame-stack manifest.

Do not include source video by default.

---

# SUMMARY FILE

The summary should make diagnosis possible without reading thousands of events first.

Include:

- app version,
- session ID,
- start/end time,
- test ID,
- overall result,
- each step result and duration,
- first failure point,
- important player errors,
- media basics,
- device basics,
- final player state,
- log file names.

---

# ERROR RECORDING

On caught errors/exceptions, record:

- exception type,
- message,
- stack trace where available,
- current screen,
- active test/step,
- media position,
- player state,
- most recent important action.

For fatal app crashes that cannot be captured in-process, future iterations may add an uncaught-exception crash file that is imported into the next diagnostic package.

---

# PRIVACY / SAFETY RULES

1. Diagnostics remain local until the user explicitly exports/shares them.
2. No automatic cloud upload.
3. Do not collect precise location.
4. Do not collect contacts, account names, phone number, device identifiers or unrelated personal content.
5. Do not copy the original body-camera video into reports.
6. Media filenames may be useful for debugging; if future privacy concerns arise, add a redact-filename option.
7. Diagnostic screenshots, if added, must be explicit and clearly indicated because video frames may contain sensitive footage.

---

# DEVELOPMENT REQUIREMENT

For every new feature commit/checkpoint:

- update the roadmap status,
- define/update its guided test,
- define diagnostic signals,
- define PASS criteria,
- define failure data,
- build,
- run the test,
- export results,
- analyze results,
- only then mark the feature DONE.

---

# CURRENT STATUS

**Date:** 2026-09-23  
**Version:** v0.4.1 development  
**Status:** v0.4.0 failed physical-device tracking/orientation validation; v0.4.1 corrective candidate awaiting Android Studio/device validation.

Implemented in v0.1.0 source:
- session-specific JSONL event logger,
- button/action and Media3 callback logging,
- device/app collector,
- media metadata collector,
- baseline self-verifying guided test,
- periodic player-state samples during tests,
- local ZIP exporter,
- persisted uncaught-crash record plus crashed-session event trail,
- on-screen test instructions and PASS/FAIL state,
- CI compile verification.

Known-good source build commit:
`9d9f560ff49d2798f7b80d4afaa71e233cfafe89`

Next validation:
Run `baseline_playback_seek_v1` on the physical device and export the ZIP regardless of PASS or FAIL.


---

# v0.2.0 INSPECTION CONTROLS GUIDED TEST

## Test ID

`inspection_controls_v2`

The v0.2.0 guided test performs a baseline regression plus the new inspection controls:

1. Open video — Media3 reaches READY.
2. Play — `isPlaying=true` and media position advances.
3. Pause — `isPlaying=false`.
4. Live scrub — preview seek is requested and a frame renders while the slider is still being dragged.
5. Pinch zoom — uniform X/Y scale reaches at least 2.0x.
6. Pan — viewport translation changes by at least 40 px while zoom remains active.
7. Reset Zoom — zoom returns to 1.0x and X/Y offsets return to zero.
8. Next frame — estimated one-frame forward timestamp is requested and Media3 confirms the new position.
9. Previous frame — estimated one-frame backward timestamp is requested and Media3 confirms the new position.
10. 0.50x playback — Media3 reports 0.50x and media position advances during slow playback.

New diagnostic signals:
- `ZOOM / VIDEO_TRANSFORM_CHANGED`
- `ZOOM / RESET_ZOOM_PRESSED`
- `SEEK / SEEK_SCRUB_PREVIEW_REQUESTED`
- `SEEK / SCRUB_PREVIEW_FRAME_RENDERED`
- `FRAME_STEP / NEXT_FRAME_PRESSED`
- `FRAME_STEP / PREVIOUS_FRAME_PRESSED`
- `SPEED / SPEED_SELECTED`
- Media3 playback-parameter, rendered-frame and position-discontinuity callbacks.

Frame stepping in v0.2.0 is best-effort: source capture frame rate is used when available, otherwise a 30 fps fallback interval is used. Exact decoded-frame stepping for long-GOP media may require a deeper decoder/index implementation later.


---

# v0.3.0 DVR REVIEW GUIDED TEST

## Test ID

`dvr_review_v3`

Purpose: verify the new frame-navigation and high-speed review controls while regressing core playback.

Guided steps:

1. **Open media** — Media3 reaches READY.
2. **Play** — playback starts and position advances.
3. **Single frame** — tap Frame > once; requested frame/timestamp advances and Media3 confirms the new position.
4. **Held frame advance** — press and hold Frame > long enough for repeated steps; diagnostics require at least 3 repeat requests and advancement of at least 3 estimated frames.
5. **2x playback** — select 2x and play; Media3 must report approximately 2.0x and media position must advance faster than real time.
6. **4x playback** — select 4x; Media3 must report approximately 4.0x and progression must be consistent with accelerated playback.
7. **16x DVR forward scan** — run forward scan long enough for measurable media movement, then Stop Scan; diagnostics verify positive media delta and high effective rate.
8. **16x DVR reverse scan** — run reverse scan and stop; diagnostics verify negative media delta and high effective rate.

New diagnostic signals include:
- `FRAME_STEP / FRAME_HOLD_STARTED`
- `FRAME_STEP / FRAME_HOLD_ENDED`
- repeated frame-step request counts
- detected frame rate and source
- estimated current/target frame numbers
- `DVR_SCAN / SCAN_STARTED`
- sampled `DVR_SCAN / SCAN_TICK`
- `DVR_SCAN / SCAN_FINISHED`
- requested scan direction/rate
- start/end media positions
- real elapsed time
- effective scan rate
- Media3 playback speed callbacks for 2x/4x.

## Frame Counter / Frame Rate Rules

v0.3 displays an estimated current/total frame counter using:
1. selected Media3 video-track `Format.frameRate` when available,
2. existing media metadata frame rate when available,
3. 30 fps fallback only when no reliable reported rate is available.

The counter is timestamp/frame-rate derived. Variable-frame-rate media and long-GOP codecs may not map perfectly to exact decoded-frame ordinal positions. Exact forensic frame indexing may require a future decoder/index pass.

## Scrollability Test

The controls/test/diagnostic area below the video is vertically scrollable so later controls remain reachable. This is a usability requirement rather than a numeric guided-test PASS condition; any inability to reach lower controls should be reported and logged as a UI regression.


## v0.3.1 Held-Frame Retest Note

The v0.3.0 diagnostic session proved the hold gesture generated repeated requests but forward stepping stalled because the next target frame could map to the same millisecond timestamp as the current frame. v0.3.1 changes the timestamp calculation so a target frame maps to the first millisecond inside that frame and maintains an independent frame cursor during held stepping.

The existing `dvr_review_v3` sequence remains the required test. Do not skip the held-frame step. The 2x, 4x, forward scan and reverse scan steps remain unvalidated until the sequence progresses beyond the repaired hold-frame step.


---

# v0.4.0 TARGET TRACKING GUIDED TEST

## Test ID

`target_tracking_v4`

Guided steps:

1. **Open media** — Media3 reaches READY.
2. **Select target** — tester enters Select Target mode, drags a box, and Confirm Target successfully initializes the tracker.
3. **Track movement** — while playing, tracker must produce at least 6 confident samples and accumulated normalized box movement of at least 0.015.
4. **TRACK LOST** — tester seeks to a substantially different part where the selected object is absent; three consecutive low-confidence matches must cause an explicit `TRACK_LOST` event instead of continuing a false track.

Diagnostic signals:
- `TRACKING / TARGET_SELECTION_STARTED`
- `TRACKING / TARGET_BOX_DRAWN`
- `TRACKING / TRACK_TARGET_CONFIRMED`
- `TRACKING / TRACK_SAMPLE` with normalized box coordinates, confidence, movement and processing time
- `TRACKING / TRACK_LOW_CONFIDENCE`
- `TRACKING / TRACK_LOST`
- guided test PASS/FAIL events for selection, movement and lost-target behavior.

Initial tracker implementation uses reduced-resolution TextureView frame capture and a local sampled grayscale template search around the prior target position. It is intentionally position-only. Rotation, scale and perspective compensation belong to later tracking/Target Lock work.


---

# v0.4.1 CORRECTIVE TARGET TRACKING + ROTATION TEST

## Test ID

`target_tracking_v4_1`

Guided steps:

1. **Open media** — Media3 reaches READY.
2. **Rotate to landscape** — PASS only if the same media remains loaded/visible and the app does not reset.
3. **Select target** — user draws and confirms a target; OpenCV MIL initializes.
4. **Verify real tracking** — play for at least 4 seconds. Objective diagnostics require repeated stable/continuous samples, meaningful movement and no repeated rejected jumps. This step does NOT auto-pass. The tester must press **Tracking Looks Correct** only if the blue box is visibly staying on the actual target. **Tracking Is Wrong** records an explicit failure.
5. **TRACK LOST** — seek to footage where the target is absent and continue playback until explicit TRACK LOST occurs.

Changed diagnostic signals:
- `APP / ORIENTATION_STATE` with `mediaStillLoaded`
- OpenCV MIL raw tracking score
- appearance histogram similarity
- per-sample allowed-jump threshold
- `TRACKING / TRACK_REJECTED_JUMP`
- `TRACKING / TRACK_NOT_LOCATED`
- `TEST / TRACKING_IMPLAUSIBLE_JUMP_FOR_TEST`
- `TEST / TESTER_CONFIRMED_TRACKING_VISUALLY_CORRECT`
- `TEST / TESTER_REPORTED_TRACKING_VISUALLY_WRONG`

A movement test must never pass solely because a box moved.
