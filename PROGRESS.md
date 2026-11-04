# PhotoEdit — Development Progress

Last updated: 2026-11-04

## Current milestone
V0.2 complete and CI verified; APK published. Real-device verification remains pending.

## Existing verified baseline
- Native Kotlin + Jetpack Compose skeleton; default branch main.
- Android SDK setup and debug APK build previously verified (run 41766155065).
- latest-apk/PhotoEdit-debug.apk already published automatically by CI.
- Repository: https://github.com/a772485252/PhotoEdit; Windows checkout: D:\Projects\PhotoEdit.

## V0.2 implemented
- Chinese UI, system photo picker with fallback, decoded image preview.
- 2x2 / 3x3 / 4x4 grid selection and numbered result previews.
- Background decode, EXIF orientation correction, centered equal-size grid geometry.
- PNG export from processing-resolution image to MediaStore; pending writes and rollback on failure.
- Android 8/9 legacy storage permission, Android 10+ permission-free gallery writes.
- Busy state prevents overlapping operations; selection cancellation preserves current work; errors shown in Chinese.
- Kept single-module MVVM and separated UI/theme/components, ViewModel, pure domain geometry and Android processing.
- Version 0.2.0 / versionCode 2.
- CI now runs testDebugUnitTest, lintDebug and assembleDebug before APK upload/publication.

## Limits and device checks still required
- Large images are sampled to ~8 MP / max 4096 dimension; actual processing resolution is displayed. Export is lossless PNG at this resolution, not guaranteed original resolution.
- Equal tile sizing trims at most n-1 pixels per axis, centered. No square crop.
- Process death resets the editor; rotation keeps ViewModel state. Session/project continuation is documented here, not editor persistence.
- Real-device testing has NOT been performed. Test vivo S18: airplane-mode local photo selection, all grid sizes, EXIF portrait/mirror, repeated selection/export, PNG order and gallery visibility.
- Test Android 8/9 allow/deny permission; Android 10+ permission-free save; low storage/error cleanup; large-image memory behavior.
- CI debug signing is not yet durable; install-over compatibility between fresh CI builds is not guaranteed.

## Handoff
1. Read AGENTS.md, PROJECT.md, PROGRESS.md and inspect main/status before edits.
2. Check the V0.2 workflow result and latest-apk publication; record actual run/commit evidence below.
3. Install APK on vivo S18 and record results; do not claim device verification based on CI.
4. Preserve current architecture. Future milestones: configurable rows/columns, batch processing, 9:16 composition, still-image motion video.

## V0.2 verified build and publication
- Feature commit: 9d2d1e9c3be083286575f828dd1cc497c5520a9b.
- Successful GitHub Actions run: https://github.com/a772485252/PhotoEdit/actions/runs/41784249143
- Job 126422610796 completed successfully. Logs show testDebugUnitTest, lintDebug and assembleDebug executed; BUILD SUCCESSFUL in 1m 10s.
- Artifact PhotoEdit-debug-apk: ID 13300257422, ZIP size 9,127,134 bytes.
- CI published the APK in commit c3b6e5ff409a1547c4f4deeeb7c120cb01300514.
- Confirmed changed APK blob: 50719869ba81a47eac344441a30f0676ab5473fb.
- Current APK: https://github.com/a772485252/PhotoEdit/raw/refs/heads/main/latest-apk/PhotoEdit-debug.apk
- No build errors occurred in this milestone. Runner action deprecation warnings are non-blocking; review action versions in a later CI maintenance task.
- This evidence verifies compilation, unit checks, lint and publication only; no emulator or physical device tests were run.

## Cloudflare setup handoff (2026-11-04)
- User registered Cloudflare and reached the Workers AI REST API setup page.
- User authorized saving Account ID `4d17f32858b5ec94a9d493d703b54fb9` publicly as the pre-filled personal default for the upcoming integration.
- Account ID was transcribed from the provided setup screenshot; no API Token was provided or stored.
- Next AI implementation must initialize the Account ID field with the value in PROJECT.md, allow overrides, and leave API Token blank.
- No AI settings/client or new APK was produced by this documentation-only update. Existing V0.2 APK remains current.
