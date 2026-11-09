# PhotoEdit — Development Progress

Last updated: 2026-11-05

## Current milestone
V0.5 selectable Groq/OpenRouter platforms and models implemented, CI verified and APK published. Live API/device verification pending.

## Existing verified baseline
- Native Kotlin + Jetpack Compose skeleton; default branch main.
- Android SDK setup and debug APK build previously verified (run 41766155065).
- latest-apk/PhotoEdit-debug.apk already published automatically by CI.
- Repository: https://github.com/a772485252/PhotoEdit; Windows checkout: D:\Projects\PhotoEdit.

## V0.2 implemented
- Chinese UI, system photo picker with fallback, decoded image preview.
- 2x2 / 3x3 / 4x4 grid selection and numbered result previews.
- Background decode, EXIF orientation correction, centered equal-size grid geometry.
- PNG export from processing-resolution image to MediaStore; pending writes and rollback on failure.
- Android 8/9 legacy storage permission, Android 10+ permission-free gallery writes.
- Busy state prevents overlapping operations; selection cancellation preserves current work; errors shown in Chinese.
- Kept single-module MVVM and separated UI/theme/components, ViewModel, pure domain geometry and Android processing.
- Version 0.2.0 / versionCode 2.
- CI now runs testDebugUnitTest, lintDebug and assembleDebug before APK upload/publication.

## Limits and device checks still required
- Large images are sampled to ~8 MP / max 4096 dimension; actual processing resolution is displayed. Export is lossless PNG at this resolution, not guaranteed original resolution.
- Equal tile sizing trims at most n-1 pixels per axis, centered. No square crop.
- Process death resets the editor; rotation keeps ViewModel state. Session/project continuation is documented here, not editor persistence.
- Real-device testing has NOT been performed. Test vivo S18: airplane-mode local photo selection, all grid sizes, EXIF portrait/mirror, repeated selection/export, PNG order and gallery visibility.
- Test Android 8/9 allow/deny permission; Android 10+ permission-free save; low storage/error cleanup; large-image memory behavior.
- CI debug signing is not yet durable; install-over compatibility between fresh CI builds is not guaranteed.

## Handoff
1. Read AGENTS.md, PROJECT.md, PROGRESS.md and inspect main/status before edits.
2. Check the V0.2 workflow result and latest-apk publication; record actual run/commit evidence below.
3. Install APK on vivo S18 and record results; do not claim device verification based on CI.
4. Preserve current architecture. Future milestones: configurable rows/columns, batch processing, 9:16 composition, still-image motion video.

## V0.2 verified build and publication
- Feature commit: 9d2d1e9c3be083286575f828dd1cc497c5520a9b.
- Successful GitHub Actions run: https://github.com/a772485252/PhotoEdit/actions/runs/41784249143
- Job 126422610796 completed successfully. Logs show testDebugUnitTest, lintDebug and assembleDebug executed; BUILD SUCCESSFUL in 1m 10s.
- Artifact PhotoEdit-debug-apk: ID 13300257422, ZIP size 9,127,134 bytes.
- CI published the APK in commit c3b6e5ff409a1547c4f4deeeb7c120cb01300514.
- Confirmed changed APK blob: 50719869ba81a47eac344441a30f0676ab5473fb.
- Current APK: https://github.com/a772485252/PhotoEdit/raw/refs/heads/main/latest-apk/PhotoEdit-debug.apk
- No build errors occurred in this milestone. Runner action deprecation warnings are non-blocking; review action versions in a later CI maintenance task.
- This evidence verifies compilation, unit checks, lint and publication only; no emulator or physical device tests were run.

## Cloudflare setup handoff (2026-11-04)
- User registered Cloudflare and reached the Workers AI REST API setup page.
- The integration uses the application's configured account identifier from encrypted/local configuration; no user-specific identifier is documented here.
- Keep API credentials out of source control and project documentation; enter them only in the app's protected local settings.
- No AI settings/client or new APK was produced by this documentation-only update. Existing V0.2 APK remains current.

## V0.3 Cloudflare AI implementation (2026-11-04)
- User confirmed Token was generated. No Token was sent to the assistant or included in the repository.
- Added AI tab with pre-filled authorized Account ID, local encrypted Token settings, text-to-image, optional single reference image, result preview and gallery save.
- Kept offline grid features and MVVM separation. Added ai/ network and credential adapters, pure domain/AiRules, AiViewModel, AiScreen and processing/GalleryWriter.
- Added INTERNET permission, no cleartext endpoints. Uses fixed Cloudflare API hostname and model, no arbitrary provider URLs.
- Generation uploads prompt and selected reference only on explicit Generate. Reference longest edge 511px; output 1024x1024.
- No automatic retries, upgrades or provider switching. Daily free quota errors stop; free plan must remain selected in the Cloudflare account to enforce no paid usage.
- Same-provider models share daily account quota; they are not fallback free pools. Other providers await verified recurring free terms and user credentials.
- Added rule tests for account/header validation, empty prompts, quota/capacity/paid-plan distinction.
- Version 0.3.0 / code 3. No actual API or real-device test yet; only CI will be verified here.
- Next device steps: install APK, open AI tab, enter Token, Save Settings, test text-only then reference generation and PNG save; check errors/usage without posting Token.
- Device validation also needed: encrypted settings survive restart, clear Token, rotation during generation, no-network/timeout, Android 8/9 gallery permission, and offline grid regression.
- Session editor images/prompts are not persisted across process death. Last successful output remains shown if a later request fails, labelled accordingly.
- Current CI debug signing remains non-durable across clean runners.
- Official API contract: https://developers.cloudflare.com/changelog/post/2026-01-15-flux-2-klein-4b-workers-ai/
- Official errors: https://developers.cloudflare.com/workers-ai/platform/errors/

## V0.3 verified CI and APK publication
- Feature commit: daf8e2136c44471e54dcb4f054b559914ba13bed.
- Successful run: https://github.com/a772485252/PhotoEdit/actions/runs/41788823975
- Job 126437135268 completed successfully; testDebugUnitTest, lintDebug and assembleDebug executed, BUILD SUCCESSFUL in 41s.
- Artifact PhotoEdit-debug-apk ID 13301858738, ZIP size 9,163,056 bytes.
- APK publication commit d1d418b8d3f1e6f08a153e46b1813168083dd8e1; APK blob c3ff647065d6cb67d47df9543e5fb64806c2f35b.
- Latest download: https://github.com/a772485252/PhotoEdit/raw/refs/heads/main/latest-apk/PhotoEdit-debug.apk
- No build errors in this milestone. No live generation was performed, because the user keeps their Token on-device.
- Next action: user installs V0.3, enters Token in AI tab, saves settings and performs a text-only then reference-image request; record actual response/error codes with credentials redacted.

## V0.4 implementation (2026-11-04)
- User explicitly rejected sharing Cloudflare image quota with chat; no Cloudflare chat endpoint was committed.
- Added Groq GPT-OSS 20B text-only assistant with independent API Key, encrypted settings, prompt/general-chat modes, copy, and explicit transfer to AI image prompt.
- Cloudflare image credentials/storage alias unchanged. Groq uses separate preferences and alias; clearing one key does not clear the other.
- Added ChatRules pure context budget rules, GroqChatClient, ChatViewModel and ChatScreen. Version 0.4.0 / code 4.
- Tests cover bounded complete-turn context, newest request retention, mode instructions and blank input.
- Request errors preserve draft/history; no automatic retries or image generation from chat. 429 is displayed as Groq quota/rate restriction.
- No actual Groq API or device test yet (no key available to assistant). New CI result must be recorded below.
- Next: user registers Groq Free, creates Key and enters it in Prompt Assistant; verify chat, prompt refinements, copy, transfer without a Cloudflare request, save/clear/restart of both credentials independently.
- Confirm no regressions in offline grids and Cloudflare image generation; validate network/403/429/timeouts with sanitized errors.

## V0.4 verified CI and publication
- Feature commit: 0f35f686811faccb11f3bb93ed905c995510d27f.
- Successful workflow: https://github.com/a772485252/PhotoEdit/actions/runs/41789846298
- Job 126440409729 passed testDebugUnitTest, lintDebug and assembleDebug; BUILD SUCCESSFUL in 46s. APK upload and publication also passed.
- APK publication commit: 170b775e9ecb8c3e6d7b761d5ea946f945f3e0a9; APK blob cc1f1c4f836500a9f34a405623fa78138c20475a.
- Latest APK: https://github.com/a772485252/PhotoEdit/raw/refs/heads/main/latest-apk/PhotoEdit-debug.apk
- Live Groq requests remain unverified; user must enter their own separate Groq key on-device.
- The prompt assistant has no reference or credentials for the Cloudflare image client. Text transfer invokes only AiViewModel.prompt, not generate.

## V0.5 implementation (2026-11-04)
- User clarified that Groq must remain selectable; no provider replacement/removal.
- Added OpenRouter, platform selection, platform-specific encrypted Keys and saved model selection, custom ID, and explicit OpenRouter model catalog refresh/search.
- Preserved Groq storage namespace/alias and all Cloudflare image settings. Clearing one chat Key does not clear the others.
- Default OpenRouter model: nvidia/nemotron-3.5-super-vl:free. Presets are not live availability checks.
- Default free-only mode rejects non-free custom IDs and submits zero prompt/completion/request price limits. Explicit paid opt-in is session-only and resets on platform switch.
- Shared HTTPS client, fixed hosts, redirects disabled, bounded responses, sanitized errors. No extra libraries; version 0.5.0 / code 5.
- New pure tests cover free gating, model syntax, malformed/unknown prices and Groq storage identity.
- Execution environment unavailable in this session; local build unavailable. GitHub Actions must verify testDebugUnitTest, lintDebug and assembleDebug; no success claimed yet.
- Device checks pending: separate Key save/clear/restart, platform/model restoration, paid toggle reset, catalog refresh/error, model switching/history, prompt copy/transfer, Groq regression and Cloudflare independence.

## V0.5 verified CI and APK publication
- Feature commit: 9645a865181fd2ea34a8f8237514afa285714288.
- Successful workflow: https://github.com/a772485252/PhotoEdit/actions/runs/41802528739
- Job 126481518512 passed testDebugUnitTest, lintDebug and assembleDebug; BUILD SUCCESSFUL in 49s. Upload and APK publication also passed.
- Artifact PhotoEdit-debug-apk ID 13308396537, ZIP size 9,221,285 bytes.
- APK publication commit: eada32618be82e01d073524f85307a4d245916a4; APK blob 6b6f9534b6e33c9e625487f030475d2b9366885e.
- APK size: 9,626,053 bytes versus V0.4 9,609,669 bytes (increase 16,384 bytes; no added SDK).
- Download: https://github.com/a772485252/PhotoEdit/raw/refs/heads/main/latest-apk/PhotoEdit-debug.apk
- No build failures. Existing GitHub Actions deprecation warnings remain non-blocking.
- No live API, emulator or real-device testing performed. User retains Keys on-device; do not infer API/model availability from the successful build.

## Personal persistent debug signing (2026-11-05)
- Configure one persistent debug-signing credential through repository Actions secrets; never document passwords, aliases, key material, or certificate fingerprints.
- Workflow restores one Actions secret PHOTOEDIT_DEBUG_KEYSTORE_BASE64, validates the key, verifies APK signature, and removes runner key after build. Missing secret blocks publication; PR checks remain independent.
- Version 0.5.1/code 6. No keystore/private key committed. Owner must provision secret because connector has no Secrets-write capability. New signed APK and device update verification are pending.

## Persistent signing verified (2026-11-05)
- Persistent signing is configured through a protected Actions secret; signing material and fingerprints are intentionally excluded from project documentation.
- Initial runs restored the correct file but AGP used a different default Android user-home key. Explicit debug signing storeFile now resolves HOME/.android/debug.keystore. Clean/no-build-cache alone was insufficient; do not rely on implicit key discovery.
- Workflow compares exported keystore certificate SHA-256 against APK certificate before upload/publication and fails on mismatch.
- Verified run https://github.com/a772485252/PhotoEdit/actions/runs/41954151050 ; job 127004887191 completed success. Unit tests, lint, assembleDebug, certificate equality, upload and latest-apk publication all passed. BUILD SUCCESSFUL in 1m 7s.
- APK and fixed keystore fingerprint matched exactly. Version 0.5.1/code 6. Fixed-key setup complete; physical-device update/data preservation remains untested. Earlier ephemeral-signed installations may require one uninstall/reinstall; preserve keys for re-entry first.

## V0.6 offline pose picker (2026-11-05)
- Implemented 24 original front-facing vector mannequin diagrams, standing/seated filters, prompt preview and explicit use/cancel. Original atlas images excluded because its license prohibits image redistribution.
- Prompt selection preserves user text/reference; switching replaces prior managed pose, no network or generation triggered. Added pure prompt replacement tests.
- Version 0.6.0/code 7; persistent signing unchanged. CI validation and real-device UI checks pending.

## V0.6 verified build and publication
- Feature commit 66ffc96456daa6150d5e386b150a468fc4ee2df0. Workflow https://github.com/a772485252/PhotoEdit/actions/runs/41958663551 , job 127019536145.
- Build step (testDebugUnitTest, lintDebug, assembleDebug), fixed certificate comparison, artifact upload and latest-apk publication all completed successfully. Job log download returned 404; recorded evidence is the workflow step results and published blob metadata.
- Publication commit 30b173c4c6c3b2edf748adb998fe52be1664a7a0; APK blob 318a63260d9594418a45fb63ba60b6412e45bcfd. APK 9,700,978 bytes versus 9,658,821, increase 42,157 bytes. Original vector XMLs total 89,174 bytes.
- All 24 diagrams rendered as a contact sheet and visually inspected; XML parsing and git diff checks passed. No emulator/device or live image-generation test performed. Device checks in docs/POSES.md remain pending.

## V0.6.1 chat-style AI layout repair
- User screenshot showed full-size attachment plus fixed settings rows pushing composer/actions outside viewport. Only history was scrollable.
- Replaced with compact provider/settings header, weighted LazyColumn conversation, and bounded scrollable bottom composer. Attachment is a 52dp thumbnail with optional full preview; details and credentials move into a scrollable dialog.
- Plus menu and pose button live beside generation; prompt has at most four visible lines, scrolls internally. Latest result save remains inside history, no duplicate output. Explicit adjustResize supports IME.
- Correct provider label/disclosure for Runware and Cloudflare; no transport, credential, reference or signing changes. Version 0.6.1/code 8. CI and device/keyboard verification pending.

## V0.6.1 CI verified
- Feature commit 43e1d33b81fa924d26516f2f3a83f35d78aac903. Run https://github.com/a772485252/PhotoEdit/actions/runs/41959683855 , job 127022819069.
- Build command running unit tests, lint and assemble completed successfully; fixed signature verification, artifact upload and latest-apk publication completed successfully.
- Native UI scrolling, small-screen/landscape/large-font behavior and keyboard transitions have not been verified on a device. Verify on the user's phone with an attachment and multi-turn conversation.

## V0.7 AI Horde integration (2026-11-05)
- Added native async Horde adapter, model refresh, encrypted optional Key, anonymous mode, NSFW setting, pending-ID resume and explicit cancel. No added dependencies or SDK.
- Retained Cloudflare/Runware, clarified paid label, persisted selected platform, exposed missing-Key recovery, fixed newly selected reference being overridden by continueEditing.
- Reviewed live swagger contract: generate/async POST, generate/check GET, generate/status GET/DELETE; webp source_image, r2, shared, allow_downgrade, queue semantics and censored results.
- Added key/header validation and queue-state tests. Version 0.7.0/code 9; signed CI and smoke-test results pending.

## V0.7 verification
- Feature dcd6416f9311183e287312f4ce54031439d9a929; run https://github.com/a772485252/PhotoEdit/actions/runs/41963725863 , job 127035783664 completed success. Build (unit tests, lint, assemble), persistent signature comparison, artifact upload and latest-apk publication all passed.
- Anonymous text-to-image and reference-image smoke tests completed successfully; returned 512x512 WebP outputs. Temporary task/request identifiers are intentionally not retained in project documentation.
- Python's default user agent initially hit Cloudflare 403/1010. Explicit PhotoEdit User-Agent plus Client-Agent succeeded. Android adapter uses both. No credentials, personal photos or explicit content used for tests.
- No real APK/device layout, personal-key save/relaunch, stop/resume/cancel interaction, portrait identity or NSFW-content generation test performed. Pending handles stay in ViewModel memory only; force-close loses them.

## V0.7.1 portrait quality configuration
- User kept 512px and requested better portrait quality. Inspected current defaults and live official reference: stable_diffusion v1.5 generalist; Realistic Vision v2.0 photoreal humans; AbsoluteReality v1.0 SD1 realism.
- Change default/migrate legacy selection once, add two portrait shortcuts, DPM++2M/Karras 28step CFG6 profiles, visible prompt augmentation, custom negative-prompt preservation and lower/selectable img2img strength. Keep custom models.
- Disable silent downgrade; preserve 512 output and increase reference WebP encoding quality to100. No face restoration/identity claims or new SDK. Version0.7.1/code10.
- Added upgrade/custom-model/negative-prompt/strength rules tests; live controlled portraits and signed CI pending.

## V0.7.1 live portrait findings
- Compared same 512px clothed adult head-and-shoulders prompt and seed13371337: legacy stable_diffusion/EulerA25/CFG7 vs Realistic Vision and AbsoluteReality with DPM++2M/Karras28/CFG6 and quality negatives. All three anonymous requests completed and returned uncensored512x512 WebP. The two portrait samples had more natural/visible skin and eye detail than the legacy sample; this is a small subjective smoke comparison, not a general quality benchmark.
- Reference tests reused the synthetic Realistic Vision portrait, same model/prompt/seed at0.35 and0.55. Both completed.0.35 retained pose but introduced visibly rougher skin/edge detail;0.55 was smoother/clearer in this sample. Therefore final default is balanced0.55, with0.35 and0.65 available rather than assuming lower strength improves clarity. No face-ID match guarantee.
- Temporary poll transport403 recovered by querying the same IDs; no duplicate submissions. Initial build run41970037343 passed unit tests/lint/assemble/signature/upload/publication. Final default-strength adjustment requires a new CI result.

## V0.7.1 final CI verified
- Final configuration commit9f276a625a25690df5a018dc318e0004e606c905. Run https://github.com/a772485252/PhotoEdit/actions/runs/41970636084 , job127057798528 completed success: unit tests/lint/assemble, persistent certificate comparison, upload and latest-apk publication all passed.
- Baseline and portrait-model text tests plus two reference-strength tests completed; outputs were downloaded and visually inspected. Temporary task/request identifiers are intentionally not retained.
- Scope: one English prompt/seed for clothed synthetic adult head-and-shoulders portraits; no real user photos, full-body benchmark, NSFW generation, Android device or identity-matching test. Visual differences support the chosen presets but not a universal quality or face-consistency guarantee.

## V0.8 APIPod integration
- Added optional paid APIPod API, generation/pro/edit route mapping, encrypted independent Key, pre-submit price/model disclosure and settled usage.cost. Existing provider credentials and fixed signing unchanged.
- Checked endpoint conventions, async lifecycle, status/result/usage schema, idempotency semantics, error taxonomy and data-URI asset support in official docs; checked individual Z-Image Spicy / Qwen Edit Spicy schemas and regional notice on public product pages.
- Pending operation retains body/idempotency/Key/ID/prompt in session memory; resumable queries and local stop; explicit end-tracking confirmation explains task may continue billing. No unverified server cancellation.
- Added domain tests for image-source routing, price selection, definitive vs ambiguous submission failures, invalid header/task IDs and prompt limits. Version0.8.0/code11. CI and real paid API/device tests pending. No APIPod Key available; cannot verify authenticated generation, balance, account region eligibility or output quality.

## V0.8 verified CI and authentication boundary
- Feature8abe4af67ba7871a3b2461caae12dcc33b49ae64. Run https://github.com/a772485252/PhotoEdit/actions/runs/41972620738 , job127064113097. Unit tests/lint/assemble, fixed certificate comparison, artifact upload and latest-apk publication all passed.
- Unauthenticated GET to the status endpoint returned HTTP401 with invalid_api_key, confirming endpoint reachability/authentication envelope without creating a task or incurring charges.
- No APIPod credentials were provided; real paid generation/editing, task resume/idempotency against an authenticated account, actual costs, regional access, photo consistency and Android device interaction remain unverified. No claims that VPN grants access.

## V0.9 Venice integration
- Added optional Venice provider with separately encrypted Key, Z-Image Turbo text generation and FireRed one-reference/continuous editing. Request model/indicative $0.01 vs $0.04 shown before generation. Existing signing/providers/default selection preserved, no dependencies added.
- Reviewed official native generation/edit schemas, raw binary responses, base64 source, min256 dimensions, model-specific content behavior and pricing. Kept platform default content blur and expose blur header rather than claiming all output clear/permissive.
- Synchronous fixed-length POST with no app retry. Timeout/unknown outcome retains draft/result and locks submission pending usage acknowledgement. No nonexistent task polling/cancellation controls; no fake settled costs.
- Added boundary tests for reference/cost mapping, 4xx vs ambiguous outcome, key injection, dimensions/overflow and oversized prompt rejection. Version0.9.0/code12. Required CI pending; authenticated generation/editing, actual fees and device interaction unverified (no Venice Key available).

## V0.9 verified build and remaining API boundary
- Feature bea7ad7453e014023bea9f48a259af6e6766a878. Run https://github.com/a772485252/PhotoEdit/actions/runs/41977097726 , job127078375041 completed success: unit tests/lint/assemble, fixed-signature equality, artifact upload and publication all passed.
- Published APK commit3d6119a6510aedcc9b398b9a614357081e41d27b. Version0.9.0/code12. No Android emulator/real-device interaction test performed.
- This environment's unauthenticated Venice API probes returned HTTP200 text/html with Site Unavailable, rather than the API JSON envelope; this does not verify service access or authentication. No credentials available and no paid requests made. Actual connectivity, generation/edit quality and billing remain unverified. Do not imply compilation proves live API operation.

## V0.9.1 full_sexy catalog (2026-11-05)
- Replaced 24 pose entries with 29 selected full_sexy IDs, Chinese titles and independently written detailed action descriptions.
- Reused original generic category illustration vectors; no upstream thumbnails or images bundled. Generic icons do not accurately depict each individual pose.
- Added unique/count test and categories; version 0.9.1/code13. CI and device validation pending.

## V0.9.2 one-request random two-panel image
- Added local random choice of two distinct poses from 29 presets, composed into one left/right two-panel prompt.
- One normal Generate action submits one image request; no additional API calls or local collage processing.
- Regenerating random selection replaces only managed two-panel block, preserving surrounding user text; single-pose block removed when switching.
- Added unit tests for uniqueness and replacement; version 0.9.2/code14. Build and device/model compliance not yet verified.

## V0.9.4 expressive poses
- Enhanced five standing-pose prompts to emphasize three-quarter angles, waist/hip silhouettes, and unobstructed pelvic outlines.
- Added custom X001 prone look-back with raised hips; 29 original full_sexy entries retained plus one app-authored pose (30 total).
- Picker count and unit tests updated; version 0.9.4/code16. CI/device validation pending.


## V0.9.13 chat-first prompt assistant redesign (2026-11-06)
- Rebuilt the Prompt Assistant tab as a chat-first screen: compact provider/model header, scrollable conversation list, fixed bottom composer, prompt/general-chat mode chips, and first-use suggestion shortcuts.
- Moved provider selection, independently encrypted OpenRouter/Groq API Keys, model catalog/search, custom model ID and paid-model opt-in into a scrollable settings dialog. Existing ChatViewModel, API clients, model policies, and encrypted storage identities are reused.
- Kept conversation memory session-only; no local chat history or image/reference upload. Prompt transfer still only populates the AI image prompt and does not submit a generation request.
- Added explicit regenerate-last-answer action using the same last user turn and existing bounded context rules. Copy and prompt transfer remain available on assistant messages.
- No new dependencies or API endpoint changes. Version 0.9.13 / versionCode 25. GitHub Actions build and physical-device layout/keyboard checks pending.


## V0.9.14 Cloudflare Workers AI chat provider (2026-11-06)
- Added a third independent chat provider, Cloudflare Workers AI, using the existing project Account ID and a separately encrypted API Token namespace; existing image-generation Cloudflare credentials are not overwritten.
- Default model: @cf/zai-org/glm-4.7-flash, listed by Cloudflare as available on Workers Free. Daily free allocation is 10,000 Neurons; quota exhaustion is surfaced and never silently switches to paid providers.
- Uses Cloudflare REST endpoint /accounts/{account}/ai/run/{model}, messages payload, and result.response envelope. Existing OpenRouter/Groq API paths and credentials remain unchanged.
- UI includes setup guidance: token needs Workers AI Read and Workers AI Edit. Model IDs are restricted to @cf namespace. Unit test added for provider/model validation.
- Version 0.9.14 / versionCode 26. CI build and live account/token/device test pending; user must supply a suitable Cloudflare token before use.


## V0.9.15 Cloudflare image-token sharing and image-provider cleanup (2026-11-06)
- ChatSettingsStore now routes the Cloudflare chat credential to the same encrypted AiSettingsStore namespace used by the image-generation Cloudflare token. Existing saved image Token and Account ID are reused automatically when selecting Cloudflare chat; no second paste/save is needed.
- Clearing the Cloudflare key from chat settings also clears the shared image-generation Cloudflare credential; the chat UI explicitly warns about this shared behavior. OpenRouter and Groq remain independent.
- Image provider selector now exposes only APIPod and Venice. Cloudflare, AI Horde and Runware have been removed from the image provider selection; legacy stored selections migrate to APIPod. Their old encrypted credentials/data are not automatically deleted.
- Image generation provider switching now accepts only APIPod and Venice. Existing APIPod/Venice clients and settings are retained. Chat Cloudflare remains available as a text provider and reuses the image Cloudflare credential.
- Version 0.9.15 / versionCode 27. CI build and device test pending.


## V0.9.16 Cloudflare prompt-only credential clarification (2026-11-06)
- Clarified in chat settings that the existing Cloudflare credential is reused from local encrypted app storage if previously configured, but Cloudflare is now only exposed as a prompt-assistant provider. It is no longer an image-generation provider; clearing its credential from chat settings only clears the stored Cloudflare credential used by prompt assistance.
- Version 0.9.16 / versionCode 28. Build verification pending.


## V0.9.17 Cloudflare-only prompt assistant (2026-11-06)
- Removed OpenRouter and Groq from the Prompt Assistant provider picker and settings UI. Prompt assistant now defaults to and loads only Cloudflare Workers AI.
- Existing legacy OpenRouter/Groq credential namespaces and code paths are retained internally for compatibility; they are no longer selectable or used by the current UI. Existing stored keys are not deleted.
- Cloudflare Token remains encrypted in the existing shared credential store used by the prompt assistant; image generation provider choices remain APIPod and Venice only.
- Version 0.9.17 / versionCode 29. APK build and device validation pending.


## V0.9.18 Venice chat provider (2026-11-06)
- Added Venice AI as a second selectable provider in Prompt Assistant alongside Cloudflare Workers AI. OpenRouter and Groq remain absent from the UI.
- Venice uses the official OpenAI-compatible endpoint https://api.venice.ai/api/v1/chat/completions and independent encrypted local Key namespace; it does not reuse or alter the Venice image-generation credential.
- Default model ID: venice-uncensored-1-2 (Venice Uncensored 1.2). Venice API requests use normal chat-completions messages and response parsing; max_tokens capped at 2048. No silent provider/model fallback.
- ChatSettingsStore persists Cloudflare/Venice selection and separate model IDs; app startup restores last selected supported provider. Cloudflare still reuses its existing Cloudflare credential.
- Settings UI now offers Cloudflare · GLM and Venice · Uncensored, with provider-specific billing/key guidance. Venice API billing is separate from Venice web free prompt quota.
- Version 0.9.18 / versionCode 30. CI build and authenticated live Venice API/device validation pending; user must register and provide Venice API Key.


## V0.9.19 Venice Lustify v8 image model and shared API key (2026-11-06)
- Venice text-to-image default model changed from Z-Image Turbo to Lustify v8 (model ID lustify-v8), a photorealistic character-focused SDXL checkpoint. Official Venice model listing describes it as uncensored and lists $0.01 per image.
- Existing Venice reference-image edit model remains FireRed Image Edit at the existing estimated $0.04 per request; Lustify v8 is used for text-to-image only.
- Disabled Venice safe_mode in the native image generation/edit request to avoid enabling optional safe-mode blur. Provider/model refusals and platform/legal restrictions may still apply; no promise of unlimited content.
- ChatSettingsStore Venice namespace now points to the same encrypted venice_private store used by AiViewModel image generation. One Venice API Key serves both chat and image generation; saving/clearing from either feature updates the same credential. UI copy warns clearing affects both.
- Version 0.9.19 / versionCode 31. Build and live API/device test pending.


## V0.9.20 Fix stale Venice model unit test (2026-11-06)
- Updated VeniceRulesTest.referenceSwitchesBothEndpointModelAndDisplayedPrice expected text-to-image model ID from z-image-turbo to lustify-v8. Production rule already correctly selected Lustify v8; the failure was a stale assertion left from the previous default model.
- Version 0.9.20 / versionCode 32. CI build and test rerun pending.


## V0.9.21 Lean unit test suite (2026-11-06)
- Kept the JUnit test dependency and unit-test task; unit tests are not packaged into the APK and do not increase installed APK size.
- Trimmed ChatModelsTest to active Cloudflare and Venice model ID validation plus malformed ID and zero-price validation. Removed obsolete OpenRouter/Groq-specific test coverage from the active prompt-assistant test suite.
- Retained focused VeniceRules tests for model/price selection, uncertain billing outcomes, API key header-injection validation, reference dimension limits, and prompt length validation.
- Retained focused AiRules tests for Cloudflare credential/prompt validation and quota/auth error classification.
- No broad UI or network-mocking test infrastructure added. CI should continue requiring testDebugUnitTest to pass before APK build.
- Version 0.9.21 / versionCode 33. CI verification pending.


## APK 构建流程规则调整（2026-11-06）
- 用户明确要求：日常代码修改、功能提交不自动触发 APK 构建，避免每次小改动都消耗构建资源。
- Android APK GitHub Actions workflow 改为仅支持手动触发（workflow_dispatch）；不再响应 main push 或 pull request。
- 只有用户明确提出“生成最新 APK 下载链接”等 APK 交付请求时，才手动启动构建。
- 交付前必须确认测试、Lint、APK 编译、签名验证均成功，并确认 latest-apk/PhotoEdit-debug.apk 已更新；未验证不得提供下载链接。
- 该规则已同步写入 AGENTS.md，后续会话必须遵守。


## V0.9.22 prompt assistant first-use UX (2026-11-06)
- Replaced the image-generation-style empty state ("想生成什么样的图片？" and four category shortcuts) with a prompt-assistant-specific introduction explaining text expansion/optimization and clarifying that this page does not generate images.
- Removed the four suggestion chips; added concise usage guidance for describing a subject, clothing, action, scene, or existing prompt.
- Added visible loading feedback while the conversation is empty, so a first request no longer appears to do nothing while the API is processing. The submit button now reads "生成提示词" in prompt mode and "发送" in normal chat mode.
- No API endpoint, model, credentials, billing, or image-generation behavior changed. API failure diagnosis still requires the exact in-app notice shown after pressing "生成提示词"; no live Cloudflare request or APK build was performed.
- Version 0.9.22 / versionCode 34 should be applied in the next version bump before an APK build. APK builds remain manual-only and are not triggered by this code edit.


## V0.9.23 chat model identity and billing disclosure correction (2026-11-06)
- Confirmed the active Prompt Assistant provider set from source: Cloudflare Workers AI / GLM-4.7-Flash, Venice AI / Venice Uncensored 1.2, and APIPod / Grok 4.5. APIPod Grok 4.5 is an existing chat provider using the APIPod image/chat shared Key; it is not a separate xAI account integration.
- Venice chat is also present in source/UI and uses the shared Venice Key used by Venice image generation. Its presence in code does not prove the user's Key is configured or that live API access works; authenticated live validation remains pending.
- Chat header now displays provider, human-readable selected model name, and exact model ID. Billing method is identified by provider. No unverified per-character price figures or unverified balance endpoint are shown.
- Real-time balance query is not yet implemented/verified for Cloudflare, Venice or APIPod. APIPod Grok 4.5 effective price depends on APIPod's route/pricing, so do not substitute xAI direct pricing. Per-10,000-character estimates require verified provider/model input and output prices plus actual token usage or an explicit approximation.
- Removed an unverified Venice balance request and speculative per-character cost figures before finalizing. No API request, model routing, credentials, or billing behavior changed.
- The V0.9.22 prompt-assistant first-use UX changes remain. Current version 0.9.22 / versionCode 34; no APK build was triggered. Device and authenticated API validation pending.


## Documentation continuity and privacy rule (2026-11-06)
- AGENTS.md now requires every meaningful code/config/UI/model/API/build-rule change to update PROGRESS.md in the same work session, and PROJECT.md whenever current architecture, active models/providers, visible behavior, constraints, or operating rules change.
- New sessions must read AGENTS.md, PROJECT.md, and the latest PROGRESS.md entries before work; source code is authoritative when documentation conflicts, and documentation must then be corrected.
- Project documentation is restricted to non-sensitive project-level technical facts and non-sensitive validation outcomes. Account identifiers, API keys/tokens/passwords, signing material/fingerprints, private request/task IDs, personal contact/location details, personal photos/metadata, exact account balances/usage, and other private user data are prohibited.
- Audited and redacted historical progress entries containing a personal account identifier, signing password/fingerprint, and temporary generation task IDs. No credentials or personal identifiers are retained in this entry.
- This is a documentation/rules-only change. No app code, model routing, API credentials, or APK artifact changed. APK build not run.

- Final privacy audit also removed the remaining personal account-identifier reference from PROJECT.md. Historical CI run/commit/artifact references are public repository build metadata; no user credentials or personal identifiers are included.


## V0.9.23 Prompt Assistant model pricing and balance display (2026-11-06)
- Chat header now emphasizes the currently selected provider/model name and exact model ID.
- Added per-10,000-Chinese-character reference estimates for the three selectable model routes:
  - Cloudflare GLM-4.7-Flash: approximately $0.0006 input / $0.0040 output, also showing approximately 55 / 364 Neurons. Based on published $0.0605/M input and $0.40/M output token pricing.
  - Venice Uncensored 1.2: approximately $0.0020 input / $0.0090 output, based on published $0.20/M input and $0.90/M output pricing.
  - APIPod Grok 4.5: approximately $0.002–$0.014 input / $0.006–$0.042 output, covering the published Grok Lite and xAI-default channel rates. APIPod channel routing can vary, so the UI shows a range.
- All character estimates explicitly assume approximately one token per Chinese character. Actual tokenization, context, channel, cache and output length affect the final charge; unknown/custom models show no invented price.
- Added Venice balance lookup through the documented GET API-key rate-limits/balances endpoint. The app displays returned USD, DIEM and bundled credits; it refreshes on provider selection, after successful chat requests, and through a manual refresh control. The balance is kept in transient UI state only and is not written to project docs or logs.
- Cloudflare's daily 10,000-Neuron free allocation is shown, but remaining account Neurons are not queried. APIPod's API-key balance is not queried in-app; the UI directs users to the APIPod console rather than calling an undocumented endpoint.
- Updated focused ChatModels unit tests for model-specific estimates and unknown-model handling.
- Files changed: ChatModels.kt, ChatClient.kt, ChatViewModel.kt, ChatScreen.kt, MainActivity.kt, ChatModelsTest.kt, app/build.gradle.kts, PROJECT.md.
- Version 0.9.23 / versionCode 35. Unit tests/build/device/API balance validation have not yet been run. No APK build was triggered; APK remains manual-only on explicit user request.
- Public references: Cloudflare GLM-4.7-Flash model pricing and Workers AI pricing; Venice API Key Rate Limits and Balances; APIPod Grok 4.5 model/channel pricing.

- Follow-up source review corrected coroutine cancellation propagation in the asynchronous Venice balance refresh. This is included in the same V0.9.23 work; no behavior or API contract changed.


## V0.9.24 APIPod Grok option visibility fix (2026-11-06)
- Confirmed APIPod is already a selectable Prompt Assistant provider and its default chat model is Grok 4.5 (model ID `grok-4.5`); this is separate from image-generation model choices.
- Fixed the provider selector layout in the “模型与 API 设置” dialog: Cloudflare and Venice remain on the first row, while APIPod · Grok 4.5 is placed on a dedicated second row so it cannot be clipped off narrow phone screens.
- No provider routing, API endpoint, credential storage, pricing, or image-generation model settings changed.
- Version 0.9.24 / versionCode 36. Source change committed; build, unit tests, and device visibility still require validation. APK build not triggered.


## UI design process rules (2026-11-06)
- Added mandatory UI design review guidance to AGENTS.md and PROJECT.md.
- Future UI edits must inspect the full screen hierarchy, theme/shared components, narrow-device behavior, dialogs, keyboard, scroll, long labels, accessibility/font scaling, and all relevant UI states before completion.
- Explicitly record whether a dedicated UI skill/design document exists; do not claim it was consulted if unavailable. Current repository and installed skill catalog do not contain a dedicated Android UI/Product Design skill.
- Documentation-only change; no app source, version, or APK changed. Build not run.


## UI_DESIGN.md established (2026-11-06)
- Added `UI_DESIGN.md` as the project-wide source of truth for visual consistency, mobile-first layout, interaction feel, accessibility, and screen review.
- Explicitly requires UI to reflect business logic: task flow, validation, constraints, API behavior, costs, permissions, side effects, and failure recovery must be understood before designing controls.
- Added user-experience requirements: predictable familiar interaction, one-hand reachability, low friction, immediate feedback, preservation of safe drafts/selections, clear errors, and no misleading controls.
- Updated AGENTS.md to require reading UI_DESIGN.md and reviewing related domain/ViewModel logic before every UI change. Updated PROJECT.md to link the standard.
- Documentation-only change. No app source/version/APK changed; build not run.


## APK workflow versioned run titles (2026-11-06)
- Updated `.github/workflows/android-apk.yml` so manually triggered runs are titled `PhotoEdit v<version> · Android APK`, making the version visible in the Actions list.
- Added required `app_version` workflow input with current default 0.9.24 and an early validation step comparing it to Gradle `versionName`. A mismatch fails before SDK setup/build to prevent mislabeled APK runs.
- Updated AGENTS.md to require versioned run titles and synchronized defaults; documented the workflow behavior in PROJECT.md.
- Workflow/docs only. No APK build triggered; no app source/version changed. Workflow execution remains pending.


## V0.9.25 Deterministic prompt composition and anatomy guidance (2026-11-06)
- Removed camera-angle and framing instructions from all eight PoseCatalog descriptions; pose presets now describe only body positions. Kept camera direction in CameraCatalog and resolved its optional framing to a single full-body composition, with hands and feet in frame.
- Updated prompt-assistant system guidance to avoid ambiguous either/or choices and conflicting pose/camera descriptions. Added anatomy guidance to discourage extra, missing, fused, malformed or unintentionally cropped limbs and fingers.
- PromptComposer now adds one managed anatomy-quality block when composing outfits, poses or camera presets; repeated edits replace rather than duplicate the block. Existing user-authored free text is preserved.
- Updated focused ChatRulesTest, PromptComposerTest and PosePromptsTest to cover guidance, deduplication and pose/camera separation.
- Version 0.9.25 / versionCode 37; synchronized manual workflow version default. No APK build requested or triggered. Unit tests and device/image-model behavior remain unverified. Prompt instructions reduce risk but cannot guarantee anatomy accuracy.


## Prompt Assistant close full-body composition (2026-11-06)
- Clarified that the requested fix applies specifically to the Prompt Assistant Generate Prompt action and its chat system instruction, including the APIPod Grok 4.5 route, rather than only the AI image screen's preset composer.
- Updated ChatRules.SYSTEM_PROMPT to default to close full-body framing with head, hands, and feet completely in frame and a small safety margin, choose one camera angle/pose when unspecified, honor explicit user framing, and output one finished prompt without alternatives or commentary.
- Added a focused ChatRulesTest assertion for the new instruction. Existing anatomy completeness guidance remains in place.
- Files: ChatRules.kt, ChatRulesTest.kt, PROJECT.md, PROGRESS.md. No API changes or UI layout changes. Version remains 0.9.25 / code 37; no APK build triggered. Tests and real-model compliance are unverified.


## V0.9.26 Editable Prompt Assistant rules (2026-11-06)
- Added a dedicated “扩写规则” button in the Generate Prompt composer and a scrollable, phone-width dialog for editing one full text instruction (up to 8000 characters), canceling, saving, or restoring the built-in default.
- Added local SharedPreferences storage via ChatSettingsStore and state/actions in ChatViewModel. ChatRules.messages now accepts a per-request instruction override in prompt mode only; ordinary chat remains unaffected.
- Saving/restoring clears transient conversation context; no local chat history is saved and editing rules does not call a paid API. Rules persist across app restarts.
- Updated ChatScreen.kt, MainActivity.kt, ChatViewModel.kt, ChatSettingsStore.kt, ChatRules.kt, Gradle version and workflow default. Source version 0.9.26 / code 38.
- UI reviewed for scrollable editor, keyboard entry, reachable dialog buttons and mode visibility; compilation, unit tests, narrow-screen device check and provider integration remain unverified. No APK build triggered.


## V0.9.27 prompt framing refinement (2026-11-06)
- Replaced conflicting near-camera language in ChatRules.SYSTEM_PROMPT with explicit full-body visibility, minimal safe margins, large subject framing, minimal background, and natural proportions/perspective. Existing local custom rules remain unchanged until Restore Default.
- Updated app/build.gradle.kts to 0.9.27 (versionCode 39) and synchronized manual workflow input default.
- Source change committed; APK build requested but not yet verified. No build success claimed.


## APK workflow quick/full modes and caching (2026-11-06)
- Added manual workflow_dispatch choice `build_mode`: Quick (default) or Full. Run title now includes both version and mode.
- Quick executes `gradle --build-cache --parallel assembleDebug`; Full executes `gradle --build-cache --parallel testDebugUnitTest lintDebug assembleDebug`.
- Both modes retain Android SDK setup, persistent debug signing, APK certificate SHA-256 verification, artifact upload, and latest-apk publication. Neither runs clean or disables Gradle build cache. setup-gradle continues to cache Gradle user-home state.
- Updated AGENTS.md with selection guidance: Quick for routine UI/text-only work; Full for domain/business, API/provider, storage, image processing, dependency, or milestone changes. Quick explicitly skips unit tests and Lint.
- Workflow and documentation only; app version unchanged; no APK build triggered. Actual speedup depends on cache hit and GitHub runner state.

## V0.9.28 Screen-off image generation (2026-11-06)
- Added `GenerationForegroundService`, started only after the user taps Generate. It posts a low-importance ongoing notification with a tap-to-open action and a clear no-auto-gallery-save message.
- Acquires a `PARTIAL_WAKE_LOCK` with a 30-minute timeout while the existing ViewModel generation coroutine runs; releases it when the service stops. Generation completion/failure stops the service.
- Added manifest permissions for foreground service, dataSync service type and wake lock; updated AI screen progress copy to explain screen-off behavior and qualify that notification visibility depends on system notification permission.
- Confirmed by source inspection that gallery writing remains exclusively in the explicit `saveImage()` action. Generation results continue to be held as in-memory Bitmaps for in-app preview; no automatic MediaStore or gallery write was added.
- Files: `GenerationForegroundService.kt`, `AiViewModel.kt`, `AiScreen.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `.github/workflows/android-apk.yml`, `PROJECT.md`, `PROGRESS.md`.
- Version 0.9.28 / versionCode 40. Build, unit tests, lint, and physical-device screen-off/battery-policy behavior are not verified. Per project policy, no APK build was triggered; wait for an explicit APK/download request.
- Limitations: screen-off continuation is best-effort under Android Doze and manufacturer battery policies. Foreground service improves process priority and the wake lock keeps CPU active, but force-stop, process death, reboot or OEM task termination can still interrupt the request. This is not a durable persisted task queue.


## V0.9.30 Chat rule editing preserves conversation (2026-11-06)
- Removed the message-clearing side effect from saving custom prompt rules and restoring defaults. Both actions now preserve the visible conversation and unsent input draft.
- Updated the rule editor explanation and status messages. New instructions apply to the next prompt-mode request; retained turns remain available as context. Starting a new chat remains the explicit clear action.
- Added a focused unit test for applying custom rules while preserving previous conversation turns.
- Updated app version to 0.9.30 / versionCode 42 and synchronized the manual APK workflow default.
- Source changes committed. Full APK build requested; CI tests, lint, signature verification and publication are pending.


## V0.9.31 Prompt Assistant background generation (2026-11-06)
- Added PromptGenerationForegroundService with a dedicated low-importance ongoing notification and a 30-minute partial wake lock. It is started from the foreground user action and stopped in the ChatViewModel operation finally block.
- Prompt Assistant send and regenerate now run with the foreground service active, so screen-off and ordinary app switching have stronger process priority and CPU availability. Existing ChatClient, selected provider/model, credential handling, and billing behavior are unchanged.
- Updated ChatScreen progress/help copy and registered the service as a non-exported dataSync foreground service. No new dependencies, automatic retries, disk history, or gallery writes.
- Limitations: this is best-effort while the process remains alive; force-stop, process death, reboot, network loss, Doze/OEM restrictions and notification permission/settings can still affect completion or notification visibility. The in-memory response/history is not durable across process death.
- Files: PromptGenerationForegroundService.kt (new), ChatViewModel.kt, ChatScreen.kt, AndroidManifest.xml, app/build.gradle.kts, .github/workflows/android-apk.yml, PROJECT.md, PROGRESS.md.
- Version 0.9.31 / versionCode 43. No APK build requested yet; compilation, tests, lint and physical-device background/screen-off behavior remain unverified.

## 2026-11-07 prompt assistant phone layout and APIPod 403 messaging
- Updated ui/screens/ChatScreen.kt: compact header, expandable model/cost details, separate mode/settings rows, full-width multiline composer and full-width primary send action to avoid narrow-phone wrapping.
- Updated ai/ChatClient.kt: HTTP 403 now explicitly identifies the status and separates possible account/key, region, and content causes without claiming a definitive diagnosis or exposing provider response bodies.
- Existing prompt drafts, model choice, API Key storage and manual-only request behavior remain unchanged.
- Build/tests and real-device verification pending; no APK build triggered per manual-build policy. Verify narrow phones, keyboard resizing, font scaling and actual APIPod response using user-owned credentials.

## 2026-11-07 Venice DeepSeek optional chat model
- Added Venice preset deepseek-v4-1-flash-uncensored and readable display name plus indicative 10,000-character token-cost guide in domain/ChatModels.kt.
- Uses existing Venice chat endpoint and encrypted Venice Key; APIPod Grok 4.5 remains independently selectable with separate balance and credentials.
- Provider model ID and prices checked against public Venice model documentation. Live API entitlement and model output not verified; build/tests pending. No APK build triggered until explicitly requested.

## 2026-11-07 APIPod Grok 4.6 option
- Added grok-4.6 to APIPod chat model presets while keeping grok-4.5 as default; existing APIPod Key and chat client are reused.
- Added display name and indicative APIPod xAI channel price guide ($1.40 input / $4.20 output per million tokens).
- Source: https://www.apipod.ai/llm/xai/grok-4.6 . Code committed; no APK build requested or triggered, API call not live-tested.

## 2026-11-07 APIPod HTTP error diagnostics
- ChatClient now reads JSON error code/message from non-2xx HTTP error streams and appends a bounded, sanitized diagnostic to the existing localized status text. Current API key is redacted.
- No endpoint, model, credential storage, billing, or retry behavior changed.
- Source commit 69d94532; live API behavior and Android build not yet verified. User requested an APK build, but this connector does not expose workflow_dispatch; manual dispatch remains required.

## 2026-11-07 Versioned APK filenames
- APK publish workflow now writes `latest-apk/PhotoEdit-v<versionName>.apk` alongside legacy `latest-apk/PhotoEdit-debug.apk`, preserving old link compatibility. Version read from Gradle. Change applies on next successful build; not retroactively published.

## 2026-11-07 Versioned-only APK policy
- User confirmed legacy `PhotoEdit-debug.apk` URL need not be maintained. Workflow now publishes only `latest-apk/PhotoEdit-v<versionName>.apk`, version read from Gradle; existing legacy file may remain but is no longer updated or recommended.
- Updated AGENTS.md mandatory rules and workflow publishing. No build triggered for this configuration-only request; verify on next explicitly requested build.

- Version bumped to v0.9.33 (versionCode 45) for the APIPod HTTP error diagnostics fix; APK build pending.

## 2026-11-07 v0.9.34 APIPod model picker UI
- Simplified APIPod model dropdown entries to `Grok 4.5` / `Grok 4.6` with a single billing line, removing repeated model IDs and provider text in the title.
- Bumped app versionName to 0.9.34 and versionCode to 46; synchronized workflow_dispatch version default.
- User explicitly requested APK build; push-triggered quick build is the available GitHub connector route.

## 2026-11-08 v0.9.35 APIPod shared-key synchronization
- Chat send and regenerate now reload the encrypted APIPod Key from shared storage immediately before the request, preventing stale in-memory credentials after the image screen changes the Key. Missing saved Key produces a clear error; no automatic retries or paid calls added.
- Chat HTTP error handling recognizes `insufficient_user_quota` specifically instead of treating it as a generic permission/region/content HTTP 403; preserves sanitized server details.
- Changed ChatViewModel.kt, ChatClient.kt, app/build.gradle.kts, android-apk.yml. Version 0.9.35 / versionCode 47. No APK build requested yet; compilation, tests, and live provider verification pending. This does not prove the remote quota issue is resolved.

## Provider recommendation rule correction (2026-11-08)
- Updated AGENTS.md to record that Venice official API is temporarily unavailable as a practical integration choice because payment/recharge is not currently accessible to the user.
- Future text-prompt expansion provider research should prioritize usable payment routes and verified model availability; do not repeatedly recommend Venice without a user-reported payment change.
- Documentation-only change. No app code, model selection, APK version, or build changed; no build triggered.

## APIPod DeepSeek Flash chat preset (2026-11-08)
- Added `deepseek-flash` to APIPod prompt-assistant presets and indicative per-character pricing in `domain/ChatModels.kt`; existing Grok selections and image generation unchanged.
- Added focused preset, validation, display name and pricing assertions in `ChatModelsTest.kt`; updated PROJECT.md.
- No new dependency or key; existing APIPod Chat Completions transport reused. Live model availability and moderation behavior not verified; no APK build or tests executed because user did not request an APK build.

## v0.9.36 release build requested (2026-11-08)
- Increased versionCode to 48 and versionName to 0.9.36 for DeepSeek Flash APIPod preset.
- Synchronized workflow_dispatch default and changed push-trigger builds to run full testDebugUnitTest + lintDebug + assembleDebug verification, instead of quick, for this API/model change.
- Build verification and publication pending workflow result; live APIPod request remains unverified.


## 2026-11-08 — Offline dress-change video milestone 1 (code committed, build pending)
- Added domain/DressChangePlanner.kt: deterministic 8/12/15-second pop-style beat templates, validation and 2–30-photo timeline planning. This produces cue data only, not a video.
- Added focused domain/DressChangePlannerTest.kt for beat alignment, continuity, maximum photo count and invalid input.
- No soundtrack assets bundled: original/licensed music must be provided before automatic audible output can work; no unlicensed trending songs downloaded.
- Still pending: photo picker/order UI, motion and transitions, audio, MP4 rendering, foreground export, manual save and real-device verification.
- No APK build triggered, per AGENTS.md. Unit tests not executed in this environment; compilation unverified.

## 2026-11-08 — Dress-change milestone 2 (source committed; no build)
- Added viewmodel/DressChangeViewModel.kt and ui/screens/DressChangeScreen.kt; wired multi-photo picker and fourth scrollable tab in MainActivity.kt.
- Users can select up to 30 photos, inspect beat timing and reorder/remove; no misleading Generate action until actual encoder is implemented.
- UI uses lazy scrolling and a scrollable tab bar for smaller phones. Preview thumbnails use sampled decoding; real-device memory and URI persistence need verification.
- No music, rendering, MP4 export or gallery save yet. No Gradle compilation, lint, unit-test execution or APK build performed; source integration is unverified.

## 2026-11-08 — Dress-change milestone 3 (source committed; no build)
- Fixed MainActivity tab 3 navigation regression: it now actually renders DressChangeScreen, rather than falling through to ChatScreen.
- Added domain/DressChangeMotion.kt for deterministic 9:16 720p/30fps frame transform calculations (Ken Burns zoom/pan and short beat fade), plus DressChangeMotionTest.kt for frame bounds, beat switch, timeline coverage.
- Rendering/export, soundtrack, background jobs and gallery save are NOT implemented. No APK build triggered per user build policy; tests/compilation and device behavior remain unverified.

## 2026-11-08 — Dress-change frame renderer milestone
- Added processing/DressChangeFrameRenderer.kt to draw cropped 9:16-compatible Ken Burns frames with beat-cut opacity/flash into a caller-owned Android Canvas.
- Reused existing domain/DressChangeMotion.kt frame scheduling (found already present on main); no duplicate motion planner added. Existing motion tests are also present.
- Still needs MediaCodec/MediaMuxer encoder, audio soundtrack, user-facing preview/export/save and device validation. No APK build requested or run; compilation remains unverified.

## 2026-11-08 — Dress-change milestone 4 (silent MP4 encoder, unverified)
- Added processing/DressChangeVideoExporter.kt: MediaCodec AVC YUV420 byte-buffer encoding, MediaMuxer MP4, frame rendering with sampled image decoding and per-frame motion. Temporary output remains in app cache; no gallery save.
- Wired ViewModel export/progress and fixed-bottom "生成无声 MP4（测试版）" button in DressChangeScreen, plus MainActivity callback.
- Explicitly no soundtrack yet. No background service, no final save, no playback preview, no device codec validation. Some devices may reject YUV420 input layout or 720p configuration; investigate through full build and device logs when APK is requested.
- No build or tests run per user build policy. Code compilation and runtime correctness remain unverified.

## 2026-11-08 — Dress-change milestone 5 (preview and manual gallery save, unverified)
- Added processing/DressChangeVideoSaver.kt to copy generated cache MP4 to Movies/PhotoEdit via MediaStore only when user explicitly taps Save; rollback on failure and Android 10+ pending visibility.
- Added ViewModel save state, screen VideoView preview and save button, plus Android 8/9 legacy storage permission flow in MainActivity. No automatic gallery writes.
- Automatic licensed soundtrack, audio muxing, robust background export and codec compatibility remain pending. Compilation, lint, tests and real-device verification NOT run; no APK build requested.

## 2026-11-08 — Dress-change milestone 6 (original audio groundwork, not yet muxed)
- Added domain/DressChangeBeatAudio.kt for deterministic, sample-free original electronic kick/clap/bass PCM in bounded chunks; no copyrighted Douyin tracks or paid API. Added DressChangeBeatAudioTest.kt for deterministic chunk continuity and bounds.
- Fixed DressChangeViewModel to invalidate previously exported video after photo order/removal changes, avoiding saving a stale montage.
- Fixed premature MP4 file-length check before MediaMuxer finalization.
- Generated PCM is NOT yet AAC-encoded or added to MP4; export remains silent. Audio synthesis is not a substitute for a currently trending licensed song. No APK build, compilation, lint or unit tests executed; integration and codec behavior remain unverified.

## 2026-11-08 — Dress-change milestone 7 (AAC + MP4 mux source committed; unverified)
- Added processing/DressChangeAudioMuxer.kt to encode generated mono 44.1kHz PCM to AAC-LC via MediaCodec/MediaMuxer, then copy video/audio samples into a combined MP4 via MediaExtractor/MediaMuxer.
- Wired ViewModel two-stage export (video 0–85% progress, AAC/remux final stage) and removed silent intermediate after use. UI now correctly says "生成带节奏 MP4（测试版）" and explains audio is original electronic beats rather than a trending licensed song.
- Still no background/foreground service; exports may stop if app process is killed. Device AAC/AVC compatibility, playback timing, generated file validity, lint, tests and compilation are unverified; no APK build requested or run.

## 2026-11-09 — Personal-use simplicity and performance review (no APK build)
- Reviewed DressChangeAudioMuxer, DressChangeVideoExporter, ViewModel, UI and project rules. The current two-stage MP4 + AAC remux writes extra temporary files, but rewriting the codec pipeline before any compilation/device test would introduce higher risk. Kept existing architecture and avoided adding foreground services or third-party libraries.
- Optimized DressChangeBeatAudio.samples: binary-search the initial beat and advance a cursor, rather than scanning every beat for each PCM sample.
- Moved DressChangeScreen thumbnail decoding to Dispatchers.IO via produceState, avoiding synchronous image I/O during scrolling.
- Known pending risks: actual codec compatibility, export resource cleanup, legacy permissions, audio/video timing, and background execution. No tests, Gradle build or APK build run; source remains unverified.
