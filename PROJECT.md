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

## Platform strategy
- Android first; support vivo S18 and other compatible Android devices.
- Future iOS version planned, but not in the initial milestone.
- Keep processing parameters, data models, and algorithms conceptually separate from Android UI and platform APIs so an iOS implementation can reuse specifications.
- Do not introduce Kotlin Multiplatform yet; reassess when iOS development begins.

## V0.2 implementation decisions
- Minimum Android SDK: 26; target/compile SDK: 35. Application ID: com.photoedit.app.
- Chinese Compose UI, image picker, source preview, 2x2 / 3x3 / 4x4 equal-grid preview and PNG gallery export.
- Preserve aspect ratio; centered crop of width % columns and height % rows pixels produces identical tile sizes. No square crop.
- Number tiles left-to-right, top-to-bottom. Export uses decoded source, not preview thumbnails.
- Decode to at most approximately 8 megapixels and 4096 pixels per dimension using power-of-two sampling; show actual processing resolution. EXIF rotation and mirroring are applied.
- Android 10+: MediaStore pending writes to Pictures/PhotoEdit, no storage permission. Android 8/9: request WRITE_EXTERNAL_STORAGE and insert through MediaStore.
- No INTERNET permission or remote processing. Cloud-only picker items may require downloading by the system picker; use on-device photos for offline operation.
- Lightweight single-module MVVM: ui/screens, ui/components, ui/theme; viewmodel coordinates processing; domain/GridSpec is platform-independent; processing/ImageProcessor owns Android APIs.
- Public distribution still needs durable signing; CI debug keystore may differ between clean builds. Do not uninstall an existing app unless its data is backed up.

## Working environment
- Local: D:\Projects\PhotoEdit (exists and is a Git repository; remote origin configured).
- Remote: https://github.com/a772485252/PhotoEdit
- No Android Studio or Java installed locally at project kickoff.

## Planned Cloudflare AI integration — personal defaults
- User explicitly authorized committing their non-secret Cloudflare Account ID as the default: `4d17f32858b5ec94a9d493d703b54fb9`.
- Pre-fill this Account ID in the future AI settings screen; retain an editable override.
- API Token must remain empty by default, entered on-device and stored using Android Keystore-backed encryption; never commit it or embed it in the APK.
- Personal use; cloud text-to-image/reference-image editing planned. Keep existing offline tools available without credentials.
- This records a configuration decision only. AI settings, API client and generation flow are not implemented in V0.2.
