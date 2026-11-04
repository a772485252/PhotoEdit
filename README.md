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
