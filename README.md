# PhotoEdit

免费原生 Android 图片工具，Kotlin + Jetpack Compose，核心处理离线运行。

## V0.2
选择图片 → 查看预览 → 选择 2×2 / 3×3 / 4×4 → 拆分并预览 → 全部保存到手机相册。

- PNG 无损输出，按从左到右、从上到下编号。
- 保持图片比例，居中裁掉不能整除的少量边缘像素，生成相同尺寸的图片。
- 大图会缩小到安全处理尺寸，界面显示实际分辨率；不是原分辨率导出保证。
- Android 8.0+。Android 8/9 保存时需允许存储权限，Android 10+ 无需存储权限。
- 本地图片无需联网，无付费 API、无图片上传。

## 获取 APK
成功构建后，最新安装包自动更新到 [latest-apk/PhotoEdit-debug.apk](latest-apk/PhotoEdit-debug.apk)。

[直接下载](https://github.com/a772485252/PhotoEdit/raw/refs/heads/main/latest-apk/PhotoEdit-debug.apk) · [GitHub Actions 构建记录](https://github.com/a772485252/PhotoEdit/actions)

Windows 本地仓库运行 `git pull` 后也可在上述路径获取 APK。也可进入成功的 Actions 运行下载 PhotoEdit-debug-apk ZIP 并解压。

当前为 debug 测试包；真机行为仍需验证。后续计划：批量处理、9:16 构图、静态图运镜视频。

开发与交接信息：[PROJECT.md](PROJECT.md)、[PROGRESS.md](PROGRESS.md)、[AGENTS.md](AGENTS.md)。

## V0.3 Cloudflare AI
打开“AI 图片”，Account ID 已预填；填写自己创建的 Workers AI Token，点击“保存设置”加密保存到本机。

- 填提示词后点“文生图”；或添加参考图，再点“参考图生成”。
- 只有点击生成才向 Cloudflare 上传提示词和参考图；结果可预览、保存到相册。
- 首版使用 FLUX.2 Klein 4B，输出 1024×1024；参考图自动缩至最长边 511px，人物一致性需实测。
- 请保持 Cloudflare Free 套餐：每日额度耗尽会停止，App 不自动重试或切换付费接口。付费账户是否扣费由平台决定。
- 不要把 Token 发到聊天、截图或 GitHub；“清除 Token”可移除本机保存的凭据。卸载应用会丢失本机设置。
- AI 功能需要联网；宫格功能继续离线运行。暂未提供其他平台自动切换。
- 新版编译和真实 API 测试状态以 PROGRESS.md 为准。

## V0.5 可选平台与模型的提示词助手
保留 Groq，新增 OpenRouter；聊天和 Cloudflare 生图额度独立，无需填写聊天 Account ID。

1. 打开“提示词助手”，选择 OpenRouter 或 Groq。首次默认 OpenRouter，之后记住上次选择。
2. 填写该平台自己的 API Key，点“保存 Key”。两个平台分别加密保存，原 Groq Key 保持兼容。
3. 点“选择模型”，也可填写完整模型 ID 后点“使用此模型并保存”。切换平台会恢复该平台上次的模型。
4. OpenRouter 可点“刷新列表”，从平台获取当前文字模型，支持搜索。初始预置模型不保证实时可用。
5. 默认只显示并允许 OpenRouter 的 :free 模型。若要使用其他模型，需手动开启“显示并允许付费模型”；该开关重启或切换平台后关闭。费用由平台决定。
6. 选择“生成提示词”或“普通聊天”；回复可复制或“带入生图页”。带入只填文字，只有在生图页点生成才请求 Cloudflare。

仅在点击发送时上传文字与最近对话，不上传参考图；刷新列表只获取目录。无自动重试、跨平台切换或付费模型回退。平台、模型或模式切换会清空当前对话，保留输入草稿；进程退出不保留对话。

OpenRouter 免费模型受每日请求和速率限制。Groq 额度与是否计费按账号套餐执行。Key 只在本机填写，不要发到聊天或提交仓库。“清除 Key”仅清除当前平台，Cloudflare Token 和另一聊天平台不受影响。免费模型的可用性、内容处理和隐私设置以平台为准。

版本编译和实机验证状态见 PROGRESS.md。
