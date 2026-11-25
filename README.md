# PhotoEdit

原生 Android 图片处理与短视频素材工具，使用 Kotlin、Jetpack Compose 和 Android Bitmap/Canvas。图片拆分、变装视频处理在本机执行；AI 功能需要联网和相应平台凭据。

## 当前功能

- 图片拆分：等尺寸宫格预览、PNG 相册导出；大图按安全处理尺寸采样。
- AI 图片：APIPod，支持文字生图和单张参考图编辑，结果手动保存到相册。
- 提示词助手：生成提示词、普通聊天、自定义扩写规则、复制和带入生图页。
- 变装视频测试版：多图排序、运镜、原创合成节奏、MP4 预览与手动保存；编解码兼容性和真机体验仍需验证。

## 当前模型

| 功能 | 平台 | 预设模型 ID |
| --- | --- | --- |
| 聊天 | Cloudflare Workers AI | @cf/zai-org/glm-4.7-flash |
| 聊天 | APIPod | grok-4.5、grok-4.6、deepseek-flash |
| 生图 | APIPod | z-image-spicy、z-image-spicy-pro；参考图使用 qwen-image-edit-spicy |

聊天保留自定义模型 ID；是否可调用以平台与账号权限为准。APIPod 聊天和生图共用其加密保存的 Key；Cloudflare 聊天保留原有加密凭据。不要将 Key、Token 或个人账户数据提交仓库。

## 模型服务选择规则

后续付费模型服务须先核实 API 充值支持支付宝或微信支付中的至少一种；网页会员支付不等于 API 充值支持。支付方式未确认时不作为可直接使用的付费方案推荐。免费且无需充值的方案可另行评估。

## 获取 APK

APK 不保存在源码仓库中。明确请求构建后，进入 [GitHub Actions](https://github.com/a772485252/PhotoEdit/actions)，打开成功的 Android APK 运行，在 Artifacts 下载 PhotoEdit-v<版本>-apk，解压得到 PhotoEdit-v<版本>.apk。下载需要 GitHub 访问权限，构建产物保留 30 天。

构建仅通过手动触发或专用 build-trigger.txt 触发；普通源码提交不构建 APK。Quick 模式仅编译打包，Full 模式执行单元测试、Lint 和打包，两者均校验固定签名后上传。源码同步不会下载 APK。

Android 8.0+。当前为 debug 测试包；构建通过不代表真实 API 或真机行为已验证。开发与交接信息见 [PROJECT.md](PROJECT.md)、[PROGRESS.md](PROGRESS.md)、[AGENTS.md](AGENTS.md)。
