# PhotoEdit — Development Progress

Last updated: 2026-11-04

## Current milestone
V0.2 implementation complete; CI verification pending for this commit.

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
