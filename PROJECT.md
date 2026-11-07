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
- Network: offline core features; optional Cloudflare AI requires internet and a user-provided token.
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

## Cloudflare AI integration — personal defaults
- The app uses a configured Cloudflare account identifier; its value is supplied through protected local configuration and is intentionally not recorded in project documentation.
- Pre-fill this Account ID in the future AI settings screen; retain an editable override.
- API Token must remain empty by default, entered on-device and stored using Android Keystore-backed encryption; never commit it or embed it in the APK.
- Personal use; cloud text-to-image/reference-image editing planned. Keep existing offline tools available without credentials.
- V0.3 adds an AI tab: editable account ID, masked token, save/clear local settings, text-to-image and one-reference-image generation, result preview and PNG gallery save.
- Direct HTTPS multipart requests to api.cloudflare.com; fixed FLUX.2 Klein 4B model, 1024x1024 output. Reference image is EXIF-normalized and reduced to max 511px per axis before PNG upload.
- No requests until the user taps Generate. No automatic retries or paid/provider fallbacks. Code 3036 explicitly reports daily quota exhaustion, 3040 capacity and 5035 paid-plan requirements.
- Platform Free-plan enforcement provides the spending limit. App cannot guarantee zero charges for an account already on a paid plan, nor accurately query remaining quota.
- Android Keystore AES-GCM encrypts the saved token in app-private preferences. Token is not saved to a Bundle, logs, source or CI; application backup stays disabled.
- Existing offline grid splitter is retained. AI image generation sends requests only on Generate; V0.4 chat sends text only on Send.
- AI integration is build-testable without credentials, but real endpoint behavior and identity consistency require device/account validation.
- Additional free providers are not configured: same-provider models share the account daily quota, so swapping them does not replenish it.

## V0.4 independent prompt assistant
- Explicit user requirement: chat must not consume Cloudflare image-generation quota.
- Separate provider: Groq REST Chat Completions, fixed https://api.groq.com/openai/v1/chat/completions and openai/gpt-oss-20b.
- Requires a separate user-created Groq API Key. Never reuse or send Cloudflare credentials to Groq, or Groq credentials to Cloudflare.
- Reuses AES-GCM storage implementation with a distinct preferences namespace and Keystore alias; original Cloudflare alias is preserved for compatibility.
- Chinese prompt-writing mode and general chat mode; mode change starts a new chat. Text only, no reference-image upload, no browsing or external tools.
- Successful replies can be copied; prompt-mode replies up to 4000 characters can populate the image prompt without starting generation. Existing reference image remains selected.
- Bound request context to 3 previous complete turns / 6000 characters plus system instruction, current input max 2000 characters, reply budget 2048 completion tokens including reasoning.
- Free Groq account subject to independent RPM/TPM/RPD/TPD limits. No automatic retry, account upgrade or paid fallback. Already-paid accounts are billed according to their platform settings.
- History stays only in ViewModel memory; no disk or Bundle persistence. Failed sends retain input and successful prior history.
- Official references: https://console.groq.com/docs/rate-limits and https://console.groq.com/docs/api-reference .

## V0.5 selectable chat platforms and models (historical milestone)
- Retain Groq and add OpenRouter; enum-based fixed endpoints and shared HTTP transport. No added SDK or Gradle dependency.
- Default first-run platform is OpenRouter; save last platform and per-platform model IDs locally.
- Groq retains groq_chat_private preferences/Keystore identity; OpenRouter uses openrouter_chat_private. Never migrate or reuse tokens across platforms. Cloudflare remains unchanged.
- Models: per-platform presets, custom model ID, explicit OpenRouter catalog refresh/search. Catalog input/output modalities must include text; exclude non-chat endpoints when supplied.
- OpenRouter defaults to :free variants; paid/custom non-free access requires explicit session-only opt-in. When off, request provider.max_price prompt/completion/request = 0 and allow only :free or openrouter/free. No paid tools/plugins enabled.
- Presets are convenience entries, not proof of live availability. Refresh reads platform catalog prices and preserves selection; failures keep the previous catalog.
- No automatic catalog fetch at startup, retry, cross-platform failover or paid-model fallback. OpenRouter may route the selected model among its own providers; manual openrouter/free may select different free models.
- Existing bounded text context, prompt transfer and encrypted user-key storage retained. Changing platform/model/mode starts a new conversation and keeps the current input draft.
- New ChatModels rules/tests cover custom model validation, paid gating, unknown prices and preserved Groq storage identity. CI runs unit checks, lint and APK build; actual API/device validation still requires on-device keys.
- Historical milestone version: 0.9.1 / versionCode 13.
- Official contracts: https://openrouter.ai/docs/api_reference/overview , https://openrouter.ai/docs/api-reference/models/get-models , https://openrouter.ai/docs/guides/routing/provider-selection .

## Personal persistent debug signing
- Reuse standard debug signing via one Actions secret PHOTOEDIT_DEBUG_KEYSTORE_BASE64. No separate password secrets or release-signing configuration. See docs/SIGNING.md.
- Push/manual publishing requires this key; PR checks use disposable keys and never publish latest-apk. Secret provisioning and signed CI build verified in run 41954151050; APK certificate matches the persistent key. Device update verification remains pending. Debug signing explicitly resolves HOME/.android/debug.keystore; workflow rejects certificate mismatches before publication.

## V0.6 offline pose picker
- Bundle 24 original VectorDrawable diagrams, front view with legs separated; 18 standing and 6 seated. No third-party image redistribution or new dependencies.
- AI prompt button opens category grid and preview; confirmation replaces only a managed pose paragraph. Existing text/reference preserved, no automatic generation or upload.

- V0.6.1 AI layout uses a compact header, scrolling conversation, and bounded bottom composer; credentials/details are in a scrollable settings dialog. Attachments display as small thumbnails, and keyboard uses adjustResize.

## V0.7 AI Horde
- Add optional free community image generation via https://aihorde.net/api/v2. Blank key uses anonymous 0000000000; personal key uses a separate encrypted namespace. No paid fallback or automatic resubmission.
- Default first selection AI Horde; persist chosen platform, model and NSFW flag. Existing provider credentials unchanged. Keep draft/reference on platform switch.
- 512x512, 25 steps, one image, platform allow_downgrade; optional img2img uses max512 WebP input, denoising strength 0.55. Model availability and compatible workers vary; refresh active model list explicitly.
- NSFW requests default enabled per user's preference; node/platform rules remain enforced. trusted_workers=true. Registered requests shared=false; anonymous txt2img sharing is controlled by Horde.
- Poll check endpoint every 3 seconds, retrieve status/results only when done. Keep ID and submitted prompt/reference flag in ViewModel memory; stop waiting and resume query reuse ID. Explicit server cancel available when not polling. No disk chat history; process exit loses pending task handle.
- Show real queue estimates, disable changing input while a task is pending, preserve draft on errors, distinguish timeout/unknown submission. Existing API Key masking/signing remain unchanged.

## V0.7.1 512px portrait defaults
- Keep output 512x512. Default Realistic Vision, with AbsoluteReality quick choice; migrate only legacy stable_diffusion default once and preserve explicit custom models.
- These two SD1 portrait models use DPM++2M/Karras, 28 steps, CFG6 and transparent photographic/negative prompt augmentation; custom models retain generic original settings. No automatic prompt translation.
- Disable allow_downgrade for every Horde request; insufficient Kudos is surfaced rather than silently lowering quality.
- Reference changes use selectable 0.35/0.55/0.65 strength, default 0.55; WebP quality100 max512 input, fit aspect preserved. Lower strength favors source retention but does not restore lost detail or guarantee identity.

## V0.8 APIPod paid image provider
- Optional provider with distinct apipod_private encrypted Key; retain free Horde as default and existing providers. No auto billing or provider fallback.
- Explicitly show selected request model and indicative per-request charge: z-image-spicy ($0.015) / optional z-image-spicy-pro ($0.025) for text; qwen-image-edit-spicy ($0.04) for one reference. Prices subject to platform settlement; usage.cost displayed when supplied.
- Normal fixed HTTPS API only. Show documented mainland China/Hong Kong/Macau restriction; do not promise VPN access or implement any region-bypass mechanism.
- Async POST with per-logical-operation Idempotency-Key and retained exact body/key; authenticated status polling with bounded backoff/jitter. In-memory pending state survives local stop/poll failures; continue-query reuses task ID or same idempotent create. No automatic POST retries.
- Data-URI PNG reference max1024, body bounded; results download without forwarding Key, HTTPS/no redirects/private-address filtering, bounded bytes and decode size.
- Stop-wait pauses locally only. Explicit confirmed end-tracking does not cancel platform task or refund. All history/operation handles are session memory only; do not force-close while task unresolved.

## V0.9 Venice image provider
- Optional paid provider, distinct encrypted venice_private Key; no SDK/dependencies and existing defaults/signing preserved.
- Text: Z-Image Turbo 1024x1024, indicative $0.01. One reference or previous result: FireRed image edit, indicative $0.04. Explicit routing/prices displayed before submission. PNG reference max1024 with256 minimum short edge; no paid prompt enhancement.
- Synchronous binary responses, bounded decode and fixed HTTPS destination. Keep default platform content processing; report blurred output. Content acceptance/anatomy not guaranteed.
- No automatic retries or claimed server cancellation/status queries. Ambiguous outcomes lock new submission until user checks usage and acknowledges; input/result retained. All operation state session-only. See docs/VENICE.md.

## V0.9.1 pose catalog
- 29 selected full_sexy IDs and detailed original Chinese pose prompts; generic original vector category illustrations, no upstream image assets. See docs/POSES.md.


## Testing strategy — lightweight and mandatory
- Keep the existing JUnit unit-test module; do not remove it to avoid a stale or failing assertion.
- Test only critical, deterministic domain logic: provider/model ID validation, API key and prompt validation, price/model routing, image/reference dimension constraints, and important error classification.
- Avoid unnecessary UI tests, snapshot suites, broad network mocks, and test-only architecture. Add such tests only when a concrete risk justifies the maintenance cost.
- When a model ID, default, price, routing rule, or validation behavior changes, update its focused test in the same change.
- Diagnose failures before changing tests. Fix production code when behavior is wrong; update assertions only when the intended behavior has deliberately changed. Never delete a valid test merely to make CI pass.
- Unit tests run in CI before APK publishing and are not packaged into the APK, so retaining JUnit does not increase installed APK size.
- Current approach: small focused tests, build/lint/test gate, then real-device/API verification for behavior that unit tests cannot prove.


## Current Prompt Assistant providers and billing visibility (2026-11-06)
- Active selectable chat providers in current source/UI: Cloudflare Workers AI (default GLM-4.7-Flash), Venice AI (Venice Uncensored 1.2), and APIPod (Grok 4.5). APIPod Grok 4.5 uses the APIPod provider and the shared encrypted APIPod key; it is not a direct xAI account integration.
- Chat header displays the selected model name and model ID, plus a per-10,000-Chinese-character price guide for known models. Estimate assumes approximately one token per Chinese character; actual tokenization and usage vary.
- Public price references used for the estimates:
  - Cloudflare GLM-4.7-Flash: $0.0605/M input tokens and $0.40/M output tokens; approximately 55 input / 364 output Neurons per 10,000 assumed tokens. Workers AI free allocation is 10,000 Neurons/day; the app does not query remaining account Neurons.
  - Venice Uncensored 1.2: $0.20/M input and $0.90/M output tokens. The app can query the documented API key rate-limits/balances endpoint and display USD, DIEM, and bundled credits when returned.
  - APIPod Grok 4.5: published xAI-default route is $1.40/M input and $4.20/M output tokens; Grok Lite is $0.20/M and $0.60/M. Because APIPod channel routing can vary, the app displays a reference range rather than claiming a fixed per-request charge.
- APIPod API-key balance is not currently queried inside the app; the UI directs users to the APIPod console. Do not invent or call undocumented balance endpoints. Cloudflare remaining Neurons are also not queried in-app.
- Actual tokenization, context length, model routing/channel, cache usage, and provider price changes affect final charges. The per-character figures are estimates only, not a billing guarantee.
- Public references: https://developers.cloudflare.com/workers-ai/models/glm-4.7-flash/ ; https://developers.cloudflare.com/workers-ai/platform/pricing/ ; https://docs.venice.ai/api-reference/endpoint/api_keys/rate_limits ; https://www.apipod.ai/llm/xai/grok-4.5 .
- Current source version: 0.9.23 / versionCode 35. APK workflow remains manual-only; do not build unless the user explicitly requests an APK.

## Documentation and privacy requirements (current, 2026-11-06)
- Every meaningful code/config/UI/model/API/build-rule change must be documented in PROGRESS.md in the same work session. Update this PROJECT.md at the same time when current architecture, active providers/models, user-visible behavior, constraints, or operating rules change.
- At the start of each new session, read AGENTS.md, PROJECT.md, and the latest end of PROGRESS.md. Source code is authoritative if a discrepancy is found; reconcile documentation before continuing.
- Never put API keys/tokens/passwords, signing material or fingerprints, personal/account identifiers, private request/task IDs, emails/phone numbers, precise locations, personal photos/metadata, exact account balances/usage, or other private user information in project documentation, commit messages, tests, or logs.
- Keep documentation limited to project-level technical facts, public API contracts, and non-sensitive test/build outcomes. Audit diffs for accidental disclosure before committing.
- Current source version: 0.9.22 / versionCode 34. APK workflow remains manual-only; do not build unless the user explicitly requests an APK.


## Prompt Assistant provider selector (current, 2026-11-06)
- “模型与 API 设置” dialog offers Cloudflare · GLM, Venice · Uncensored, and APIPod · Grok 4.5. APIPod's chat model ID is `grok-4.5`; it reuses the encrypted APIPod key shared with image generation.
- Provider choices use a two-row layout so APIPod remains visible on narrow devices. This provider selector is for text chat/prompt expansion; it is distinct from image-generation model selection.
- Current source version: 0.9.24 / versionCode 36. APK workflow remains manual-only.


## UI design system and implementation guidance (current)
- UI framework: Jetpack Compose + Material 3. Reuse the existing theme and shared components in `ui/theme` and `ui/components`; avoid one-off styling where a shared pattern exists.
- Layout principles: phone-first, adaptive width, clear hierarchy, consistent spacing/typography/shapes, reachable primary actions, and readable localized labels. Prefer wrapping or grouped rows over clipped horizontal control rows.
- Review all relevant states: empty, loading, error, selected, disabled, long text, keyboard visible, and scroll behavior. Consider narrow Android phone widths, system bars, dialogs, and font scaling.
- Before editing a screen, inspect the whole screen and its parent container rather than changing a single control in isolation. Use Material 3 conventions and preserve visual consistency across screens.
- No dedicated Android UI/Product Design skill or standalone design-system document is currently present in the repository or installed skill catalog. Follow AGENTS.md UI design and review rules until one is added.


## UI design standard (mandatory)
- The project-wide UI and interaction design standard is maintained in `UI_DESIGN.md`. Read it before every UI change.
- UI decisions must account for user feel, mobile ergonomics, one-hand reachability, screen insets/keyboard/scrolling, accessibility, and all relevant UI states.
- UI must be designed around real business logic: user goals, validation, constraints, side effects, permissions, costs, API availability, and error recovery. Do not create controls or promises unsupported by implementation.
- UI_DESIGN.md is the source of truth for reusable interaction and visual rules; AGENTS.md makes following it mandatory.


## APK workflow run naming (current)
- GitHub Actions manual APK runs display `PhotoEdit v<versionName> · Android APK` in the Actions run list.
- The `workflow_dispatch` input `app_version` defaults to the current app version and is validated against `app/build.gradle.kts` before any build starts. If the app version changes, update the default; mismatches fail early instead of producing a misleading run title.


## Prompt determinism and anatomy integrity (current)
- Pose presets describe body position only; camera presets independently determine direction, height and full-body framing. Do not embed competing camera directions inside pose descriptions.
- Prompt-assistant system guidance asks for one definite selection per visual dimension, avoiding alternatives or ambiguous phrasing while respecting explicitly selected outfit, pose and camera.
- PromptComposer adds one managed anatomy-quality block to generated composed prompts: natural limb proportions, correct counts, complete hands/feet/fingers, and no duplicated, missing, fused or unintentionally cropped limbs. This is a quality instruction, not a guarantee of image model output.
- Source version 0.9.25 / versionCode 37; the manually dispatched APK workflow default version is 0.9.25. No automatic APK build.


## Prompt assistant close full-body default (current)
- In the Prompt Assistant's Generate Prompt mode (including APIPod Grok 4.5), the system instruction defaults to close full-body framing: subject fills most of the frame while head, hands, and feet remain fully visible with a small safe margin.
- The assistant must pick a single definite pose/camera direction when unspecified, respect user-specified composition, avoid mutually exclusive alternatives, and return one directly usable prompt rather than options or explanations.
- Anatomy-quality guidance applies to this chat-generated text too; no guarantee of image-model compliance. This changes chat prompt generation guidance, not image generation or API routing.


## Editable prompt-assistant instructions (v0.9.26)
- In Generate Prompt mode, an “扩写规则” control opens one editable multi-line instruction field with Save, Cancel and Restore Default actions.
- The custom instruction is stored locally in Android SharedPreferences and applied as the system instruction to future prompt-generation requests for all active chat providers; ordinary chat keeps its own default instruction. No prompt history is persisted.
- Saving or restoring rules updates the system instruction for the next prompt request without clearing the in-memory conversation or unsent input draft. The default instruction remains ChatRules.SYSTEM_PROMPT. Existing conversation context is retained and may still inform the next request. Maximum rule length is 8000 characters; empty rules cannot be saved.
- No network request is made when editing rules. Source version 0.9.26 / code 38; APK workflow remains manual-only.


## V0.9.27 prompt framing refinement
- Default prompt assistant instructions prioritize fully visible head, hands and feet with narrow safe margins before maximizing subject size. Minimize unnecessary background space and perspective distortion. Existing locally saved custom instructions are preserved until the user selects Restore Default.
- Manual APK workflow version updated to 0.9.27 / versionCode 39.


## APK build modes and performance (current)
- Manual-only GitHub Actions workflow offers Quick (default) and Full modes; run title includes app version and selected mode.
- Quick: `gradle --build-cache --parallel assembleDebug`. It compiles and packages the APK, then performs the same fixed-keystore signature verification and publication steps. It skips unit tests and Android Lint; therefore it does not establish behavioral correctness or lint cleanliness.
- Full: `gradle --build-cache --parallel testDebugUnitTest lintDebug assembleDebug`, followed by identical signature verification and publication.
- Gradle setup-gradle action caches Gradle user-home dependencies/state. Neither mode runs `clean` or disables build cache, allowing incremental/cache reuse on compatible runners.
- Use Quick for routine UI/text-only changes. Use Full for domain/business rules, API/provider, storage, image-processing, dependency changes, and important milestone releases.

## V0.9.28 Screen-off image generation (2026-11-06)
- Added a user-started Android foreground service for AI image generation. The service displays an ongoing notification and holds a bounded 30-minute partial wake lock so the existing ViewModel request/polling coroutine can continue while the display is off.
- The notification opens PhotoEdit and explicitly states that the generated result is previewed in-app and is not automatically saved to the gallery. Gallery export remains behind the existing explicit “保存图片” action.
- Added the Android foreground-service/data-sync and WAKE_LOCK declarations. No WorkManager dependency or provider/API routing change.
- Scope/limitations: this protects normal screen-off operation while the app process remains alive; it does not make provider task state durable across force-stop, process death, device reboot, or OEM battery-manager termination. Doze/network policy and vendor power management may still affect connectivity. Results remain in ViewModel memory and are not written to gallery automatically.
- Source version 0.9.28 / versionCode 40. APK workflow remains manual-only.


## V0.9.30 Preserve Prompt Assistant conversation when editing rules (2026-11-06)
- Fixed Save Rules and Restore Default: both operations previously cleared the in-memory chat messages. They now update only the active prompt instruction and status notice; the conversation transcript and unsent composer draft remain unchanged.
- The updated instruction is used on the next prompt-mode request. Existing conversation turns remain available as context; users can explicitly start a new chat when they want a clean context.
- Updated the rule-editor explanation to state that the current conversation and input draft are retained. Added a focused ChatRules test proving a custom system instruction can be applied while previous conversation turns remain in the request context.
- Source version 0.9.30 / versionCode 42. APK workflow remains manual-only; this release is built only on explicit user request.


## V0.9.31 Prompt Assistant background generation (2026-11-06)
- Added a dedicated foreground service for Prompt Assistant send and regenerate actions. It starts only after the user explicitly taps Generate/Regenerate, shows an ongoing notification, and holds a bounded 30-minute partial wake lock while the existing ViewModel request runs.
- The chat request remains in ChatViewModel and uses the existing provider/model/key and billing path; no duplicate request, automatic retry, new API, or dependency was introduced. The service improves process priority and screen-off CPU availability but does not make the request durable if Android kills the app process or the user force-stops it.
- Added a separate notification channel and service declaration. Chat UI now explains that the user can switch apps during generation and that notification visibility depends on Android notification permission/settings.
- The generated text remains in the existing in-memory chat state. No automatic file/gallery write or chat-history persistence was added.
- Source version 0.9.31 / versionCode 43. APK workflow remains manual-only.

## 2026-11-07 prompt assistant interaction update
- Prompt assistant displays compact provider/model summary; cost and model metadata are expandable rather than always occupying the conversation viewport.
- Mobile composer uses a full-width input and full-width primary action, with mode and secondary settings controls on separate rows.
- Chat HTTP 403 reports a service-side rejection with possible causes, not a verified content-moderation diagnosis. No automatic paid retries or model switching.
- Device/API verification and full CI checks remain pending until user requests an APK build.

## Venice optional DeepSeek text model (2026-11-07)
- Prompt assistant Venice selector offers DeepSeek V4.1 Flash Uncensored (ID deepseek-v4-1-flash-uncensored) alongside Venice Uncensored 1.2. Uses existing Venice API Key, HTTPS chat client and per-token billing. APIPod credits cannot fund Venice requests.
- Venice public listed prices: USD 0.33 input / 1.32 output per million tokens; displayed character-based estimate is approximate. Model availability and credits require user account verification.

## APIPod Grok 4.6 (2026-11-07)
- Prompt assistant APIPod models include grok-4.5 (default) and grok-4.6 (optional). Both use existing APIPod credentials, client, and balance. Grok 4.6 API ID is `grok-4.6` and displayed estimated pricing uses APIPod xAI channel rates; actual billed usage may differ.
- Build only on explicit user request for APK download; pending build verification.

## 2026-11-07 Versioned APK filenames
- APK publish workflow now writes `latest-apk/PhotoEdit-v<versionName>.apk` alongside legacy `latest-apk/PhotoEdit-debug.apk`, preserving old link compatibility. Version read from Gradle. Change applies on next successful build; not retroactively published.

## 2026-11-07 Versioned-only APK policy
- User confirmed legacy `PhotoEdit-debug.apk` URL need not be maintained. Workflow now publishes only `latest-apk/PhotoEdit-v<versionName>.apk`, version read from Gradle; existing legacy file may remain but is no longer updated or recommended.
- Updated AGENTS.md mandatory rules and workflow publishing. No build triggered for this configuration-only request; verify on next explicitly requested build.
