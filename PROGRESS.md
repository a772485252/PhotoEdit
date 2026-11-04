# PhotoEdit — Development Progress

Last updated: 2026-11-04

## Verified
- Windows local directory D:\Projects\PhotoEdit exists.
- Git is installed (reported 2.40.1.windows.1).
- Local directory is a Git repository and origin points to https://github.com/a772485252/PhotoEdit.git.
- Connected GitHub account: a772485252.
- Remote repository exists, is public, has default branch main, and was empty before initialization.
- GitHub connection has push permission.
- The user confirmed ordinary equal-grid splitting, so OpenCV is not needed.

## Newly confirmed decision
- Android first, with a future native iOS version planned. Keep core processing specifications independent of platform UI; no cross-platform framework required now.

## Current milestone
M1 — Native Android skeleton committed; first CI build pending verification.

## Implemented (not yet build-verified)
- Kotlin / Jetpack Compose Android application skeleton with image picker.
- Gradle configuration for Android API 26+ and Java 17.
- GitHub Actions workflow to assemble and upload a debug APK.
- Source files committed directly to main on 2026-11-04.

## UI architecture refactor (2026-11-04; build unverified)
- Moved home screen Compose UI into ui/screens/HomeScreen.kt.
- Added HomeViewModel and immutable HomeUiState in viewmodel/.
- MainActivity now coordinates Android photo picker and screen.
- Added AndroidX Lifecycle dependencies.
- Updated AGENTS.md to require UI/business/processing separation.
- CI build must still be checked; no verified APK yet.

## CI investigation
- Workflow run 41765933343 failed in android-actions/setup-android@v3 before compilation.
- Log: sdkmanager attempted obsolete package "tools" and exited with code 1.
- Replaced setup-android with runner SDK installation of platform android-35 and build-tools 35.0.0.
- Awaiting a new build result; APK is not verified yet.

- Run 41766023129 failed at Configure Android SDK because sdkmanager was not on PATH (exit 127).
- Updated workflow to discover sdkmanager within the runner's Android SDK cmdline-tools directory.
- Awaiting build verification after commit 99f1adf.

## Not yet done
- Verified successful APK build.
- Device installation and testing.
- Grid splitting and other app features.

## Next actions
1. Verify the existing Android APK workflow and fix any build failures.
2. Check workflow run results and fix build failures.
3. Download and test debug APK.
4. Implement equal-grid splitting and export.
5. Update this file with actual build/run evidence.

## Session handoff
Read AGENTS.md and PROJECT.md before coding. Verify repository state, then continue from the next unchecked milestone. Never mark a task complete solely because code was written.
