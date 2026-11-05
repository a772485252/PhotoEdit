# APIPod image provider

AI 图片 → top settings → APIPod (paid). Enter/save a separately created APIPod Key on the phone. Default free Horde remains available; no automatic switch to APIPod. No provider credentials reused.

Text generation uses z-image-spicy; optional Pro uses z-image-spicy-pro. A selected photo or continue-editing result uses qwen-image-edit-spicy, with its separate indicative price shown before Generate. APIPod controls native output size, unlike Horde fixed512. One PNG reference is fit to max1024, encoded as a data URI and persisted by APIPod as an asset. Prompt rewriting disabled on generation; no extra prompt-assistant API request. Max2000 characters validated before submission without truncation.

Official page prices reviewed2026-11-05: $0.015 / $0.025 generation, $0.04 editing. Actual completed usage.cost shown when available. No fabricated remaining balance. APIPod model pages state these routes are unavailable in mainland China, Hong Kong and Macau. A VPN is not a guarantee of eligibility/access; no app proxy, location spoofing or region-bypass logic is added. Platform/model policies apply.

Operation: prepare exact body + Key + UUID idempotency key before submit; POST /v1/images/generations; retain task_id; GET /v1/images/status/{id} with same account Key. Unknown POST outcome retains original operation; Continue replays same idempotent body/key. Known4xx create rejection allows correction. Successful-task download failure retains ID for retrieval without regeneration. Terminal failed/cancelled clears pending state. No automatic POST retry. Polling uses bounded interval/jitter and stops on errors, which can be manually resumed.

Stop waiting affects only local polling. End local tracking requires confirmation and does not cancel/refund server task. No cancellation endpoint is documented, so none is claimed. Process exit discards operation handles/history; check platform records before another submission. No request bodies, photos or credentials logged. Result downloads never receive API auth headers.

Validation needed on device/account: Key save/clear/restart; each generation mode; reference/continue editing effective source; settled cost; network interruption before/after ID; stop/resume; end-tracking dialog; region/auth/balance errors; signed update and all provider regressions.

Sources:
- https://docs.apipod.ai/endpoint-conventions
- https://docs.apipod.ai/asynchronous-tasks
- https://docs.apipod.ai/query-image-task
- https://docs.apipod.ai/openai-image-sync (data-URI asset persistence)
- https://docs.apipod.ai/error-codes
- https://www.apipod.ai/models/qwen-image-spicy/qwen-image-edit-spicy
- https://www.apipod.ai/models/qwen-image-spicy/z-image-spicy
