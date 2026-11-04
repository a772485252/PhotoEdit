# PhotoEdit — Development Progress

Last updated: 2026-11-04

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
- User authorized saving Account ID `4d17f32858b5ec94a9d493d703b54fb9` publicly as the pre-filled personal default for the upcoming integration.
- Account ID was transcribed from the provided setup screenshot; no API Token was provided or stored.
- Next AI implementation must initialize the Account ID field with the value in PROJECT.md, allow overrides, and leave API Token blank.
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
