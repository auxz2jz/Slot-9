# PROJECT_MEMORY.md

## Purpose

This file is the mandatory working memory and operating procedure for the DVR Video Player project.

**Repository:** auxz2jz/Slot-9  
**Project:** Android DVR Video Player  
**Current planned version:** v0.1.0  
**Status:** Planning / pre-code  
**Source of truth priority:** 1) this file, 2) DVR_PLAYER_ROADMAP.md, 3) current source code and saved test reports, 4) conversation history.

The project must remain understandable and recoverable even if the chat history disappears.

---

# NON-NEGOTIABLE STARTUP RULE

Before doing any work on this project, the developer/agent MUST:

1. Read this entire file.
2. Read `DVR_PLAYER_ROADMAP.md`.
3. Inspect the current project version and the files relevant to the requested change.
4. Read the latest test report and known-good checkpoint if they exist.
5. Review current known bugs and failed approaches.
6. Update the **Current Task** section below with the new request.
7. Write the intended implementation plan BEFORE editing code.
8. Identify the files expected to change.
9. Preserve the last known working state before risky changes.
10. Only then begin implementation.

If these steps have not been completed, coding has not started correctly.

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
**Current version:** v0.1.0 planned  
**Last known working version:** None yet — pre-code  
**Build status:** No Android project committed yet  
**Current phase:** Architecture and roadmap approval

---

# CURRENT TASK

## User Request

Create the project memory/instruction system and the DVR Video Player roadmap before coding. Store both in the next empty GitHub slot and use them as permanent project references.

## Goal

Establish durable project instructions, anti-loop safeguards, checkpoint rules, and a full roadmap containing all DVR player ideas discussed before implementation begins.

## Implementation Plan

1. Use the first unused GitHub slot.
2. Create this mandatory project-memory file.
3. Create `DVR_PLAYER_ROADMAP.md`.
4. Record the initial Android architecture and feature roadmap.
5. Present both files for review.
6. Do not begin application code until these planning files are reviewed.

## Files Expected to Change

- `PROJECT_MEMORY.md` — permanent working instructions and live project state.
- `DVR_PLAYER_ROADMAP.md` — feature roadmap and implementation order.

## Files That Should NOT Be Changed

None. Repository is being initialized for this project.

---

# WORK IN PROGRESS

- GitHub slot search completed.
- Slot-8 is occupied.
- Slot-9 is the next empty repository and has been selected.
- Project memory/instructions are being initialized.
- DVR player roadmap is being initialized.
- Application source code has not started yet.

---

# CURRENT KNOWN GOOD STATE

**Version:** Planning checkpoint 0  
**Status:** Documentation only  
**Confirmed:** Repository selection and project specification  
**Code:** None yet

Until the first application build succeeds, this is the recovery point.

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

No errors yet.

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
    test_report
    media_info
```

This is a planning map, not a requirement to create every file immediately.

Update this section when the actual architecture is established.

---

# NEXT STEPS

1. User reviews `PROJECT_MEMORY.md`.
2. User reviews `DVR_PLAYER_ROADMAP.md`.
3. Apply requested roadmap/instruction changes.
4. Create the Android v0.1.0 project foundation.
5. Build and test basic local video playback before advanced features.

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
4. Saved test reports.
5. Git/version checkpoints.

If losing the conversation would make the next action unclear, the project documentation is not current enough.

---

# FINAL PRINCIPLE

**Read first. Plan before coding. Save constantly. Protect working code. Test small changes. Record failures. Break loops deliberately. Leave a recoverable checkpoint.**
