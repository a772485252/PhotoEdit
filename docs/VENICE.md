# Venice image API

Optional paid provider; independent `venice_private` Keystore-backed key. Set it on the phone, never in code/CI. The existing default provider is retained. No SDK or new dependencies.

| Effective input | Endpoint | Explicit model | Indicative cost |
| --- | --- | --- | --- |
| Text only | POST /api/v1/image/generate | z-image-turbo | $0.01/image |
| Selected reference or previous result | POST /api/v1/image/edit | firered-image-edit | $0.04/edit |

These are 2026-11-05 published estimates, not measured charges or account balance. UI updates model and estimate before submit. No alternate model, paid prompt enhancement or automatic provider fallback. Key save is not authentication verification.

Text request: 1024x1024, one variant, WebP binary response. Edit: base64 PNG reference, longest edge at most1024 while preserving aspect; source and scaled image must each be at least256x256. Extreme aspect ratios can fail this minimum and are rejected locally. Responses are synchronous raw images, not an async task envelope. Continuing edits uses actual previous result; choosing a new reference disables continuing the old result.

`safe_mode=true` keeps the platform's default adult-output blur. No uncensored model or moderation bypass is configured. Check response headers for content violations or blur; blur is reported to the user. Platform rules remain applicable. Output quality, anatomy, content acceptance and face consistency are not guaranteed.

State contract:

| State | Actions | Effect |
| --- | --- | --- |
| Missing key | Open settings, enter/save key | No upload |
| Ready | Edit draft/source, select poses, generate | Explicit submit only |
| Busy | Read history/settings | Input/source/model locked; no duplicate submit or fake percent |
| Success | Save, continue editing | Result retained in session; no invented settled cost |
| Explicit 4xx rejection | Fix draft/key, submit manually | Draft and previous result retained |
| Timeout/ambiguous server/response failure | Open API usage, acknowledge after checking | New generation locked until acknowledgement; acknowledgement does not retry |

No documented task-query or cancellation capability is used. A lost response cannot be resumed; a subsequent generation is a new billable request. There is no automatic POST retry in the app. Fixed HTTPS destination, redirects disabled, fixed-length POST, bounded request/source, bounded binary response24MB, decoded dimension8192 and sampling to2048. Never log provider bodies or echo credentials. Session drafts/history/unknown state are not persisted; process exit loses them, so check provider usage before submitting again after an interrupted operation.

Official contracts:
- https://docs.venice.ai/guides/media/image-generation
- https://docs.venice.ai/guides/media/image-editing
- https://docs.venice.ai/api-reference/endpoint/image/generate
- https://docs.venice.ai/api-reference/endpoint/image/edit
- https://docs.venice.ai/overview/pricing

Validation: domain boundary tests plus required CI unit tests/lint/assemble/signature verification. Authenticated generation/editing, actual fees, output quality and Android UI/device interaction require account/device testing. No user key was available during implementation.
