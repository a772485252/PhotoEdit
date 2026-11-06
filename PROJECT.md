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
- Active selectable chat providers in current source/UI: Cloudflare Workers AI (default GLM-4.7-Flash), Venice AI (Venice Uncensored 1.2), and APIPod (Grok 4.5).
- APIPod Grok 4.5 uses the APIPod provider and the same encrypted APIPod Key as APIPod image generation. It is not an xAI direct API connection; do not use xAI direct prices as a proxy for APIPod routing prices.
- Venice chat and Venice image generation use the same encrypted Venice Key namespace. This is a separate Venice API integration; the presence of code does not mean a key has been configured or that live authenticated API calls have been validated.
- Chat header displays provider, selected model display name, and exact model ID. It labels each provider's billing method but intentionally does not invent per-character prices.
- Live balance lookup is not yet implemented/verified for these providers. To show approximate cost per 10,000 Chinese characters, obtain verified input/output prices for the exact provider/model route and state the approximation assumptions. Actual charges should use returned token usage/settlement data where available.
- Current code version: 0.9.22 / versionCode 34. Latest prompt-assistant first-use UI refinement and model identity display are committed; APK build remains manual-only and requires explicit user request.


## Documentation and privacy requirements (current, 2026-11-06)
- Every meaningful code/config/UI/model/API/build-rule change must be documented in PROGRESS.md in the same work session. Update this PROJECT.md at the same time when current architecture, active providers/models, user-visible behavior, constraints, or operating rules change.
- At the start of each new session, read AGENTS.md, PROJECT.md, and the latest end of PROGRESS.md. Source code is authoritative if a discrepancy is found; reconcile documentation before continuing.
- Never put API keys/tokens/passwords, signing material or fingerprints, personal/account identifiers, private request/task IDs, emails/phone numbers, precise locations, personal photos/metadata, exact account balances/usage, or other private user information in project documentation, commit messages, tests, or logs.
- Keep documentation limited to project-level technical facts, public API contracts, and non-sensitive test/build outcomes. Audit diffs for accidental disclosure before committing.
- Current source version: 0.9.22 / versionCode 34. APK workflow remains manual-only; do not build unless the user explicitly requests an APK.
