# PhotoEdit — Project Specification

## Purpose
A free native Android application for preparing images and short-form video materials, including Douyin/TikTok-style vertical content.

## Functional roadmap
1. Equal-grid splitter: 2x2, 3x3, 4x4 and configurable row/column counts; preview and export numbered tiles.
2. Batch image processing: resize, crop, compress, convert supported formats, and batch export.
3. 9:16 composition: crop, fit and background fill for vertical output (e.g. 1080x1920).
4. Still-image motion video: pan, zoom, keyframes, transitions and H.264 MP4 export.

## Technical decisions
- Language: Kotlin.
- UI: Jetpack Compose with Material 3.
- Photo processing: Bitmap and Canvas; OpenCV not needed for equal-grid splitting.
- Video: plan for OpenGL ES + MediaCodec; implementation details to be validated on devices.
- Storage: Android Photo Picker / Storage Access Framework / MediaStore as appropriate.
- Build: Gradle + GitHub Actions, with downloadable APK artifact.
- Network: no paid APIs; offline core features.
- Distribution: share APK directly; configure durable release signing before public releases.

## Pending decisions
- Minimum Android SDK version (Android 8 / API 26 is proposed, not yet confirmed).
- Final application ID/package name.
- Exact first-release UI and supported output formats.

## Working environment
- Local: D:\Projects\PhotoEdit (exists and is a Git repository; remote origin configured).
- Remote: https://github.com/a772485252/PhotoEdit
- No Android Studio or Java installed locally at project kickoff.
