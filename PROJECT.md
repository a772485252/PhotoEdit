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
- User explicitly authorized committing their non-secret Cloudflare Account ID as the default: `4d17f32858b5ec94a9d493d703b54fb9`.
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

## V0.5 selectable chat platforms and models (current)
- Retain Groq and add OpenRouter; enum-based fixed endpoints and shared HTTP transport. No added SDK or Gradle dependency.
- Default first-run platform is OpenRouter; save last platform and per-platform model IDs locally.
- Groq retains groq_chat_private preferences/Keystore identity; OpenRouter uses openrouter_chat_private. Never migrate or reuse tokens across platforms. Cloudflare remains unchanged.
- Models: per-platform presets, custom model ID, explicit OpenRouter catalog refresh/search. Catalog input/output modalities must include text; exclude non-chat endpoints when supplied.
- OpenRouter defaults to :free variants; paid/custom non-free access requires explicit session-only opt-in. When off, request provider.max_price prompt/completion/request = 0 and allow only :free or openrouter/free. No paid tools/plugins enabled.
- Presets are convenience entries, not proof of live availability. Refresh reads platform catalog prices and preserves selection; failures keep the previous catalog.
- No automatic catalog fetch at startup, retry, cross-platform failover or paid-model fallback. OpenRouter may route the selected model among its own providers; manual openrouter/free may select different free models.
- Existing bounded text context, prompt transfer and encrypted user-key storage retained. Changing platform/model/mode starts a new conversation and keeps the current input draft.
- New ChatModels rules/tests cover custom model validation, paid gating, unknown prices and preserved Groq storage identity. CI runs unit checks, lint and APK build; actual API/device validation still requires on-device keys.
- Current version: 0.6.1 / versionCode 8.
- Official contracts: https://openrouter.ai/docs/api_reference/overview , https://openrouter.ai/docs/api-reference/models/get-models , https://openrouter.ai/docs/guides/routing/provider-selection .

## Personal persistent debug signing
- Reuse standard debug signing via one Actions secret PHOTOEDIT_DEBUG_KEYSTORE_BASE64. No separate password secrets or release-signing configuration. See docs/SIGNING.md.
- Push/manual publishing requires this key; PR checks use disposable keys and never publish latest-apk. Secret provisioning and signed CI build verified in run 41954151050; APK certificate matches the persistent key. Device update verification remains pending. Debug signing explicitly resolves HOME/.android/debug.keystore; workflow rejects certificate mismatches before publication.

## V0.6 offline pose picker
- Bundle 24 original VectorDrawable diagrams, front view with legs separated; 18 standing and 6 seated. No third-party image redistribution or new dependencies.
- AI prompt button opens category grid and preview; confirmation replaces only a managed pose paragraph. Existing text/reference preserved, no automatic generation or upload.

- V0.6.1 AI layout uses a compact header, scrolling conversation, and bounded bottom composer; credentials/details are in a scrollable settings dialog. Attachments display as small thumbnails, and keyboard uses adjustResize.
