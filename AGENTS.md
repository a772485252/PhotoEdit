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
11. APK build policy (user-requested only): ordinary code edits and commits must NOT trigger GitHub Actions APK builds. APK builds must only be triggered on explicit user request, via `workflow_dispatch` or the dedicated `build-trigger.txt` push trigger when workflow dispatch is unavailable; never trigger by routine code changes. Accumulate related code changes and commits without building. Only trigger a build when the user explicitly asks for an APK/download link (e.g. “生成最新 APK 下载链接”). Then verify tests, lint, build, signing, and that the versioned APK `PhotoEdit-v<versionName>.apk` was uploaded as an Actions artifact from the successful run before sharing a link. Do not manually start a build just to validate each small code change.
12. APK workflow supports two modes (manual dispatch or explicitly requested push-trigger): quick (default) runs Gradle assembleDebug with build cache and parallel execution, then always verifies the fixed APK signature before publishing; full runs testDebugUnitTest + lintDebug + assembleDebug before the same signature verification and publishing. Use quick for routine UI/text-only changes; use full for domain/business logic, API/provider, storage, image-processing, dependency, or important release changes. The mode must be visible in the run title and docs. Never describe quick mode as tested by unit tests or lint.
13. APK workflow run titles must include the app version (e.g. `PhotoEdit v0.9.24 · Android APK`). The manually entered/workflow-default version must be validated against `app/build.gradle.kts` before building; keep the workflow_dispatch default synchronized whenever versionName changes. Do not publish a misleading version title.
14. Keep a lightweight unit-test suite for critical domain rules; do not remove the test module merely to avoid occasional stale assertions.
15. Prioritize tests for model/provider validation, API credential and prompt validation, billing/price selection, image/reference constraints, and error classification.
16. Do not add UI snapshot tests, broad network mocks, or test-only architecture unless a concrete regression risk justifies them.
17. When behavior or model defaults change, update the corresponding focused tests in the same change. If a test fails, determine whether production behavior or the assertion is outdated; never delete a valid test just to make CI green.
18. Keep unit tests in the test source set; they are not packaged into the APK. CI should run testDebugUnitTest before publishing an APK.


## APK artifact distribution (mandatory)
- Never commit APK/AAB binaries to the source repository. Ignore them in Git; do not use force-add, repository publishing commits, or a latest-apk directory.
- Upload the signed APK as a GitHub Actions artifact named PhotoEdit-v<versionName>-apk containing PhotoEdit-v<versionName>.apk. Derive the version from app/build.gradle.kts.
- Keep workflow contents permission read-only. No automated commits, reset, or push during builds.
- Share a verified successful Actions run/artifact link; artifacts currently expire after 30 days and downloads require GitHub access. An expired artifact requires a new explicitly requested build.
- Preserve fixed signing identity and monotonic versionCode for app updates. Build only on explicit user request.
- 每次发布包含新代码的新 APK，versionName 必须使用未发布过的新版本号，versionCode 必须大于此前发布值；不得沿用旧版本号发布不同内容。构建前核对版本历史与最新成功构建，并同步 app/build.gradle.kts、workflow_dispatch 默认值、工作流标题和产物名称。仅重试同一源码版本的失败构建可保留版本号；已发布产物的原样重建须明确说明。

## UI design and review (mandatory)
- Every UI change must read and follow the project-local skill `.agents/skills/photoedit-ui/SKILL.md`. It adapts task-based design reasoning to this native Android app. Apply only the checks relevant to the change; a text edit does not require a full redesign.
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
- Keep the local UI skill and UI_DESIGN.md consistent; the skill describes the design method and UI_DESIGN.md records current shared interaction patterns.

## Proportionate implementation and tests (mandatory)
- Do not add speculative defensive code, repeated internal validation, automatic retries, fallback frameworks, or routine confirmation dialogs. Keep checks that protect real external-input boundaries, resource limits and duplicate paid submissions.
- Keep the existing critical domain tests. Pure wording/style/layout changes do not require new tests that mirror code, snapshot suites or broad mocks. Add a focused test only when a changed business rule or concrete regression warrants it; do not expand the test surface for completeness alone.

## Token 与工具使用效率（mandatory）
- 新会话仍按连续性规则读取项目规则与当前进度；同一会话中，未变化且已经读过的文件和已核实结果直接复用，不反复整篇读取。
- 搜索先用 rg 定位，再读取相关函数、行段或差异。仅在理解依赖确有需要时扩大范围；避免一次输出多个完整大文件，导致截断后再补读。
- 工具结果先输出必要的状态摘要、错误位置和相关上下文，不默认打印全部日志、源码、工具说明或大型元数据。日志仅在失败或结论不明确时深入读取。
- 独立的搜索和读取尽量合并；有依赖的操作依次执行。小改动只检查受影响部分，不为确认同一结论重复搜索或增加机械测试。
- CI 通常每 30～45 秒查询一次精简状态；没有变化不重复读取完整步骤或日志。等待时使用等待工具，不靠连续短轮询；失败后再定位具体任务。进度更新遵循当前会话要求，避免反复叙述相同状态。
- 同一次成功构建的日志、签名步骤、产物元数据和下载验证结果记录后复用；没有新提交、失败或未解决问题时，不重复构建、下载或全面验证。
- 外部资料只查当前决策需要的官方接口、价格、支付与政策；已有且仍适用的核实结果可复用，存在时效变化或疑点时再针对性刷新。交付说明简洁说明改动、证据和限制，不重复工作流水账。
- 节省 Token 不能省略保护用户工作、隐私、付费请求、必要构建验证或交付真实性所需的检查。无法取得实际用量时，不编造 Token 数或节省比例。

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

## Model/provider selection and payment (mandatory, 2026-11-25)
- 后续寻找、推荐或接入付费模型服务，必须先核实其 API 充值支持支付宝或微信支付中的至少一种；这是可用方案的筛选条件，不只是排序偏好。
- 使用服务商当前官方充值/支付说明核实，区分 API 余额充值与网页会员订阅。支付方式不明时标记为未确认，不将其推荐为可直接使用的付费方案；不能仅凭第三方宣传或历史支持作结论。
- 同时核实目标模型可用性、API 价格、地区资格和内容规则。不要因为支付方便就推断某个模型已被提供；不要建议绕过地区资格。
- 免费、无需充值即可使用的方案不强加支付方式要求；如果涉及转为付费或扩展额度，再应用上述筛选条件。
- Venice 聊天与生图已按用户要求移除，不再作为当前接入或可用推荐。只有用户明确改变要求时才重新评估。
- Existing integrations are not proof of current payment support. Future paid-provider recommendations require current verification, including for previously integrated platforms.
