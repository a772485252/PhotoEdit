package com.photoedit.app.domain

data class CameraPreset(val id: String, val title: String, val description: String)

object CameraCatalog {
    val presets = listOf(
        CameraPreset("CAM01", "正面仰拍", "镜头位于人物正前方，低机位轻微向上拍摄，人物正面朝向镜头；采用自然透视的全身构图，保持头部、躯干和四肢比例协调。"),
        CameraPreset("CAM02", "背面仰拍", "镜头位于人物正后方，低机位轻微向上拍摄，人物背面朝向镜头；采用自然透视的全身构图，清楚呈现整体姿态与服装背面结构。")
    )
}

object CameraPrompts {
    private val block = Regex("(?s)【镜头】.*?【/镜头】")
    fun apply(existing: String, camera: CameraPreset): String {
        val replacement = "【镜头】${camera.description}避免夸张广角、极端透视和不自然裁切。【/镜头】"
        return if (block.containsMatchIn(existing)) block.replace(existing) { replacement }
        else listOf(existing.trimEnd(), replacement).filter { it.isNotBlank() }.joinToString("\n\n")
    }
}
