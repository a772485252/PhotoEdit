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
11. APK build policy (user-requested only): ordinary code edits and commits must NOT trigger GitHub Actions APK builds. The Android APK workflow must remain manual-only (`workflow_dispatch`). Accumulate related code changes and commits without building. Only trigger a build when the user explicitly asks for an APK/download link (e.g. “生成最新 APK 下载链接”). Then verify tests, lint, build, signing, and that `latest-apk/PhotoEdit-debug.apk` was updated before sharing a link. Do not manually start a build just to validate each small code change.
12. Keep a lightweight unit-test suite for critical domain rules; do not remove the test module merely to avoid occasional stale assertions.
13. Prioritize tests for model/provider validation, API credential and prompt validation, billing/price selection, image/reference constraints, and error classification.
14. Do not add UI snapshot tests, broad network mocks, or test-only architecture unless a concrete regression risk justifies them.
15. When behavior or model defaults change, update the corresponding focused tests in the same change. If a test fails, determine whether production behavior or the assertion is outdated; never delete a valid test just to make CI green.
16. Keep unit tests in the test source set; they are not packaged into the APK. CI should run testDebugUnitTest before publishing an APK.


## UI design and review (mandatory)
- Before any UI change, inspect the target screen, its parent navigation/container, shared components, and existing theme. Read the current UI guidance in PROJECT.md; do not make an isolated visual patch without understanding surrounding layout.
- Preserve a consistent product design system: typography hierarchy, spacing scale, component shapes, colors, interaction states, and information density. Prefer existing Material 3 components and shared components over one-off styling.
- Design for real phone widths first. Check narrow and typical Android widths, portrait layout, system bars, keyboard/IME, scrolling, dialogs, long localized labels, and accessibility/font scaling where relevant.
- Never rely on a horizontal row of multiple long chips/buttons if they can be clipped or pushed off-screen. Use adaptive wrapping, FlowRow, Lazy layouts, or deliberate grouping; ensure every action remains discoverable and reachable.
- Keep primary actions visible and visually prioritized. Avoid accidental scroll traps, oversized headers, excessive nested cards, crowded settings dialogs, and controls that require horizontal scrolling unless the pattern is intentional.
- Before finishing, review the full screen hierarchy and state variants (loading, empty, error, selected, disabled, long text). If a UI preview/test infrastructure exists, use it; otherwise perform a reasoned layout review and explicitly mark device verification as pending.
- When a UI change affects the design system or reusable patterns, update the UI guidance in PROJECT.md. Record every UI change and its validation limits in PROGRESS.md in the same session.
- If a dedicated UI/product-design skill or repository design-system document is available, read and follow it before editing. If none is available, say so and apply these rules; never claim a skill was consulted when it was not.

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
