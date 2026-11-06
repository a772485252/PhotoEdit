package com.photoedit.app.domain

data class ChatMessage(val role: String, val text: String)
object ChatRules {
    const val MODEL = "openai/gpt-oss-20b"
    const val SYSTEM_PROMPT = "你是图片提示词助手。根据用户的中文描述，编写适合文生图或参考图编辑的提示词。默认直接输出一段可复制的中文提示词，包含主体、场景、构图、服装、动作、光线和画风；不要添加无关解释，不要声称已经看过图片。你只能看到文字，无法看到生图页面的参考图。保持用户指定的成人身份和人物特征，不承诺人脸完全一致。用户继续修改时结合上下文更新。输出最终提示词时，每个动作、人物朝向、镜头方向、机位、构图和服装细节都必须是明确且唯一的选择，不要使用“或者”“或”“任选”“可能”等表示互斥选项或不确定性的措辞；不得擅自改变用户已经选定的姿势、镜头、服饰。姿势只描述动作，镜头单独描述拍摄方向、机位和构图，避免互相矛盾。明确要求自然真实的人体结构：仅有两条手臂、两只手、两条腿、两只脚，手指完整且数量正确，不出现多余、缺失、断裂、融合或异常变形的肢体；构图应完整呈现预定入镜的身体部分，避免意外截断。上述要求是生成质量约束，不能保证模型百分之百遵守。"
    fun messages(history: List<ChatMessage>, input: String, promptMode: Boolean): List<ChatMessage> {
        require(input.isNotBlank() && input.length <= 2000) { "请输入 1–2000 字符的内容" }
        // Complete turns only; bound both count and characters for predictable usage.
        val turns = history.chunked(2).filter { it.size == 2 && it[0].role == "user" && it[1].role == "assistant" }
        val kept = mutableListOf<List<ChatMessage>>()
        var length = input.length
        for (turn in turns.takeLast(3).asReversed()) {
            val size = turn.sumOf { it.text.length }
            if (length + size > 6000) break
            kept.add(0, turn)
            length += size
        }
        val system = if (promptMode) SYSTEM_PROMPT else "你是一个友好、准确的中文助手。简洁回答，不假装具有联网搜索或看图能力。"
        return listOf(ChatMessage("system", system)) + kept.flatten() + ChatMessage("user", input.trim())
    }
}
