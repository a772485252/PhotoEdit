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
10. APK build policy (user-requested only): ordinary code edits and commits must NOT trigger GitHub Actions APK builds. The Android APK workflow must remain manual-only (`workflow_dispatch`). Accumulate related code changes and commits without building. Only trigger a build when the user explicitly asks for an APK/download link (e.g. “生成最新 APK 下载链接”). Then verify tests, lint, build, signing, and that `latest-apk/PhotoEdit-debug.apk` was updated before sharing a link. Do not manually start a build just to validate each small code change.
11. Keep a lightweight unit-test suite for critical domain rules; do not remove the test module merely to avoid occasional stale assertions.
12. Prioritize tests for model/provider validation, API credential and prompt validation, billing/price selection, image/reference constraints, and error classification.
13. Do not add UI snapshot tests, broad network mocks, or test-only architecture unless a concrete regression risk justifies them.
14. When behavior or model defaults change, update the corresponding focused tests in the same change. If a test fails, determine whether production behavior or the assertion is outdated; never delete a valid test just to make CI green.
15. Keep unit tests in the test source set; they are not packaged into the APK. CI should run testDebugUnitTest before publishing an APK.

## UI and business separation (mandatory)
- Use a lightweight single-app-module MVVM structure: ui/screens, ui/components, ui/theme, viewmodel, domain, and processing.
- Compose UI renders immutable UI state and emits user intents through callbacks.
- ViewModels own screen state and coordinate use cases; they must not depend on Compose UI widgets.
- Domain rules and processing algorithms must not depend on Android Activity or Compose.
- Keep Bitmap/Canvas and future video encoding inside processing adapters, not Composables.
- Do not do heavy image or video work on the main thread.
- Changes to colors, layout, and typography should generally stay in ui/.
- Keep the architecture lightweight; no unnecessary DI framework or multiple Gradle modules initially.
- Design processing parameters so they can be reimplemented for a future native iOS app.

## Next task
Create a minimal Kotlin + Compose Android application with a GitHub Actions debug APK build workflow.
