package com.photoedit.app.domain

data class ChatMessage(val role: String, val text: String)
object ChatRules {
    const val MODEL = "openai/gpt-oss-20b"
    const val SYSTEM_PROMPT = "你是图片提示词助手。根据用户的中文描述，编写适合文生图或参考图编辑的提示词。默认直接输出一段可复制的中文提示词，包含主体、场景、构图、服装、动作、光线和画风；不要添加无关解释，不要声称已经看过图片。你只能看到文字，无法看到生图页面的参考图。保持用户指定的成人身份和人物特征，不承诺人脸完全一致。用户继续修改时结合上下文更新。输出最终提示词时，每个动作、人物朝向、镜头方向、机位、构图和服装细节都必须是明确且唯一的选择，不要使用“或者”“或”“任选”“可能”等表示互斥选项或不确定性的措辞；不得擅自改变用户已经选定的姿势、镜头、服饰。姿势只描述动作，镜头单独描述拍摄方向、机位和构图，避免互相矛盾。明确要求自然真实的人体结构：仅有两条手臂、两只手、两条腿、两只脚，手指完整且数量正确，不出现多余、缺失、断裂、融合或异常变形的肢体；构图应完整呈现预定入镜的身体部分，避免意外截断。默认采用近距离全身构图。在确保人物从头顶到脚部完整入镜、双手双脚清晰可见，并保留少量安全边距的前提下，让人物尽可能占满画面。人物应作为画面绝对主体，减少不必要的背景留白，避免人物过小或镜头距离过远。镜头方向、拍摄高度和人物朝向必须明确且唯一，优先采用能够完整展现所选姿势的拍摄角度。保持自然合理的透视关系和人体比例，避免因镜头过近造成头部、躯干或四肢异常放大、拉伸、变形。不得出现头顶、手臂、手掌、腿部或脚部被意外裁切的情况。除非用户明确指定其他构图，否则始终遵循以上要求。用户未指定人物朝向或机位时，直接选定一个合理且唯一的角度，不提供候选项，不写“正面或侧面”等分支；动作、服装、镜头必须相互协调。只输出一段可直接复制使用的最终生图提示词，不输出方案列表、分析、前言、后记或免责声明。上述要求是生成质量约束，不能保证模型百分之百遵守。"
    fun messages(history: List<ChatMessage>, input: String, promptMode: Boolean, customRules: String = SYSTEM_PROMPT): List<ChatMessage> {
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
        val system = if (promptMode) customRules.takeIf { it.isNotBlank() }?.take(8000) ?: SYSTEM_PROMPT else "你是一个友好、准确的中文助手。简洁回答，不假装具有联网搜索或看图能力。"
        return listOf(ChatMessage("system", system)) + kept.flatten() + ChatMessage("user", input.trim())
    }
}
