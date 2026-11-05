# AI Horde use and behavior

AI 图片 → top provider/settings → AI Horde. Blank Key uses anonymous free mode. A personal Key can be entered/saved separately and improves account control; Kudos are queue credits, not fixed daily image allotments. Explicit refresh lists currently active models and node counts, not guaranteed img2img capability. Default stable_diffusion uses 512×512 at 25 steps; allow_downgrade lets Horde reduce cost for low-Kudos accounts.

Adding a reference uses img2img, not instruction editing; 0.55 denoising does not guarantee identity or precise outfit changes. Reference images are max512 WebP sent to volunteer trusted workers; trusted does not guarantee privacy. NSFW permission defaults on per the user's preference, and remains subject to platform/node rules. This flag routes jobs; it does not bypass safety checks. Registered requests use shared=false. Anonymous txt2img images/prompts are shared under Horde terms.

States: editing → submitting → queued/generating → result or recoverable failure. After obtaining an ID, polling errors/stop-wait retain that ID and the submitted input snapshot in memory. Continue-query never re-POSTs. Stop-wait pauses local polling; cancel-task calls DELETE. An already completed result may not be recovered by cancellation. Switching/new session/editing remain blocked until a pending job completes, expires or is canceled. Process exit loses pending handles, matching session-only history. Submission without an ID is reported as unknown; do not repeatedly submit.

No automatic generation, paid fallback or credential reuse. Only poll check every3s, fetch image once done, bound responses, verify UUID IDs and HTTPS R2 result hosts, reject censored results. Pending results can be retrieved again after download errors.

Device validation: anonymous text generation; personal Key save/clear/relaunch; live model refresh; optional reference request; pose prompt; queue/stop/resume/cancel; network interruption and task expiry; save to gallery; platform persistence; Cloudflare/Runware regression; keyboard/short-window layout.

Official contract: https://aihorde.net/api/swagger.json
Service behavior: https://www.aihorde.net/docs/image/learn/concepts_horde/
Terms: https://aihorde.net/terms/
