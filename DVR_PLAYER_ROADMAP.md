# DVR_PLAYER_ROADMAP.md

## Project

**Name:** DVR Video Player  
**Repository:** auxz2jz/Slot-9  
**Platform:** Android  
**Primary use case:** Review body-camera and other local video footage with DVR-style transport, precise seeking, non-distorting zoom, object tracking, target lock, and multi-frame enhancement.

This roadmap is a permanent source of truth. Before implementing or changing features, read `PROJECT_MEMORY.md` first, then this file, then `TESTING_DIAGNOSTICS.md`.

---

# PRODUCT PRINCIPLES

1. The original video file is read-only by default.
2. Playback controls should feel more like a DVR/security recorder than a basic phone video player.
3. Zoom must preserve geometry and aspect ratio.
4. The user must be able to keep watching while zoomed and panned.
5. High-speed review should prioritize useful visual scanning over decoding every single frame.
6. Analysis tools should work on a manually selected target even when the target is not a known object class.
7. Advanced enhancement must never invent text or claim detail that was not present in the source frames.
8. Every new feature must be testable independently.
9. Working playback must remain usable even if advanced tracking/enhancement features are unavailable.
10. New ideas go into this roadmap before they are added to the application.

---

# STATUS LEGEND

- PLANNED
- IN PROGRESS
- PARTIAL
- WORKING
- DONE
- BLOCKED
- FAILED
- DEFERRED

---

# PHASE 0 — PERMANENT TESTING & DIAGNOSTICS INFRASTRUCTURE

## 0.1 Guided test runner — DONE

Every new user-facing feature must have an in-app guided test.

The test UI must:
- tell the tester exactly what action to perform,
- show one step at a time,
- show current progress,
- automatically detect objective success signals where possible,
- mark PASS / FAIL / PARTIAL,
- never require the tester to describe normal diagnostic details manually when the app can record them itself.

Initial baseline guided test:
1. Open a video.
2. Press Play.
3. Press Pause.
4. Drag/seek the timeline.
5. Export results.

Each step advances only when corresponding app/player signals confirm it.

---

## 0.2 Structured event diagnostics — DONE

Record timestamped diagnostic events such as:
- app/session start,
- screen/control actions,
- button presses,
- file picker requested/result,
- selected media URI metadata without copying source media,
- play/pause requests,
- actual player isPlaying changes,
- playback state changes,
- seek start/update/finish,
- position discontinuities,
- playback speed,
- video size,
- errors/exceptions,
- guided-test step transitions,
- future zoom/pan/tracking/Target Lock events.

Use machine-readable JSON Lines plus a human-readable summary.

---

## 0.3 Device/application diagnostics — DONE

Capture:
- app version/build,
- Android version/API,
- manufacturer/model/device,
- CPU ABI,
- memory class/available memory where practical,
- display dimensions/density,
- locale,
- diagnostic session start/end,
- relevant permissions/state when needed.

Do not collect unrelated personal data.

---

## 0.4 Media diagnostics — DONE

For the opened video record, where available:
- display name,
- MIME type,
- duration,
- video width/height,
- rotation,
- frame rate,
- bitrate,
- video/audio codec information,
- player-reported tracks,
- playback errors.

Do not include or upload the video itself unless the user separately chooses to provide it.

---

## 0.5 Diagnostic ZIP export — DONE

A completed or interrupted test session must be exportable as a single ZIP package containing:
- README/SUMMARY,
- device_app_info.txt,
- media_info.txt,
- events.jsonl,
- guided_test_results.json,
- error details where present.

The user chooses where to save the ZIP using Android's document save UI.

Core diagnostics stay on-device and are not automatically uploaded.

---

## 0.6 Feature-specific test definitions — REQUIRED

Whenever a new roadmap feature is implemented, its guided test must be added in the same development cycle.

Examples:

**Zoom**
- instruction: pinch to at least 2x,
- diagnostics: record scale gesture and resulting internal zoom scale,
- success: scale reaches expected threshold without independent X/Y distortion.

**Pan**
- instruction: while zoomed, drag the video,
- diagnostics: record drag delta and resulting viewport offset,
- success: offset changes while zoom remains active.

**2x playback**
- instruction: choose 2x,
- diagnostics: record requested speed and player playback parameters,
- success: actual player speed becomes approximately 2.0.

**DVR scan**
- instruction: start 16x forward scan for several seconds,
- diagnostics: record start/end media positions and elapsed real time,
- success: effective media advance is consistent with high-speed scan behavior.

**Object tracking**
- instruction: select a target and allow it to move,
- diagnostics: record target box coordinates/confidence per sampled frame,
- success: tracker remains active and target position changes coherently.

**Target Lock**
- instruction: lock target and play,
- diagnostics: record tracked center and displayed lock-point error,
- success: target remains within allowed screen-position tolerance.

**Frame stacking**
- instruction: create stacked result,
- diagnostics: record candidate frames, alignment scores, rejected frames, weights/method and output dimensions,
- success: pipeline completes without hidden fallback or failure.

---

# PHASE 1 — CORE DVR PLAYER

## 1.1 Local video opening — WORKING

Requirements:
- Open local video files from Android storage/file picker.
- Handle body-camera footage and ordinary phone/video files.
- Display filename, duration, resolution and basic media information.
- Preserve original file; never overwrite it.
- Gracefully report unsupported codecs/containers.

Preferred playback foundation:
- AndroidX Media3 / ExoPlayer.

---

## 1.2 Main playback view — PARTIAL

Requirements:
- Large video display.
- Portrait and landscape layouts.
- Full available-screen viewing without stretching the picture.
- Play / pause.
- Current position and total duration.
- Resume from recently viewed position where practical.
- Keep controls usable on a phone.

---

## 1.3 DVR timeline / seek bar — WORKING

Requirements:
- Large, easy-to-grab seek bar.
- Drag to any point in the video.
- Show current seek time while dragging.
- Update the displayed frame while scrubbing when performance allows.
- Fast preview while dragging.
- More accurate final seek when the user releases the slider.
- Do not force the user to wait for perfect frame accuracy during the drag itself.

Future enhancement:
- Timeline thumbnails.
- Visible marks/bookmarks.
- Target/tracking event markers.

---

## 1.4 Jump controls — PLANNED

Possible controls:
- -10 seconds
- +10 seconds
- -30 seconds
- +30 seconds

Exact UI may be refined after phone testing.

---

## 1.5 Frame stepping — WORKING

Requirements:
- When paused, step backward approximately one frame.
- When paused, step forward one frame.
- Show timestamp while frame stepping.
- Seek as accurately as the source format allows.

Use cases:
- Inspect short actions.
- Select a tracking target.
- Find the best visible sign/plate/person frame.

---

# PHASE 2 — PLAYBACK SPEED AND DVR SCANNING

## 2.1 Slow playback — WORKING

Target speeds:
- 0.10x
- 0.25x
- 0.50x
- 1.00x

Goal:
Useful inspection of short actions without manually stepping every frame.

---

## 2.2 Normal accelerated playback — WORKING

Target speeds:
- 2x
- 4x

Use true accelerated playback where device/codec performance is acceptable.

Audio behavior:
- Default behavior to be tested.
- At high enough speeds, audio may be muted automatically.
- Future option may allow user preference.

---

## 2.3 DVR-style forward scan — WORKING

Target scan rates:
- 8x
- 16x
- 32x
- 64x

Important:
These rates do not have to decode every frame.

Preferred behavior:
- Rapidly advance through the video.
- Skip frames as necessary.
- Keep showing useful visual updates.
- Maintain responsive controls.
- Make the effective scan rate close to the requested rate even when the codec cannot be played conventionally at that speed.

Display the current mode clearly, for example:
- >> 8x
- >> 32x
- >> 64x

---

## 2.4 DVR-style reverse scan — WORKING

Target scan rates:
- 2x reverse
- 4x reverse
- 8x reverse
- 16x reverse
- 32x reverse
- 64x reverse

Because ordinary playback engines are not designed for continuous negative-speed decoding, reverse scan may use:
- repeated backward seeks,
- frame extraction,
- keyframe-aware stepping,
- adaptive skipping.

Goal:
A usable security-DVR-style rewind experience, not necessarily literal reverse decoding of every frame.

---

## 2.5 Adaptive scan performance — WORKING

The player should adjust how many frames it displays during high-speed scanning based on:
- source frame rate,
- codec,
- resolution,
- device performance,
- requested scan speed.

Priority:
Responsiveness over trying to render impossible frame rates.

---

# PHASE 3 — PINCH ZOOM AND PAN

## 3.1 Pinch-to-zoom — WORKING

Requirements:
- Two-finger pinch in/out.
- Continuous zoom, not only preset steps.
- Video continues playing while zoomed.
- Zoom works while paused.
- Zoom works during slow/normal playback.
- Test interaction with high-speed DVR scan.

Suggested range:
- 1.0x minimum
- approximately 8.0x maximum initially

The maximum may be adjusted after testing.

---

## 3.2 No-distortion rule — REQUIRED

Horizontal and vertical scale must stay linked.

The app may:
- magnify,
- crop,
- translate,
- pan.

The app must NOT:
- stretch only horizontally,
- stretch only vertically,
- change the video's geometry merely to fill the screen.

---

## 3.3 Pan while zoomed — WORKING

Requirements:
- Drag the enlarged video to inspect a different area.
- Clamp panning so the interaction remains sensible.
- Continue playback while panning.
- Do not accidentally seek the timeline when the user is manipulating the video.

---

## 3.4 Reset zoom — WORKING

Provide a fast control to return to:
- 1.0x zoom
- centered view
- normal orientation.

---

## 3.5 Zoom lock — PLANNED

Purpose:
After positioning a zoomed view, lock it so playback controls can be used without accidentally moving the image.

---

# PHASE 4 — MANUAL TARGET SELECTION AND TRACKING

## 4.1 Manual region selection — IN PROGRESS

Workflow:
1. Pause or play video.
2. Enter tracking mode.
3. Draw/resize a box around the desired target.
4. Confirm target.
5. Start tracking.

Target can be:
- person,
- vehicle,
- license plate,
- sign,
- bag,
- hand,
- arbitrary visual object.

The system must not require the target to belong to a predefined object class.

---

## 4.2 Blue tracking box — IN PROGRESS

Requirements:
- Draw a visible blue box around the tracked target.
- Follow the target as it moves.
- Box should update frame-to-frame.
- Keep overlay separate from the original video.
- User can hide/show tracking overlay.

---

## 4.3 Traditional visual tracking — IN PROGRESS

Candidate techniques:
- OpenCV optical flow,
- feature tracking,
- template matching,
- correlation-based tracking,
- region tracking.

Exact algorithm will be chosen through testing.

Goal:
Track manually selected regions without requiring cloud AI.

---

## 4.4 Detector-assisted reacquisition — PLANNED

For people/vehicles where useful, an optional local detector may periodically help reacquire the target if visual tracking drifts.

Possible future technology:
- MediaPipe or another Android-compatible on-device detector.

This should supplement manual tracking, not replace it.

---

## 4.5 Tracking-lost handling — PLANNED

If target confidence becomes too low:
- show TRACK LOST,
- stop pretending the target is known,
- preserve the last reliable location,
- let the user reselect/reacquire the object.

Never silently move the box to an unrelated object.

---

# PHASE 5 — TARGET LOCK / OBJECT-CENTERED STABILIZATION

## 5.1 Center Lock — PLANNED

User selects an object.

The app transforms the displayed video so the selected object's center stays at a fixed screen location, normally the center.

Example:
- A license plate moves through the original frame.
- In Center Lock mode, the plate stays centered.
- The rest of the scene moves around it.

This is a viewing transformation only.

---

## 5.2 User-positioned lock point — PLANNED

Instead of forcing the target to dead center, allow the user to place it at a chosen fixed point on screen.

Example:
- center,
- upper center,
- custom position.

---

## 5.3 Position-only lock — PLANNED

Compensate for target X/Y movement.

Simplest Target Lock mode and likely first implementation.

---

## 5.4 Position + scale lock — PLANNED

Attempt to keep:
- target center fixed,
- target apparent size approximately fixed.

Useful when a vehicle/sign gets closer or farther from the camera.

---

## 5.5 Position + rotation + scale lock — PLANNED

Advanced mode.

Attempt to compensate for:
- translation,
- rotation,
- scale change.

Potentially use:
- feature matching,
- affine transformation,
- perspective/homography where justified.

This mode may require a larger tracked region than a very small object.

---

## 5.6 Pinch zoom while Target Lock is active — PLANNED

The user should be able to:
- lock a target,
- zoom into it,
- keep playback running,
- keep the target at the selected screen location.

This is especially important for signs and plates.

---

# PHASE 6 — BEST-FRAME SEARCH

## 6.1 Analyze nearby frames — PLANNED

User selects a target and chooses a time window such as:
- +/- 0.5 sec
- +/- 1 sec
- +/- 2 sec
- custom future option

The app examines nearby decoded frames.

---

## 6.2 Frame quality scoring — PLANNED

Possible measures:
- sharpness / edge detail,
- motion blur,
- contrast,
- target visibility,
- obstruction,
- exposure,
- tracker confidence.

Important:
The best frame is NOT assumed to be an I-frame/keyframe.

Decoded image quality determines the choice.

---

## 6.3 Best single frame — PLANNED

Output:
- the highest-quality candidate frame or target crop,
- with timestamp,
- with ability to compare against nearby frames.

---

## 6.4 Candidate comparison — PLANNED

Possible UI:
- Best Frame
- Alternate 1
- Alternate 2
- Original/current frame

Allow manual choice if the automatic score picks the wrong candidate.

---

# PHASE 7 — FRAME ALIGNMENT

## 7.1 Align tracked target across frames — PLANNED

Before stacking, transform nearby target crops so the same physical details occupy the same pixel locations as closely as possible.

Alignment may compensate for:
- translation,
- rotation,
- scale,
- perspective where enough reliable features exist.

---

## 7.2 Alignment preview — PLANNED

Provide a visual check that alignment is correct.

Possible tool:
- blend/opacity slider between two aligned frames.

Purpose:
Reveal ghosting or misregistration before final stacking.

---

## 7.3 Reject bad frames — PLANNED

Exclude frames that are:
- too blurry,
- badly misaligned,
- mostly obstructed,
- tracker-lost,
- too different in angle,
- excessively compressed/noisy relative to other candidates.

---

# PHASE 8 — MULTI-FRAME STACKING / ENHANCEMENT

## 8.1 Average stack — PLANNED

Combine aligned frames with equal contribution.

Useful for:
- noise reduction,
- stable low-motion targets.

Risk:
Misalignment creates blur/ghosting.

---

## 8.2 Weighted average stack — PLANNED

Preferred general method.

Sharper/better-aligned frames receive higher weight.

Example concept:
- very sharp frame: high weight,
- moderate frame: medium weight,
- blurry frame: low or zero weight.

This is better than blindly giving every frame identical opacity.

---

## 8.3 Median stack — PLANNED

Use median pixel values across aligned frames.

Potential benefit:
- reject transient noise,
- reduce some temporary artifacts/obstructions.

---

## 8.4 Best-detail composite — PLANNED

Experimental/advanced.

Use locally stronger detail from different aligned frames where valid.

Must be carefully designed to avoid creating misleading artifacts.

---

## 8.5 Multi-frame enhancement result — PLANNED

Show at minimum:
- best single frame,
- stacked/enhanced result,
- original/current frame.

Let the user switch between them.

---

## 8.6 Truthfulness rule for enhancement — REQUIRED

Enhancement may combine information genuinely present in multiple source frames.

It must NOT:
- fabricate letters,
- hallucinate a plate number,
- invent sign text,
- silently substitute AI-generated content as evidence.

If OCR is added later, OCR text must be labeled as machine interpretation and remain separate from the image itself.

---

# PHASE 9 — SCREENSHOTS AND EXPORT

## 9.1 Save current frame — PLANNED

Save a decoded still image from the current position.

---

## 9.2 Save target crop — PLANNED

Export only the selected/tracked object region.

---

## 9.3 Save best frame — PLANNED

Export the chosen Best Frame result as a new image.

---

## 9.4 Save stacked result — PLANNED

Export the multi-frame result as a new image.

Original video remains untouched.

---

## 9.5 Export stabilized/Target Lock clip — PLANNED

Future feature.

Render a new video where the selected target remains centered/stabilized.

The exported file must be clearly separate from the original.

---

# PHASE 10 — REVIEW TOOLS

## 10.1 Bookmarks — PLANNED

User can mark important timestamps.

Potential fields:
- timestamp,
- short note,
- optional target association.

---

## 10.2 A-B loop — PLANNED

Choose start and end timestamps and repeatedly play that range.

Useful for reviewing short incidents.

---

## 10.3 Rotation controls — PLANNED

Allow viewing rotation without modifying original media.

---

## 10.4 Non-destructive video image adjustments — PLANNED

Add a viewing-only adjustment panel for improving visibility while reviewing footage.

Initial controls to evaluate:
- brightness / exposure
- contrast
- sharpness
- saturation / color intensity
- hue
- gamma / midtone brightness
- color temperature (warmer / cooler)
- tint (green / magenta)
- highlights
- shadows
- optional black level / white level if useful after testing

Requirements:
- adjustments affect only the displayed video by default,
- original source video remains unchanged,
- work while paused and during playback where performance allows,
- allow instant Reset to original,
- provide an Original / Adjusted comparison toggle,
- preserve reasonable adjustment ranges so controls do not easily destroy the image,
- adjustment values should be visible numerically,
- settings should be included in diagnostics when changed,
- exported enhanced stills/clips should record which adjustments were applied where practical.

Potential UI:
- collapsible Adjustments panel,
- sliders with numeric values,
- Reset All,
- optional presets such as Low Light, High Contrast, or Neutral only after individual controls are proven.

Guided testing should verify:
- each control changes the rendered view,
- Reset restores the unadjusted view,
- original media remains unmodified,
- adjustment values survive ordinary UI interactions as intended.

---

## 10.5 Media information — PLANNED

Show:
- filename,
- container,
- video codec,
- audio codec,
- resolution,
- frame rate,
- duration,
- bitrate where available,
- file size,
- keyframe-related information where useful.

---

# PHASE 11 — FUTURE FORENSIC/ANALYSIS IDEAS

These are ideas only and should not delay the core player.

## 11.1 OCR assistance — DEFERRED

Run OCR on a selected sign/plate region.

Important:
- keep OCR text separate from the image,
- show confidence when available,
- never present OCR as guaranteed truth.

---

## 11.2 Motion path visualization — DEFERRED

Draw the tracked object's path across the frame.

---

## 11.3 Multiple simultaneous targets — DEFERRED

Track more than one manually selected object.

Possible uses:
- two people,
- person + vehicle,
- multiple vehicles.

---

## 11.4 Side-by-side original vs Target Lock — DEFERRED

Show original footage and stabilized target view together if phone performance allows.

---

## 11.5 External display / tablet layout — DEFERRED

Optimize larger screens after phone workflow is stable.

---

# INITIAL IMPLEMENTATION ORDER

The project should NOT jump directly into object tracking or frame stacking.

Recommended sequence:

### v0.1.x — Playback foundation + diagnostic harness
1. Android project shell.
2. Permanent diagnostic/event logger.
3. Guided-test controller and on-screen instructions.
4. Diagnostic ZIP exporter.
5. Local file picker.
6. Media3 playback.
7. Play/pause.
8. Current/total time.
9. DVR seek bar.
10. Reliable scrubbing.
11. Baseline self-verifying guided test.
12. Exported diagnostic report.

### v0.2.x — Zoom and inspection
1. Pinch zoom.
2. Pan.
3. Aspect-ratio protection.
4. Reset zoom.
5. Frame step.
6. Slow speeds.
7. Test report.

### v0.3.x — DVR high-speed review
1. 2x/4x.
2. Forward scan 8x-64x.
3. Reverse scan.
4. Adaptive frame skipping.
5. Test report.

### v0.4.x — Target selection and tracking
1. Draw target box.
2. Blue tracking overlay.
3. Basic manual-region tracker.
4. Lost-target behavior.
5. Test report.

### v0.5.x — Target Lock
1. Position-only Center Lock.
2. Zoom while locked.
3. User-positioned lock point.
4. Scale/rotation stabilization experiments.
5. Test report.

### v0.6.x — Best Frame and alignment
1. Nearby frame extraction.
2. Quality scoring.
3. Candidate selection.
4. Target alignment.
5. Alignment preview.
6. Test report.

### v0.7.x — Frame stacking
1. Average.
2. Weighted average.
3. Median.
4. Best-detail experiments.
5. Compare results.
6. Export still.
7. Test report.

### Later
- bookmarks,
- A-B loop,
- stabilized clip export,
- OCR assistance,
- multiple targets,
- larger-screen layouts.

Version numbers may shift if testing requires additional intermediate builds. The order matters more than the exact number.

---

# ACCEPTANCE CRITERIA FOR CORE APP

Before calling the core DVR player stable, it should successfully demonstrate:

- Open a local body-camera video.
- Play/pause reliably.
- Seek through a long video.
- Scrub and visibly update position.
- Frame-step while paused.
- Pinch zoom without distortion.
- Pan while video continues playing.
- Reset zoom instantly.
- Play at normal and slow speeds.
- Scan forward at very high effective speeds.
- Scan backward in a usable DVR style.
- Keep original video unchanged.

Advanced milestone acceptance later:

- Select an arbitrary visual target.
- Track it with a visible box.
- Detect/report tracker loss.
- Keep target centered while the scene moves.
- Find a best nearby frame.
- Align multiple target frames.
- Produce at least one useful stacked result.
- Export derived images without modifying source video.

---

# TEST MEDIA STRATEGY

Use multiple short samples instead of relying only on one long video.

Recommended test set:
1. Short high-quality video with clear motion.
2. Body-camera footage with camera shake.
3. Video containing a readable sign.
4. Vehicle/plate-like target crossing the frame.
5. Person walking across frame.
6. Low-light/noisy sample.
7. High-resolution/high-bitrate sample.
8. Long-duration sample for seeking.

Keep notes about which sample exposes which issue.

---

# PERFORMANCE PRINCIPLES

1. Avoid blocking the UI thread with frame analysis.
2. Decode only what is needed for the current mode.
3. Use lower-cost previews during scrubbing when necessary.
4. High-speed DVR scan may deliberately skip many frames.
5. Advanced analysis can temporarily reduce playback complexity if necessary.
6. Tracking should degrade gracefully rather than freeze the app.
7. On-device processing is preferred where practical.
8. Do not require an internet connection for core playback/tracking features.

---

# ROADMAP CHANGE RULE

When the user introduces a new feature idea:

1. Add it to this roadmap.
2. Record its relationship to existing features.
3. Decide which phase it belongs to.
4. Do not silently implement it during unrelated work.
5. Update `PROJECT_MEMORY.md` if the idea changes architecture or current work.
6. Add or update that feature's guided test and required diagnostic signals in `TESTING_DIAGNOSTICS.md`.

---

# CURRENT ROADMAP CHECKPOINT

**Date:** 2026-09-25  
**Known-good baseline:** v0.3.1 physical-device guided test PASS  
**v0.4.0 result:** FAILED tracking/orientation validation. Manual selection worked, but the sampled-template tracker visibly drifted/jumped while still reporting high confidence; the movement test falsely passed; TRACK LOST arrived only after the tester stopped the failed test. Landscape rotation also reset/disappeared the player state.  
**Current candidate:** v0.4.1 — OpenCV MIL tracking, appearance/motion plausibility checks, rejected teleports, tester-confirmed movement validation, explicit lost-target handling, and preserved landscape state.  
**Next action:** Compile/install v0.4.1, run `target_tracking_v4_1`, and upload the latest diagnostic ZIP. Keep v0.3.1 as recovery baseline until v0.4.1 passes.
