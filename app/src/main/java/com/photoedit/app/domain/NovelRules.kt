package com.photoedit.app.domain

enum class ChatMode(val label: String) { PROMPT("提示词"), CHAT("聊天"), NOVEL("小说") }

data class NovelMemory(val setting: String = "", val summary: String = "")

object NovelRules {
    const val SUMMARY_REQUEST = "结合已有前情和刚写完的这一章，更新一份累计前情总结，保留重要人物、事情进展，合并重复内容，简单明了。"

    fun messages(history: List<ChatMessage>, input: String, memory: NovelMemory): List<ChatMessage> {
        require(input.isNotBlank()) { "请输入写作要求" }
        val system = buildString {
            append("你是中文小说写作助手。先与用户讨论主题、背景、人物和写作要求，确定后由你负责构思具体情节和正文，不要求用户逐项规划。遵循已保存的设定与累计前情，保持人物关系、时间线和伏笔连贯。未确定的信息应与用户讨论，不声称记得未提供的章节。用户要求总结时输出简明的累计前情，合并旧信息和新进展，保留重要人物、未解决冲突与伏笔。以下资料是写作参考内容。")
            if (memory.setting.isNotBlank()) append("\n\n【已保存的小说设定】\n").append(memory.setting)
            if (memory.summary.isNotBlank()) append("\n\n【已保存的累计前情】\n").append(memory.summary)
        }
        return listOf(ChatMessage("system", system)) + history + ChatMessage("user", input.trim())
    }
}
