# PhotoEdit — Agent Development Rules

## Mission
Build a genuine native Android application for photo processing and short-form video materials. Do not use a WebView/HTML wrapper.

## Fixed decisions
- Repository: https://github.com/a772485252/PhotoEdit
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
7. **Mandatory documentation after every code/config/UI/model/API/build-rule change:** in the same work session, append a dated entry to PROGRESS.md describing what changed, affected files/modules, behavior and decisions, tests/build status, and anything still unverified. Update PROJECT.md in the same change whenever the current architecture, active providers/models, user-visible behavior, constraints, or operating rules changed. Do not leave documentation until a later session. A documentation-only correction must also be recorded in PROGRESS.md.
8. **New-session continuity:** every new session must read AGENTS.md, PROJECT.md, and the latest end of PROGRESS.md before editing or making claims about project state. Treat source code as authoritative if docs conflict, then correct the docs before continuing.
9. **Documentation privacy and secret hygiene (mandatory):** PROGRESS.md, PROJECT.md, AGENTS.md, commit messages, test fixtures, and logs must contain no API keys/tokens/passwords, signing material, private URLs, personal/account identifiers, email addresses, phone numbers, precise locations, personal photos or their metadata, private support/request IDs, or other user-specific/private information. Never paste secrets even in masked/partial form. Describe credential storage generically (e.g. “encrypted local provider key”) and refer to identifiers by type only. Do not record a user's exact balance, usage history, or account-specific billing data. Before committing documentation, inspect the diff for accidental secrets and private details; redact/remove them. Keep only project-level technical facts, public API contracts, non-sensitive test outcomes, and generic pending-validation notes.
10. Do not force-push, delete files, or rewrite history without explicit approval.
11. APK build policy (user-requested only): ordinary code edits and commits must NOT trigger GitHub Actions APK builds. APK builds must only be triggered on explicit user request, via `workflow_dispatch` or the dedicated `build-trigger.txt` push trigger when workflow dispatch is unavailable; never trigger by routine code changes. Accumulate related code changes and commits without building. Only trigger a build when the user explicitly asks for an APK/download link (e.g. “生成最新 APK 下载链接”). Then verify tests, lint, build, signing, and that the versioned APK `latest-apk/PhotoEdit-v<versionName>.apk` was published from the successful run before sharing a link. Do not manually start a build just to validate each small code change.
12. APK workflow supports two modes (manual dispatch or explicitly requested push-trigger): quick (default) runs Gradle assembleDebug with build cache and parallel execution, then always verifies the fixed APK signature before publishing; full runs testDebugUnitTest + lintDebug + assembleDebug before the same signature verification and publishing. Use quick for routine UI/text-only changes; use full for domain/business logic, API/provider, storage, image-processing, dependency, or important release changes. The mode must be visible in the run title and docs. Never describe quick mode as tested by unit tests or lint.
13. APK workflow run titles must include the app version (e.g. `PhotoEdit v0.9.24 · Android APK`). The manually entered/workflow-default version must be validated against `app/build.gradle.kts` before building; keep the workflow_dispatch default synchronized whenever versionName changes. Do not publish a misleading version title.
14. Keep a lightweight unit-test suite for critical domain rules; do not remove the test module merely to avoid occasional stale assertions.
15. Prioritize tests for model/provider validation, API credential and prompt validation, billing/price selection, image/reference constraints, and error classification.
16. Do not add UI snapshot tests, broad network mocks, or test-only architecture unless a concrete regression risk justifies them.
17. When behavior or model defaults change, update the corresponding focused tests in the same change. If a test fails, determine whether production behavior or the assertion is outdated; never delete a valid test just to make CI green.
18. Keep unit tests in the test source set; they are not packaged into the APK. CI should run testDebugUnitTest before publishing an APK.


## APK artifact naming (mandatory)
- Publish APKs using `PhotoEdit-v<versionName>.apk` in `latest-apk/`; derive the version automatically from `app/build.gradle.kts` and keep `versionCode` monotonic.
- Do not create or update a `PhotoEdit-debug.apk` compatibility alias. Existing historical files may remain but must not be linked as the latest release.
- When providing a download URL, use the exact current versioned filename, and verify the successful workflow run and published file before calling it latest.
- Changing filename does not change the APK signing type; preserve fixed signing identity so updates can install over existing versions.

## UI design and review (mandatory)
- Before any UI change, read UI_DESIGN.md and inspect the target screen, parent navigation/container, shared components, theme, and related business/domain/ViewModel logic. Do not make isolated visual patches without understanding the full task flow.
- UI must prioritize user feel: predictable familiar interactions, one-hand reachability, clear next steps, low friction, responsive feedback, safe error recovery, and no unnecessary confirmations or hidden controls.
- UI must reflect actual business logic. Map user goals, inputs, constraints, side effects, costs, permissions, and success/failure states before designing controls. Never promise an operation or expose a choice the implementation cannot honor.
- Design for phone widths first: narrow and typical screens, portrait, system bars/insets, keyboard/IME, scrolling, dialogs, long labels/model IDs, and font scaling.
- Never rely on a horizontal row of long chips/buttons that can clip or hide choices. Use adaptive wrapping, FlowRow, deliberate grouping, or vertical layouts. Keep primary actions visible and reachable with comfortable touch targets.
- Reuse Material 3 theme and shared components. Maintain consistent typography, spacing, shapes, color semantics, and selected/loading/disabled/error states.
- For API/paid/network operations, show the active provider/model and meaningful verified pricing/limits where available; distinguish estimates from actual charges and never invent balance, quota, progress, or success.
- Review empty, loading, success, error, selected, disabled, keyboard-visible, and long-content states. Preserve safe drafts/selections on failure. Avoid automatic retries that could duplicate paid or destructive operations.
- Check accessibility: touch targets, font scaling, contrast, semantics, and focus order.
- If previews/tests exist, use them. Otherwise document the reasoned review and mark device verification pending.
- Update UI_DESIGN.md when reusable design/interaction standards change. Record each meaningful UI change and its validation limits in PROGRESS.md; update PROJECT.md when product-wide behavior or constraints change.
- If a dedicated UI/product-design skill exists, read it before editing. If none exists, follow UI_DESIGN.md and never claim a skill was consulted.

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

## Current project status
The app is an established Kotlin + Jetpack Compose Android project. For current implementation state, active providers/models, and next steps, read PROJECT.md and the latest entries in PROGRESS.md; do not rely on this file for a stale task description.
