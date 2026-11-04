# PhotoEdit — Agent Development Rules

## Mission
Build a genuine native Android application for photo processing and short-form video materials. Do not use a WebView/HTML wrapper.

## Fixed decisions
- Repository: https://github.com/a772485252/PhotoEdit
- Local Windows directory: D:\Projects\PhotoEdit
- Kotlin and Jetpack Compose for Android UI.
- Image operations: Android Bitmap / Canvas.
- Video rendering: evaluate OpenGL ES and MediaCodec when implementing motion video.
- Standard equal-size grid splitting; OpenCV is not required.
- Core functionality must run offline and without paid APIs.
- APKs are distributed directly, not via app stores.
- GitHub Actions will compile APKs. Android Studio and local Java are not required for the initial workflow.

## Working rules
1. Inspect existing files and Git status before changing anything; never overwrite unreviewed work.
2. Implement in small buildable milestones. Prefer simple, maintainable Kotlin modules.
3. Never commit secrets, tokens, signing passwords, or private keystores.
4. Preserve original image quality where practical; avoid loading huge images into memory unnecessarily.
5. Handle Android storage permissions and user-selected files using modern Android APIs.
6. Check build logs and record verified results; never claim a build or APK works without evidence.
7. After each meaningful change, update PROGRESS.md and keep PROJECT.md consistent with decisions.
8. If a new session starts, read AGENTS.md, PROJECT.md and PROGRESS.md first.
9. Do not force-push, delete files, or rewrite history without explicit approval.
10. Prefer GitHub Actions for builds; test generated APKs on a real Android device.

## Next task
Create a minimal Kotlin + Compose Android application with a GitHub Actions debug APK build workflow.
